# Trampolim — referência técnica

Atualizada em 13/11/2026 a partir da implementação local. Esta é a entrada principal da documentação. Os relatórios em [histórico](historico/README.md) registram decisões e testes de versões específicas; não devem ser interpretados como comprovação permanente do estado atual.

## Produto e limites

Ecossistema de empregabilidade e educação com implantação inicial planejada em Irecê-BA e região e expansão por configuração territorial. A página institucional encaminha para Oportunidades, Empresas e Capacita. Não há empresas, oportunidades, clientes ou estatísticas fictícias intencionalmente cadastradas pelas novas migrações. Fixtures de teste devem ficar isoladas.

Diagnósticos e pontuações são orientação explicável, sem garantia de contratação. Conquistas internas não equivalem a certificado oficial. Indicadores representam somente registros da plataforma, não todo o mercado de trabalho regional. A implementação não certifica conformidade jurídica integral, acessibilidade integral ou capacidade para milhares de usuários.

## Arquitetura e isolamento

Monorepo: [frontend](../frontend/) React, TypeScript, Vite e Tailwind CSS v4; [backend](../backend/) Java 21, Spring Boot, Spring Security/JWT, JPA/JDBC, PostgreSQL e Flyway. A stack concreta está nos arquivos de dependências, que prevalecem sobre versões citadas em relatórios antigos.

As três trilhas possuem contas, sessões, JWT, conexões e históricos Flyway independentes. Uma conta não cria outra automaticamente. Oportunidades utiliza o banco principal; Empresas e Capacita usam datasources/transaction managers próprios. Comunicação autorizada ocorre por APIs/DTOs, com minimização de dados e segredo interno quando necessário. Não existe acesso livre entre bancos privados.

| Trilha | Responsabilidades | Histórico Flyway |
| --- | --- | --- |
| Oportunidades | Candidatos, currículo, diagnóstico, Radar, plano, interesses, portfólio, consentimentos e outbox de eventos | [migration](../backend/src/main/resources/db/migration/) |
| Empresas | Organizações, membros, vagas, pagamentos, recrutamento, demandas futuras, Hub para associações e instituições de ensino, feirão, desafios, agregados e outbox de eventos | [migration-empresas](../backend/src/main/resources/db/migration-empresas/) |
| Capacita | Estudantes/professores, aprendizagem, mídia, documentos, avaliações, competências, Passaporte, agregados e outbox de eventos | [migration-capacita](../backend/src/main/resources/db/migration-capacita/) |

A autorização empresarial é por organização e recurso; links ocultos no frontend não substituem validação no backend. PlatformAccess e LearningAccess preservam os mecanismos administrativos existentes. Concessão de papéis não deve tornar credenciais de outra trilha válidas.

## Funcionalidades e integração

| Área | Recursos implementados | Referência detalhada |
| --- | --- | --- |
| Navegação e experiência | Sidebar/Topbar, perfis autorizados, foto privada, ações prioritárias, formulários progressivos e responsividade | [navegação](historico/NAVIGACAO_REVISAO_2026-11-06.md), [dashboards](historico/DASHBOARDS_REVISAO_2026-11-06.md), [formulários](historico/FORMULARIOS_REVISAO_2026-11-06.md) |
| Busca territorial | Município IBGE, UF/região configurável, filtros combinados, lista e mapa sob demanda com coordenadas existentes | [busca](historico/BUSCA_REGIONAL.md), [mapa](historico/MAPA_REGIONAL.md) |
| Radar e preparação | Competências declaradas/comprovadas, lacunas e pontuação explicável, interesses futuros e recomendações reais | [Radar](historico/RADAR_COMPETENCIAS_PRIORIDADE1.md), [demandas futuras](historico/BANCO_TALENTOS_FUTUROS_PRIORIDADE2.md) |
| Plano de Empregabilidade | Objetivos revisáveis, etapas, progresso e próxima ação, reutilizando diagnóstico, Radar, cursos e vagas | [backend](historico/PLANO_EMPREGABILIDADE_2026-11-07.md), [interface](historico/MEU_PLANO_INTERFACE_2026-11-07.md) |
| Portfólio profissional | Projetos e experiências selecionados, competências verificadas revalidadas, prévia, link temporário restrito ao recrutador e revogação | [privacidade, APIs e validação](historico/PORTFOLIO_PROFISSIONAL_2026-11-13.md) |
| Mentoria profissional | Mentores sujeitos a aprovação, agenda, pedidos e confirmações privadas, histórico, avaliações e denúncias moderadas | [escopo, APIs e limites](historico/MENTORIA_PROFISSIONAL_2026-11-13.md) |
| Recrutamento | Etapas, histórico, notas internas protegidas, filtros, paginação e concorrência | [recrutamento](historico/RECRUTAMENTO_EMPRESAS_2026-11-06.md) |
| Capacita | Cursos → disciplinas → módulos → aulas, PDFs, avaliações, progresso e competências | [aulas](historico/CAPACITA_VIDEOAULAS_PARTE1.md), [biblioteca](historico/CAPACITA_BIBLIOTECA_PARTE3.md), [avaliações](historico/CAPACITA_AVALIACOES_PARTE4.md) |
| Comunicação | Mural privado, respostas individuais/em lote, notificações persistidas, preferências e entrega controlada | [mural](historico/CAPACITA_MURAL_PARTE2.md), [notificações](historico/NOTIFICACOES_BACKEND_2026-11-07.md) |
| Passaporte | Aprendizagem adaptativa, evidências verificáveis, compartilhamento explícito e revogação | [Passaporte](historico/CAPACITACAO_ADAPTATIVA_PASSAPORTE_PRIORIDADE4.md) |
| Institucional | Associações e instituições de ensino verificadas, representantes, permissões, programas, eventos, campanhas e limites comerciais | [Hub](historico/TRAMPOLIM_HUB_PRIORIDADE3.md), [parcerias com instituições](historico/PARCERIAS_INSTITUICOES_ENSINO_2026-11-13.md), [fundação SaaS](historico/SAAS_ARCHITECTURE.md) |
| Feirão | Backend de eventos, agenda, empresas confirmadas, referências a vagas/cursos e inscrições independentes | [feirão](historico/FEIRAO_BACKEND_2026-11-11.md) |
| Desafios profissionais | Backend de propostas, revisão independente, rubrica, limite de participação, resultados e vínculo consentido com o Passaporte | [desafios](historico/DESAFIOS_PROFISSIONAIS_2026-11-11.md) |
| Observatório v2 | Capturas transacionais em segundo plano, filtros mensais/geográficos, indicadores definidos e supressão de grupos pequenos | [métricas/endpoints](historico/OBSERVATORIO_INDICADORES_2026-11-11.md) |
| Eventos de domínio | Outbox transacional independente por trilha, contratos versionados, ledger idempotente e retentativas locais | [contratos e diagnóstico](historico/EVENTOS_DOMINIO_2026-11-12.md) |
| Feature flags | Liberação por ambiente, perfil, empresa e município; administração e auditoria por banco da trilha | [configuração e ciclo de vida](historico/FEATURE_FLAGS_2026-11-12.md) |
| Métricas de produto | Ativação, retorno, candidaturas, recrutamento, cursos e competências agregados, com limite mínimo de coorte | [fórmulas, fontes e limites](historico/METRICAS_PRODUTO_2026-11-13.md) |

