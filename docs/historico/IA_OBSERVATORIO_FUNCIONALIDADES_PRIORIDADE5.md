# Prioridade 5 — Mentor, observatório e recursos avançados

## Escopo e situação

Implementação incremental sobre a versão local. Não houve reconstrução, união dos bancos, publicação em produção ou envio ao GitHub. Alterações locais anteriores foram preservadas. A homologação completa está registrada ao final; capacidades que dependem de configuração externa não são consideradas homologadas em produção.

## Funcionalidades

- Oportunidades → **Carreira e projetos**: mentor, simulações, jornada do primeiro emprego, desafios, portfólio, oportunidades por regime e observatório.
- Empresas → **Desafios e impacto**: cadastro/edição/publicação, critérios ponderados, entregas privadas paginadas, avaliação, resultados de seleção, requisitos complementares de vagas, indicadores próprios e consulta de portfólio autorizado.
- Capacita → Aprendizagem: **Observatório regional** com consentimento próprio e **Estágios e aprendizagem**, sem usar credencial de Oportunidades.
- Associação: consulta opcional da amostra pública geral, explicitamente distinta dos indicadores privados institucionais anteriores.
- Administração SaaS: custo operacional mensal informado por empresa, origem verificável e registro de auditoria. Não gera fatura ou pagamento.

### Mentor e entrevistas

OpenAI Responses é uma integração opcional; `MENTOR_AI_ENABLED=false` por padrão. O modo sem IA usa regras identificadas, sem chamada ao provedor. As orientações não decidem contratação nem prometem resultados.

Apenas o texto selecionado e autorizado é enviado; não se anexam currículos, perfis, senhas, sessões ou dados de outras trilhas automaticamente. Remoção de e-mails, padrões de contato/documento e segredos reconhecidos é uma proteção adicional, não uma classificação infalível de dados sensíveis. O usuário deve revisar o texto.

O provedor recebe `store=false`, sem ferramentas, com limite de saída. Isso **não equivale a ausência de qualquer retenção no provedor**. As políticas contratuais e de dados do provedor precisam ser avaliadas antes da ativação.

Reservas diárias de chamadas/tokens ficam no PostgreSQL, com trava de orçamento global, limites por usuário, limitação de frequência e duas chamadas simultâneas por instância. A identificação do pedido permite repetir a mesma operação sem nova chamada. Uma falha ambígua conserva a reserva e não provoca repetição automática potencialmente cobrável. Preços só são usados para estimar USD quando informados pelo operador; não se inventaram tarifas.

O endpoint Premium anterior usa o mesmo serviço e precisa de consentimento e identificação do pedido. O novo status reflete a habilitação explícita da IA. O feedback fica registrado para retomada/controle de uso; o texto integral do pedido não fica no registro de uso da IA. Respostas da simulação permanecem no histórico pessoal até exclusão.

Entrevistas têm três perguntas de preparação, respostas e feedback persistidos, acesso privado, controle de concorrência e limite de dez criações por dia. Prática opcional de áudio de até dois minutos ocorre somente no navegador; o aluno ouve e escreve/transcreve a resposta. Não há transcrição automática, envio de áudio, análise biométrica, análise emocional ou pontuação para seleção.

### Desafios e portfólio

O desafio declara competências do catálogo existente, condições de uso, materiais em texto/link HTTPS, critérios com pesos totalizando 100, prazo e situação. Empresa aprovada e prazo aberto são necessários para publicação. Conteúdo, condições e critérios não podem ser trocados após a primeira entrega. O servidor calcula a pontuação; avaliações são preservadas e tentativas repetidas não duplicam a avaliação.

Entregas contêm somente a apresentação escolhida, texto do projeto e link opcional autorizado. Não se compartilham contatos ou perfil automaticamente. Oportunidades guarda o original; uma fila durável entrega o contrato mínimo à API de Empresas. Há retomada em falhas, recibos idempotentes, retirada e proteção contra entrega tardia após retirada. A exclusão da conta enfileira a retirada e elimina payloads pendentes que contenham o projeto.

Materiais/projetos binários não foram adicionados: usa-se texto e referência HTTPS, sem o servidor buscar links externos. Isso reduz superfície de arquivos/SSRF, mas não substitui uma futura política de revisão de links e entregas.

