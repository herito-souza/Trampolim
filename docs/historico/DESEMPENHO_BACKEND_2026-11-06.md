# Otimização e capacidade do backend Trampolim — 06/11/2026

## Escopo

Auditoria e otimização incrementais da versão local. Java 21, Spring Boot 4.1.2, PostgreSQL, Spring Data JPA no legado Oportunidades e JdbcTemplate com fontes e transações nomeadas por trilha. Não houve Redis, broker, nova biblioteca, implantação ou modificação de dados pessoais reais. As alterações locais anteriores de segurança e frontend foram preservadas.

Inventário estático de 65 controllers em ops/backend-performance-2026-11-06/endpoint-source-inventory.json. É um inventário de código, não uma prova de disponibilidade/autorização de todas as rotas em produção.

## Gargalos e intervenções

| Caminho | Situação encontrada | Intervenção |
|---|---|---|
| /capacita/learning/performance | Transferência de cada aula e progresso; varreduras repetidas por curso e módulo; três consultas separadas para contagens | Agregação por módulo no PostgreSQL, composição linear em Java e uma consulta de contagens. Mantidos os critérios de conclusão e os campos JSON |
| Sessão de Oportunidades | A consulta de autenticação JPA carregava a entidade inteira, incluindo foto, currículo e hash de senha, em toda requisição autenticada | Projeção somente de ID/e-mail/versão, mantendo a consulta viva e as mesmas condições de autenticação |
| Permissões educacionais | Consulta da conta seguida por outra consulta aos papéis em cada acesso | Uma consulta com EXISTS indexados. Sem cache de permissões ou redução da verificação de conta ativa/e-mail verificado |
| Indicadores de anúncios empresariais | Subconsultas correlacionadas contavam eventos por anúncio sem índice adequado | Índice por listing_id/event_type e índice de ordenação estável das páginas |
| Catálogo legado | Busca de todos os ativos e filtragem de aprovação/validade em memória | Aprovação e validade aplicadas na consulta JPA, antes da paginação |
| Listagens legadas | Ausência de opção de paginação em catálogos/administração e listas empresariais | Parâmetros opcionais limit/page, com validação e ordenação estável. Resposta continua sendo array |
| Histórico de progresso | Ordenação por atividade recente sem índice correspondente | Índice student_id/updated_at/lesson_id |
| Aprovação de avaliações | EXISTS por estudante e avaliação para tentativas aprovadas | Índice parcial de tentativas aprovadas |
| Limitador de requisições | Com mapa cheio, novas chaves provocavam varredura de todas as entradas, sob bloqueio | Fila de expiração: limpeza amortizada, sem varredura integral por tentativa rejeitada; memória limitada |
| Eventos do catálogo | Mapa de endereços só removia expirados e podia continuar crescendo com chaves novas | Reuso do limitador limitado a 10.000 chaves, preservando 240 eventos/hora por endereço |
| Consultas demoradas | Sem prazo uniforme no JdbcTemplate das três trilhas | Prazo configurável de 15 segundos por consulta JDBC; padrão de 15.000 ms para consultas JPA |

## N+1 e filtros

A árvore de curso já utilizava três consultas de estrutura em lote; esse mecanismo e seu teste de orçamento foram preservados. Os problemas principais encontrados nos caminhos exercitados foram subplanos correlacionados sem índice, tráfego de linhas desnecessário e cálculos repetidos em memória, e não uma nova árvore JPA com lazy loading.

Os feeds regionais existentes continuam com cursor e limite de até 50, com municípios oficiais/configuração territorial. Filtros combinados permanecem no SQL, sem regras exclusivas de Irecê. O ensaio inclui município, área, modalidade e texto, além de uma busca textual mais seletiva. Para termos únicos normalizados, uma condição necessária mais barata (minúsculas/transliteração) elimina não correspondências antes da expressão regular. A condição canônica permanece na consulta, preservando acentos, pontuação e frases que atravessam campos; essa equivalência é testada no PostgreSQL. Frases de várias palavras continuam com a verificação anterior.

