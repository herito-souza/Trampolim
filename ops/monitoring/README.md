# Monitoramento operacional

A API já inclui Actuator/Micrometer/Prometheus. O conjunto opcional usa Prometheus, Alertmanager e Grafana; não instala agentes externos nem altera bancos. Logs JSON ECS são emitidos no console pela configuração nativa do Spring Boot.

## Inicialização

1. Inicie a aplicação e seus bancos conforme o compose existente. Descubra a rede privada com docker network ls; não publique a porta de gerenciamento na internet.
2. Configure MONITORING_KEY (mínimo 32 caracteres) na API. Grave o mesmo valor em arquivo privado fora do repositório. Não coloque a chave em comandos, URLs ou documentação.
3. Configure MANAGEMENT_ADDRESS=0.0.0.0 apenas dentro da rede privada dos contêineres. Para execução direta no computador, mantenha 127.0.0.1.
4. No PowerShell, defina TRAMPOLIM_APP_NETWORK com o nome real da rede da aplicação; MONITORING_KEY_FILE e GRAFANA_PASSWORD_FILE com caminhos absolutos para arquivos privados. O segundo arquivo deve conter uma senha forte exclusiva.
5. Execute docker compose -f ops/monitoring/compose.yml config --quiet e depois docker compose -f ops/monitoring/compose.yml up -d.
6. Abra localhost:3000 (usuário inicial admin e a senha do arquivo). Prometheus em localhost:9090 e Alertmanager em localhost:9093. As interfaces estão vinculadas somente ao loopback.
7. Confirme que os dois alvos api1:9001 e api2:9001 estão UP. Ajuste os nomes em ops/prometheus.example.yml se sua implantação tiver outra topologia.

A configuração provisiona o datasource e o painel Trampolim — Operações. Retenção Prometheus: 15 dias. Volumes guardam métricas, configuração do painel e silenciamentos. Proteja e faça backup dos volumes; dados agregados também exigem controle de acesso. Dimensione disco conforme número de séries e réplicas.

Alertmanager recebe alertas, mas o receptor padrão é local, sem envio de e-mail/WhatsApp. Antes de depender de plantão, configure um destino aprovado e faça um teste de disparo e recuperação. Nenhuma mensagem externa é enviada por este conjunto padrão. Imagens estão fixadas por versão; revise atualizações de segurança antes de produção.

## Sinais e limites

- HTTP: contagem, duração e histogramas automáticos; tags de rota devem ser modelos, nunca IDs ou parâmetros. Regras gerais excluem health, Actuator, streams e rotas cujo nome indica mídia. Revise a exclusão conforme novos endpoints forem criados. Uploads sem essas palavras podem exigir regra específica.
- Pools: Hikari fornece conexões ativas, ociosas, pendentes e limites por pool. O painel não contém credenciais.
- Bancos: trampolim_database_up por trilha: 1 disponível, 0 indisponível, -1 não habilitada/ainda não amostrada. Amostragem a cada 30s, fora da coleta Prometheus. Cada probe usa a conexão da própria trilha. Timeout de aquisição segue Hikari; isValid usa 2s.
- Filas: communication_queue_pending/failed e communication_queue_oldest_due_seconds observam entregas persistidas. A idade conta desde next_attempt_at vencido, não desde a criação; digests futuros não são atrasos. Falha de consulta gera up=0 e NaN nas contagens, nunca fila vazia falsa. A consulta agregada ocorre a cada 30s, não por scrape. Custos crescem com retenção; acompanhe o plano SQL antes de reduzir intervalos.
- Worker: communication_worker_duration_seconds por trilha e outcome; communication_worker_failures_total e communication_retention_failures_total. Desativar communications.worker-enabled remove este worker e as métricas da sua fila.
- JVM/CPU/SSE: métricas existentes reutilizadas. Não há fila externa instalada.

Liveness não depende do banco (evita ciclos de reinício). Readiness exige todos os bancos habilitados, inclusive quando um bean esperado está ausente. Em implantação única servindo as três trilhas, uma trilha indisponível retira a instância da prontidão global. O /api/health mantém sua resposta legada: pode retornar 200 DEGRADED se um banco secundário falhar; para readiness use /actuator/health/readiness.

## Correlação e privacidade

