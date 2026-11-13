# Privacidade e ciclo de vida — 12/11/2026

## Diagnóstico

Já existiam exportações próprias das três trilhas, exclusão autenticada por senha, bloqueio de exclusão de responsáveis por empresas/cursos, auditoria de status e papéis, controle de acesso por trilha/recurso, arquivos privados e rotinas específicas de retenção. Currículos são PDFs de até 10MB em bytea; download do titular tem no-store/nosniff e filename saneado. Não havia fluxo comum de protocolo/análise de exclusão com estados, previsão de descarte dos próprios metadados ou trilha de pedidos de exportação.

Não foi refeito o cadastro/autenticação nem criado acesso compartilhado entre bancos. O mapa de dados e a política técnica vigente foram consolidados na [referência principal](../TRAMPOLIM.md#ciclo-de-vida-dos-dados-pessoais-12112026), evitando documentos concorrentes de política.

## Controles entregues

1. Componente reutilizável de solicitação/consulta nas três contas, com linguagem clara, confirmação, estado vazio/erro/carregamento, protocolo, motivo e reavaliação; sem anexos, relato livre ou senha armazenada.
2. API do titular usa somente sua conta autenticada. Chave idempotente, um protocolo ativo por conta, limite de dez novas solicitações por dia, paginação e cancelamento com revisão esperada.
3. Revisão administrativa restrita ao administrador global existente, pela fronteira explícita já utilizada pela plataforma. Não concede acesso a recrutadores, associações, professores ou administradores educacionais.
4. Revisão tem motivo padronizado, referência interna restrita, controle de concorrência e auditoria com identificador do administrador, estados e data. RETAINED exige reavaliação futura em até 90 dias (limite operacional, não legal). Administrador não consegue inventar COMPLETED sem a exclusão ocorrer.
5. Triggers locais impedem excluir a conta quando REVIEWING/RETAINED, inclusive via endpoints antigos; mensagens de conflito não expõem SQL. Liberação READY não apaga nada; senha e transferência de responsabilidades continuam exigidas pelos fluxos anteriores.
6. Exclusão autorizada de conta de teste registra conclusão na mesma transação. Chaves de sujeito tornam-se NULL e referências holder são removidas; recibo e auditoria necessários permanecem minimizados, sem promessa de anonimização irreversível.
7. Exportações existentes reutilizadas e pedidos registrados sem conteúdo pessoal. Cache privado/no-store e nosniff em perfis, contas e privacidade. Exportação docente adicional paginada de autoria própria, sem alunos ou chaves de arquivos.
8. Prévia de descarte administrativa oferece apenas contagens de metadados antigos/encerrados; exclui estados ativos dos candidatos e não altera registros. Não foi criado job de exclusão geral ou remoção de dados reais.

## Migrações

| Banco/trilha | Migração nova |
| --- | --- |
| Oportunidades | V25__privacy_requests_and_account_review.sql |
| Empresas | V23__privacy_requests_and_account_review.sql |
| Capacita | V26__privacy_requests_and_account_review.sql |

Cada banco recebe privacy_requests, privacy_audit, privacy_export_audit, índices e trigger própria de exclusão da conta. Não há FK para usuário de outro banco. Histórico Flyway anterior preservado. As migrações não apagam registros existentes e foram executadas somente em schemas temporários nesta tarefa. Ao reiniciar implantação real, Flyway segue o processo habitual: backup/ensaio e homologação antes da ativação.

## Testes executados

- Primeira seleção de backend: 18 testes aprovados, zero falhas/erros/ignorados; inclui atualização das migrações de históricos anteriores preservando usuários/candidaturas. As expectativas de versão nos testes foram atualizadas para os novos históricos, sem remover verificações de preservação.
- Seleção final: 39 testes aprovados, zero falhas/erros/ignorados, Maven verify/build aprovado. Há sobreposição com a primeira seleção: não somar como 57 testes distintos.
- Os 14 testes de privacidade validam idempotência, isolamento/sessões, acesso ao protocolo de outra conta, revisão, concorrência, restrição de exclusão, minimização após remoção sintética, retenção justificada/reavaliação, prevenção de conclusão fictícia, exportações sem credenciais/dados de outra conta e previsão de descarte sem remoção.
- Regressão incluiu seis testes das contas empresariais, 16 testes de isolamento JWT e testes de autenticação, conta estudantil e autorização administrativa.
- Frontend: lint, 23 testes existentes e build com TypeScript/Vite aprovados. Aviso de chunks grandes de mapa/PDF continua preexistente; não foi desabilitado.
- Playwright: três testes do painel em 320/375/1440px, com APIs de teste simuladas. Validam envio/cancelamento/novo protocolo com nova chave, checkbox por teclado, ausência de overflow e axe sem violações detectadas no componente para as regras selecionadas. Não constituem teste de leitor de tela humano ou acessibilidade integral de todas as páginas.
- Schemas de teste isolados removidos pelo harness. Exclusões dos testes atingiram somente fixtures recém-criadas. Sem envio de e-mails, implantação ou eliminação de produção.
- git diff --check sem erros.

## Limites e pendências

O estado de retenção protege a linha da conta, não suspende todas as limpezas de mensagens/notas/arquivos nem preserva evidência em sistemas externos. Um caso que exija preservação abrangente precisa de procedimento específico, escopo, configuração das rotinas afetadas e decisão do responsável.

A interface administrativa de revisão não foi adicionada; os endpoints estão disponíveis e documentados para atendimento operacional autorizado. Não há eliminador genérico transversal entre bancos, nem descarte automático dos novos metadados sem definição de política. Exportação existente de conta não cobre integralmente cada arquivo e informação de terceiros; módulos históricos grandes ainda podem demandar paginação/job dedicado.

Precisam de validação especializada: controlador/operadores, bases legais, prazos por categoria, menores/representação, canal e resposta fundamentada, contratos/provedores e retenção de evidências. O mapa não presume que consentimento seja base legal de todo tratamento. Não se declara conformidade jurídica integral.

Infraestrutura ainda deve confirmar criptografia de volumes/bancos, custódia e descarte dos logs e backups, reconciliação de exclusões/revogações após restauração e inspeção/sanitização de arquivos. Não houve auditoria exaustiva de cada log/biblioteca nem varredura antimalware universal. Medidas técnicas existentes e novas reduzem riscos, sem comprovar por si conformidade jurídica.

## Arquivos criados/modificados nesta etapa

- backend/src/main/java/com/trampolim/api/privacy/PrivacyConfig.java
- backend/src/main/java/com/trampolim/api/privacy/PrivacyStore.java
- backend/src/main/java/com/trampolim/api/privacy/PrivacyController.java
- backend/src/main/java/com/trampolim/api/privacy/PrivacyAdminController.java
- backend/src/main/java/com/trampolim/api/privacy/PrivacyErrors.java
- backend/src/main/java/com/trampolim/api/privacy/PrivacyWebConfig.java
- backend/src/main/java/com/trampolim/api/education/CapacitaController.java
- backend/src/main/resources/db/migration/V25__privacy_requests_and_account_review.sql
- backend/src/main/resources/db/migration-empresas/V23__privacy_requests_and_account_review.sql
- backend/src/main/resources/db/migration-capacita/V26__privacy_requests_and_account_review.sql
- backend/src/test/java/com/trampolim/api/privacy/PrivacyPostgresTest.java
- backend/src/test/java/com/trampolim/api/company/RecruitmentMigrationPostgresTest.java
- backend/src/test/java/com/trampolim/api/radar/RadarMigrationPostgresTest.java
- frontend/src/features/account/PrivacyRequests.tsx
- frontend/src/features/account/ProfilePage.tsx
- frontend/src/features/account/CompanyPortal.tsx
- frontend/src/pages/CapacitaPage.tsx
- frontend/e2e/privacy-requests.spec.ts
- docs/TRAMPOLIM.md
- docs/historico/README.md
- docs/historico/PRIVACIDADE_2026-11-12.md
- docs/evidencias/privacidade-2026-11-12.json