LIKE com curinga e normalização pode continuar custoso em catálogos nacionais; não foi instalado pg_trgm nem criado um mecanismo de busca sem medição que o justificasse.

## Paginação e compatibilidade

Rotas com parâmetros adicionais:
- GET /api/opportunities/{kind}?limit=20&page=0
- GET /api/admin/catalog/{kind}?limit=20&page=0
- GET /api/company/listings?limit=20&page=0
- GET /api/company/applications?limit=20&page=0

limit aceita 1–200; page aceita 0–1000 e exige limit quando não for zero. Valores inválidos recebem 400. A ordenação usa data e id como desempate; não é executado um COUNT total para cada página. O isolamento da empresa e as permissões de recrutamento são aplicados antes da leitura.

Sem limit, as listas legadas continuam completas para preservar clientes que esperam o array inteiro. Portanto, estes endpoints NÃO se tornaram obrigatoriamente limitados por padrão. A adoção da paginação pelo frontend empresarial e administrativo é uma próxima entrega recomendada; truncar respostas silenciosamente comprometeria indicadores e funcionalidades existentes. Para novos consumidores e busca territorial, preferir os feeds de cursor existentes. OFFSET profundo continua tendo custo e páginas podem se deslocar com inserções concorrentes; não se promete um snapshot de longa duração.

## Migrações preparadas

- Oportunidades V19__catalog_public_order_index.sql: índice parcial por tipo/data/id de publicações ativas e aprovadas.
- Empresas V16__listing_event_count_index.sql: índices de contagem de eventos e de paginação por empresa/data/id.
- Capacita V20__progress_and_passed_attempt_indexes.sql: atividade recente e tentativas aprovadas.

São criações de índices, sem apagar ou reescrever registros e sem mudar migrações anteriores. Cada banco mantém seu histórico. As migrações usam lock_timeout de cinco segundos para não esperar indefinidamente por bloqueios. CREATE INDEX comum pode bloquear escritas durante a construção: em bases grandes é necessária janela planejada e avaliação de construção concorrente em procedimento próprio. lock_timeout não limita a duração da construção já iniciada. Nada foi aplicado aos schemas reais locais ou à produção.

Os índices ocupam disco e encarecem gravações. O prefixo do antigo índice de listagens empresariais também existe no novo índice com desempate por id; sua eventual consolidação deve considerar pg_stat_user_indexes e o custo de escrita, sem remover índices de outros fluxos às cegas.

## Pools, concorrência, arquivos e filas

Mantidos Hikari independentes, padrão de máximo 10 e mínimo 1 por trilha, espera de aquisição de cinco segundos. O teste usa máximo seis por pool de aplicação para revelar contenção. Não houve aumento arbitrário de conexões ou threads. Bancos separados não isolam CPU, heap ou o ciclo de vida deste processo único; o JPA primário de Oportunidades continua sendo uma dependência de inicialização. O ensaio de esgotamento isola Capacita em tempo de execução, não certifica alta disponibilidade de todos os bancos na partida.

Orçamento: para bancos em servidores físicos separados, cada servidor precisa acomodar réplicas × pool máximo daquela trilha, além de manutenção/monitoramento. Se os três bancos dividirem o mesmo PostgreSQL, somar os três pools: padrão de 30 conexões por réplica, mais margem operacional. Mais conexões não substituem CPU, memória ou consultas eficientes.

Uploads de vídeo/PDF já possuem limites de tamanho, semáforos, validação e armazenamento fora do PostgreSQL. A conversão de videoaulas ocorre fora da transação de metadados e revalida autorização ao gravar; esse desenho foi preservado. FFmpeg usa processamento limitado e as videoaulas continuam sujeitas à duração real de 180–300 segundos. As validações de mídia não foram substituídas por simulação.

Notificações já são persistidas e recuperadas em lotes, com limite padrão de 500 conexões SSE por instância e duas por conta. Os remetentes possuem bloqueio por canal para evitar acúmulo de envios simultâneos. Ainda é necessário testar consumidores lentos e múltiplas réplicas em escala maior; não foi alegado suporte ilimitado a conexões em tempo real.

