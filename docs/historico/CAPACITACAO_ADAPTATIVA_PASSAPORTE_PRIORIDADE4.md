# Capacitação adaptativa e Passaporte Digital — Prioridade 4

Entrega local: 30/10/2026. Somente a Prioridade 4 foi implementada; a Prioridade 5 não foi iniciada. Não houve publicação no GitHub nem implantação em produção.

## Auditoria e reaproveitamento

A versão local foi a fonte de verdade. O Git estava na branch `main`, com alterações e arquivos ainda não commitados das etapas anteriores; foram preservados. Confirmados React/TypeScript/Vite/Tailwind, Java 21/Spring Boot/Spring Security, JWT separados, PostgreSQL e Flyway por trilha.

A estrutura existente já corrigia avaliações no servidor, registrava tentativas imutáveis e criava `learning_competency_awards` apenas após aprovação. Havia progresso de videoaulas, compartilhamento individual de conquistas e integração consentida com o Radar por códigos temporários e recibos de evidência. O trabalho ampliou essas estruturas, sem outra autenticação, outro cadastro de competências ou duplicação dos cursos/avaliações.

Sequência executada: auditoria; migração aditiva do Capacita; regras e trilhas; validade/revogação das conquistas existentes; compartilhamento dirigido; invalidação durável do Radar; interfaces; testes integrados e de regressão.

## Funcionalidades entregues

### Minhas Trilhas e Meu Progresso

Em **Capacita → Aprendizagem → Minhas Trilhas**, o aluno pode criar e remover objetivos profissionais. São permitidos até 20 objetivos por conta, cada um com até 20 competências canônicas e níveis de 1 a 4. O objetivo pode partir de competências escolhidas ou de requisitos de uma vaga/previsão pública realmente disponível em Empresas. Previsão continua identificada como previsão, sem contratação garantida.

A análise mostra competência necessária, nível comprovado, lacuna, percentual do objetivo e conteúdos publicados relacionados: curso, módulo, videoaulas, avaliação/exercício e progresso. Somente conquistas válidas, não revogadas/não expiradas, com aprovação e pontuação compatíveis com a tentativa de origem, contam como comprovação. Declarar um objetivo ou visitar um conteúdo não emite credencial.

As recomendações usam regras transparentes `AdaptiveRules.VERSION=1`:

| Situação | Ação recomendada |
|---|---|
| Todas as competências do conteúdo já comprovadas e dispensa habilitada | Conteúdo dominado; não exigir repetição recomendada |
| Aulas e avaliação do contexto já concluídas | Estudo concluído |
| Módulo prévio ainda incompleto | Cumprir pré-requisito |
| Última tentativa abaixo do critério e nova tentativa disponível | Revisar videoaulas e realizar exercício novamente |
| Tentativas esgotadas, prazo vencido ou conquista anterior que precisa de nova comprovação | Solicitar orientação do professor |
| Aulas ainda pendentes | Estudar/retomar videoaulas |
| Aulas estudadas e tentativa disponível | Realizar avaliação |

O professor configura pré-requisitos de módulos do mesmo curso, dispensa por domínio e validade de futuras conquistas em **Gerenciar cursos → módulo → Regras adaptativas e credenciais do curso**. A API rejeita dependências circulares, autorreferências, módulos de outro curso e edição por terceiros. O início e o envio da avaliação verificam os pré-requisitos no servidor. A dispensa de uma recomendação não remove critérios de conclusão da avaliação e não emite outra competência sem prova.

A recomendação de vídeo considera apenas aulas publicadas, validadas e com duração real já registrada entre **180 e 300 segundos**. A validação de upload/publicação existente foi preservada. A interface abre o reprodutor, o módulo, os materiais ou o exercício real; não cria recursos fictícios.

Conteúdos sem correspondência educacional não são inventados. Comprovar uma competência não equivale a concluir todos os cursos oferecidos, obter contratação ou receber certificado formal. Objetivos com nível superior ao demonstrado continuam com lacuna mesmo que o conteúdo básico já esteja dominado.

### Passaporte Digital e competências conquistadas

