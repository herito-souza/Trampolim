# Auditoria técnica, otimização e preparação para escala — Trampolim

Data: 29/10/2026. Fonte de verdade: projeto local `C:\Users\Windows\trampolim-main`.

## Resultado e alcance

Foram implementadas melhorias mensuráveis de carregamento, consultas, paginação, consumo de conexões, segurança operacional e observabilidade. Leituras e notificações foram preparadas para réplicas, preservando bancos e autenticações separados. **Não foi comprovada capacidade de 5.000 usuários, 5.000 SSE, 100 mil contas ou 1.000/3.000 RPS.** Há pendências técnicas e de infraestrutura explicitadas ao final; esta entrega não é uma certificação de capacidade, acessibilidade integral ou segurança absoluta.

O estado inicial, as prioridades e os riscos estão em [AUDITORIA_DIAGNOSTICO_INICIAL.md](AUDITORIA_DIAGNOSTICO_INICIAL.md). A auditoria não reconstruiu o produto, não alterou preços ou critérios de aprendizagem, não trocou tecnologias e não descartou as melhorias locais.

## Diagnóstico confirmado

Inventário: 83 arquivos Java de aplicação, 69 arquivos frontend TS/TSX, 239 declarações de endpoints no código, migrações independentes 13 Oportunidades / 10 Empresas / 15 Capacita após as adições. O inventário não conta endpoints internos do Actuator nem constitui análise manual linha a linha de cada endpoint.

Stack confirmada: Java 21 / Spring Boot 4.1 / Spring Security / JWT / JPA primário e JDBC por trilha; React 19 / Vite 8 / TypeScript 6 / Tailwind 4; PostgreSQL 15.19 local / Flyway. CI já possuía frontend, navegador e backend com três PostgreSQL. A branch permanece `main`, sem divergência em relação à referência local `origin/main` no início; nenhum fetch foi necessário, e as alterações não commitadas foram preservadas.

| Gravidade inicial | Evidência | Correção / estado |
|---|---|---|
| Alta | Todas as páginas importadas estaticamente por App | Carregamento sob demanda por área e fallback acessível de falha |
| Alta | Curso consulta módulos por disciplina e aulas por módulo | Estrutura carregada por três consultas e agrupamento em memória |
| Alta | Busca/lista de cursos e materiais sem navegação paginada no frontend | Páginas de 24 + sentinela, busca no servidor, debounce e cancelamento |
| Alta | Feed de estágios integral no navegador | Cursor por ID e filtros no servidor; chamadas antigas continuam compatíveis |
| Alta | Pools criados manualmente sem binding específico | Limites e timeouts de aquisição configuráveis por banco |
| Alta | Estruturas de tentativas de autenticação podiam crescer com chaves ativas ilimitadas | Limitador local com capacidade, expiração e rejeição sem expulsar limites ativos |
| Alta | Integrações externas sem timeout explícito no cliente | Conexão de 5 s e leitura de 15 s (pagamentos) / 45 s (entrevista); sem retry automático de comandos |
| Alta | Sem métricas privadas padronizadas e correlação | Actuator/Prometheus protegido, probes, pools/JVM/HTTP/SSE e X-Request-ID |
| Média | Dados públicos de seções ocultas eram buscados ao entrar no Capacita | Busca somente no módulo correspondente e aborto ao trocar |
| Média | Cursor reaproveitado ao reabrir estágios podia anexar resultados duplicados | Reinício da página e união por ID; teste de regressão |
| Média | Jobs de catálogo/e-mail duplicariam em réplicas | Worker designado via configuração; não é lease distribuído nem fila |
| Média | Mídia local e processamento síncrono limitam crescimento | Volume compartilhado privado e limites operacionais preparados; object storage/filas ainda pendentes |
| Média | Listas/relatórios legados de outras áreas continuam amplos | Contratos preservados, plano de evolução e limites de capacidade documentados; não houve remoção ou truncamento silencioso |