Portfólio permite experiências, cursos declarados, projetos e referência a desafio próprio. O candidato seleciona itens e comprovantes educacionais já vinculados mediante o mecanismo de consentimento existente. Links têm segredo aleatório, somente hash armazenado, destinatário empresarial autenticado, prazo de 1–30 dias e revogação. Evidências externas são revalidadas por API; indisponibilidade não é tratada como comprovação.

Declaração pessoal, avaliação empresarial de projeto, competência educacional e certificado oficial permanecem distintos. Não se criaram certificados oficiais.

### Observatório e impacto

Somente oportunidades publicadas, ativas, de empresas aprovadas e aptas segundo as regras existentes entram na consulta pública. O filtro por período considera a criação de oportunidades/previsões atualmente publicadas: não reconstrói artificialmente o passado. UF estruturada é preferida; a UF da empresa é fallback explicitado. A série diária começa com observações reais após a versão, sem preenchimento retroativo inventado.

Indicadores pessoais exigem consentimento regional separado, em cada trilha. Coortes/grupos menores que dez são ocultos; contagens pessoais divulgadas usam faixas. Conclusões verificam aulas e avaliações obrigatórias, não apenas acesso à página. O escopo de previsões/programas é limitado a 200 por consulta e apresenta cobertura parcial quando aplicável. O cache público dura no máximo um minuto; não armazena dados privados de empresas/associações. Consentimentos retirados deixam de ser usados nas novas agregações após a atualização desse cache.

Fontes indisponíveis são identificadas, sem zeros apresentados como confirmação de atividade inexistente. A amostra **não é estatística de toda a população ou de todo o mercado regional**. Não há inferência individual de deficiência, personalidade ou atributos protegidos.

No painel empresarial, entrevista agendada, entrevista realizada por relato, contratação relatada e contratação confirmada são diferentes. `ACCEPTED` em uma candidatura antiga não é contado como contratação. O código de confirmação exige login do candidato com o e-mail da candidatura e revisão da empresa/data. Inclusão em indicador público exige opção adicional. A confirmação é um registro das partes, não prova jurídica de vínculo trabalhista.

Tempo para preenchimento só usa data de publicação efetivamente capturada após a versão. Não se usou criação como substituto. A média pública também exige ao menos dez registros válidos especificamente para esse cálculo. Permanência continua indisponível por falta de fonte confiável. Custos são informações administrativas, não receita nem cobrança simulada.

### Primeiro emprego, estágio e aprendizagem

A jornada reúne planejamento declarado, existência de currículo/resumo, prática registrada de entrevistas, projeto enviado, cursos reais disponíveis e acesso ao Radar/Capacita. A conclusão educacional continua subordinada às avaliações e ao passaporte existentes, sem fabricar progresso.

Os regimes de emprego, estágio e aprendizagem são distintos. O formulário empresarial complementa escolaridade, instituição, documentação, UF e primeira experiência; a aprendizagem requer declaração empresarial de revisão legal para exibição. Ele não converte uma vaga comum em estágio nem contorna o cadastro/pagamento específico de **R$ 50,00**.

Orientações legais são gerais, com fontes oficiais, sem aprovação automática ou exclusão por algoritmo. Idade informada em consulta opcional é transitória; não se coleta diagnóstico de deficiência. A revisão jurídica, institucional e documental do caso concreto permanece necessária.

## Arquitetura e dados

- Oportunidades / Flyway **V17**: pedidos de IA, entrevistas, projetos, fila de integração, portfólio, autorizações de compartilhamento, planos, consentimento regional, satisfação e auditoria.
- Empresas / Flyway **V14**: desafios/entregas/avaliações, regime complementar da vaga, captura de publicação, resultados de seleção, custos administrativos, recibos retirados e observações diárias.
- Capacita / Flyway **V19**: consentimento regional educacional.

Cada migração executa no histórico/conexão da própria trilha. Não há FK entre bancos. Não se alteraram migrações anteriores aplicadas. APIs internas exigem `X-Trampolim-Bridge` configurado no servidor; JWTs de trilhas diferentes continuam rejeitados. O navegador não recebe a chave interna.