Em **Capacita → Aprendizagem → Passaporte Digital**, as conquistas existentes passam a apresentar:

- UUID verificável do registro original;
- competência, descrição, curso/módulo e professor responsável da evidência;
- emissor `Trampolim Capacita`, critério de aprovação e data de emissão;
- validade, quando definida pelo responsável, e estado válido/expirado/revogado/evidência indisponível;
- indicação explícita de credencial interna de aprendizagem, sem equivalência a certificado oficial.

Não foi criada uma segunda tabela de credenciais. `learning_competency_awards` continua ligada à tentativa aprovada, ao aluno e à avaliação. A validação também compara a pontuação da conquista à tentativa e verifica o contexto/autor da evidência. Conquistas antigas sem código canônico não são convertidas automaticamente em competências do Radar.

A validade do módulo é opcional, entre 1 e 3650 dias, aplicada a **novas** conquistas no momento da emissão. Registros anteriores permanecem com seus critérios e sem expiração presumida. Mudar a configuração não altera retroativamente a prova já emitida. Os mecanismos existentes de aprovação, limitação de tentativas e prevenção de duplicação foram preservados.

Professor responsável ou administrador educacional pode revogar a credencial com justificativa. O professor só consulta/revoga registros de cursos que pode administrar. A revogação é definitiva e auditada; não existe botão para restaurar comprovação sem nova evidência. Correções pedagógicas devem gerar nova conquista elegível pelos fluxos de avaliação, preservando o histórico.

O painel anterior de competências também identifica registros expirados/revogados. Eles não podem gerar novos compartilhamentos nem novos créditos no Radar. O botão **Atualizar passaporte** permite consultar novamente a situação atual.

### Compartilhamento e verificação

O aluno seleciona até 20 credenciais válidas, informa o e-mail empresarial do destinatário, escolhe prazo de 1 a 30 dias e confirma o consentimento. Uma identificação textual opcional é autodeclarada, não uma identidade documental verificada.

O código usa 32 bytes aleatórios; somente seu hash é armazenado. A interface apresenta o link uma vez para envio manual. Não houve envio de e-mail ou mensagem a terceiros.

O destinatário abre `/?passport=CODIGO#empresas`, entra com sua conta independente de Empresas e confirma a consulta. O servidor valida conta ativa/verificada e papel `OWNER`, `ADMIN` ou `RECRUITER`; o destinatário é derivado da autenticação, nunca de um e-mail arbitrário enviado pelo navegador. Conta de aluno, analista ou recrutador de outro e-mail não recebe os dados. Há também o módulo **Passaportes recebidos** no painel empresarial.

O retorno contém somente as credenciais selecionadas e a identificação opcional autorizada. Não inclui e-mail/nome cadastral do aluno, notas individuais, respostas, currículo ou outras competências. Nomes de alunos são apresentados apenas ao professor autorizado ao consultar os registros do próprio curso, não ao recrutador por esse mecanismo.

Revogar o compartilhamento bloqueia consultas futuras, sem revogar a conquista original. Revogar/expirar a credencial impede que o mesmo link continue apresentando sua prova como válida. O destinatário vê o estado, sem o conteúdo da evidência que deixou de ser válida. Revogar não apaga cópias que o destinatário já tenha obtido legitimamente.

Em `/?credential=UUID`, a consulta pública informa somente identificador, emissor, datas e situação, sem revelar competência ou dados pessoais. Há limitação de 60 consultas por minuto por endereço remoto, com memória limitada por instância. A conferência detalhada exige o compartilhamento dirigido. A verificação é online contra o backend: **não é uma credencial W3C/Open Badges assinada para validação offline**, nem comprovação de reconhecimento oficial.

O compartilhamento individual anterior permanece compatível e explicitamente autorizado, para quem possuir aquele link. Sua interface informa essa diferença e orienta usar o Passaporte Digital para acesso restrito a destinatário. Expiração/revogação também bloqueiam os links individuais anteriores.

## Integração e consistência com o Radar

