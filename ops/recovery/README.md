# Continuidade e recuperação — Trampolim

## Escopo e diagnóstico

O repositório já tinha pg_dump/pg_restore em Test-BackendSecurity.ps1, limitado a schemas sintéticos e contagem de tabelas/migrações. Não havia rotina recorrente versionada de cópia completa, custódia criptográfica, retenção ou plano de recuperação. Não foi auditado um provedor de backup externo nem presumida sua existência.

Componentes críticos:

| Componente | O que preservar | Recuperação |
| --- | --- | --- |
| Oportunidades PostgreSQL | contas, currículos, diagnóstico, planos, candidaturas, notificações, consentimentos | dump independente + migrações correspondentes |
| Empresas PostgreSQL | organizações, vagas, pagamentos/referências, processos, verificação e auditoria | dump independente + reconciliação com provedores |
| Capacita PostgreSQL | alunos, cursos, progresso, avaliações, competências, permissões | dump independente + arquivos privados correspondentes |
| Armazenamento privado | vídeos, PDFs/textos auxiliares, mural e logotipos | snapshot/arquivo do armazenamento inteiro |
| Configurações e segredos | variáveis efetivas, TLS, identidade das chaves JWT, ponte, monitoramento, SMTP/pagamento, bootstrap de papéis | cofre separado + cópia criptografada restrita |
| Código/imagens | versão exata de frontend/backend, Compose, proxy e migrations | Git + registro de imagens com digest; Git sozinho não guarda dados |
| Monitoramento | regras, painel, volumes quando necessários | configuração no Git; histórico tem menor prioridade |

O caminho CAPACITA_MEDIA_ROOT inclui subpastas library/discussion. HUB_LOGO_DIRECTORY pode estar fora dele. No compose atual /app/storage cobre os dois apenas se todos os caminhos efetivos estiverem corretamente montados ali. Perfis/arquivos adicionais precisam constar no inventário de implantação. Não afirmar backup completo se houver arquivos em camadas graváveis dos contêineres ou pastas não incluídas.

## RPO e RTO propostos

RPO é a perda máxima de dados aceitável; RTO é o tempo até restabelecer o serviço validado.

| Item | RPO proposto | RTO proposto |
| --- | --- | --- |
| Três bancos + mídias relacionadas | até 1 hora | até 4 horas para o conjunto |
| Configuração/segredos | após cada mudança e cópia diária | até 4 horas |
| Histórico do monitoramento | até 24 horas | até 8 horas |

São propostas para aprovação operacional, não garantias ou resultados de carga. O agendamento sugerido é de 30 minutos, com duração de backup e cópia externa somadas inferiores a 30 minutos. Se isso não for atendido, o RPO de uma hora não está sustentado. Disparar investigação se não houver conjunto completo e cópia externa confirmada há 60 minutos; tolerância de alerta deve refletir volume/horários reais. Tempo de restauração desta fixture pequena não representa um banco de produção.

Cada pg_dump tem snapshot transacional próprio. Não há transação distribuída entre os bancos nem snapshot atômico com arquivos. O manifesto registra consistentAcrossDatabases=false e mediaQuiesced=false. Para ponto de recuperação coordenado, usar janela de manutenção com gravações, uploads e limpeza de arquivos suspensos, ou solução de snapshot coordenado homologada. Somente parar o frontend não interrompe workers.

## Implementação

recovery.mjs usa Node.js >=22 e bibliotecas nativas, Docker local e pg_dump/pg_restore da imagem PostgreSQL 15 existente. Para arquivos locais, precisa de tar (Windows inclui tar.exe em instalações compatíveis). Não muda a stack da aplicação nem instala Redis, fila, agente ou serviço pago.

