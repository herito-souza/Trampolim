# Testes de desempenho reproduzíveis

## O que foi executado

`results/baseline-local.json` e `results/after-local.json`: cinco estágios curtos, 10/25/50/100/10 clientes HTTP, três contas fictícias, três bancos locais em schemas temporários, uma API Java com heap máximo 768 MiB, Windows com seis CPUs lógicas. O dataset inclui um curso com 20 disciplinas, 200 módulos e 1.000 aulas publicadas (somente metadados). Não é teste com 100 usuários distintos nem distribuição de vídeo/PDF. Os resultados não comprovam 1.000 RPS ou 5.000 usuários.

`results/multi-node-local.json`: duas APIs com os mesmos bancos temporários e chaves por trilha. Publicação de dúvida por API, entrega ao professor no outro nó, reconexão por cursor, revogação via logout e preservação da sessão empresarial. Não é um teste de 5.000 SSE.

`results/course-query-budget.json`: comparação de consultas de estrutura com cinco repetições, PostgreSQL real e conexões novas por consulta. Não equivale à latência de produção com Hikari.

`results/feed-explain-analyze.txt`: 20.000 cursos fictícios, upgrade V14 → V15, 20.000 registros preservados, plano antes/depois. São observações locais, não uma garantia estatística de latência.

## Benchmark local limitado

Requisitos: três contêineres de banco de desenvolvimento em execução, Java 21, Python 3, Docker e backend empacotado. O script usa somente os bancos, usuários e portas de desenvolvimento explicitados em seu cabeçalho, sem ler contas existentes. Cria e remove schemas próprios com nomes aleatórios; recusa portas de teste ocupadas. Não use o script com credenciais ou ambiente de produção.

```powershell
cd C:\Users\Windows\trampolim-main
$env:AUDIT_ALLOW_LOCAL = '1'
python3.14 tools/performance/local_benchmark.py --jar backend/target/backend-0.0.1-SNAPSHOT.jar --output tools/performance/results/retest.local.json
python3.14 tools/performance/local_benchmark.py --jar backend/target/backend-0.0.1-SNAPSHOT.jar --multi-node --protocol-only --output tools/performance/results/protocol.local.json
```

As portas reservadas são 5098, 5099 e, no teste com dois nós, 5096. Não execute em paralelo com outros benchmarks. O runner desativa e-mail, credenciais de provedor e jobs de catálogo somente nos processos de teste. Não altera os serviços iniciados pelo usuário. Os schemas e processos temporários são removidos no encerramento normal; uma interrupção forçada exige conferir os nomes registrados antes de limpar exclusivamente os próprios fixtures.

O JAR antigo de referência foi preservado temporariamente na máquina durante a medição; seu hash está no JSON. Reproduzir o “antes” exige esse artefato ou a versão equivalente. A versão antiga não deve ser reconstruída a partir do Git remoto presumindo que ele continha as melhorias locais.

## k6 — metas maiores em ambiente isolado

`ecosystem.k6.js` teve sua sintaxe e opções validadas com `k6 inspect`, sem rede nem execução de carga. Os cenários grandes **não foram executados**. Use os limites existentes, sem desativar autorização, cotas ou validação para obter números melhores.

Copie `fixtures.example.json` para `fixtures.local.json` e configure contas/tokens fictícios de cada trilha. Esse arquivo privado é ignorado pelo Git. Os exemplos com `REPLACE_` são deliberadamente rejeitados. Não utilize dados de usuários reais. Prepare cursos publicados, docentes, questões/respostas e empresas de teste aprovadas quando o cenário exigir. IDs de exemplo precisam ser substituídos. Contas que realizam avaliações devem ter condições legítimas de acesso e tentativas disponíveis; o roteiro de escrita executa uma jornada por conta, sem repetir provas indiscriminadamente.

```powershell
$env:ISOLATED_TEST_ENV = 'yes'
$env:FIXTURE_FILE = './fixtures.local.json'
$env:BASE_URL = 'https://SEU-AMBIENTE-ISOLADO'
$env:MODE = 'users'
$env:MAX_USERS = '100'
k6 run ecosystem.k6.js
```

Para estágios até 500/1.000/2.500/5.000, configure `MAX_USERS` e confirme `ALLOW_HIGH_LOAD=yes` somente em infraestrutura apropriada, com autorização. Os estágios crescem gradualmente e permanecem cinco minutos por padrão; altere `DURATION` conforme o ensaio. Garanta contas distintas e volume de dados representativo — reutilizar poucos tokens não comprova capacidade de milhares de usuários.

Para throughput, `MODE=throughput`, `RATE=1000`, depois `RATE=3000`, `MAX_USERS` suficiente e `ALLOW_HIGH_LOAD=yes`. Cada iteração faz uma requisição; `dropped_iterations=0` é obrigatório para não confundir carga solicitada com carga realmente gerada. Objetivos: p95 de leituras <500 ms, erros servidor/transporte <1%, checks >99%. Login usa `MODE=login`; 429 é resultado de limite aplicado e deve ser analisado, não contornado.

`MODE=writes` e `ALLOW_TEST_WRITES=yes` usam ações explícitas do fixture para cadastro/publicação de vaga, dúvidas e prova. Não acionam pagamentos ou serviços de IA. Revise os destinatários/dados e as permissões; parâmetros exemplificativos não são conteúdo real. Publicação empresarial exige empresa de teste aprovada. O envio da prova precisa das alternativas do fixture. As regras de aprovação e de limite de tentativas permanecem no backend.

## SSE e mídia

`sse_probe.py` prepara testes de aceitação/liveness com uma conta Capacita independente por conexão. Recusa carga >100 sem `ALLOW_HIGH_LOAD=yes`; teto 5.000. A ferramenta não foi executada em grande escala e não comprova entrega/reconexão apenas por abrir conexões. Os testes funcionais de dois nós cobrem esses comportamentos em escala pequena.

```powershell
python3.14 sse_probe.py --fixture fixtures.local.json --base https://SEU-AMBIENTE-ISOLADO --clients 100 --seconds 60 --output sse.local.json
```

Mídia precisa de ensaio separado: arquivos sintéticos válidos, Range/206, throughput em bytes, interrupção/retomada, autorização, tempos de processamento e filas, CPU/memória/IO e tráfego. Não misture isso com consultas JSON na meta de 500 ms. Não foram realizados teste prolongado de resistência, certificação de vazamento de memória, 100 mil contas, 5.000 conexões ou injeção de falhas de infraestrutura em produção.

Resistência prolongada requer observar a validade e renovação das credenciais por trilha. Os JWT atuais expiram em duas horas; o runner k6 de leitura usa tokens do fixture e não renova sessões automaticamente. Planeje o ciclo de autenticação em ensaios longos sem desativar expiração ou rate limiting.