A vinculação existente continua exigindo o código gerado na conta de Oportunidades e a confirmação na conta do Capacita. Não há sincronização automática de contas, senhas, sessões ou perfis. Cada nova comprovação enviada segue esse consentimento explícito.

O fluxo é:

1. A avaliação corrigida pelo servidor registra a conquista no Capacita.
2. A próxima análise da trilha usa as novas provas e recalcula as lacunas locais.
3. Quando autorizada, a conquista é enviada pelo fluxo de recibos existente ao Radar.
4. A fila durável invalida as análises relacionadas no banco de Oportunidades, por API interna protegida.
5. O Radar revalida as evidências no Capacita antes de atribuir pontos e recalcula a análise quando consultada.
6. Previsões interessadas são marcadas para recálculo pelo trabalhador já existente. A linha de base das lacunas é preservada; o indicador antigo é marcado desatualizado em vez de apagar seu histórico inicial.

`learning_radar_invalidation_outbox` usa recibos opacos, versões de entrega, lotes de 20, reserva de trabalho, bloqueio concorrente e retentativas com espera crescente. Expirações também geram invalidação de forma atômica. O trabalhador roda fora do agendador compartilhado das notificações, para não bloquear eventos educacionais durante falhas de rede.

Se a integração falhar, o trabalho permanece pendente. Não é exibida confirmação fictícia. Mesmo antes da entrega da invalidação, uma evidência revogada/expirada não recebe pontos em nova análise, pois o Radar consulta sua validade no Capacita. Indicadores agregados que dependem de snapshots e do trabalhador possuem consistência eventual; não são prometidos como atualização instantânea durante indisponibilidade.

## Desempenho e limites

A recomendação é calculada somente para a trilha aberta, não para todos os alunos a cada carregamento. O resultado fica em cache por trilha/aluno com impressão dos requisitos, competências válidas, progresso, tentativas, versões de avaliação e dependências. O cache é limitado a uma janela de até um minuto e pode ser atualizado explicitamente. Requisitos de vaga/previsão são consultados a cada cinco minutos ou mediante atualização forçada. Fontes indisponíveis não geram requisitos presumidos.

Um bloqueio por aluno/trilha evita recomputações concorrentes do mesmo objetivo. Pré-requisitos e dados de progresso são buscados em lote, com índices por aluno/módulo/competência. Não há cache privado compartilhado entre usuários ou tenants.

Cada análise considera até 80 contextos educacionais e até três vídeos curtos por contexto. O retorno identifica cobertura parcial quando o limite for alcançado. Tentativas são consultadas em lote com limite defensivo. Credenciais usam páginas de até 50; a interface usa 20. Cada aluno pode manter até 100 compartilhamentos ativos; a lista prioriza os ativos e inclui histórico recente. O painel do professor limita a consulta a 100 registros por curso. Históricos maiores exigirão paginação adicional dessa consulta docente e de compartilhamentos.

Teste local de consultas simultâneas não certifica capacidade para milhares de usuários. Para produção, medir latência/filas, dimensionar pools e o trabalhador, observar grupos grandes e homologar a infraestrutura representativa.

## Banco de dados e migração

Foi adicionada **somente no Capacita**:

`backend/src/main/resources/db/migration-capacita/V18__adaptive_paths_and_digital_passport.sql`

Ela acrescenta emissor, validade e revogação às conquistas; política adaptativa e validade de novas conquistas aos módulos; e cria:

- `learning_module_prerequisites`;
- `learning_adaptive_goals`, incluindo cache privado da recomendação;
- `learning_passport_shares` e `learning_passport_share_items`;
- `learning_radar_invalidation_outbox`.

Não remove dados, não substitui migrações aplicadas e não cria novas credenciais sem aprovação. Há índices para competências válidas, objetivos do aluno, compartilhamentos e fila pendente. As FKs são locais ao Capacita. Oportunidades e Empresas não receberam novas tabelas/migrações nesta etapa; suas APIs acessam exclusivamente seus bancos locais e usam HTTP para os contratos autorizados.

