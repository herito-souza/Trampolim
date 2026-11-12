# Notificações e comunicação — auditoria e evolução backend

## Escopo e fonte de verdade
Foi usada a versão local, preservando as alterações anteriores de plano de empregabilidade, dashboards e frontend. A implementação é backend, com entrega dentro da plataforma. Não foram ativados SMTP, WhatsApp, serviço pago ou mensagens reais em testes. Não houve implantação ou alteração de dados operacionais.

## Diagnóstico anterior
- Capacita: learning_notifications persistia atividade do mural e respostas; LearningNotifications oferece histórico, leitura e SSE autenticado, com reconexão por cursor, validação de sessão e limites. O frontend já combina eventos por ID.
- Mural: envio individual e em lote já possuía chaves idempotentes e UNIQUE de destinatário/mensagem/tipo. Mudanças de situação sem efeito já eram ignoradas. Esses comportamentos foram preservados.
- Oportunidades: OpportunityAlertService possuía filtros opt-in de áreas/localidade/modalidade/contratação/jornada/distância/palavras-chave e digest diário/semanal. O caminho de e-mail existente só envia se explicitamente habilitado e configurado.
- Empresas: alterações de etapa possuem eventos versionados de recrutamento; entrevistas com agendamento efetivo existem em company_selection_outcomes.
- Outras filas e notificações já existem para Talentos Futuros, compartilhamento de competências e integrações de carreira. Não foram substituídas nem confundidas com os novos eventos. Comunicações críticas de autenticação, recuperação de senha e segurança não são desativadas pelas preferências opcionais desta entrega.

Lacunas: não havia uma camada comum de preferências opcionais, registro padronizado de entrega interna, fila de fanout e política uniforme de retentativas para os eventos aqui tratados. Muitos candidatos em Empresas possuem somente contato informado na candidatura pública, sem vínculo autenticado com Oportunidades.

## Arquitetura implementada
CommunicationStore tem uma instância explicitamente configurada por trilha, usando somente seu JdbcTemplate e TransactionManager. Não há banco unificado, consulta privada entre bancos nem escolha de trilha por parâmetro do usuário. Controladores têm caminhos fixos por ambiente, sujeitos ao JWT já existente.

Cada banco recebe quatro estruturas locais:
- communication_settings: preferências opcionais, revisão, limite diário, intervalo por categoria e categorias habilitadas.
- communication_events: eventos de origem, chave única, cursor de destinatários, tentativas e próximo processamento.
- communication_deliveries: fila e caixa de entrada persistida por usuário, chave idempotente, status, motivo técnico genérico, tentativas, agendamento, template/versionamento, referência ao recurso e leitura.
- communication_preference_audit: revisão e evidência da alteração de preferências, sem conteúdo de perguntas, notas de recrutamento ou contatos.

Triggers aditivas registram eventos na mesma transação da mudança do domínio. Não é necessário publicar para um serviço de rede durante a alteração de curso ou candidatura. Não há backfill de notificações para alterações anteriores à migração.

Worker local processa até dez eventos de origem por ciclo e até cem destinatários por evento, com cursor persistido e SKIP LOCKED. Em seguida processa até cem entregas pendentes por trilha. Há controle transacional por destinatário para não ultrapassar limites sob concorrência. Filas, preferências e referências permanecem no banco correspondente.

O mural preserva a projeção learning_notifications e o SSE existentes. Quando permitido pelos limites/preferências, a resposta é persistida imediatamente na mesma transação. Em caso de adiamento, a projeção é criada somente na entrega posterior. Nenhum ID de curso ou avaliação é fingido como ID de pergunta para reutilizar o sino do mural.

## Eventos e público real
| Evento | Origem | Destinatários e limite |
|---|---|---|
| APPLICATION_UPDATED — Oportunidades | Mudança efetiva de status em job_applications | Dono do acompanhamento da própria conta. Não significa decisão confirmada pela empresa. |
| NEW_MATCH | Primeira publicação elegível de vaga do catálogo local | Contas verificadas PERSON com alertas explicitamente habilitados e filtros existentes satisfeitos. Não é pontuação completa de compatibilidade do Radar. |
| APPLICATION_UPDATED — Empresas | Evento versionado de mudança real de etapa | Proprietário e recrutadores atualmente autorizados da empresa correspondente. Não inclui observações internas nem contatos do candidato no aviso. |
| INTERVIEW_INVITATION — Empresas | Inclusão ou alteração real de interview_at | Aviso interno para os responsáveis autorizados da empresa. Não comprova que um convite chegou ao candidato. Mudar somente a etapa para INTERVIEW não inventa um agendamento. |
| COURSE_CHANGED | Alteração relevante de curso publicado/antes publicado | Alunos ativos/verificados com progresso real registrado no curso; aviso genérico, sem expor conteúdo privado. |
| ASSESSMENT_AVAILABLE | Publicação de versão de avaliação | Participantes com progresso no curso, com revalidação de publicação, versão atual e disponibilidade do curso na entrega. Requisitos de estudo continuam valendo. |
| QUESTION_REPLY / QUESTION_ACTIVITY | Respostas e demais atividades reais do mural | Participantes autorizados do contexto, respeitando as preferências, sem copiar texto, áudio ou vídeo para a notificação. |