As novas interfaces do feirão, da revisão/importação de desafios profissionais e do Observatório v2 ainda dependem de etapa frontend. O endpoint antigo do Observatório permanece compatível e dinâmico; o benefício da camada preparada exige migrar seus consumidores. Não assumir que uma funcionalidade backend já possui tela.

## Verificação empresarial e prevenção de anúncios fraudulentos

O backend diferencia aprovação operacional de selo cadastral revisado, exige evidência manual independente, valida DV numérico/alfanumérico do CNPJ e mantém denúncias privadas, análise revisável de anúncios, recursos e auditoria. Publicações suspeitas podem ficar temporariamente pausadas, inclusive após pagamento, sem apagar registros. Não existe consulta automática oficial contratada nem selo concedido pela simples validade sintática. [Critérios, APIs e limitações](historico/VERIFICACAO_EMPRESARIAL_2026-11-12.md). As novas telas ainda precisam de integração.

## Regras importantes

- Divulgação de estágio: R$ 50 por anúncio, validado pelo servidor. Publicação exige pagamento confirmado pelo provedor e aprovações aplicáveis; clique no navegador não confirma pagamento. O meio de pagamento precisa de configuração externa.
- Videoaulas: duração real validada no backend entre 180 e 300 segundos. Arquivos fora do PostgreSQL; processamento depende de FFmpeg/FFprobe. Não presumir legendas/transcrição sem conteúdo fornecido.
- Avaliações: correção no backend, tentativas preservadas e critérios de aprovação. Evidência empresarial não deve ser convertida em tentativa acadêmica fictícia.
- Desafios: participação voluntária, finalidade educacional, direitos do autor preservados e proibição de uso produtivo/comercial disfarçado. Moderação humana necessária. Desafios antigos precisam de revisão; entregas legadas sem revisão identificável não recebem aprovação automática.
- Feirão: inscrição não reserva emprego ou assento; contagens representam contas, não pessoas físicas únicas. Vagas são referências aos registros originais.
- Observatório: publicações registradas, estoque ativo, relatos de entrevista e confirmações são métricas diferentes. Métricas pessoais usam meses/UF e limiares/faixas; não se infere município educacional a partir da residência ou do curso.

## Configuração e execução

Pré-requisitos: Java 21; Node.js compatível com a versão do Vite do projeto; Docker Compose; executáveis FFmpeg/FFprobe para validar/processar mídia. As configurações completas estão em [backend/.env.example](../backend/.env.example), [frontend/.env.example](../frontend/.env.example) e [application.properties](../backend/src/main/resources/application.properties). Spring Boot não carrega automaticamente qualquer arquivo .env: disponibilize as variáveis no ambiente/processo conforme o comando de inicialização utilizado.

No PowerShell, a partir da raiz do checkout:

~~~powershell
Set-Location backend
docker compose up -d
.\mvnw.cmd spring-boot:run
~~~

Em outro terminal, na raiz:

~~~powershell
Set-Location frontend
npm ci
npm run dev
~~~

A interface local usa http://localhost:5173 e a API http://localhost:8080. Os bancos locais ficam em 5432, 5433 e 5434 conforme [Compose](../backend/docker-compose.yml). Credenciais de demonstração do ambiente local não são credenciais de produção. Nunca publicar .env real, tokens, backups, currículos, mídia privada ou logs com dados de usuários.

Variáveis por família:

| Configuração | Finalidade |
| --- | --- |
| OPORTUNIDADES_DB_*, EMPRESAS_DB_*, CAPACITA_DB_* | URLs e credenciais independentes; conferir precedência com DATABASE_* nos exemplos |
| Segredos JWT por trilha | Assinatura, emissor/audiência e sessões independentes; usar nomes concretos dos exemplos atuais |
| RADAR_INTERNAL_API_URL e RADAR_BRIDGE_SECRET | APIs controladas entre trilhas; rede privada/HTTPS e segredo forte |
| CAPACITA_FFMPEG e CAPACITA_FFPROBE | Processamento confiável de mídia |
| Configurações de armazenamento Capacita | Caminhos/serviço de arquivos privados; não tornar a pasta pública |
| SMTP e Mercado Pago | Integrações externas pendentes de credenciais/homologação; não simular entrega ou pagamento |
| OBSERVATORY_CAPTURE_ENABLED e OBSERVATORY_V2_REFRESH_MS | Captura periódica, padrão de quinze minutos para v2 |

Irecê é referência inicial por configuração territorial e catálogo IBGE; novos municípios/UFs não devem exigir duplicar a aplicação. Ver [configuração geográfica](../backend/src/main/resources/geography/).

## Testes e evidências

Frontend, dentro de frontend:

~~~powershell
npm run lint
npm test
npm run build
npm run test:e2e
~~~

Playwright exige navegadores instalados e as condições descritas em sua configuração. Não interpretar build como teste de todos os fluxos de navegador.

Backend: [ops/scripts/Test-BackendSecurity.ps1](../ops/scripts/Test-BackendSecurity.ps1) prepara schemas aleatórios nos três bancos Docker locais, desativa envios reais e remove os schemas ao terminar. Execute da raiz:

~~~powershell
.\ops\scripts\Test-BackendSecurity.ps1 -EvidenceName validacao-local
~~~

Configure FFmpeg/FFprobe para incluir processamento real de mídia nos testes. O modo de carga é opcional e não deve usar dados pessoais reais. Para testes unitários convencionais e build, utilize o Maven Wrapper do backend.

Última regressão completa do backend: **12/11/2026**, 282 testes registrados, **281 aprovados**, zero falhas/erros e um ensaio opcional de carga não habilitado. As validações posteriores focadas de verificação empresarial passaram (41 testes e, depois do ajuste final de prova, 18 testes; são rodadas sobrepostas). [Evidência de verificação empresarial](evidencias/verificacao-empresarial-2026-11-12.json). Estes resultados não comprovam infraestrutura de produção nem capacidade de carga. Logs brutos e capturas locais não acompanham esta publicação; os scripts permitem reprodução. Em **12/11/2026**, lint, 23 testes unitários e build/typecheck do frontend passaram. O build mantém avisos de tamanho dos módulos de mapa/PDF; não houve novo ensaio de desempenho ou execução de Playwright nesta organização documental. [Evidência frontend](evidencias/frontend-2026-11-12.json).

## Segurança e implantação

Preservar autorização no servidor, isolamento por organização, JWT por trilha, CORS restrito, proteção de arquivos, validações de upload, limites de requisições, idempotência, tratamento seguro de erros e logs sem segredos. Os documentos de [segurança](historico/SEGURANCA_BACKEND_2026-11-06.md) e [qualidade](historico/QA_IMPLANTACAO_IRECE.md) são registros de auditorias, não certificado permanente de segurança.

Antes de produção: backups independentes e ensaio de restauração; homologação das migrações novas em cópia isolada; segredos exclusivos; HTTPS/proxy; armazenamento privado; SMTP/pagamentos em ambiente de testes; monitoramento; política de retenção e atendimento; revisão de acessibilidade e testes de carga representativos. Não ativar funcionalidades pagas, planos ou serviços externos sem decisão/configuração apropriadas.

## Pendências conhecidas

1. Interfaces dos módulos exclusivamente backend descritos acima e migração do dashboard para o Observatório v2.
2. Credenciais/homologação de SMTP e pagamentos; infraestrutura operacional de mídia, backups e monitoramento.
3. Revisão de desafios e entregas legadas, preservando consentimentos e critérios aceitos.
4. Indicadores educacionais municipais: fonte atual só possui UF autorizada; nenhuma atribuição municipal foi inventada.
5. Medições representativas de carga, disponibilidade e acessibilidade em produção. Resultados locais não autorizam afirmar suporte a milhares de pessoas.
6. Revisão contínua da documentação quando código, contratos, dependências ou configuração mudarem.

## Como manter a documentação

Atualizar este documento para o estado vigente. Relatórios datados pertencem a [historico/](historico/README.md), preservando contexto, data, testes e limitações. Manter evidências pequenas e sanitizadas em evidencias/; guardar logs brutos apenas localmente. Evitar novos diagnósticos soltos na raiz. Links relativos devem continuar funcionando ao mover um arquivo.

## Observabilidade operacional (12/11/2026)

Logs JSON ECS com identificação por requisição, correlação autenticada de POSTs internos, readiness das três trilhas, métricas HTTP/Hikari/JVM e amostragem das filas de comunicação. Configuração opcional de Prometheus, Alertmanager e Grafana, indicadores propostos e procedimentos: [manual operacional](../ops/monitoring/README.md). O receptor padrão não envia mensagens externas. Sem implantação em produção nem tracing OTLP configurado. Consulte o [relatório](historico/OBSERVABILIDADE_2026-11-12.md) para evidências e limites. Não foram criadas migrações nesta etapa.