Radar já possui cache por usuário/fonte/vaga/fingerprint, invalidação e limite de entradas, e o passaporte usa outbox PostgreSQL com leases e retentativas. Workers já evitam execuções locais sobrepostas. Não foi adicionada cache de progresso privado ou uma fila externa: não há evidência atual que justifique Redis/RabbitMQ/Kafka. Para múltiplas réplicas, validar leases, idempotência, falhas no meio do lote e distribuição de SSE no ambiente de homologação.

As integrações HTTP já possuem limite de conexões concorrentes e prazos de conexão/leitura. Os testes existentes verificam fonte lenta/indisponível e preservação dos dados quando uma trilha falha. Cobranças e outras mutações não receberam retentativas automáticas novas.

## Configurações

- JDBC_QUERY_TIMEOUT_SECONDS=15 (intervalo aceito: 1–300).
- JPA_QUERY_TIMEOUT_MS=15000.
- Pools existentes: OPORTUNIDADES_DB_POOL_MAX/MIN, EMPRESAS_DB_POOL_MAX/MIN e CAPACITA_DB_POOL_MAX/MIN.
- DB_CONNECTION_TIMEOUT_MS continua com padrão de 5000.

O prazo JDBC é por instrução; não é um prazo global de toda a requisição. Flyway não utiliza o prazo de JdbcTemplate. Ajustar proxy, prazos HTTP e consultas analíticas conforme medições, sem desativar limites apenas para ocultar gargalos.

## Ensaios e resultados

Métricas do benchmark final, com Java 21.0.11, 6 processadores lógicos disponíveis, heap máximo de aproximadamente 2 GiB e PostgreSQL 15.19 local. Dez rodadas após aquecimento para as comparações de serviço e sessão:

| Métrica | Antes | Depois |
|---|---:|---:|
| Consultas JDBC por chamada do serviço de progresso (sem o filtro JWT) | 8 | 5 |
| Linhas recebidas pelo Java por chamada desse serviço | 10.104 | 602 |
| Mediana do serviço de progresso | 151,88 ms | 37,19 ms |
| p95 do serviço de progresso | 180,6 ms | 51,63 ms |
| Mediana da consulta de sessão com binários sintéticos no perfil | 31,07 ms | 3,48 ms |
| p95 dessa consulta de sessão | 62,9 ms | 7,41 ms |
| Materialização dos binários de foto/currículo nessa consulta | 1,25 MiB | Não selecionados |

O teste de SQL confirma que a projeção não seleciona resume_content, photo_content ou password_hash, e que a mudança de token_version aparece na consulta seguinte. Não há cache de autenticação. A igualdade do JSON de progresso anterior/novo é verificada contra a implementação de referência de teste.

Planos específicos EXPLAIN ANALYZE/BUFFERS, não latência ponta a ponta da API:

| Consulta de referência | Antes | Depois |
|---|---:|---:|
| Contagem de eventos dos 20 anúncios empresariais selecionados | 188,8 ms | 1,96 ms |
| 100 registros recentes de progresso | 5,48 ms | 0,26 ms |
| 20 itens do catálogo em ordem de publicação | 24,48 ms | 0,1 ms |
| Busca seletiva por município/área/modalidade/texto | 539,52 ms | 229,57 ms |

A comparação da última linha vem de rodadas locais separadas do mesmo conjunto sintético; os arquivos anteriores foram preservados. Cache de páginas, aquecimento e variabilidade da máquina influenciam os tempos. O plano ainda custa mais na busca seletiva que no catálogo simples; não há alegação de busca nacional de custo constante.

Carga HTTP real com assinatura/verificação JWT e dados isolados:

| Cenário | Clientes | Requisições | p95 | Erros |
|---|---:|---:|---:|---:|
| Filtros seletivos combinados | 4 | 48 | 507,47 ms | 0 |
| Carga inicial | 8 | 320 | 180,48 ms | 0 |
| Pico concorrente | 32 | 320 | 881,43 ms | 0 |
| Ensaio contínuo de 120,07 segundos | 12 | 21.662 | 182,98 ms | 0 |

