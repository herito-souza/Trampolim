# Eventos de domínio do Trampolim

**Data:** 12/11/2026
**Escopo:** backend Java/Spring e os bancos isolados de Oportunidades, Empresas e Capacita.

## Diagnóstico

- O módulo de comunicação já possui eventos de origem e entregas persistidas por trilha, com deduplicação, tentativas, limites de frequência e retenção. Esses registros são voltados a notificações, não a contratos gerais entre domínios.
- O Capacita já possui uma outbox específica para invalidação de compartilhamentos de credenciais com o Radar. Ela continua restrita a esse propósito.
- O recrutamento empresarial já registra mudanças de etapa e observações internas em uma trilha de auditoria transacional. Observações não são copiadas para eventos de domínio.
- `INTERVIEW` é uma etapa de processo seletivo, não contém data ou horário. Não há atualmente operação que comprove o agendamento de uma entrevista.
- O estado legado de progresso do catálogo não equivale à conclusão de um curso do Capacita. A conclusão educacional usa aulas publicadas/validadas e avaliações obrigatórias aprovadas.

## Envelope e entrega

Cada evento segue o envelope versionado `eventId`, `trail`, `eventName`, `schemaVersion`, `aggregateType`, `aggregateId`, `idempotencyKey`, `occurredAt`, `correlationId` opcional e `payload`. Nomes usam `lower.dot.case.vN`; mudanças incompatíveis exigem novo sufixo de versão.

Há uma outbox e uma tabela de recibos independentes em cada banco. O evento é inserido na mesma transação JDBC da alteração de negócio. Um worker local usa `FOR UPDATE SKIP LOCKED` e um recibo único por evento/consumidor (`domain-event-ledger.v1`) para tornar o consumo idempotente. Falhas são tentadas novamente com atraso exponencial limitado; após dez tentativas ficam em `FAILED`, com código de erro sem conteúdo pessoal. Não há broker externo nesta etapa.

Os eventos não cruzam bancos automaticamente. Consumidores futuros devem usar uma API/bridge autorizada e contratos que minimizem os dados compartilhados. O recibo atual comprova processamento pelo ledger local; não comprova entrega a sistemas externos.

## Contratos v1 implementados

| Evento | Banco/trilha | Fato que o produz | Conteúdo mínimo |
|---|---|---|---|
| `application.created.v1` | Empresas | Candidatura pública aceita e inserida | IDs de candidatura, vaga e empresa; sem nome, e-mail, telefone ou mensagem |
| `application.stage_changed.v1` | Empresas | Alteração efetiva da etapa de uma candidatura | IDs, revisão e estados anterior/novo |
| `vacancy.closed.v1` | Empresas | Transição efetiva do anúncio para `CLOSED` | ID da vaga e empresa |
| `course.completed.v1` | Capacita | Todas as videoaulas publicadas e validadas concluídas e avaliações obrigatórias aprovadas | IDs de estudante/curso e versão do critério; sem respostas |
| `competency.verified.v1` | Capacita | Nova credencial criada após aprovação objetiva | IDs de credencial, avaliação, curso/módulo, código/nível de competência e nota |

## Contratos ainda sem produtor

- `interview.scheduled.v1`: não emitir até existir fluxo real de agendamento que registre data/hora e participantes autorizados. A etapa `INTERVIEW` sozinha não satisfaz o contrato.
- `application.updated.v1`: por enquanto, mudanças de processo são representadas pelo evento mais específico `application.stage_changed.v1`; observações internas permanecem exclusivamente na auditoria empresarial.
- `application.created.v1` para Oportunidades: o módulo de candidaturas dessa trilha é um acompanhamento pessoal e não prova envio à empresa. A candidatura enviada publicamente é criada no banco Empresas.

## Retentativa e operação

`domain_event_outbox.state` aceita `PENDING`, `PROCESSED` e `FAILED`. O worker processa até 25 eventos por ciclo por trilha, individualmente e em transação curta. Erros transitórios voltam a `PENDING`; erros após o limite ficam `FAILED` para investigação/reprocessamento operacional futuro. Um processo diário remove registros terminais após 365 dias por padrão (configurável de 30 a 3650 dias); itens `PENDING` nunca são removidos por retenção. Os recibos são apagados em cascata. Os logs não incluem payload, dados de candidatos nem observações.

## Migrações

- Oportunidades: `V26__domain_event_outbox.sql`.
- Empresas: `V25__domain_event_outbox.sql`.
- Capacita: `V27__domain_event_outbox.sql`.

As migrações são independentes e aditivas; nenhum dado ou histórico anterior é apagado.

## Configuração

O polling usa `domain-events.initial-delay-ms` (padrão 5000 ms), `domain-events.poll-delay-ms` (padrão 1000 ms), `domain-events.retention-days` (padrão 365) e `domain-events.retention-cron` (padrão diário às 03:17). Os workers são registrados apenas quando a conexão da respectiva trilha está habilitada. Nenhum segredo ou serviço externo é necessário.