Cada requisição recebe um novo X-Request-ID gerado no servidor. Logs de falha/lentidão incluem esse identificador no MDC. Chamadas internas POST do Radar propagam correlationId somente com segredo de integração válido; um cabeçalho público não pode escolher o identificador raiz. O identificador de cada salto continua distinto. Não há propagação de sessão entre trilhas.

Não registre corpos, consultas, Authorization, cookies, senhas, URLs com tokens, nomes ou e-mails. A estrutura JSON não anonimiza mensagens arbitrárias de bibliotecas. Revise níveis de log de dependências e retenção antes de produção. Não publique logs brutos de testes como evidência.

Tracing distribuído completo foi avaliado e permanece opcional: o fluxo atual usa principalmente um backend e integrações HTTP controladas. Não há exportador OTLP, coletor, agente ou backend de traces configurado. correlationId não é um trace OpenTelemetry. Só ativar sampling/exportação após definir destino privado, retenção e política de atributos. Captura automática de cabeçalhos e baggage deve permanecer desabilitada.

## Indicadores propostos (não resultados medidos)

Disponibilidade de API: 1 - respostas 5xx / total de respostas, por janela de 30 dias, excluindo probes/streams/mídia. Sem tráfego é desconhecido. 4xx e 429 devem ser analisados separadamente; falhas antes da API exigem probe externo independente. Objetivo inicial sugerido: 99,5%, sujeito a decisão operacional. Latência de referência: p95 <=500ms para APIs comuns; não aplicar a uploads ou SSE. Alertas atuais usam janela de 5min, não comprovam SLO de 30 dias. up mede sucesso do scrape, não disponibilidade percebida pelo usuário.

## Investigação

1. Alvo DOWN: verificar processo, rede, chave, porta privada e coleta antes de concluir que o usuário está sem acesso.
2. Banco DOWN/readiness 503: identificar trilha pelo gauge; verificar conexão, limite, timeout e infraestrutura. Não reiniciar todos os bancos nem restaurar backup automaticamente. Confirmar recuperação com readiness e métrica.
3. Erros/latência: filtrar por instância e rota no Prometheus; correlacionar X-Request-ID nos logs privados. Comparar fila do pool, JVM e CPU. Não copiar dados pessoais para chamados.
4. Entregas atrasadas/falhas: verificar queue_up, worker, estados e retentativas existentes. Não reenviar manualmente em lote: preservar idempotência e preferências. FAILED exige investigação; falhas resolvidas precisam do procedimento funcional apropriado, não alteração direta por monitoramento.
5. Falta de dados: conferir targets, relógio, coleta e worker habilitado. Não interpretar NaN/ausência como zero.
6. Incidente: registrar período, impacto, trilhas, mudanças e evidências sanitizadas; validar recuperação e elaborar prevenção.

## Validação

Execute os testes Java de ObservabilityPostgresTest, TrailReadinessTest, OperationalRequestFilterTest e CommunicationWorkerMetricsTest pelo harness isolado ops/scripts/Test-BackendSecurity.ps1. O harness executa migrações em schemas temporários dos três bancos locais, sem dados de produção.

Valide regras com a imagem oficial Prometheus usando /bin/promtool check rules e /bin/promtool test rules; monte ops em /work, diretório /work/monitoring, arquivo alerts.test.yml. Testes cobrem indisponibilidade/recuperação do alvo e de banco e banco desativado. Painel está em dashboard.json.

## Pendências operacionais explícitas

Não foi implantado em produção. Precisam de configuração: rede/segredos locais, receptor real de alertas, probe externo de usuário, hospedagem/HTTPS/backup do monitoramento e coleta central de logs. Painel provisionado por arquivo requer homologação visual no ambiente final. Outboxes de denúncias, carreira e passaporte, processamento de vídeo/PDF e snapshots do observatório ainda não possuem cobertura específica de fila/heartbeat neste painel; logs e sinais existentes não equivalem a monitoramento completo desses jobs. Defina alertas adicionais de ausência de série/heartbeat conforme quais jobs ficam habilitados em cada réplica.

Referências: [logs estruturados Spring Boot](https://docs.spring.io/spring-boot/reference/features/logging.html), [testes de regras Prometheus](https://prometheus.io/docs/prometheus/3.9/configuration/unit_testing_rules/).
