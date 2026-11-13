# Central de Candidaturas — 2026-11-12

## Diagnóstico

Oportunidades já oferecia `GET/POST /api/profile/applications` para salvar vagas e manter um status pessoal, notas e lembretes. Esse registro não representa o fluxo real de recrutamento. Empresas já registra os estados `RECEIVED`, `IN_REVIEW`, `CONTACTED`, `INTERVIEW`, `ACCEPTED`, `DECLINED` e `CLOSED`, além de eventos de histórico. A API empresarial de histórico é restrita à organização e inclui recursos internos; não podia ser exposta ao candidato diretamente.

## Implementado

- Nova rota `#oportunidades/applications`, lazy-loaded, entrada na Sidebar e ação a partir do resumo de candidaturas do dashboard.
- Nova tela com duas seções claramente identificadas: etapas informadas pela empresa (somente leitura) e acompanhamento pessoal (gerenciado no perfil).
- Filtros de situação, empresa, vaga e intervalo de datas; paginação das candidaturas empresariais, carregamento, erro/retentativa e estados vazios.
- Linha do tempo baseada apenas em `RECEIVED`, `BASELINE` e `STAGE_CHANGED`. O último item só recebe destaque de etapa atual quando seu status coincide com o estado atual da candidatura. `BASELINE` é identificado como início do histórico disponível; nenhuma transição anterior é fabricada.
- Orientações dependem do estado real e não prometem contratação. A seleção é explicitamente distinta de contratação confirmada.
- A tela não consulta processos empresariais automaticamente. O titular autoriza a consulta pontual; o endpoint Oportunidades usa apenas a identidade do token, exige conta `PERSON` e e-mail confirmado e encaminha o endereço internamente. Não há campo de e-mail ou identificador de candidato no contrato do navegador.
- Endpoint interno Empresas validado pelo segredo de integração, filtra candidaturas com consentimento registrado e retorna somente vaga, empresa, localidade, status e eventos permitidos. Não retorna contato, mensagem, autoria ou notas internas.
- Índice parcial Empresas V24 para a busca exata por e-mail associado à candidatura e ordenação temporal.
- A notificação Oportunidades já existente para atualizações do acompanhamento pessoal agora abre a Central.

## Arquivos alterados/adicionados

Backend:

- `backend/src/main/java/com/trampolim/api/catalog/ApplicationCenterController.java`
- `backend/src/main/java/com/trampolim/api/company/CandidateApplicationCenterController.java`
- `backend/src/main/java/com/trampolim/api/config/SecurityConfig.java`
- `backend/src/main/java/com/trampolim/api/communication/CommunicationTemplates.java`
- `backend/src/main/resources/db/migration-empresas/V24__candidate_application_center_lookup_index.sql`
- `backend/src/test/java/com/trampolim/api/catalog/ApplicationCenterControllerTest.java`
- `backend/src/test/java/com/trampolim/api/company/RecruitmentPostgresTest.java`
- `backend/src/test/java/com/trampolim/api/company/RecruitmentMigrationPostgresTest.java`
- `backend/src/test/java/com/trampolim/api/radar/RadarMigrationPostgresTest.java`

Frontend:

- `frontend/src/features/account/ApplicationCenterPage.tsx`
- `frontend/src/features/account/AccountNavigation.tsx`
- `frontend/src/features/account/accountNavigationModel.ts`
- `frontend/src/features/account/OpportunitiesPage.tsx`
- `frontend/src/features/account/ProfilePage.tsx`
- `frontend/src/App.tsx`
- `frontend/e2e/application-center.spec.ts`

Documentação:

- `docs/TRAMPOLIM.md`
- `docs/historico/README.md`

## Rotas e dados

- `POST /api/profile/applications/center`: candidato autenticado, filtros e confirmação pontual; a identidade é obtida do token/registro Oportunidades.
- `POST /api/integrations/application-center/query`: integração interna Empresas com `X-Trampolim-Bridge`; nunca chamada pelo navegador.
- A API empresarial de recrutamento e suas mutações não foram abertas nem alteradas.
- Migração nova: Empresas V24. Oportunidades, Capacita, dados existentes e históricos aplicados não foram reescritos.

## Notificações e limitação conhecida

As notificações existentes de Oportunidades sobre mudanças no registro pessoal abrem a Central. A fila de eventos de mudança empresarial permanece destinada aos usuários recrutadores. Não existe atualmente uma associação persistente e verificada entre usuário Oportunidades e candidatura empresarial, nem uma entrega de notificação entre bancos para o candidato. Por isso, a Central mostra as mudanças empresariais ao consultar/atualizar a página, mas não promete alerta automático ao candidato. Uma entrega desse tipo requer uma integração de eventos entre trilhas com identidade, consentimento, idempotência e retentativa próprios; não foi simulada.

## Testes e validação

Validação focalizada executada em 12/11/2026:

- Backend: `ApplicationCenterControllerTest` (3), `RecruitmentPostgresTest` (10), `RecruitmentMigrationPostgresTest` (1), `RadarMigrationPostgresTest` (1) e `JwtAuthenticationFilterTrailIsolationTest` (16): **31 testes, 0 falhas e 0 erros**. Inclui migração Empresas V24 e busca PostgreSQL isolada.
- Frontend: lint aprovado; testes unitários **23/23**; build de produção aprovado. O Vite ainda reporta avisos de chunks grandes já existentes (mapa regional e leitor/worker de PDF).
- Playwright: cinco cenários focalizados passaram individualmente: viewport de 320, 375, 768 e 1440 px, mais resposta 403. Os cenários verificam filtros, autorização explícita, não exposição de notas internas, ausência de overflow horizontal e axe WCAG 2.2 AA. O processo que executou o arquivo completo não encerrou normalmente no ambiente Windows; por isso, os resultados são reportados a partir das cinco execuções individuais, não como conclusão limpa da suíte agrupada.
- `git diff --check`: aprovado; apenas avisos do Git sobre conversão LF/CRLF em arquivos já modificados.

Os testes PostgreSQL usaram somente schemas aleatórios isolados do harness local. Nenhum dado real ou registro de produção foi usado.

## Pendências

- Avaliar um mecanismo explícito e idempotente para notificações de etapa destinadas ao candidato, sem compartilhar sessões entre trilhas.
- Executar validação manual com leitor de tela e backend/Empresas disponíveis na mesma instalação local; axe automatizado não substitui essa revisão.