## Continuidade e recuperação (12/11/2026)

Automação preparada para cópia criptografada dos três bancos e diretórios privados/configurações, verificação de integridade, ensaio isolado e plano de retenção sem exclusão. Propostas de RPO até uma hora/RTO até quatro horas dependem de aceite e teste em volume real. Agendamento persistente, destino externo, custódia e alerta de frescura ainda exigem configuração; não estão ativos. Sem restauração ou alteração de produção. [Plano operacional](../ops/recovery/README.md), [relatório e resultados](historico/RECUPERACAO_2026-11-12.md).

## Ciclo de vida dos dados pessoais (12/11/2026)

### Mapa técnico de categorias, finalidades e acesso

Este inventário descreve os módulos examinados no código local; não substitui registro jurídico de operações nem revisão integral de todos os endpoints. As bases legais e os prazos necessários devem ser definidos pelo controlador com avaliação especializada. Códigos de motivo no sistema não estabelecem, por si, uma base legal válida.

| Categoria | Finalidade técnica | Local e fluxos | Acesso/minimização e limites |
| --- | --- | --- | --- |
| Identificação e autenticação | cadastro, sessão e recuperação | app_users / company_users / student_users nos respectivos bancos; hashes de senha, versões de token e hashes de tokens temporários | JWT validado por trilha; exportações não retornam hashes de senha, tokens ou segredos; não há sincronização automática de contas |
| Contato, município e preferências | perfil e busca de oportunidades | perfil/alertas de Oportunidades; contas corporativas/educacionais; geografia por códigos oficiais | localização precisa é opcional nas preferências que a utilizam; não inferir residência a partir de uso; preferir município |
| Currículo, experiência, formação, foto | apresentação profissional e candidaturas | currículo/foto em bytea no banco de Oportunidades; perfil público tem campos selecionados pelo titular | currículo via rota autenticada do próprio usuário; PDF até 10MB e assinatura inicial validada; foto até 5MB; nome saneado, download de currículo com no-store/nosniff; validação de assinatura não equivale a antivírus |
| Diagnóstico, competências declaradas e plano | orientação profissional e recomendações explicáveis | diagnóstico/Radar/plano/portfólio em Oportunidades; evidências educacionais por contrato autorizado | não usar características sensíveis para seleção; vínculos de evidências e compartilhamentos têm controles próprios; exportação não é decisão de contratação |
| Candidaturas e recrutamento | candidatura, entrevista, andamento e resultado | acompanhamento do candidato e cópia cadastral da candidatura em Empresas são registros distintos | acesso por empresa/recurso; excluir conta de candidato não apaga automaticamente a cópia da empresa; observações internas e evidências de seleção precisam de análise própria |
| Contas empresariais, vínculos e representantes | gestão de organizações e permissões | Empresas: company_users, empresas, vínculos, convites e auditoria | exclusão de acesso corporativo exige transferência quando houver responsabilidade; dados da organização/pagamentos não são apagados indiscriminadamente |
| Pagamentos, verificação, denúncias e decisões | publicação, prevenção de fraude, investigação e rastreabilidade | Empresas; denúncias com integrações autorizadas; referências de transações | não armazenar cartão; preservar evidências necessárias com controle de acesso; validade sintática de CNPJ não comprova legitimidade |
| Perfil educacional e faixa etária | adequação da experiência e aprendizagem | Capacita: student_users, etapa, instituição opcional, faixa 14–17/18+ declarada | não coletar data completa/diagnóstico de saúde no novo fluxo; não inferir deficiência; procedimentos para adolescentes dependem também de governança e avaliação especializada |
| Aulas, avaliações e progresso | ensino e comprovação de aprendizagem | Capacita: cursos, módulos, progressos, tentativas, respostas, competências e passaporte | professor tem escopo de curso; aluno vê dados próprios; histórico não é certificado oficial; exportação docente adicionada é paginada e não traz listas de alunos |
| Mural, áudio/vídeo, transcrições e materiais | comunicação educacional e apoio didático | metadados em Capacita e arquivos em CAPACITA_MEDIA_ROOT; PDFs/library e mídias/discussion | acesso autorizado/tickets temporários; não publicar dúvida individual; descrição/texto são alternativas; conteúdo enviado pode conter dados de terceiros e exige cuidado operacional |
| Acessibilidade e apoio | preferências voluntárias e atendimento | preferências e solicitações em Capacita, inclusive texto livre de apoio | acesso do titular e equipes autorizadas; preferências não constituem diagnóstico; evitar solicitar documentos clínicos; mensagens existentes podem conter dados sensíveis e precisam de revisão de necessidade |
| Professores e autoria | permissões e organização didática | papéis docentes, cursos e materiais no Capacita | não conceder administração global a professor; transferência de cursos antes de excluir responsável; autoria/contribuições podem precisar de conservação e avaliação |
| Consentimentos e compartilhamentos | participação, evidências e verificação autorizada | contratos Radar/Passaporte/Hub e recibos locais, sem SQL livre entre bancos | acesso e revogação próprios por recurso; retirada de consentimento não apaga automaticamente registro obrigatório ou cópia já recebida por terceiro |
| Indicadores | análise agregada de uso da plataforma | snapshots do Observatório por trilha | supressão de grupos pequenos conforme regras existentes; dados representam a plataforma, não estatísticas oficiais; agregação não dispensa avaliar reidentificação |
| Logs, auditorias e métricas | investigação, segurança e operação | logs JSON, auditorias de conta/papéis/recrutamento; métricas privadas | logs novos não contêm corpo, documento ou e-mail; caminhos/credenciais não devem virar tags; bibliotecas e configurações do ambiente exigem revisão contínua |
| Backups e configurações | continuidade operacional | conjuntos criptografados e configuração de recuperação preparada | recuperação deve reconciliar exclusões/revogações posteriores ao snapshot; chave privada separada; rotinas/offsite ainda exigem ativação/configuração; restauração não autoriza reter indefinidamente |
| Solicitações de privacidade | protocolo, análise, resposta operacional e rastreabilidade | privacy_requests, privacy_audit, privacy_export_audit próprios em cada banco | sem relato livre, senha ou anexo; referências técnicas e estados; auditoria de exportação registra pedido, não prova que o navegador baixou o arquivo |

