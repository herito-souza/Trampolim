# Backup e recuperação — 12/11/2026

## Diagnóstico

Preservado o mecanismo anterior de restauração sintética em ops/scripts/Test-BackendSecurity.ps1. Faltavam automação recorrente versionada de backups completos, criptografia, planejamento de retenção e manual de incidente. Não houve auditoria de infraestrutura externa nem afirmação de que não existam cópias fora do projeto.

Serviços críticos: bancos independentes de Oportunidades, Empresas e Capacita; armazenamento de aulas, PDFs, mídias do mural/logotipos; configuração/segredos e artefatos de implantação. Não basta copiar Git ou apenas PostgreSQL.

## Entrega

- Automação de backup lógico dos três bancos em formato custom + tar dos diretórios explicitamente configurados, com criptografia em fluxo AES-256-GCM e encapsulamento da chave por RSA-OAEP/SHA-256 (RSA >=3072).
- Identidade dos artefatos autenticada, manifesto de tamanhos/checksums e publicação de sucesso somente após todas as fontes concluírem. Não grava dump aberto no destino de backup.
- Bloqueio de execução simultânea, ACL privada e diretório .incomplete quando houver falha. Nenhuma cópia anterior é sobrescrita.
- Ensaio que aceita apenas conjunto completo, verifica integridade e autentica todos os arquivos antes de restaurar em contêiner novo sem rede/portas e com armazenamento temporário. Nenhum endpoint de restauração de produção foi criado.
- Gerador de chaves fora do repositório, sem sobrescrita; instalador de tarefa Windows com simulação -WhatIf e intervalo sugerido de 30 minutos. A rotina NÃO foi registrada/ativada: falta destino real, custódia e configuração da conta operacional.
- Política proposta sete diários/quatro semanais/doze mensais. Plano de retenção não apaga arquivos e ignora conjuntos inválidos/incompletos.
- Manual com inventário, propostas RPO <=1h/RTO <=4h (não garantidos), custódia, offsite, restauração em paralelo, rollback, reconciliação de integrações e checklist.

## Resultados executados

Suíte final de recuperação: nove testes aprovados, zero falhas/ignorados, incluindo comprovação de que o gerador não sobrescreve a chave privada existente. Validados criptografia por fluxo, rejeição de corrupção/chave incorreta/chave fraca/identidade incorreta, manifesto inseguro, configuração incompleta e retenção sem exclusão. Teste em -WhatIf do instalador não registrou tarefa; sintaxe PowerShell final validada.

Ensaio com dados exclusivamente sintéticos:

- Três bancos com tabelas de prova, chave estrangeira/check, quatro registros por banco, somatório 100, hash de valores e histórico de migração sintético. Origem alterada após backup para demonstrar recuperação do ponto anterior.
- Dois conjuntos de arquivos (mídia e configuração fictícias), hashes SHA-256 conferidos depois da extração no ambiente isolado.
- Backup levou **1.588s**; restauração **9.346s**. São tempos da fixture local pequena; não demonstram RTO de produção nem desempenho em volume real.
- Rejeitados backup de fonte indisponível e conteúdo adulterado, inclusive com checksum do manifesto recalculado pelo teste. Diretórios temporários e contêineres de teste removidos após conferir identidade/rótulo.

Complemento do harness existente: quatro testes de integração aprovados, Maven verify/build aprovado, cópia/restauração das estruturas atuais em bancos novos de teste, com contagens de tabelas e migrações conferidas:

| Trilha | Tabelas no schema restaurado | Migrações bem-sucedidas |
| --- | --- | --- |
| Oportunidades | 69 | 24 |
| Empresas | 64 | 22 |
| Capacita | 60 | 25 |

Schemas e bancos temporários foram removidos pelo harness. Nenhum dado real foi incluído. Não foram criadas migrações nem alterados contratos, autenticação ou banco de produção. Evidência sanitizada: [JSON](../evidencias/recuperacao-2026-11-12.json).

## Limites e condições de produção

RPO/RTO ainda precisam de aceite e ensaio representativo. Não foi realizado restore/failover de produção, PITR, teste de perda física de host, recuperação integral da aplicação ou armazenamento externo. Dumps separados e mídias não têm snapshot distribuído coordenado; manifesto deixa isso explícito. pg_dump não preserva roles globais/segredos/configuração do servidor; bootstrap/cofre e inventário são necessários. Artefatos permanecem isolados por trilha, sem unificação de bancos.

A tarefa Windows preparada é interativa e depende de usuário conectado/Docker disponível. Produção exige host/conta de serviço persistentes e alerta de falha/frescura, que não foram configurados. Receptor externo e cópia offsite/imutável continuam pendentes. A restauração temporária contém texto aberto durante o ensaio; usar host criptografado e privado. Formato criptográfico não teve auditoria independente; SHA-256/manifesto não provam autoria ou impedem substituição integral/replay: proteger o catálogo e a origem.

A política de retenção apenas sugere revisão, sem exclusão automática. Não foram executadas operações destrutivas sobre dados existentes. As alterações anteriores de verificação empresarial e observabilidade foram preservadas.

## Arquivos criados/modificados nesta etapa

- ops/recovery/recovery.mjs
- ops/recovery/recovery.test.mjs
- ops/recovery/new-key.mjs
- ops/recovery/backup.config.example.json
- ops/recovery/README.md
- ops/scripts/Install-RecoveryTask.ps1
- ops/.gitignore (inclusão da pasta recovery versionada)
- docs/TRAMPOLIM.md
- docs/historico/README.md
- docs/historico/RECUPERACAO_2026-11-12.md
- docs/evidencias/recuperacao-2026-11-12.json

[Plano e procedimentos operacionais](../../ops/recovery/README.md).