Não foi comprovado que um endpoint público ou uma integração estivesse obsoleto. Nenhum foi eliminado por ausência de referências. Foram removidos os dois tipos/mapas duplicados de controle de tentativas nos controllers de autenticação e seus imports, substituídos por utilitário testado. Não foi aplicada memoização, Redis, RabbitMQ, microsserviços ou Kubernetes indiscriminadamente.

## Implementação

### Frontend

App mantém as rotas e carrega as páginas de cada trilha sob demanda. A página institucional permanece pronta no pacote inicial. `PageBoundary` evita tela branca em falha de importação e oferece recarregamento sem expor informações internas.

Cursos: busca por título/categoria no servidor, debounce 300 ms, cancelamento por AbortController, 24 registros exibidos e 25 solicitados para detectar a próxima página. A consulta de identidade não é repetida por simples mudança de página; a revalidação após alterações de curso/permissões permanece. O modo StrictMode de desenvolvimento pode iniciar e abortar uma montagem adicional; não é garantia de exatamente uma chamada em desenvolvimento.

Conteúdos por etapa: limite/página/busca no servidor. Estágios: filtros e cursor por ID, carregamento incremental, reinício ao reabrir e deduplicação defensiva. Estados vazios distinguem ausência na página dos dados globais. Falhas de uma seção pública não invadem a área de aprendizagem. No logout/troca de conta, o progresso privado e os campos de confirmação de exclusão são limpos; uma resposta atrasada da conta anterior não restaura o progresso na tela. Os fluxos de currículo, diagnóstico e filtros avançados existentes foram preservados.

### Backend e bancos

As consultas de estrutura de curso agora carregam disciplinas, módulos e aulas em lote, mantendo ordem, status, propriedade e moderação. Rascunhos não aparecem ao estudante. A autorização continua anterior à leitura; não foi colocado cache em permissões ou dados privados.

As assinaturas Java e os contratos antigos de conteúdo/estágio foram preservados por overloads. Novos parâmetros são opcionais e validados. Chamadas novas usam limites; chamadas legadas sem limite continuam compatíveis. Não se afirma que todas as APIs do ecossistema estejam paginadas.

Cada HikariDataSource recebe seu próprio grupo de configuração. Valores iniciais: máximo 10 / mínimo 1 por banco e instância, aquisição 5 s / validação 2 s. Conexões por banco precisam ser orçadas por número de réplicas. Não se aumentou o limite global do PostgreSQL.

Migrações adicionadas:

- Capacita `V15__bounded_course_feed_indexes.sql`: feed público por data/ID e feed do professor por proprietário/data/ID.
- Empresas `V10__public_internship_cursor_index.sql`: índice parcial para estágios publicados e pagos por cursor.
- Oportunidades: nenhuma migração nova, mantendo o histórico V1–V13.

Não foram alteradas migrações já aplicadas. O upgrade educacional V14 → V15 foi exercitado com 20 mil cursos, sem alteração da contagem. As duas migrações foram exercitadas em inicializações de bancos/schemas de teste. Ao encerrar a validação, os históricos públicos locais estavam em Oportunidades V13, Empresas V10 e Capacita V15; os índices adicionais foram aplicados localmente durante as inicializações. Um schema fictício de um teste interrompido foi removido e nenhum dos schemas próprios identificados nos logs permaneceu. Não houve remoção de dados de aplicação. Criação de índices em produção exige avaliar bloqueios e janela; instruções em `ops/OPERACAO_E_ESCALA.md`.

### Segurança e operação

`BoundedRateLimiter` preserva janelas e máximos anteriores, limita a 10 mil chaves por instância, expira entradas e rejeita novas chaves quando saturado. O teste concorrente confirma 12 aceitações para 100 tentativas no mesmo bucket. Não encerra sessões válidas nem muda chaves JWT. Outros limitadores específicos ainda devem ser considerados no dimensionamento global.

O cliente HTTP reutilizado possui conexão/leitura limitadas e não segue redirects automaticamente. O teste de timeout usa somente servidor local lento. Não houve chamada real de cobrança ou IA. Pagamento continua dependendo da confirmação do provedor, valor validado no backend e condições de publicação existentes.