V18 foi validada em banco vazio e em atualização a partir do histórico anterior; a versão local do Capacita foi confirmada como 18 após a suíte. Mantidos Oportunidades V16 e Empresas V13.

## Configuração e execução

Reutilizados os bancos/JWT separados, o catálogo e `RADAR_INTERNAL_API_URL` / `RADAR_BRIDGE_SECRET` já existentes. O segredo precisa ser configurado com segurança no servidor e nunca enviado ao navegador ou ao chat. Não foi criada integração paga, exigência de SMTP ou serviço externo de emissão de certificado.

Novas opções no exemplo de ambiente:

```text
PASSPORT_DELIVERY_ENABLED=true
PASSPORT_DELIVERY_DELAY_MS=30000
```

A chave interna e a URL devem permitir os contratos protegidos entre as áreas. Desabilitar o trabalhador mantém as invalidações pendentes até nova ativação; a checagem de validade das evidências continua acontecendo nas análises.

Para desenvolvimento, reinicie o backend atualizado a partir de `backend` com `.\mvnw.cmd spring-boot:run`; inicie o frontend em `frontend` com `npm run dev`. As migrações pendentes são aplicadas nas conexões habilitadas. Os artefatos finais também foram compilados.

Antes de atualizar produção, faça backup separado dos bancos e mantenha o armazenamento anterior de arquivos. Não use `flyway clean`, remoção de volumes ou exclusão de registros para instalar a funcionalidade. Instalar a migração e atualizar os serviços deve fazer parte de uma implantação planejada, sem expor segredos ou habilitar acessos indevidos.

## Testes e resultados

| Verificação executada | Resultado |
|---|---|
| Suíte completa de backend | **138 testes aprovados**, zero falhas, erros ou testes ignorados; inclui mídia real com FFmpeg/FFprobe |
| Reexecução dos fluxos afetados e empacotamento | **18 testes aprovados**, incluindo avaliações, Radar e passaporte |
| Verificação final de expiração/revalidação e novo pacote | **8 testes aprovados**; JAR final gerado com sucesso |
| Suíte completa de navegador | **70 cenários aprovados**, incluindo regressão das etapas anteriores |
| Verificação final dos módulos novos | **6 cenários aprovados**, após posicionar a navegação acima do conteúdo e adicionar atualização do passaporte |
| Testes unitários existentes do frontend | **7 aprovados** |
| Lint e compilação TypeScript/Vite | Aprovados; frontend final gerado |
| Migrações | Instalação vazia e atualização validadas; Capacita local em V18 |
| Concorrência | **20 consultas HTTP simultâneas da trilha**, todas autorizadas corretamente, lote local em **567 ms** |
| Acessibilidade | Axe sem violações nas regras A/AA verificadas nos novos painéis; teclado e larguras 1440/390 px testados; telas móveis inspecionadas |
| Integridade do diff | `git diff --check` sem erros de espaços em branco; alterações anteriores preservadas |

A suíte geral foi seguida por reexecuções específicas dos ajustes finais. Os números não devem ser somados como testes distintos. As medições de concorrência são de uma máquina local com PostgreSQL; não representam p95, capacidade homologada de produção ou garantia para milhares de usuários simultâneos.

Cenários cobertos: lacunas corretas; domínio sem repetição; revisão após reprovação; dependências circulares e pré-requisitos no servidor; validade de novas conquistas; emissão exclusivamente após aprovação real; envio repetido idempotente; expiração e revogação; seleção/consentimento; acesso do destinatário correto; rejeição de aluno, analista e outro recrutador; bloqueio de tentativa de falsificar destinatário; minimização de dados; invalidação do cache; falha e retentativa durável; isolamento das três sessões; regressão das videoaulas, avaliações, biblioteca, mural e Hub.

Na conferência final foi corrigida a recomendação de uma prova já vinculada a conquista vencida/revogada ou que não comprova o nível atual. Ela passa a pedir nova comprovação ao responsável, sem sugerir que repetir a mesma avaliação restaura automaticamente o registro anterior. Esse comportamento e a expiração foram revalidados pelo teste HTTP.

Evidência resumida sem credenciais: `ops/priority4-validation-2026-10-30.json`.