No pico houve até 25 threads aguardando conexão em um pool; zero erro não significa ausência de contenção. A fase contínua observou média de 180,42 requisições/s, cerca de 253,36 MiB de heap máximo amostrado e 140 coletas. Essas são observações desse ensaio, não limites de capacidade ou prova de ausência de vazamento de memória. Não foram medidos RSS, pausas de GC ou CPU do PostgreSQL em isolamento.

O esgotamento intencional do pool Capacita respondeu 503 em 5.012,43 ms, mantendo Empresas acessível. Após liberar as conexões, Capacita voltou a responder 200. O cancelamento real de uma consulta por timeout também preservou o uso posterior da conexão.

Conjunto: 50.000 itens de catálogo, 100 empresas, 1.000 anúncios, 100.000 eventos empresariais, 500 estudantes, 50 cursos, 10.000 aulas e 49.920 registros de progresso. A identidade candidata recebeu binários sintéticos de 1 MiB de currículo e 256 KiB de foto para não ocultar o custo da leitura antiga. Nenhum arquivo pessoal real foi utilizado.

As URLs da carga usam paginação. O comportamento de clientes legados que pedem o array completo não é representado por essas taxas. Adoção da paginação no cliente é condição importante para expansão.

Suíte completa final: 181 testes aprovados, zero falhas/erros e zero ignorados, seguida por um teste adicional aprovado de isolamento da coleta de estatísticas: 182 casos únicos executados. O Maven verify e o empacotamento Spring Boot passaram. A checagem adicional alterou somente o apoio dos testes; não houve mudança de produção após o verify completo. O pacote está em backend/target/backend-0.0.1-SNAPSHOT.jar.

Foram exercitados autenticação/autorização das trilhas, revogação de sessão, migrações novas e atualizações preservando usuários, paginação, filtros, concorrência de avaliações, idempotência, integrações indisponíveis/lentas, uploads reais de vídeo/PDF e orçamento de consultas da árvore educacional. Nenhum teste existente foi desativado para aprovar a execução. O teste de migração foi atualizado para esperar as novas versões 19/16/20.

A coleta ANALYZE do harness está delimitada por código a schemas perf_UUID, com identificação validada e nomes de tabela qualificados. O teste adicional comprova que outro schema de controle não recebe essas estatísticas e que uma conexão sem o escopo permitido é recusada. As duas camadas de schemas temporários da execução completa foram confirmadas ausentes nos três bancos após a limpeza.

Evidências: metrics.json, verification-summary.json, isolated-tests.json, planos JSON, full-verify.log, harness-isolation.log e testes focados na pasta ops/backend-performance-2026-11-06/. O log initial-verify.log é histórico e contém a expectativa antiga de versão de migração; o resultado final é full-verify.log.


O primeiro pico com seis conexões por pool e espera artificial de um segundo resultou em 318 respostas 200 e duas respostas 503 em 320 requisições, com 32 clientes. Resultado preservado em burst-1s-timeout.json e first-run-metrics.json. A rodada final usa a espera padrão de cinco segundos; não houve aumento do pool para mascarar o problema.

Metodologia: dados sintéticos em três schemas descartáveis, na máquina local; comparação com a implementação anterior copiada somente para uma classe de teste; aquecimento e dez rodadas para o cálculo educacional; planos EXPLAIN ANALYZE/BUFFERS reais; carga HTTP autenticada nas três trilhas, burst, resistência curta, cancelamento SQL e esgotamento/recuperação isolada de um pool. Não há usuários pessoais reais no ensaio.

O cliente de carga e a API compartilham a JVM/máquina. CPU e heap observados pertencem ao processo Java, incluindo o gerador de carga; não representam CPU exclusiva do servidor HTTP ou de PostgreSQL. A resistência curta não substitui horas de teste. O mix de carga é de leitura e utiliza três identidades sintéticas; centenas de contas adicionais compõem os dados de fundo. Não é uma simulação de milhares de pessoas únicas utilizando toda a plataforma.

## Reprodução

No PowerShell, na raiz do projeto, com os três containers PostgreSQL locais ativos:

    .\ops\scripts\Test-BackendSecurity.ps1 -Performance

