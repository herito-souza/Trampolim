# Dashboards orientados à próxima ação — Trampolim

## Escopo e auditoria

Revisão incremental do frontend local, sem alterar contratos de API, bancos, credenciais, políticas de autorização ou bibliotecas. As alterações de backend e navegação que já estavam no diretório foram preservadas.

Problemas encontrados:
- DashboardActions apresentava cada atalho como um card de mesma importância.
- Oportunidades sempre sugeria revisar perfil e diagnóstico, mesmo quando os dados básicos e o diagnóstico já estavam preenchidos. Havia outra consulta ao perfil já carregado pela aplicação.
- Empresas misturava tarefas operacionais com um hero institucional extenso e repetia a consulta de identificação da empresa feita pelo portal.
- Capacita oferecia atalhos e histórico, mas não reunia progresso, tentativas em andamento e próxima aula na entrada.
- Falhas de consulta precisam ser distinguidas de listas vazias e contadores zero.

## Implementação

### Componentes compartilhados
- DashboardActions: uma ação prioritária destacada e ações secundárias em lista, com descrições associadas aos botões, foco visível e alvos de interação de pelo menos 44 px.
- DashboardSkeleton: carregamento anunciado por leitor de tela, formas decorativas ocultas e animação que respeita movimento reduzido.
- Modelos puros para elegibilidade de anúncios e ordenação da próxima aula.

### Oportunidades
- Completar perfil somente quando faltam nome, contato, município ou resumo. Experiência anterior e foto não são exigidas para essa ação.
- Diagnóstico inicial somente quando ainda não existe resultado salvo.
- Com diagnóstico disponível, acesso ao Radar para comparar requisitos, sem prometer contratação.
- Acompanhamento de candidaturas somente quando o histórico carregado possui registros.
- Preservados catálogo, recomendações existentes, favoritos, histórico, filtros geográficos e atalhos de busca remota/estágio.
- O dashboard reaproveita o perfil autenticado; alterações no perfil atualizam esse resumo. Atualização explícita após erro continua podendo consultar o servidor.

### Empresas
- Hero institucional substituído por uma visão operacional compacta.
- Prioridade para candidaturas sem análise, pagamentos pendentes e rascunhos, quando os dados e o papel permitem.
- Vagas ativas incluem somente anúncios de emprego publicados, não expirados e, no caso de estágio, pagos. Essa é uma indicação visual; a autorização e a disponibilidade oficial continuam no servidor.
- Candidaturas recentes e etapas existentes de seleção, sem inventar uma entidade de processo seletivo nem tratar candidaturas como pessoas únicas.
- Indicadores existentes de visualizações, cliques e candidaturas/inscrições. Valores indisponíveis aparecem como traço, não como zero.
- Analistas continuam sem consulta a candidaturas individuais ou ações de publicação.
- Preservados publicação de cursos, estágio de R$ 50,00, anúncios, pagamento, perfil empresarial, equipe e demais módulos.

### Capacita
- Continuar aprendendo com a próxima aula publicada e validada, respeitando a ordem real do curso e o progresso conhecido.
- Consulta de metadados de um único curso do histórico, sem baixar o vídeo; os dados já obtidos são reutilizados ao abrir a aula.
- Progresso e competências calculados pela API existente.
- Avaliações pendentes indicam tentativas IN_PROGRESS já iniciadas. Não se presume elegibilidade nem vencimento de provas ainda não iniciadas.
- Notificações recentes reutilizam a mesma fonte persistida/SSE do sino, sem criar outro canal nem uma consulta paralela de resumo.
- Resumo de desempenho e tentativas reaproveitado pelo painel de desempenho; eventos de alteração invalidam os dados necessários.
- Pesquisa e paginação do catálogo não recarregam histórico e resumo.
- Ferramentas de professor permanecem nos módulos e permissões atuais.

## Fontes de dados existentes

Oportunidades: /profile, /profile/favorites, /profile/applications e diagnóstico salvo; busca territorial pública existente.

Empresas: /company/dashboard, /company/listings e /company/applications conforme papel. Métricas detalhadas permanecem no módulo existente.