Paginação por cursor em catálogos e entregas; limites em operações/seleções; índices de proprietário, publicação, fila e consentimento; agregações em lote; cache público limitado; filas duráveis para evitar dependência de disponibilidade síncrona durante o envio do projeto.

## Configuração e execução

Mantenha as três conexões PostgreSQL e as configurações de autenticação independentes já documentadas no README. Ao iniciar a nova versão do backend, Flyway aplica V17/V14/V19 na conexão correspondente. Faça backup independente antes de atualização de produção e valide em homologação; nenhum deploy produtivo foi realizado.

Variáveis novas (também em `backend/.env.example`):

| Variável | Padrão / função |
|---|---|
| `MENTOR_AI_ENABLED` | `false`; habilitação explícita |
| `OPENAI_API_KEY` / `OPENAI_MODEL` | configuração de provedor existente; sem credenciais no repositório |
| `MENTOR_BASE_URL` | `https://api.openai.com`; somente destino confiável administrado |
| `MENTOR_USER_DAILY_CALLS` / `MENTOR_GLOBAL_DAILY_CALLS` | 20 / 1000 |
| `MENTOR_USER_DAILY_TOKENS` / `MENTOR_GLOBAL_DAILY_TOKENS` | 40000 / 1000000 reservados conservadoramente |
| `MENTOR_MAX_OUTPUT_TOKENS` | 600 |
| `MENTOR_INPUT_USD_PER_MILLION` / `MENTOR_OUTPUT_USD_PER_MILLION` | vazios; informar preços vigentes antes de usar estimativas |
| `RADAR_BRIDGE_SECRET` | segredo de integração existente, mínimo 32 caracteres |
| `RADAR_INTERNAL_API_URL` | URL interna existente apontando ao backend correto |
| `CAREER_DELIVERY_ENABLED` / `CAREER_DELIVERY_DELAY_MS` | fila ativa / 30000 ms |
| `OBSERVATORY_CAPTURE_ENABLED` / `OBSERVATORY_CAPTURE_DELAY_MS` | captura ativa / 3600000 ms |

Os workers seguem também a configuração operacional existente. Em testes, desative workers automáticos para executar entregas de forma controlada. `mentor.allow-test-http` é exclusivo de testes locais e não deve ser ativado em produção.

Acesso: entre em Oportunidades e selecione Carreira e projetos; em Empresas selecione Desafios e impacto; no Capacita selecione Aprendizagem e o módulo desejado. Links de portfólio direcionam Empresas e exigem a conta indicada. Não há login compartilhado.

## Segurança, privacidade e operação

- Autorização por trilha e proprietário no servidor; validação de estado/prazo da empresa/desafio; nenhum token de Capacita é usado no endpoint privado de Oportunidades.
- Pontuação de desafios calculada no servidor e critérios congelados; integrações com versão/recibo/consentimento; retirada serializada para impedir reentrada tardia.
- URLs de projetos apenas referenciadas, não baixadas pelo backend. Links públicos abrem com proteção de contexto do navegador.
- Segredos e prompts não entram nos logs de aplicação. Respostas de erro não expõem detalhes de restrições SQL.
- Exportação própria dos novos dados e exclusão de entrevista pela API. Exclusão de conta aproveita o fluxo existente e aciona retirada de projetos. Não se exportam hashes de tokens, chaves ou payloads internos.
- Dados próprios de portfólio/projetos são mantidos enquanto a conta/finalidade estiver ativa ou até retirada/exclusão. Compartilhamentos expiram. A organização operadora ainda precisa formalizar prazos legais de retenção/auditoria/backups e procedimentos de atendimento LGPD antes do uso comercial. Não se aplicou limpeza destrutiva retroativa.

## Testes e evidências

Resultados finais e limitações estão registrados em `ops/priority5-validation-2026-10-30.json`. Testes de navegador utilizam respostas sintéticas apenas no ambiente de teste; a integração do servidor usa três PostgreSQL reais com schemas isolados e um provedor HTTP local para IA. Não foram geradas chamadas pagas, contratações reais, cursos fictícios em produção ou transações simuladas.