### Controles implementados e fluxo de solicitação

Nas áreas de conta das três trilhas há um componente reutilizável para enviar e consultar pedidos. Criação requer autenticação da própria trilha e confirmação explícita, tem chave idempotente e um único protocolo ativo por conta; novas solicitações são limitadas a dez por dia. A consulta mostra os 50 registros mais recentes; API aceita páginas adicionais. Não armazena senha, documento ou diagnóstico na solicitação.

Estados: OPEN (recebida), REVIEWING (em análise), READY (análise concluída para prosseguir), RETAINED (retenção sujeita à revisão), COMPLETED (exclusão da conta registrada) e CANCELLED. COMPLETED não significa que todos os registros em sistemas externos ou backups foram eliminados.

A administração global já autorizada no backend pode analisar protocolos. Gestores empresariais, associações, professores e administradores educacionais não ganham esse acesso automaticamente. Não há nova tela administrativa de análise; operações estão disponíveis por API e precisam de integração no atendimento operacional. Revisão registra referência interna sem texto livre, motivo padronizado, estado anterior/novo, identificador do administrador e data. Não copiar nomes, e-mails, dados de saúde ou documentos na referência.

RETAINED exige motivo diferente de USER_REQUEST e data futura de reavaliação em até 90 dias: é um limite técnico de reavaliação, não prazo de retenção previsto em lei. Vencimento sinaliza necessidade de revisão; não libera automaticamente a exclusão. REVIEWING/RETAINED impedem exclusão física da conta por trigger PostgreSQL, inclusive nos endpoints antigos. Titular não pode cancelar uma restrição ativa para contornar a análise. A liberação exige nova revisão administrativa; controles antigos de transferência de empresa/curso continuam valendo.

O trigger protege a exclusão da LINHA DA CONTA. Não é um bloqueio jurídico abrangente de todo tratamento: rotinas de retenção de mensagens, notas e arquivos continuam com suas regras. Se houver preservação de evidências, definir escopo e suspender os descartes pertinentes por procedimento autorizado, além de preservar cópia restrita. Não presumir que marcar RETAINED preserve automaticamente todos os arquivos, cópias ou registros de terceiros.

Nenhuma solicitação ou decisão READY dispara exclusão automática. O titular ainda precisa confirmar senha no fluxo de exclusão existente. O banco registra conclusão apenas se a exclusão efetivamente ocorrer e a transação confirmar; user_id dos protocolos/auditorias de exportação torna-se NULL e referências holder são removidas. O protocolo e referências administrativas restantes são dados minimizados/pseudonimizados; não afirmar anonimização irreversível.

Exports existentes foram reutilizados. Dados docentes adicionais ficam em /api/capacita/account/export/teaching, seções courses/materials, 100 itens por página (hasMore indica continuação). Exportação só retorna autoria da própria conta; não traz alunos, credenciais ou chaves de armazenamento. Exportação JSON de conta não é cópia integral de cada arquivo/módulo: arquivos privados e direitos de terceiros devem ser tratados por atendimento autorizado, sem acesso irrestrito a outra trilha. Respostas de perfil/conta/privacidade recebem private, no-store e nosniff. Esses cabeçalhos não retiram arquivos já baixados pelo usuário.

### Retenção e descarte

| Dados/controle atual | Configuração ou regra observada | Decisão/limite operacional |
| --- | --- | --- |
| Currículo/foto/perfil | remoção individual ou exclusão de conta; cópias da empresa são independentes | não há descarte geral por inatividade nesta etapa; validar necessidade e instruir remoção individual |
| Apoio educacional fechado | CAPACITA_SUPPORT_CLOSED_RETENTION_DAYS, padrão 90 | texto pode conter dados sensíveis; prazo é configuração técnica, não obrigação legal |
| Dúvidas encerradas | CAPACITA_QUESTION_CLOSED_RETENTION_DAYS, padrão 0 | zero não ativa descarte de dúvidas; arquivos órfãos/tickets têm limpeza própria |
| PDFs marcados excluídos | CAPACITA_LIBRARY_DELETED_RETENTION_DAYS, padrão 30 | rotinas existentes removem referências/arquivos órfãos; preservação necessária exige procedimento específico |
| Notas de recrutamento | padrão 180 dias; histórico padrão 730; RECRUITMENT_RETENTION_ENABLED | notas são esvaziadas/tombstones conforme política atual; não exclui candidatura/evidência de contratação em massa |
| Comunicações | rotina existente de 90 dias em filas/eventos/auditoria de preferência | avaliar preferências e estados pendentes; marcar restrição da conta não suspende esta rotina |
| Novos protocolos e auditoria de exportação | sem eliminação automática | prévia administrativa conta registros fechados/antigos por janela de 30–3650 dias; números não autorizam descarte |
| Logs e auditoria administrativa | dependem de política do ambiente; rotação não equivale a retenção jurídica | restringir acesso, revisar prazos, evitar log bruto como evidência pública |
| Pagamentos/contabilidade/evidências de segurança | sem regra nova de exclusão geral | decisão documentada pelo controlador/assessoria; não inventar prazo fiscal ou apagar obrigações |
| Backups | proposta sete diários/quatro semanais/doze mensais, ainda não ativada | revisão antes de descarte; cópia offline não deve voltar a servir dados excluídos sem reconciliação |