Os testes HTTP usam schemas temporários nos três PostgreSQL e os removem ao encerrar. Cursos, alunos, empresas e lições sintéticas existem somente nessas fixtures. A emissão testada percorre a avaliação real, inclusive reprovação sem conquista e aprovação com geração idempotente; não se limita a criar uma linha de credencial para simular o fluxo.

Comandos principais:

```powershell
# Em backend, configurar as ferramentas locais de mídia para os testes existentes:
$env:CAPACITA_FFMPEG = 'CAMINHO_LOCAL_DO_FFMPEG'
$env:CAPACITA_FFPROBE = 'CAMINHO_LOCAL_DO_FFPROBE'
$env:PASSPORT_DELIVERY_ENABLED = 'false' # testes controlam a entrega explicitamente
.\mvnw.cmd test
# Em frontend:
npm run lint
npm test
npm run build
npm run test:e2e -- --workers=3
```

## Arquivos criados e modificados nesta etapa

Criados no backend, sob `src/main/java/com/trampolim/api/`:

- `education/AdaptiveRules.java`, `AdaptiveService.java`, `AdaptivePassportController.java`, `AdaptivePassportErrors.java`;
- `education/PassportService.java`, `PassportOutbox.java`, `PassportVerificationController.java`;
- `company/CompanyPassportController.java`;
- `radar/RadarInvalidationController.java`.

Criados: migração V18; testes `education/AdaptiveRulesTest.java` e `AdaptivePassportHttpPostgresTest.java`.

Modificados no backend: `education/AssessmentService.java`, `EducationRadarController.java`, `LearningPerformanceService.java`, `CapacitaController.java`; `config/SecurityConfig.java`; `application.properties`; `.env.example`; `radar/RadarMigrationPostgresTest.java`.

Criados no frontend: `features/learning/AdaptivePanel.tsx`, `PassportPanel.tsx`, `PassportVerification.tsx`, `AdaptiveModulePolicy.tsx`; `e2e/adaptive-passport-journey.spec.ts`.

Modificados no frontend: `features/learning/LearningPortal.tsx`, `PerformancePanel.tsx`, `RadarExportPanel.tsx`, `assessmentTypes.ts`; `features/account/CompanyDashboard.tsx`; `App.tsx`. Foram adicionados módulos dentro das rotas já existentes, além dos parâmetros de navegação `credential` e `passport` para verificação. Rotas antigas foram preservadas.

Documentação: este relatório, `README.md` e `ops/priority4-validation-2026-10-30.json`. Outros arquivos alterados anteriormente no Git foram preservados e não são atribuídos a esta entrega.

## Limitações e preparação para produção

- Regras pedagógicas iniciais são transparentes e não usam modelos de inferência sobre deficiência, características sensíveis ou contratação. Professores ainda precisam configurar pré-requisitos, mapear competências e publicar materiais reais.
- Recomendações refletem os conteúdos publicados disponíveis; não há geração fictícia de aulas ou provas. Não é um sistema de tutoria automática com exercícios gerados por IA.
- Credenciais são internas, verificáveis online, sem reconhecimento oficial presumido, assinatura interoperável ou validação offline. Identificação opcional compartilhada é autodeclarada.
- A integração entre contas depende do consentimento e do mecanismo de códigos existente. Novas credenciais não são enviadas indiscriminadamente a outras trilhas ou recrutadores.
- Revogação impede novas consultas/autorização de uso; não controla cópias obtidas anteriormente. Políticas de retenção, atendimento e conferência de identidade devem ser homologadas pelo responsável da plataforma.
- Houve verificação automática WCAG e uso de teclado, além de inspeção das telas móveis; não é certificação integral de acessibilidade. Ainda é necessária homologação com leitores de tela e usuários de tecnologias assistivas.
- Para produção: TLS, gestão de segredos, observabilidade da fila/latência, recuperação de backups e testes de carga representativos. Limitadores em memória são por instância; proteção distribuída pode ser aplicada no gateway existente. Nenhum ambiente externo foi alterado.