Verificações novas: regras de links/UF/paginação/redação de contatos; idempotência e orçamento de IA; privacidade de entrevistas; desafio/entrega/avaliação/retirada; portfólio seletivo e revogável; resultados de seleção e consentimento; proteção da cobrança de estágio; supressão e agregação regional; migrações com preservação de usuários; navegação de teclado, responsividade e verificações automáticas WCAG em desktop/celular.

Uma medição inicial de 12 consultas concorrentes do catálogo público com fixture sintética completou em **167 ms** no ambiente local. Não representa capacidade para milhares de usuários, p95 de produção nem teste de carga sustentada. Não se declara conformidade integral de acessibilidade a partir de uma varredura automática.

### Resultados executados em 30/10/2026

- Backend: suíte ampla **143 testes, 0 falhas, 0 erros, 0 ignorados**. Após os ajustes finais de contratos/paginação/exportação, a bateria focada executou novamente **6 testes, todos aprovados**. A compilação inclui todas as classes de teste.
- Navegador: suíte ampla **74 cenários aprovados**; bateria final de carreira **5 cenários aprovados**, incluindo o cenário empresarial adicional. São **75 cenários distintos cobertos**, executados nessas baterias (não se somam os quatro cenários sobrepostos).
- Frontend: **7 testes unitários aprovados**; TypeScript/Vite compilados e lint concluído sem erro. Pacote executável do backend gerado após os testes.
- Testes de migração: banco vazio, atualização dos três históricos e preservação dos usuários. As versões locais passaram a Oportunidades V17, Empresas V14 e Capacita V19 durante a inicialização dos testes existentes; nenhuma atualização produtiva foi feita.
- Acessibilidade: cenários novos em 1440 e 390 px, teclado e análise automatizada WCAG A/AA sem violações nos escopos testados; formulário empresarial também testado a 390 px. A captura móvel empresarial foi inspecionada visualmente. Isso não constitui auditoria integral com leitores de tela ou dispositivos físicos.
- Contrato de IA testado com servidor HTTP local: consentimento, remoção de padrões de contato/segredo, `store=false`, limite diário, repetição idempotente e falha sem nova chamada automática. Nenhuma chamada real paga foi executada.
- A última verificação focada completou 12 consultas concorrentes sintéticas em **53 ms**. A medição inicial foi 167 ms; ambas são observações locais de fixture pequena, não um benchmark de produção.

Os primeiros ensaios identificaram bancos locais parados, uma fixture educacional pendente em vez de ativa e rótulo ambíguo no teste do formulário. Foram corrigidos e repetidos. Um teste antigo do Passaporte excedeu o tempo durante mudanças com recarga de desenvolvimento; a suíte ampla estabilizada passou integralmente. Não se desativaram testes ou regras para obter aprovação.

## Limitações e piloto recomendado

1. IA externa preparada, mas sem homologação real paga; revisar contrato de dados, chave, modelo, orçamento, taxas atuais e alertas antes de habilitar.
2. Áudio de entrevistas é ensaio local; não há STT automático. Projetos/desafios usam texto e links, sem novo armazenamento binário.
3. Indicadores dependem dos registros e consentimentos reais; grupos insuficientes e permanência sem fonte aparecem como indisponíveis. Série histórica começa na captura real.
4. Revisão de condições de desafios, proteção de menores, retenção e regimes de contratação requer responsável institucional/jurídico; a plataforma não aprova contratos.
5. SMTP e Mercado Pago permanecem dependentes da configuração externa já documentada. Não foram ativados nem simulados.
6. Áudio de entrevista ainda requer conferência de microfone/permissões em dispositivos físicos; não se realizou homologação assistiva com NVDA/VoiceOver nas telas novas.
7. Antes de ampliar: piloto com empresas e estudantes autorizados, monitoramento das filas, taxa de erro/latência, custos de IA, pool de conexões, backups/restauração e carga representativa. Avaliar CDN/armazenamento de mídia e capacidade do PostgreSQL com métricas reais; não estimar milhares de usuários apenas pela stack.

## Fontes consultadas