Política de descarte operacional: inventariar e justificar; conferir restrições/casos, titulares e obrigações; executar a prévia; aprovar escopo e prazo por responsável; aplicar eliminação/anonimização apropriada em procedimento homologado e registrar resultado sem conteúdo pessoal; reconciliar destinatários e backups. A nova prévia não exclui nem altera registros. Nenhum job de descarte de produção foi disparado nesta entrega. Ferramenta genérica de eliminação transversal não foi criada, pois poderia apagar evidências e dados de terceiros indevidamente.

Proposta de revisão periódica: verificar mensalmente protocolos em retenção/reavaliação vencida, concessões de papéis, compartilhamentos ativos, necessidade de dados e permissões do armazenamento. Uma janela de 730 dias para metadados encerrados pode ser analisada pelo controlador, mas não foi ativada como prazo obrigatório.

### APIs e operação autorizada

Prefixos do titular (autenticação independente): /api/profile/privacy, /api/empresas/account/privacy, /api/capacita/account/privacy.

- GET /requests?page=0: apenas protocolos do titular, sem referência interna ou identidade de revisor.
- POST /requests: corpo clientKey (UUID) e confirmed=true; não exclui dados.
- POST /requests/{id}/cancel: expectedRevision; só OPEN/READY da própria conta.
- GET /api/admin/privacy/{trail}/requests?page=0: fila mínima para administrador global autenticado em Oportunidades.
- PUT /api/admin/privacy/{trail}/requests/{id}: state, basisCode, caseReference, reviewAfter e expectedRevision; não aceita COMPLETED arbitrário.
- GET /api/admin/privacy/{trail}/requests/{id}/audit?page=0: histórico administrativo restrito.
- GET /api/admin/privacy/{trail}/disposal-preview?days=730: contagens, sem exclusão; estados ativos não são candidatos.

Trilhas permitidas são oportunidades, empresas e capacita. Estados/motivos/referências são validados; conflitos de revisão retornam 409. Administrador precisa confirmar a avaliação e manter processo externo com o protocolo UUID para explicação individual, já que o banco não guarda justificativa pessoal em texto livre. Um motivo padronizado não substitui resposta fundamentada ao titular. Quem perdeu acesso deve recorrer ao canal de atendimento verificado, com validação proporcional de identidade/representação fora do sistema; não criar exportação pública por e-mail como atalho.

### Riscos remanescentes e implantação

Não houve exclusão de dados reais. Migrações novas: Oportunidades V25, Empresas V23, Capacita V26, testadas em bancos/schemas temporários e em atualização de histórico anterior. Não editar migrations já aplicadas. Antes de ativar no ambiente definitivo, fazer backup/ensaio, revisar política e aplicar pelo Flyway habitual com acompanhamento.

Continuam necessárias decisões sobre controlador/operadores, canal formal de atendimento, bases legais por finalidade, menores/representação, prazos de resposta e conservação, eventual atendimento de bloqueio/oposição/correção, contratos com destinatários e provedores de IA/SMTP/pagamento. Não designar automaticamente o responsável técnico como encarregado formal. Currículos/PDFs não têm garantia de ausência de conteúdo ativo/malware só pela assinatura. Metadados de imagens/documentos podem conter informações adicionais; não foi implementada sanitização universal. Criptografia de banco/volume em repouso depende da infraestrutura; backup criptografado não equivale a cifrar o armazenamento ativo.

Restauração de backup exige registro autorizado de exclusões/revogações posterior ao ponto restaurado, preferencialmente em custódia independente. A reconciliação não foi automatizada. Exportações completas históricas ainda podem crescer e demandar paginação/job dedicado; não afirmar exaustividade de todas as informações distribuídas. A prévia de descarte de novos metadados não examina todos os bancos/arquivos do ecossistema.

Referência oficial de direitos e limites: [ANPD — direitos dos titulares](https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados/direito-dos-titulares). Estes controles são técnicos e não constituem declaração de conformidade jurídica integral.

Resultados, arquivos e limites da validação: [relatório de privacidade](historico/PRIVACIDADE_2026-11-12.md).

## Central de Candidaturas

Oportunidades agora distingue o registro pessoal de acompanhamento das etapas registradas por recrutadores. Os controles existentes em `/api/profile/applications` continuam preservando os contratos de lista, anotação, lembrete e remoção; seus status seguem sendo privados e editáveis pelo titular. A nova tela `#oportunidades/applications` oferece uma consulta separada das candidaturas recebidas pelo módulo Empresas, com linha do tempo, filtros por estado/empresa/vaga/período e paginação.