Métricas: JVM/CPU, HTTP e histogramas, pools; SSE acrescenta conexões, capacidade, envios em andamento e polling. Portas/chaves e rede privada foram configuradas, sem exposição de env, heapdump ou configurações. Foi observado 403 sem chave, 200 com chave e liveness 200. Gauges de pool e SSE apareceram no endpoint real. A chave de monitoramento não vai ao navegador.

Os logs usam campos chave/valor e identificador aleatório de requisição. O filtro não registra corpos, URLs privadas, queries, tokens ou identidades. Não há tracing distribuído completo implantado. Os logs e métricas ainda precisam de coleta/retensão em produção.

### Escala e infraestrutura

Dockerfiles em estágios, API com usuário sem privilégios, ferramentas reais de vídeo/PDF, configurações de shutdown, rede privada e volume compartilhado. Configuração de duas instâncias para leituras, Nginx com SSE sem buffering, uploads sem dupla retenção no proxy, timeouts e proibição de retry de comandos. Assets com hash recebem cache longo e compressão gzip no proxy; HTML revalida, dados privados usam no-store.

Autenticação/gravações/uploads ficam na API 1 para não multiplicar controles ainda locais. API 2 recebe leituras. Esse é um preparo incremental de escala de leitura, **não alta disponibilidade completa de gravações**. Jobs de catálogo/e-mail ficam em uma única instância. Não foram instalados serviços pagos, Prometheus/Grafana, broker ou storage externo.

## Medições reais

### Pacote inicial

| Medida | Antes | Depois |
|---|---:|---:|
| JavaScript inicial | 611,27 kB | 260,91 kB |
| Estimativa gzip do build inicial | 155,58 kB | 79,16 kB |

Redução aproximada de 57,3% em tamanho bruto e 49,1% comprimido. Os códigos das áreas continuam existindo em chunks carregados quando necessários. O PDF.js tem seu próprio pacote de cerca de 436,5 kB; isso não é soma transferida na página institucional. Não foram medidos Lighthouse/LCP/INP em produção ou celular físico.

### Consultas e plano de execução

20 disciplinas, 200 módulos e 1.000 aulas publicadas + 200 rascunhos: **221 → 3 consultas de estrutura**. Há consultas adicionais fixas de autenticação, autorização e cabeçalho do curso; elas não foram suprimidas para melhorar o resultado.

Última execução do ensaio de cinco rodadas com DriverManagerDataSource: mediana **8,56 s → 259 ms**. Uma execução anterior encontrou 7,74 s → 235 ms. A diferença varia com a máquina; a comparação usa conexões novas por consulta e não equivale ao Hikari em produção. O número constante de consultas é a evidência principal.

`EXPLAIN (ANALYZE, BUFFERS)`, 20.000 cursos fictícios (10.000 públicos): antes Seq Scan + top-N Sort, execução observada 11,093 ms; depois Index Scan no índice publicado, execução observada 0,095 ms. É uma observação local de plano/cache, não uma garantia de ganho proporcional em produção.

### HTTP — ensaio local curto

Uma instância, heap máximo 768 MiB, seis CPUs lógicas, três PostgreSQL locais, três contas fictícias reutilizadas pelos clientes. Mix de curso/lista/notificações/catálogos/perfil/painel empresarial. O curso tinha 1.000 aulas em metadados. Sem tráfego de vídeo/PDF ou provedor externo. Mesmas rotas, fixtures e sequência antes/depois; medições de carga sem as suítes completas rodando simultaneamente.

| Estágio | Clientes | p95 antes (ms) | p95 depois (ms) | RPS antes | RPS depois | Erros servidor/transporte depois |
|---|---:|---:|---:|---:|---:|---:|
| warmup | 10 | 742.55 | 57.19 | 46.35 | 124.01 | 0 |
| nominal | 25 | 1478.33 | 133.80 | 82.36 | 211.85 | 0 |
| stress | 50 | 2532.78 | 411.15 | 82.03 | 231.90 | 0 |
| short-burst | 100 | 4175.55 | 712.05 | 68.66 | 268.95 | 0 |
| short-resistance | 10 | 535.68 | 38.85 | 70.70 | 147.04 | 0 |