Capacita: /capacita/learning/me, /courses, /courses/{id}, /progress, /performance, /assessment-attempts, /notifications e /notifications/stream. Não há acesso direto a bancos de outra trilha.

## Arquivos desta revisão

Modificados:
- frontend/src/App.tsx
- frontend/src/features/account/OpportunitiesPage.tsx
- frontend/src/features/account/ProfilePage.tsx
- frontend/src/features/account/CompanyPortal.tsx
- frontend/src/features/account/CompanyDashboard.tsx
- frontend/src/features/dashboard/CandidateNextStep.tsx
- frontend/src/features/dashboard/DashboardActions.tsx
- frontend/src/features/learning/LearningPortal.tsx
- frontend/src/features/learning/PerformancePanel.tsx
- frontend/src/features/learning/QuestionNotifications.tsx
- frontend/e2e/dashboard-journey.spec.ts

Criados:
- frontend/src/features/dashboard/CompanyOverview.tsx
- frontend/src/features/dashboard/DashboardSkeleton.tsx
- frontend/src/features/dashboard/dashboardModels.ts
- frontend/src/features/dashboard/learningModels.ts
- frontend/src/features/learning/LearningOverview.tsx
- frontend/src/features/learning/useLearningOverview.ts
- frontend/src/features/learning/performanceTypes.ts
- frontend/tests/dashboard-models.test.mjs
- DASHBOARDS_REVISAO_2026-11-06.md

## Verificação

- Lint (npm run lint): aprovado, sem avisos.
- Tipos e build de produção (npm run build, incluindo tsc -b): aprovados.
- Testes unitários (npm test): 17 aprovados.
- Suíte completa de navegador (npm run test:e2e -- --workers=2): 173 aprovados em 6,6 minutos, incluindo 25 cenários específicos dos dashboards.
- Larguras verificadas: 320, 375, 768, 1024 e 1440 px.
- Verificações de teclado, overflow e axe/WCAG dos dashboards aprovadas. Isso não equivale a uma certificação integral de acessibilidade ou a uma validação manual completa com leitores de tela.
- Inspeção visual das capturas móveis das três trilhas e do painel empresarial no desktop.
- Regressões cobertas pela suíte completa: autenticação e logout independentes, perfil/foto, diagnóstico, busca, filtros, cursos, videoaulas, PDFs, avaliações, competências, notificações, Hub, navegação e mapas.
- Durante a validação foram corrigidos cancelamento de consultas obsoletas, reconsultas durante navegação com carregamento sob demanda e uma resposta de teste do catálogo regional que não seguia o contrato existente. As contagens de consultas consideram respostas concluídas, separando tentativas canceladas pelo modo de desenvolvimento.
- Log final: ops/dashboard-2026-11-06/e2e.log. Capturas sintéticas: ops/dashboard-2026-11-06/screenshots/.
- Testes de backend não foram repetidos nesta revisão de frontend.

Para reproduzir, na pasta frontend: npm run lint, npm test, npm run build e npm run test:e2e -- --workers=2.

Os testes de navegador utilizam dados sintéticos identificados e interceptações das APIs. Validam o frontend, sem criar registros em produção. Não substituem testes de integração com os serviços reais, homologação de pagamentos ou validação jurídica.

## Limitações e configurações

- Nenhuma migração ou configuração externa nova necessária para estes dashboards.
- A listagem atual de progresso educacional é limitada pelo backend. O resumo não alega representar um histórico ilimitado; usa também o progresso presente no detalhe do curso.
- Não há um endpoint de avaliações elegíveis pendentes: são apresentadas apenas tentativas iniciadas e um acesso às avaliações existentes.
- Vagas compatíveis abrem o Radar existente; nenhuma recomendação ou vaga nova é fabricada no dashboard.
- O build continua avisando sobre o tamanho de módulos de mapa/PDF já existentes. Eles continuam separados e carregados sob demanda; não foram trocadas bibliotecas nesta revisão.
- Não foram realizadas publicação no GitHub, implantação ou alterações de dados reais nesta etapa.