Para consultar processos empresariais, o titular precisa confirmar a consulta pontual. O endpoint autenticado `POST /api/profile/applications/center` resolve a identidade pelo token da trilha Oportunidades, exige conta pessoal com e-mail confirmado e deriva o endereço da conta no servidor; o corpo não aceita e-mail/ID de candidato. O backend usa o contrato interno `POST /api/integrations/application-center/query` com `X-Trampolim-Bridge`. A Empresas consulta somente candidaturas consentidas (`consent_at` preenchido) associadas exatamente a esse endereço confirmado, devolvendo título, empresa, localidade, estado e até 25 eventos de recebimento/mudança. Paginação é limitada a 10 registros por página na interface e até 50 no contrato interno.

O contrato nunca seleciona notas internas, mensagens/telefone do candidato, autores recrutadores ou eventos `NOTE_ADDED`/`NOTE_REMOVED`. `BASELINE` significa apenas que aquele estado já existia quando o histórico passou a ser mantido; não é apresentado como transição inventada. O registro empresarial, o pessoal e as notificações de cada ambiente permanecem em seus bancos e sessões próprios. Empresas continua sendo a autoridade para mudar etapas; as rotas existentes de estágio continuam autorizadas somente no token de Empresas. A Central usa estados de carregamento, erro/retentativa e ausência de dados, filtros combinados, navegação por teclado e layout responsivo.

Limite conhecido: a fila de notificações de `APPLICATION_UPDATED` do banco Empresas é atualmente dirigida aos usuários recrutadores, e Oportunidades não mantém uma vinculação persistente entre seu usuário e uma candidatura recebida por e-mail. Portanto a Central atualiza a linha do tempo quando consultada, mas não afirma enviar alerta persistente ao candidato após a mudança de etapa. Automatizar esse alerta exigiria uma integração de eventos/idempotência entre trilhas com escopo de identidade explícito; não se reutilizou incorretamente uma notificação empresarial nem se criou vínculo apenas por e-mail.

Rotas e implementação: `POST /api/profile/applications/center` e contrato interno assinado; `ApplicationCenterPage` e item “Candidaturas” na navegação Oportunidades. O histórico reutiliza `company_applications`, `company_recruitment_events` e `job_applications`; a única migração é o índice parcial Empresas V24 para consultas pelo e-mail consentido e data. Resultados de testes e arquivos estão em [Central de Candidaturas](historico/CENTRAL_CANDIDATURAS_2026-11-12.md).

## Onboarding por perfil

Novos cadastros podem receber um tutorial curto e não modal no primeiro acesso de cada trilha. Os passos usam somente funções e rotas já disponíveis e variam conforme permissões existentes para candidato, administrador, empresa/recrutador, aluno e professor. O usuário pode avançar, voltar, adiar, pular ou reabrir “Ver tutorial inicial” no menu do perfil. O andamento é salvo localmente por trilha e e-mail normalizado; não há sincronização entre dispositivos nem persistência no backend. Login não marca uma conta existente como novo cadastro. Implementação, testes e limites estão em [Onboarding por perfil — 12/11/2026](historico/ONBOARDING_2026-11-12.md).

## Busca universal por trilha

A Topbar das áreas autenticadas oferece pesquisa por `Ctrl+K`/`Cmd+K`. Páginas e ações são filtradas a partir da navegação autorizada da sessão; conteúdos usam os catálogos e APIs já existentes de cada ambiente. Resultados empresariais são limitados às próprias publicações e removem campos internos; conteúdo do Capacita respeita papel e etapa. A interface tem navegação por teclado, Escape, estados vazios/erro/carregamento e debounce. Não foi criado endpoint novo. Evidências e limites estão em [Busca universal por trilha — 12/11/2026](historico/BUSCA_UNIVERSAL_2026-11-12.md).

## PWA e uso offline

O frontend inclui manifesto instalável, Service Worker registrado em builds de produção, página de indisponibilidade e controles de instalação, conexão e atualização. O cache contempla o shell, arquivos estáticos com hash e, quando a API do Capacita está no mesmo domínio e responde explicitamente `Cache-Control: public`, trilhas e conteúdos publicados sem filtros. O conteúdo de API tem limite de tamanho/quantidade e validade offline limitada. APIs autenticadas, respostas com autorização, vídeos, arquivos PDF, candidaturas e ações críticas não são enfileirados nem tratados como concluídos offline. Consulte [PWA e uso offline — 12/11/2026](historico/PWA_2026-11-12.md).

## Simulador de entrevistas

Oportunidades oferece prática textual com perguntas contextualizadas por área, ocupação, nível de experiência e tipo de entrevista. O feedback local permanece disponível sem provedor externo. Quando IA estiver habilitada no servidor, cada envio requer consentimento explícito; o provedor recebe apenas o contexto da entrevista e o texto da resposta após a redação de e-mails, telefones e segredos comuns. A orientação considera clareza, estrutura, pertinência e exemplos escritos; não prevê contratação e não pontua sotaque, voz, aparência ou atributos sensíveis. O ensaio de voz é opcional, limitado a dois minutos, armazenado somente na página do navegador e não enviado ao backend nem à IA.