Com 25 clientes, p95 133,80 ms e taxa 211,85 RPS, contra 1.478,33 ms e 82,36 RPS. O pico curto de 100 clientes teve p95 712,05 ms: **não atendeu à meta de 500 ms**. Nenhum desses ensaios comprova capacidade sustentada de 1.000 RPS. Os estágios duram poucos segundos e os clientes compartilham contas fictícias; não são 100 usuários independentes. A etapa de 12 s após o pico mostrou recuperação de latência, mas não prova resistência prolongada ou ausência de vazamento de memória.

### Duas instâncias

Execução com dois processos Java, schemas temporários nos três bancos e contas fictícias independentes por trilha. Uma dúvida enviada pela API 1 gerou notificação no professor conectado por SSE à API 2. Após desconexão, a reconexão na outra instância recuperou a segunda dúvida pelo cursor persistido. Logout na API 1 invalidou o token Capacita na API 2; a sessão de Empresas continuou válida. Os resultados estão no JSON de dois nós. Quantidade de conexões foi pequena; não se infere capacidade de milhares.

## Testes e verificações

- Referência inicial backend: 96 testes, sem falhas, quatro casos de mídia ignorados por ferramenta ausente no PATH. O caminho real de FFmpeg/ffprobe foi fornecido nas execuções finais; as verificações não foram desativadas.
- Backend final: **103 testes, zero falhas/erros/ignorados**. Inclui consultas, índices/migração, controles concorrentes, timeout, autenticação e isolamento, pagamentos/publicação, biblioteca, aulas, mural, avaliações e competências conforme a cobertura existente.
- Frontend: compilação TypeScript/Vite, lint e sete testes unitários aprovados.
- Suíte de navegador: 45 cenários aprovados. O encerramento do servidor temporário precisou de permissão adequada no Windows; apenas os processos de teste foram encerrados, preservando o frontend/backend do usuário.
- Verificações focadas finais de paginação e reabertura: **quatro cenários aprovados** (incluindo cursor e limpeza de progresso no logout; 47 cenários distintos ao combinar com a suíte). A segunda montagem StrictMode é considerada ao medir chamadas; o teste verifica ausência de consultas extras por paginação, sem enfraquecer a proteção do servidor.
- Percurso real professor/aluno com PostgreSQL + FFmpeg + PDF + SSE + prova: **um cenário aprovado em 2,3 minutos**. O roteiro foi adaptado para retomar explicitamente o vídeo após a biblioteca, respeitando a pausa entre módulos. Não houve fabricação de progresso.
- Acessibilidade: os testes de teclado, foco, reflow e axe da suíte permaneceram aprovados. Não foi repetida homologação falada com NVDA/JAWS/VoiceOver nem declarada conformidade integral AA.
- `npm audit` final: zero vulnerabilidades conhecidas reportadas para a árvore consultada. A primeira consulta falhou por restrição de rede e foi repetida com acesso adequado. O inventário Maven foi gerado; não foi executada uma auditoria completa de CVEs Java/imagens nem pentest.
- Benchmark real antes/depois, protocolo de dois nós, autorização das métricas e EXPLAIN executados. k6 inspect, nginx -t e os dois Compose foram validados sem subir a plataforma.
- Imagens completas dos aplicativos, grande carga k6, probe SSE de grande escala, teste prolongado e restauração de backup não foram executados. Não se atribui aprovação a esses itens.

## Evidências e comandos

Evidências sanitizadas em [tools/performance/results](tools/performance/results): JSON de carga antes/depois, dois nós, orçamento de consultas, EXPLAIN, inventário de fontes/dependências e resumo npm. Não contêm tokens/senhas das contas fictícias. Os traces locais de navegador devem continuar privados/ignorados.

Instruções de execução, fixtures e limites: [tools/performance/README.md](tools/performance/README.md). Operação, mídia, métricas, probes, migração, backup e recuperação: [ops/OPERACAO_E_ESCALA.md](ops/OPERACAO_E_ESCALA.md). CI mantém as verificações e preserva evidências sintéticas de banco; a porta de gerenciamento dos testes usa alocação dinâmica pelo Surefire.