- Backup: três dumps custom completos e arquivos tar em fluxo, criptografados antes de gravar no destino. Não há dump/PDF/vídeo aberto em disco durante a cópia.
- Criptografia: chave AES-256-GCM aleatória por artefato, IV de 12 bytes e tag de 16 bytes; chave de dados protegida por RSA-OAEP/SHA-256 (RSA >=3072). Cabeçalho/identidade do conjunto incluídos como dados autenticados. Streaming evita carregar vídeos inteiros na memória.
- Manifesto SHA-256 e tamanhos; conclusão só depois de todos os artefatos e finalização dos processos. Erro deixa diretório .incomplete sem manifesto de sucesso; não substitui cópia anterior.
- Bloqueio exclusivo evita concorrência. Encerramento forçado pode deixar .backup.lock; investigar PID e tarefa antes de removê-lo manualmente. Não há remoção automática de lock supostamente antigo.
- Pastas privadas: ACL Windows para operador e SYSTEM; Unix 0700 e arquivos 0600. Administradores do host/Docker continuam com privilégios amplos. Revisar permissões da conta de serviço e do destino.
- verify confere inventário/checksums, não descriptografa nem prova recuperação. drill autentica TODOS os artefatos antes de executar restauração e confere constraints/catalogação. Nenhum comando de restore aceita servidor ou banco de produção.
- O ensaio cria contêiner descartável, sem rede e sem portas, dados em tmpfs, sem socket Docker montado. Dumps são SQL potencialmente executável: executar somente de origem confiável; nunca restaurar arquivos recebidos de terceiros em um servidor compartilhado.
- A chave privada só é necessária no ensaio/recuperação; tarefa de backup recebe somente a pública. Memória e diretório de estágio privados são limpos ao final; exclusão de arquivo não é garantia de apagamento físico em SSD. Usar BitLocker/LUKS no host de recuperação e tratar paginação/hibernação conforme a política.

O formato encapsula primitivas padrão, mas não teve auditoria criptográfica independente. AES-GCM detecta corrupção/adulteração do conteúdo com a chave, não identifica o autor do backup. Manifesto não é assinado; substituição integral/replay exige mitigação por catálogo confiável, armazenamento imutável e controle de acesso. Não tratar checksum como prova de autenticidade de origem. Reavaliar solução gerenciada/PITR quando houver necessidade operacional.

## Configuração e automação

1. Escolher disco/volume privado FORA do repositório, espaço livre, responsável e destino externo. Não usar a mesma unidade física como única proteção.
2. Criar uma pasta nova de chaves, cujo diretório pai já exista:

~~~powershell
node ops/recovery/new-key.mjs 'D:\TrampolimRecoveryKeys'
~~~

O gerador não sobrescreve diretório ou chave existente. Nunca imprimir chaves ou incluí-las no Git. Mover a privada para custódia offline/cofre separado, com segunda cópia controlada e teste de acesso por outro responsável. A pública pode permanecer no host de backup. Se a privada se perder, os backups não poderão ser recuperados. Manter chaves antigas enquanto existirem cópias que dependam delas; testar rotação.

3. Copiar backup.config.example.json para arquivo privado fora do Git. Ajustar nomes reais dos contêineres, bancos, usuários e TODOS os diretórios. Nomes api1 e contas locais são exemplos; Compose pode gerar nomes prefixados. Consultar docker compose ps. Se as APIs estiverem paradas, arquivo de mídia do contêiner não estará acessível por docker exec; planejar montagem de volume read-only em ferramenta de backup ou reinício controlado em ambiente de recuperação. O script atual não faz isso automaticamente.
4. recovery-config deve ser uma pasta privada preparada pelo operador: configuração efetiva de implantação, versões/digests, bootstrap de roles/permissões, certificados e instruções de referência ao cofre. Não guardar a chave privada de backup nessa pasta. Segredos reais só entram no arquivo criptografado e no cofre; nunca no exemplo versionado. Guardar também cópia protegida do recovery.config e do inventário fora do host original.
5. Autenticação PostgreSQL: o script usa docker exec e --no-password; requer socket local autorizado ou .pgpass protegido dentro do contêiner. Nunca passar senha na linha de comando. Produção pode exigir conta de backup com privilégios suficientes; revisar mínimo necessário (incluindo sequências e objetos grandes). Não relaxar pg_hba.conf para fazer o backup funcionar.
6. Realizar primeiro backup e ensaio manual:

~~~powershell
node ops/recovery/recovery.mjs backup 'D:\TrampolimRecoveryConfig\backup.json'
node ops/recovery/recovery.mjs verify 'D:\TrampolimBackups\backup-AAAAMMDDTHHMMSSmmmZ-xxxxxxxx'
node ops/recovery/recovery.mjs drill 'D:\TrampolimBackups\backup-AAAAMMDDTHHMMSSmmmZ-xxxxxxxx' 'E:\Custodia\backup-private.pem'
~~~

Nomes de conjuntos acima são placeholders: usar o identificador real retornado. verify/drill não alteram fontes. drill guarda texto aberto temporariamente em subpasta privada ao lado do conjunto; copiar primeiro para área de ensaio privada se o armazenamento for somente leitura. A imagem padrão é PostgreSQL 15; validar extensão e versão da origem. Não usar cliente mais antigo para servidor mais novo sem homologação.

7. Simular registro e revisar agendamento:

~~~powershell
pwsh -File ops/scripts/Install-RecoveryTask.ps1 -ConfigPath 'D:\TrampolimRecoveryConfig\backup.json' -IntervalMinutes 30 -WhatIf
~~~

Depois de verificar fontes, espaço, custódia, primeiro restore e política, executar sem -WhatIf registra uma tarefa nova (não sobrescreve existente). A tarefa atual usa usuário interativo limitado e exige sessão Windows aberta/Docker disponível; não é um serviço de produção 24x7. Para produção, equipe de operação deve instalar em host persistente com conta de serviço/cofre, ou serviço agendador equivalente. O instalador não coleta senha nem altera política de execução. Não registramos nenhuma tarefa nesta entrega.

Consultar LastTaskResult/LastRunTime e conjunto completo mais recente; tarefa apenas iniciada não significa sucesso. Instâncias simultâneas são ignoradas; execução limitada a duas horas. Alertar falhas/ausência de cópia e espaço livre por ferramenta do ambiente, com destinatário e teste de entrega. Exportar resultado sanitizado para monitoramento; não enviar manifesto bruto com informações de implantação a público. Essa integração de paging/frescura não foi ativada.

## Armazenamento e retenção

Proposta 3-2-1-1: três cópias, dois meios/falhas independentes, uma externa, uma offline/imutável. Cópia externa deve receber conjunto COMPLETO e manifesto, conferir checksums e manter catálogo/registro de replicação em destino com acesso separado. Não contratamos nem configuramos nuvem/NAS externo. Sem isso, falha do host/disco pode destruir todas as cópias locais.

Retenção sugerida: sete pontos diários, quatro semanais e doze mensais; preservar sempre o conjunto completo mais recente. A rotina seleciona o mais recente disponível por janela UTC, com semana em blocos fixos de sete dias; janelas podem se sobrepor e não equivalem a promessa de quantidade diária se houver falhas de execução.

~~~powershell
node ops/recovery/recovery.mjs retention-plan 'D:\TrampolimBackups'
~~~

É somente plano de revisão: KEEP/REVIEW_EXPIRED, sem apagar ou mover nada. Diretórios inválidos, .incomplete e arquivos avulsos não entram como candidatos. Antes de excluir qualquer cópia, confirmar restore recente, replicação, obrigação de retenção, incidente/investigação e disponibilidade das chaves. Exclusões reais não foram implementadas por esta etapa não autorizar operações destrutivas. Rever retenção de dados pessoais e solicitações de exclusão conforme política da organização; restauração não deve ressuscitar contas/dados cuja exclusão precisa ser reaplicada.

## Procedimento de incidente e restauração real

1. Declarar incidente, registrar horário e impacto por trilha, responsável e autorização. Preservar evidências da falha e cópia afetada; evitar apagar/reinicializar volumes.
2. Conter: se suspeita de corrupção/invasão, suspender gravações e integrações que propaguem dano. Não substituir banco em execução.
3. Recuperar cofre/chave privada, inventário, versão do código e configuração validada. Selecionar ponto anterior à falha e posterior às mudanças necessárias, com chaves correspondentes. Verificar catálogo externo, tamanho, checksums e descriptografia autenticada.
4. Executar drill primeiro; aprovar resultados. O modo genérico valida restauração transacional, catálogo e constraints. Comparar adicionalmente contagens, somatórios, relacionamentos, migrações, arquivos e regras críticas com inventário autorizado do ponto de backup; sucesso de pg_restore não valida sozinho a aplicação.
5. Provisionar bancos e armazenamento NOVOS, isolados e sem tráfego. Recriar usuários/grants com bootstrap revisado; os dumps usam --no-owner/--no-acl e não restauram senhas ou roles globais. O ensaio usa postgres só dentro do contêiner descartável.
6. Restaurar dumps com pg_restore --single-transaction --exit-on-error --no-owner --no-acl em bancos vazios novos; não usar --clean, --create em servidor compartilhado, DROP DATABASE ou sobrescrita da origem. Para restauração real, preparar procedimento específico e obter autorização antes de executar. O comando drill não promove nem mantém seu banco descartável.
7. Restaurar mídia/configuração em diretórios novos, sem extrair arquivos arbitrários como administrador sobre a raiz do host; validar caminhos e hashes. Arquivos podem conter dados pessoais. Registrar faltas e inconsistências; não liberar aula/documento sem arquivo correspondente.
8. Subir versão compatível do aplicativo contra os destinos restaurados. Inicialmente manter SMTP/pagamentos, broadcasts, workers de entrega e tarefas de limpeza DESABILITADOS. Conferir token/conta, isolamento, permissões e migrações. Não alterar checksums do Flyway para contornar falhas.
9. Reconciliar pagamentos e entregas com provedores; eventos já enviados depois do ponto restaurado podem reaparecer. Usar idempotência e validação confiável; não marcar pagamentos por suposição nem reenviar notificações em lote.
10. Testar login de cada trilha, publicação autorizada, acesso a currículo/arquivos, progresso, avaliação, health/readiness e operações essenciais com contas de homologação. Confirmar que nenhum dado privado cruza ambientes.
11. Só após autorização, fazer cutover de configurações/proxy para novos destinos. Monitorar erro, latência, filas e reclamações. Registrar perda observada de dados e tempo real total.
12. Manter origem anterior isolada para investigação/rollback dentro da retenção autorizada; não permitir gravações concorrentes divergentes. Encerrar incidente, registrar causas e repetir backup verificado após estabilização.