O tempo pode ser configurado com -SoakSeconds (1–3600 segundos; padrão de 20). A execução final desta entrega usa -Performance -SoakSeconds 120.

O parâmetro adicional é compatível com o script anterior e habilita a medição junto ao Maven verify, usando schemas e armazenamento de testes separados. FFmpeg/FFprobe devem ser configurados em CAPACITA_FFMPEG/CAPACITA_FFPROBE para executar também as integrações de mídia. Não apontar os testes a produção. O benchmark recusa URLs de banco que não sejam loopback e limpa seus próprios schemas após a execução.

Somente o benchmark, a partir de backend:

    .\mvnw.cmd '-Dtest=BackendScalePostgresTest' '-Daudit.performance.enabled=true' test

A execução isolada do benchmark não substitui o verify completo. Evidências ficam em ops/backend-performance-2026-11-06/ na execução pelo script.

## Arquivos desta entrega

Produção: user/SessionAccount.java (novo), user/AppUserRepository.java, security/JwtAuthenticationFilter.java, geography/RegionalQuery.java, catalog/CatalogController.java, catalog/CatalogItemRepository.java, catalog/CatalogMetricController.java, company/CompanyController.java, education/LearningAccess.java, education/LearningPerformanceService.java, education/LearningProgressSummary.java (novo), security/BoundedRateLimiter.java, query/ListWindow.java (novo), config/TrailDataSourceConfig.java, application.properties, backend/.env.example e as três migrações acima. Os caminhos Java são relativos a backend/src/main/java/com/trampolim/api/.

Testes: FixtureStatistics.java e PerformanceHarnessIsolationPostgresTest.java (novos, com verificação de escopo do ANALYZE), SessionProjectionInspector.java (novo), CatalogReadPostgresTest.java, CatalogMetricRateLimitTest.java, LearningProgressSummaryTest.java, BackendScalePostgresTest.java, LegacyLearningPerformanceBaseline.java e ListWindowTest.java (novos), além de CriticalFlowsTest.java, BoundedRateLimiterTest.java, RadarMigrationPostgresTest.java e JwtAuthenticationFilterTrailIsolationTest.java adaptados. Caminhos em backend/src/test/java/com/trampolim/api/ nos respectivos pacotes.

Execução/evidências: README.md (link e comando), ops/scripts/Test-BackendSecurity.ps1 (parâmetro Performance), ops/backend-performance-2026-11-06/ e este relatório. Outros arquivos já modificados anteriormente no diretório não foram atribuídos a esta revisão.

## Próximos passos de infraestrutura

1. Adotar a paginação já disponível nos clientes legados, sem perder totais nem ações de gerenciamento.
2. Repetir carga em homologação separando gerador/API/banco, incluindo gravações, candidaturas, autenticação com hash de senha, pagamento de teste e SSE.
3. Executar resistência de várias horas e falhas de múltiplas réplicas; acompanhar p95/p99, saturação do pool, erros, GC, disco e planos pelo volume real.
4. Dimensionar CPU/disco e storage de mídia com vídeos de celular representativos; avaliar workers dedicados somente quando a saturação observada justificar a mudança de contrato/infraestrutura.
5. Usar métricas Actuator/Prometheus existentes, com acesso privado, alertas de espera do Hikari e consultas lentas. Configurar autovacuum/ANALYZE, backups e restauração por banco.
6. Para busca textual nacional, medir seletividade e custo de LIKE; considerar índice de texto apropriado somente a partir desses dados.
7. Manter limitação global no proxy/gateway quando houver múltiplas instâncias: os limites em memória são por instância. Não presumir que eles constituam um contador global distribuído.

O resumo educacional mantém o teto anterior de 200 cursos iniciados; não é um total irrestrito para um histórico individual maior. Consultas completas de perfil/login ainda usam a entidade, enquanto o caminho frequente de validação JWT utiliza a nova projeção.

A entrega melhora os caminhos medidos. Não certifica capacidade para milhares de usuários nem prontidão de uma infraestrutura de produção ainda não ensaiada.