## Arquivos desta auditoria

Backend modificado: `pom.xml`, `.env.example`, `docker-compose.yml`, `application.properties`; `config/TrailDataSourceConfig.java`, `SecurityConfig.java`, `CorsConfig.java`; `auth/AuthController.java`, `IndependentTrailAuthController.java`; `education/LearningController.java`, `LearningNotifications.java`, `CapacitaController.java`; `catalog/CatalogMaintenanceService.java`, `JobReminderService.java`, `OpportunityAlertService.java`; `company/PublicCompanyController.java`, `CompanyPaymentController.java`; `user/PremiumController.java`.

Backend criado: `config/OperationalRequestFilter.java`, `config/BoundedRestClients.java`, `security/BoundedRateLimiter.java`, dois arquivos de migração V15/V10; testes `OperationalRequestFilterTest`, `BoundedRestClientsTest`, `BoundedRateLimiterTest`, `CourseQueryBudgetPostgresTest`, `FeedIndexMigrationPostgresTest`; `Dockerfile` e `.dockerignore`.

Frontend modificado: `src/App.tsx`, `src/pages/CapacitaPage.tsx`, `src/features/learning/LearningPortal.tsx`, `.gitignore`, `e2e-integrated/capacita-live.spec.ts`. Criados: `src/components/ui/PageBoundary.tsx`, `e2e/performance-behavior.spec.ts`, `Dockerfile`, `.dockerignore`, `ops/nginx.conf`.

Operação e documentação: `ops/compose.local.yml`, `.env.example`, `.gitignore`, `prometheus.example.yml`, `alerts.example.yml`, `OPERACAO_E_ESCALA.md`; scripts, exemplos e evidências `tools/performance`; diagnóstico inicial, este relatório, README e CI. Arquivos das etapas educacionais anteriores permanecem, inclusive alterações locais já existentes antes da auditoria. Nenhum comando destrutivo de Git ou push foi executado.

## Pendências reais e próximos incrementos

| Prioridade | Pendência | Motivo / condição de conclusão |
|---|---|---|
| Alta | Homologação 500–5.000 usuários, 1.000/3.000 RPS e 100 mil contas | Precisa de dataset representativo, contas independentes e infraestrutura isolada; não forçar esta máquina |
| Alta | Paginação de contratos legados de Oportunidades, feed geral empresarial e relatórios administrativos | Filtros avançados, recomendações e consumidores atuais exigem evolução compatível; contratos não foram truncados para fingir escala |
| Alta | Processamento durável/outbox de e-mails, relatórios e mídia | Caminhos atuais ainda têm trabalho síncrono/local; fila completa, retentativas, lease e observabilidade devem vir juntos |
| Alta | Escritas com failover seguro e cotas globais | Desenho atual concentra comandos na API 1; controles específicos por processo/proxy não equivalem a quota global distribuída |
| Alta | Object storage/CDN ou armazenamento multi-host de mídia | O volume Compose cobre somente nós no mesmo host; mídia ainda passa pela API |
| Alta | Produção: SMTP/MP, TLS, segredos, restore e monitoramento | Credenciais e infraestrutura não configuradas; não houve implantação ou transação real |
| Média | CVEs de Java/imagens, pentest, políticas de sessão/token no navegador | npm audit não substitui SCA completo; migração para cookies/refresh exige plano de compatibilidade, não troca silenciosa |
| Média | Teste prolongado e observação de memória/IO | Ensaio de segundos não comprova resistência nem ausência de vazamentos |
| Média | Homologação assistiva e dispositivos físicos | Verificações automáticas e teclado não certificam toda a experiência |

Banco Oportunidades continua sendo dependência primária de inicialização do monólito; indisponibilidade de bancos secundários possui o tratamento já existente. Preparar réplicas de leitura não equivale a independência operacional completa dos três módulos. O código local melhorou e as medições demonstram efeito, mas a liberação comercial em grande escala continua condicionada aos itens acima.