## Rollback

Preferir rollback do artefato da aplicação somente se compatível com o schema já aplicado. Flyway é histórico: não apagar versões, não editar migrations existentes nem aplicar SQL reverso improvisado. Quando alteração de schema não é retrocompatível, preparar correção adiante ou restauração em ambiente paralelo, com avaliação de perda de dados desde o snapshot. Rollback de código não reverte pagamento, mensagem ou arquivo removido. Proxy/configuração pode voltar ao destino anterior apenas após verificar ausência de divergência; nunca permitir dois escritores na mesma trilha durante cutover. Documentar decisão e responsável.

## Checklist

- [ ] Responsável e substituto definidos; RPO/RTO aceitos.
- [ ] Todos os bancos, arquivos, configurações, versões e extensões inventariados.
- [ ] Destino privado externo ao repositório e espaço livre conferidos.
- [ ] Chave privada offline/cofre acessível em recuperação, com custódia dupla.
- [ ] Primeiro conjunto completo e restauração de integridade aprovados.
- [ ] Agendamento persistente/serviço e falha/frescura monitorados.
- [ ] Cópia externa/imutável e catálogo independente testados.
- [ ] Retenção revisada sem apagar último ponto válido ou cópia necessária.
- [ ] Ensaio mensal e após mudanças de schema, extensões, versão ou armazenamento.
- [ ] Teste semestral do ambiente inteiro, incluindo cofre e indisponibilidade do host.
- [ ] Reconciliar integrações e exclusões de dados antes de liberar ambiente restaurado.
- [ ] Registrar tempos, falhas, volume e limitações sem divulgar dados pessoais.

## Testes locais e limitações

~~~powershell
node --test ops/recovery/recovery.test.mjs
$env:RECOVERY_DOCKER_TEST='1'
node --test ops/recovery/recovery.test.mjs
Remove-Item Env:RECOVERY_DOCKER_TEST
~~~

O segundo comando habilita criação de contêineres novos PostgreSQL sem rede/portas e dados sintéticos, removidos pelo próprio teste após conferência de rótulo/identidade. Teste não usa credenciais nem dados da aplicação real. A opção de teste inclui prova de registros/somatório/hash, FK/checks, histórico sintético e hashes de arquivos; backup falho e adulteração são rejeitados. O agendador é exercitado em -WhatIf, sem criar tarefa.

Complemento para migrações reais (precisa dos três bancos locais de desenvolvimento existentes, usa schemas/bancos temporários):

~~~powershell
pwsh -File ops/scripts/Test-BackendSecurity.ps1 -TestRestore -EvidenceName recovery-migrations-2026-11-12 -TestPattern ObservabilityPostgresTest
~~~

Não foram executados failover/PITR, carga com volume de produção, restauração de produção, envio externo, restauração completa do aplicativo ou ensaio de perda física de host. Dump lógico não fornece PITR nem inclui configuração do servidor PostgreSQL, tablespaces/roles globais completos ou arquivos de dados físicos. Se o RPO diminuir ou volume tornar dump lento, avaliar pgBackRest/Barman/serviço gerenciado com base backups + WAL contínuo, monitoramento de arquivamento e ensaio PITR, antes de introduzir ferramentas. Backups precisam ser compatíveis com extensões e versão; ensaio atual PostgreSQL 15 não garante outra versão.

Referências oficiais: [pg_dump](https://www.postgresql.org/docs/15/app-pgdump.html), [pg_restore](https://www.postgresql.org/docs/15/app-pgrestore.html), [criptografia Node.js](https://nodejs.org/api/crypto.html). Resultados e arquivos: [relatório](../../docs/historico/RECUPERACAO_2026-11-12.md).