Referências são revalidadas na entrega. Recursos removidos/indisponíveis e contas não autorizadas não são anunciados como disponíveis. Cursos e avaliações não são enviados a todos os estudantes indiscriminadamente; a audiência inicial usa participação registrada em learning_lesson_progress.

## Preferências, consentimento e limites
Somente notificações opcionais dentro da plataforma são ativadas pela nova camada. E-mail e WhatsApp Business aparecem como NOT_ACTIVATED; habilitar notificações internas não constitui consentimento para esses canais externos.

Preferências: enabled; dailyLimit entre 1 e 500 (padrão 100); minimumIntervalMinutes entre 0 e 10080 (padrão 0); mapa categories com booleanos das categorias permitidas na própria trilha; expectedRevision obrigatório nas alterações. Revisão divergente retorna 409. Alterações registram snapshot das preferências e sua revisão na auditoria. A desativação suprime itens pendentes correspondentes; não apaga avisos anteriores nem impede acesso às respostas/conteúdos originais.

O limite diário considera entregas internas confirmadas no dia de America/Sao_Paulo. O intervalo mínimo é por destinatário/categoria. Itens limitados permanecem PENDING com DAILY_LIMIT ou FREQUENCY_LIMIT; não são marcados como entregues. Os alertas de vagas preservam também a escolha anterior de envio immediate/daily/weekly. Digest diário/semanal é adiado até 9h no calendário da plataforma; a nova caixa não transforma a escolha semanal em envio imediato. A fila interna conserva avisos individuais; não foi criado um agrupamento visual novo de digest.

O e-mail legado de alertas continua condicionado às configurações e preferências anteriores e passa a respeitar também o opt-out opcional correspondente; contas não verificadas/legadas corporativas não recebem alertas de candidatos. Não foi criado novo adaptador SMTP. Segurança e recuperação de acesso mantêm seus mecanismos próprios.

## Idempotência, entrega e falhas
- UNIQUE por destinatário/chave de evento impede repetir a persistência da mesma notificação. Eventos de publicação possuem chave estável; mudanças sem efeito não criam novos eventos.
- DELIVERED significa persistência confirmada na caixa de entrada interna. Não significa leitura, entrega de e-mail, recebimento de WhatsApp ou confirmação de comparecimento à entrevista. A API identifica channel=IN_APP e deliveryMeaning=PERSISTED_INBOX_NOT_EXTERNAL_RECEIPT.
- SUPPRESSED distingue opt-out, indisponibilidade, expiração ou autorização inválida de falha técnica. PENDING significa espera de processamento/frequência. FAILED é terminal após cinco tentativas com erro de persistência.
- Retentativas têm atraso exponencial limitado; erros não registram texto de usuário, senha, token, telefone, notas internas ou stack trace no histórico público.
- A entrega interna e a projeção do mural são transacionais. Leitura pelo endpoint novo atualiza a projeção antiga; leitura pelo endpoint antigo atualiza o registro novo. Ambos usam a mesma serialização para evitar ordem inversa de bloqueios. legacy_id permite correlacionar a projeção sem contar dois avisos em um consumidor futuro.
- SSE pode reenviar o mesmo ID após reconexão; o consumidor deve manter deduplicação por ID, como já ocorre no componente existente. Não se promete entrega de rede exatamente uma vez.
- Worker registra falhas não contidas do ciclo por trilha em métricas; falhas individuais e terminalidade ficam persistidas nas filas. O ciclo tenta os demais bancos quando um deles falha. Não foi introduzido Redis, Kafka, RabbitMQ ou serviço externo: persistência local e lotes limitados atendem a esta etapa.

Retenção técnica inicial: entregas e auditoria de preferências antigas são removidas em lotes após 90 dias; preferências atuais permanecem até alteração/exclusão da conta. Entregas pendentes expiram após 30 dias. Eventos de origem antigos também encerram como expirados após 30 dias, sem gerar avisos que pareçam novos. Exclusão de conta remove as estruturas vinculadas por FK; exclusão de pergunta remove sua projeção e entrega associada. O prazo técnico precisa ser revisado com a política de dados antes da produção; não é declaração de conformidade jurídica integral.

