# Observabilidade — 12/11/2026

## Diagnóstico e implementação

Reutilizados Actuator, Micrometer, Prometheus, métricas HTTP/JVM/Hikari e o filtro que protege coleta por chave. A base possuía correlação por requisição e histogramas, mas readiness considerava apenas o estado do processo; logs eram texto e faltavam painel, ligação de regras ao coletor e sinais das entregas persistidas.

Implementados JSON ECS nativo, correlação autenticada em POSTs internos do Radar, readiness estrita dos bancos habilitados e sondagem periódica por trilha. Banco secundário habilitado sem datasource agora é indisponível, não desativado. Liveness e contrato legado de /api/health preservados.

Filas de comunicação: quantidade pendente/falha, atraso de itens vencidos, estado da coleta, duração/resultado do worker. Falha de amostragem é explícita; métricas não consultam banco durante scrape. Tags limitadas a trilha, pool e resultado; não incluem usuários, conteúdo, IDs nem URLs. Sem novas dependências ou migrações.

Infraestrutura opcional versionada: Prometheus, Alertmanager, Grafana; painel de 12 visualizações, 12 regras (duas de gravação, dez alertas), provisionamento e roteiro de operação. Portas de interface somente loopback, segredos por arquivo e coleta na rede privada. Não foi iniciada esta pilha nem implantado em produção.

## Validações executadas

- Testes iniciais: nove aprovados, posteriormente substituídos pela seleção ampliada.
- Harness PostgreSQL isolado: 32 testes aprovados, zero falhas/erros/ignorados; migrações existentes executadas separadamente em schemas temporários dos três bancos; Maven verify/build aprovado. Inclui regressão de comunicação, timeout do cliente, health, correlação, readiness, métricas e porta HTTP real de gerenciamento.
- Revalidação adicional: quatro testes de ObservabilityPostgresTest, incluindo consultas reais das filas e nomes dos três pools; não somar aos 32 como testes distintos (há sobreposição).
- Prometheus oficial v3.9.0: promtool check rules aprovou 12 regras; promtool test rules aprovou indisponibilidade/recuperação de alvo e banco e banco desativado.
- docker compose config --quiet aprovado com arquivos sintéticos temporários, removidos depois; não inicia serviços nem envia mensagens.
- JSON ECS observado nos logs dos testes. Testes verificam omissão de detalhes dos health checks, coleta negada sem chave, endpoints sensíveis indisponíveis, rejeição de correlação pública e limpeza de MDC.
- git diff --check sem erros. Frontend não alterado nesta etapa. Não houve teste de carga/SLO de 30 dias nem auditoria integral dos logs de todas as bibliotecas.

## Limites e produção

Tracing OpenTelemetry completo foi avaliado e não ativado: correlação de logs não é tracing. Exportador/coletor só após necessidade, destino, sampling e política de dados definidos. Não há nova fila externa.

Grafana foi preparado por provisionamento, sem homologação visual da pilha em execução. Alertas possuem receptor local; entrega externa e teste de plantão permanecem pendentes. Também faltam probe externo de usuário, logs centralizados, monitoramento específico dos demais outboxes/jobs e definição operacional final de SLO. As metas propostas são referência, não disponibilidade medida. Alterações locais anteriores de verificação empresarial foram preservadas.

Manual de configuração, indicadores, privacidade, limitações e investigação: [ops/monitoring/README.md](../../ops/monitoring/README.md). Evidência resumida: [JSON](../evidencias/observabilidade-2026-11-12.json).

## Arquivos desta etapa

- backend/src/main/java/com/trampolim/api/config/OperationalRequestFilter.java
- backend/src/main/java/com/trampolim/api/config/OperationalDatabaseMetrics.java
- backend/src/main/java/com/trampolim/api/config/TrailReadiness.java
- backend/src/main/java/com/trampolim/api/config/HealthController.java
- backend/src/main/java/com/trampolim/api/radar/RadarBridge.java
- backend/src/main/java/com/trampolim/api/communication/CommunicationStore.java
- backend/src/main/java/com/trampolim/api/communication/CommunicationWorker.java
- backend/src/main/resources/application.properties
- backend/.env.example
- backend/src/test/java/com/trampolim/api/config/OperationalRequestFilterTest.java
- backend/src/test/java/com/trampolim/api/config/TrailReadinessTest.java
- backend/src/test/java/com/trampolim/api/config/ObservabilityPostgresTest.java
- backend/src/test/java/com/trampolim/api/communication/CommunicationWorkerMetricsTest.java
- ops/.gitignore
- ops/prometheus.example.yml
- ops/alerts.example.yml
- ops/monitoring/alertmanager.yml
- ops/monitoring/alerts.test.yml
- ops/monitoring/compose.yml
- ops/monitoring/dashboard.json
- ops/monitoring/datasource.yml
- ops/monitoring/provider.yml
- ops/monitoring/README.md
- docs/TRAMPOLIM.md
- docs/historico/README.md