O histórico é privado e depende de escolha afirmativa no início da simulação. Sem essa escolha, respostas transitórias são removidas após a conclusão ou por limpeza de rascunhos após 24 horas; sessões salvas podem ser excluídas individualmente. Sugestões do Capacita vêm somente do endpoint público autorizado e incluem conteúdo publicado correspondente, sem acesso direto ao banco daquela trilha. A integração de IA usa uma interface de provedor, mas somente o adaptador OpenAI está implementado; outro provedor exige integração, avaliação de privacidade/custo e configuração antes de uso.

Para habilitar IA, configure `MENTOR_AI_ENABLED=true`, `MENTOR_PROVIDER=openai`, `OPENAI_API_KEY` e um modelo suportado em `OPENAI_MODEL`. Os limites atuais são 20 chamadas por usuário/dia, 1.000 global/dia, 40.000 tokens reservados por usuário/dia, 1.000.000 global/dia, três chamadas por usuário/minuto, duas chamadas simultâneas e até 600 tokens de saída por resposta (configurável, com teto de 2.000). As cotas são compartilhadas com os demais usos do mentor de carreira. Por segurança, não há repetição automática quando o resultado externo é incerto. Para estimar custo, configure `MENTOR_INPUT_USD_PER_MILLION` e `MENTOR_OUTPUT_USD_PER_MILLION`; sem preços, o sistema registra tokens mas não calcula custo estimado.

Estimativa meramente ilustrativa: assumindo 1.000 tokens de entrada e 600 de saída em cada uma de três respostas, a tabela pública consultada para GPT‑5 mini (US$ 0,25 por milhão de tokens de entrada e US$ 2 por milhão de saída) resulta em aproximadamente **US$ 0,00435 por simulação completa**. O consumo real varia conforme prompt, resposta, modelo, câmbio, impostos e alterações de preços. A página oficial atualmente marca GPT‑5 mini como depreciado; portanto revise modelo e preços antes de ativar a integração. Alternativamente, a página do GPT‑5.4 mini lista US$ 0,75/US$ 4,50 por milhão de tokens de entrada/saída, o que daria cerca de US$ 0,01035 sob a mesma hipótese. Consulte [preços e status do GPT‑5 mini](https://developers.openai.com/api/docs/models/gpt-5-mini) e [preços do GPT‑5.4 mini](https://developers.openai.com/api/docs/models/gpt-5.4-mini). Não há medição de custo real nem credencial incluída.

Rotas: `POST /api/career/interviews`, `GET /api/career/interviews`, `GET /api/career/interviews/{id}`, `POST /api/career/interviews/{id}/answers`, `GET /api/career/interviews/{id}/training`, `DELETE /api/career/interviews/{id}`. A migração Oportunidades V29 adiciona contexto e consentimento de histórico preservando sessões existentes com valores padrão. Ver [relatório do simulador — 13/11/2026](historico/SIMULADOR_ENTREVISTAS_2026-11-13.md).

## Jornada Primeiro Emprego

O módulo existente de carreira agora permite que a própria pessoa indique, altere ou desmarque a busca do primeiro emprego. Quando ativa essa preferência, a página oferece orientação de currículo sem experiência, acesso ao diagnóstico, cursos publicados, simulador de entrevistas e busca filtrada de vagas sinalizadas para primeira experiência, sem duplicar os módulos originais. O progresso usa evidências reais disponíveis na conta (perfil/currículo, diagnóstico, entrevista salva e status pessoal de candidatura) e explica que o acompanhamento de candidatura não confirma ação da empresa. Cursos e progresso educacional permanecem no Capacita; recomendações usam conteúdo publicado retornado pela integração existente.

Oportunidades mantém filtros de emprego, estágio e aprendizagem e disponibiliza orientação para requisitos gerais com idade opcional processada somente na requisição. A checagem sempre retorna `HUMAN_VALIDATION_REQUIRED`, não grava idade, não aprova/rejeita candidato e não substitui validação da instituição, empregador ou profissional qualificado. O resumo diferencia estágio, aprendizagem e emprego, aponta fontes oficiais e ressalta proteções aplicáveis a menores. A Lei do Estágio exige vínculo educacional, termo e supervisão; a aprendizagem profissional segue regra geral etária específica e exceções legais; a idade mínima geral de trabalho e proteções de menores também são apresentadas. Os textos foram revisados em 13/11/2026 com referência ao [MTE sobre aprendizagem profissional](https://www.gov.br/trabalho-e-emprego/pt-br/assuntos/aprendizagem-profissional), [Lei do Estágio](https://www.planalto.gov.br/ccivil_03/_ato2007-2010/2008/lei/l11788.htm) e [MTE sobre combate ao trabalho infantil](https://www.gov.br/trabalho-e-emprego/pt-br/assuntos/inspecao-do-trabalho/areas-de-atuacao/Combate-trabalho-infantil). É orientação de produto, não parecer jurídico.

A preferência é salva em `career_first_job_steps`; a migração Oportunidades V30 amplia o domínio do campo `step` sem apagar registros. Rotas existentes foram mantidas: `GET /api/career/first-job`, `PUT /api/career/first-job/{step}`, `GET /api/public/career/opportunities`, `GET /api/public/career/legal-guidance` e `POST /api/career/eligibility`. Consulte [relatório da jornada — 13/11/2026](historico/PRIMEIRO_EMPREGO_2026-11-13.md).