## Endpoints
Bases fixas: /api/communications (Oportunidades), /api/empresas/communications e /api/capacita/communications.

- GET /preferences — preferências próprias, revisão, categorias disponíveis e estado dos canais.
- PUT /preferences — alteração da própria conta com expectedRevision.
- GET ?before=...&limit=20 — caixa interna entregue, cursor e total não lido. Limite máximo 50.
- GET /deliveries?before=...&limit=20 — status próprios, tentativas e agendamento. Não expõe contatos ou histórico de outros usuários.
- PUT /{id}/read — marca somente entrega da própria conta; ID de outro usuário retorna 404.

Não existem endpoints públicos para enfileirar mensagens arbitrárias, informar destinatários ou escolher banco de dados. Os templates são texto padronizado e versionado, sem HTML fornecido por usuários. Combinações de categoria/trilha não suportadas são rejeitadas, em vez de gerar destinos inexistentes.

## Migrações e configuração
- Oportunidades: V21__communication_delivery_and_preferences.sql.
- Empresas: V18__communication_delivery_and_preferences.sql.
- Capacita: V21__communication_delivery_and_preferences.sql.

Somente migrações novas e aditivas. Históricos existentes não foram reescritos. As migrações devem ser aplicadas pelo Flyway no processo normal de implantação, após backup. Nenhuma foi aplicada ao schema operacional nesta entrega.

Variáveis documentadas em backend/.env.example: COMMUNICATIONS_WORKER_ENABLED (padrão true), COMMUNICATIONS_POLL_MS (2000) e COMMUNICATIONS_RETENTION_POLL_MS (3600000). Cada trilha continua usando sua conexão e credenciais próprias. Testes desabilitam o worker agendado para controlar os ciclos explicitamente e mantêm app.mail.enabled=false. Não há credenciais de provedores novos.

## Arquivos
Criados em backend/src/main/java/com/trampolim/api/communication/: CommunicationStore.java, CommunicationConfig.java, CommunicationTemplates.java, CommunicationSchedule.java, CommunicationWorker.java, CommunicationApi.java, OpportunityCommunicationsController.java, CompanyCommunicationsController.java e LearningCommunicationsController.java. As três migrações são específicas de cada diretório de banco.

Modificados: OpportunityAlertService.java (reuso dos filtros e opt-out/conta válida no e-mail legado), QuestionService.java (registro/controlador de entrega com projeção compatível), LearningNotifications.java (sincronização de leitura), application.properties, backend/.env.example, testes de migração do Radar/recrutamento, DiscussionPostgresTest.java (construção explícita da dependência) e ops/scripts/Test-BackendSecurity.ps1 (worker desabilitado somente no ambiente isolado de testes).

Novos testes: CommunicationTemplatesTest.java, CommunicationScheduleTest.java e CommunicationPostgresTest.java. As evidências ficam em ops/communications-*.log e pastas de schemas temporários geradas pelo harness. README.md referencia este relatório.

## Testes e resultados
- Primeira compilação de produção: aprovada (ops/communications-compile.log).
- Primeiro teste novo teve conflito de nome no helper de teste e não compilou; o helper foi corrigido, sem alteração de verificações. Primeira rodada focada completa: 14 testes aprovados, zero falhas/erros/ignorados (ops/communications-focused2.log).
- Verificação prévia do mural/migrações: seis casos registrados, cinco aprovados e um de mídia não executado por falta de ferramenta nessa rodada (ops/communications-regression.log).
- Suíte geral: 227 casos registrados; 225 aprovados, uma falha na expectativa da quantidade de migrações do teste RecruitmentMigrationPostgresTest e um teste de carga opt-in não executado (ops/communications-full.log). FFmpeg/ffprobe foram configurados; as integrações de mídia foram executadas. A suíte não é declarada integralmente aprovada nessa rodada.
- A expectativa de upgrade do recrutamento foi corrigida para V17+V18; verificações de preservação de candidatos, hashes e histórico foram mantidas, e foi acrescentada a ausência de eventos retrospectivos de comunicação.
- Revisão final identificou inversão de bloqueios na marcação de leitura pelos endpoints antigo/novo. A leitura das duas projeções agora passa pela mesma serialização transacional por destinatário. Foi acrescentado teste com 12 chamadas concorrentes. A fila/SSE não foi substituída.
- Foram acrescentados testes do calendário diário/semanal nos limites de 9h, expiração da origem e rejeição de templates fora da trilha.
- Reteste final: **28 aprovados, zero falhas/erros/ignorados**, Maven verify/empacotamento aprovados (ops/communications-final.log). Inclui 16 testes de comunicação com PostgreSQL/HTTP, três de calendário, dois de templates, cinco de mural/mídia e dois de migração. Cobertura: persistência, opt-out, revisão, filtros reais, deduplicação, frequência, concorrência, cinco falhas seguidas, expiração, leitura própria, JWT cruzado, audiência de cursos/avaliações e limites da comunicação empresarial.
- A suíte inteira não foi repetida após os ajustes finais restritos a comunicação/leitura/migrações; os testes afetados foram repetidos. Não se somam rodadas repetidas como casos únicos.
- Evidência final: ops/communications-final-2026-11-07/isolated-tests.json, com verifyExitCode=0, realDataUsed=false e temporaryMedia=true. Schemas temporários removidos ao término; nenhuma migração aplicada ao schema operacional.
- git diff --check passou nos arquivos acompanhados desta etapa; avisos CRLF não são falhas. Sem alterações de fonte após o reteste final.
- Não houve teste Playwright/frontend nesta etapa, pois não foi alterado código frontend. A homologação de entrega externa real e carga representativa não foi realizada nem é declarada aprovada.