- [OpenAI — geração de texto / Responses](https://developers.openai.com/api/docs/guides/text)
- [OpenAI — controles e retenção de dados](https://developers.openai.com/api/docs/guides/your-data)
- [Câmara dos Deputados — Lei 11.788/2008 (estágio), texto atualizado](https://www2.camara.leg.br/legin/fed/lei/2008/lei-11788-25-setembro-2008-581200-normaatualizada-pl.html)
- [Câmara dos Deputados — CLT, texto atualizado, aprendizagem](https://www2.camara.leg.br/legin/fed/declei/1940-1949/decreto-lei-5452-1-maio-1943-415500-normaatualizada-pe.html)

## Arquivos desta etapa

- `backend/src/main/java/com/trampolim/api/career/CareerAccountController.java`
- `backend/src/main/java/com/trampolim/api/career/CareerAiService.java`
- `backend/src/main/java/com/trampolim/api/career/CareerCoachingController.java`
- `backend/src/main/java/com/trampolim/api/career/CareerCostController.java`
- `backend/src/main/java/com/trampolim/api/career/CareerDeliveryWorker.java`
- `backend/src/main/java/com/trampolim/api/career/CareerEligibilityController.java`
- `backend/src/main/java/com/trampolim/api/career/CareerErrors.java`
- `backend/src/main/java/com/trampolim/api/career/CareerOperationsController.java`
- `backend/src/main/java/com/trampolim/api/career/CareerPortfolioController.java`
- `backend/src/main/java/com/trampolim/api/career/CareerProjectsController.java`
- `backend/src/main/java/com/trampolim/api/career/CareerRegionalController.java`
- `backend/src/main/java/com/trampolim/api/career/CareerRules.java`
- `backend/src/main/java/com/trampolim/api/career/CompanyCareerController.java`
- `backend/src/main/java/com/trampolim/api/career/CompanyCareerService.java`
- `backend/src/main/java/com/trampolim/api/career/CompanySelectionService.java`
- `backend/src/main/java/com/trampolim/api/career/RegionalObservatory.java`
- `frontend/src/features/career/careerApi.ts`
- `frontend/src/features/career/CareerExtras.tsx`
- `frontend/src/features/career/CareerPanel.tsx`
- `frontend/src/features/career/CareerResult.tsx`
- `frontend/src/features/career/CompanyCareerPanel.tsx`
- `frontend/src/features/career/InterviewAudioPractice.tsx`
- `frontend/src/features/career/ObservatoryPanel.tsx`
- `frontend/src/features/career/PortfolioPanel.tsx`
- `frontend/src/features/career/ProjectsPanel.tsx`
- `frontend/src/features/career/YoungOpportunities.tsx`
- `backend/src/test/java/com/trampolim/api/career/CareerHttpPostgresTest.java`
- `backend/src/test/java/com/trampolim/api/career/CareerRulesTest.java`
- `backend/src/main/java/com/trampolim/api/education/LearningRegionalController.java`
- `backend/src/main/resources/db/migration/V17__career_mentor_portfolio_and_programs.sql`
- `backend/src/main/resources/db/migration-empresas/V14__career_challenges_selection_and_observatory.sql`
- `backend/src/main/resources/db/migration-capacita/V19__regional_learning_consent.sql`
- `backend/src/main/java/com/trampolim/api/config/SecurityConfig.java`
- `backend/src/main/java/com/trampolim/api/user/PremiumController.java`
- `backend/src/main/resources/application.properties`
- `backend/.env.example`
- `backend/src/test/java/com/trampolim/api/radar/RadarMigrationPostgresTest.java`
- `frontend/src/App.tsx`
- `frontend/src/features/account/AccountNavigation.tsx`
- `frontend/src/features/account/CompanyDashboard.tsx`
- `frontend/src/features/account/AssociationDashboardPage.tsx`
- `frontend/src/features/account/SaasAdminPage.tsx`
- `frontend/src/features/account/PremiumPage.tsx`
- `frontend/src/features/learning/LearningPortal.tsx`
- `frontend/e2e/career-journey.spec.ts`
- `README.md`
- `IA_OBSERVATORIO_FUNCIONALIDADES_PRIORIDADE5.md`
- `ops/priority5-validation-2026-10-30.json`