O teste de upgrade do recrutamento inicialmente esperava somente a V17 pendente; com a V18 aditiva, sua expectativa foi atualizada para duas migrações, mantendo as verificações de preservação de candidatos/histórico e acrescentando que a migração não inventa eventos retrospectivos de comunicação. Nenhum teste foi removido ou desabilitado.

## Pendências reais e fronteira de segurança
1. Notificação automática da empresa para uma conta de candidato: depende de vínculo explícito e verificável de uma candidatura com a conta de Oportunidades, consentimento, revogação e contrato de integração. Hoje muitas candidaturas empresariais são públicas e só possuem contato. Não se fez associação automática por e-mail, sincronização de credenciais nem consulta ao banco privado de outra trilha. O aviso corporativo de entrevista é interno; entrega ao candidato não está concluída.
2. E-mail futuro: avaliar adapter oficial/configurável, consentimento por canal, unsubscribe, idempotência suportada pelo provedor, confirmação/erros e tratamento de resultados incertos. O caminho SMTP legado não foi convertido nesta entrega para exatamente uma vez; a nova garantia de persistência refere-se à caixa interna. SMTP segue sem configuração/ativação nova.
3. WhatsApp Business: somente API oficial, opt-in explícito e templates aprovados na integração futura. Nenhum SDK, contrato pago, número ou token foi criado.
4. Frontend de preferências e caixa geral: os endpoints estão prontos, mas não foi redesenhado o sino das três trilhas nesta etapa backend. O mural continua recebendo os avisos pela projeção/SSE existente. Avisos novos de curso/avaliação e demais eventos gerais precisam ser apresentados por um consumidor da nova API; não foram fingidos como dúvidas.
5. Capacidade: lotes e índices não comprovam suporte a milhares de usuários. Medir filas, latência, consumo e fanout com carga representativa em ambiente próprio antes da expansão.
6. Talentos Futuros, pagamentos, mensagens críticas e invalidações de credenciais mantêm os fluxos anteriores; as novas preferências não são apresentadas como controle universal de toda comunicação possível da plataforma.

## Reprodução
Na raiz do projeto, com Docker local disponível: executar ops/scripts/Test-BackendSecurity.ps1 -EvidenceName communications-check -TestPattern 'Communication*Test,DiscussionPostgresTest,RadarMigrationPostgresTest,RecruitmentMigrationPostgresTest'. O harness cria schemas aleatórios, usa dados sintéticos, desativa envio externo e limpa os schemas ao término. A suíte geral exige as ferramentas de mídia configuradas para não ignorar esses testes. Não executar comandos de limpeza em bancos operacionais para reproduzir estes testes.

## Exemplo de configuração pela API
Consultar primeiro GET /api/communications/preferences para obter revision. Em seguida, PUT /api/communications/preferences com um corpo como:

```json
{"enabled":true,"dailyLimit":20,"minimumIntervalMinutes":30,"categories":{"NEW_MATCH":false},"expectedRevision":0}
```

O exemplo é um contrato ilustrativo, não uma configuração aplicada a contas reais. Categorias de Capacita/Empresas devem ser consultadas nas respectivas bases; não enviar o token de uma trilha para outra.

## Implantação e operação
Antes de ativar o backend atualizado: backup, aplicar as três migrações pelas conexões correspondentes e validar permissões do usuário da aplicação para tabelas, sequências e triggers novas. Monitorar filas PENDING/FAILED, atraso e contagem de tentativas. Ajustar cadência/limpeza de acordo com volume medido. A retenção é em lotes e pode ter atraso; não representa garantia de remoção instantânea no nonagésimo dia. Não ativar canais externos somente porque a fila interna passou nos testes.
