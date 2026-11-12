# Feirão de Empregos Digital — backend

## Escopo e arquitetura

Implementação incremental sobre o Hub institucional existente (Java 21, Spring Boot, PostgreSQL e Flyway). O organizador nesta versão é uma associação ACTIVE, com perfil Hub publicado. Gestores ASSOCIATION_ADMIN gerenciam; ASSOCIATION_MEMBER consulta. O contrato institucional existente continua limitando escritas. O administrador global usa a gestão de organizações já existente para ativar a associação e conceder seus vínculos; não recebe acesso implícito às inscrições privadas.

Empresas armazena eventos, convites, referências de vagas e cursos, agenda e auditoria. Oportunidades e Capacita armazenam somente as inscrições das respectivas contas. Não há conexão direta de repositories entre bancos. Cursos são consultados pelo contrato público existente do Capacita; contagens chegam por HTTP autenticado com segredo de integração. Nenhuma conta, empresa, vaga ou curso demonstrativo é criado pelas migrações.

## Regras

- Modalidades PRESENCIAL, DIGITAL e HIBRIDO. Datas ISO-8601 com fuso; encerramento posterior ao início. Eventos físicos/híbridos exigem município IBGE válido e local. O catálogo territorial existente permite outras regiões; Irecê é 2914604, sem restrição exclusiva.
- Situações DRAFT, PUBLISHED, CANCELLED e ARCHIVED. Fase UPCOMING/LIVE/ENDED calculada pelo relógio do servidor. Publicar exige período ainda aberto, associação ativa, perfil publicado e campanha publicada quando vinculada.
- CANCELLED pode somente ser arquivado; ARCHIVED é terminal. Retornar um publicado para DRAFT permite corrigir seus dados antes de republicar. Datas não podem excluir atividades já cadastradas na agenda.
- Cancelados já publicados continuam consultáveis publicamente para informar o cancelamento. Arquivados, rascunhos e eventos de organizadores/campanhas indisponíveis deixam de ser públicos. Recursos de eventos encerrados/cancelados não são apresentados como abertos.
- Convite somente para empresa aprovada e vinculada ativamente à associação. O responsável corporativo verificado aceita ou retira a participação. Associação suspensa não recebe novos aceites. Retirada exige novo convite para aceitar novamente.
- Vagas são referências ao registro original. Somente vagas públicas, disponíveis, de empresas confirmadas e ainda autorizadas aparecem. Estágios continuam exigindo PAID segundo a regra existente de R$ 50; este módulo não gera cobrança adicional nem simula pagamento.
- Cursos são referências ao registro educacional existente, revalidadas em sua fonte pública. Retirar uma referência preserva o curso/vaga original.
- Palestras e oficinas precisam caber no período do evento. Divulgar o nome do palestrante exige confirmação de autorização. Não há transmissão de vídeo, agendamento individual de entrevistas ou reserva de assentos.
- Inscrição é opcional por evento, exige consentimento explícito e conta verificada da própria trilha. Não reserva emprego, contratação ou assento. Não há limite de capacidade nesta versão.
- Controle de versão expectedRevision nas escritas; conflito retorna 409. Criação de eventos e inscrições aceita requestKey UUID: repetir a mesma operação não duplica registros; payload diferente para a mesma chave conflita. Retentativa antiga após cancelamento da inscrição não reativa a conta.

## Endpoints

Paginação por cursor before (ID exclusivo, padrão Long.MAX_VALUE) e limit (1–50, padrão 20). Resposta de lista segue o helper existente HubService.pageResult. Cursos permitem até 20 por página. IDs pertencem ao banco de origem, não identificam pessoas entre trilhas.

### Gestão institucional

Base: /api/empresas/hub/organizations/{org}/job-fairs; JWT Empresas e permissão institucional.

| Método/caminho | Operação |
| --- | --- |
| GET / | Listar; filtro status |
| POST / | Criar evento |
| GET /{id} | Consultar próprio evento |
| PUT /{id} | Editar rascunho |
| PUT /{id}/status | Publicar, recolher, cancelar ou arquivar |
| GET /{id}/companies | Consultar convites |
| POST /{id}/companies | Convidar empresa; id + expectedRevision do evento |
| DELETE /{id}/companies/{company} | Retirar empresa; expectedRevision na query |
| POST /{id}/jobs ou /courses | Vincular registro existente; id + expectedRevision do evento |
| GET/POST /{id}/sessions | Consultar/criar agenda |
| PUT /{id}/sessions/{session} | Editar atividade |
| DELETE /{id}/{jobs,courses,sessions}/{resource} | Desvincular; expectedRevision na query |
| GET /{id}/indicators | Contagens agregadas autorizadas |

Criação de evento (exemplo de contrato, não carga de dados):

~~~json
{
  "title": "Título informado pelo organizador",
  "description": "Descrição informada pelo organizador",
  "startsAt": "2027-02-01T12:00:00Z",
  "endsAt": "2027-02-01T20:00:00Z",
  "modality": "HIBRIDO",
  "municipalityCode": "2914604",
  "venue": "Local confirmado pelo organizador",
  "registrationEnabled": true,
  "campaignId": null,
  "expectedRevision": 0,
  "requestKey": "00000000-0000-4000-8000-000000000001"
}
~~~

Use UUID novo por intenção de criação, reutilizando-o apenas em sua retentativa. Atualizações usam a revisão devolvida pelo servidor. Publicação: {"status":"PUBLISHED","expectedRevision":1}. Agenda: kind TALK/WORKSHOP, title, description, speakerName opcional, speakerConsent, startsAt, endsAt e expectedRevision do evento. Não interpretar revision da participação como revisão do evento.

### Empresa participante

- GET /api/company/job-fairs: convites da empresa cujo responsável é a conta atual.
- PUT /api/company/job-fairs/{id}/participation: {"accepted":true,"expectedRevision":0}; revisão da participação, não do evento. accepted=false retira a participação.

### Consulta pública

GET /api/public/job-fairs e /{id}; GET /{id}/companies, /jobs, /courses, /sessions.

Filtros da lista: q, region (catálogo territorial configurado), municipalityCode, state (UF), modality, from/to ISO-8601, includePast, before e limit. As datas filtram períodos sobrepostos. Vagas permitem area e modality conforme os valores atuais do cadastro de vagas.

### Inscrição privada

Bases: /api/job-fairs (JWT Oportunidades) e /api/capacita/job-fairs (JWT Capacita).

- GET /registrations: inscrições da própria conta, paginadas.
- GET /{id}/registration: inscrição própria e disponibilidade pública atual do evento.
- PUT /{id}/registration: {"consent":true,"expectedRevision":0,"requestKey":"UUID"}.
- DELETE /{id}/registration?expectedRevision=1: retirada local, inclusive se a fonte pública estiver indisponível.

Não se aceita parâmetro userId para escolher outro participante. Credenciais e JWT de outras trilhas são rejeitados pelos filtros existentes.

### Integração interna

POST /api/integrations/job-fairs/opportunities/counts e /learning/counts. Header X-Trampolim-Bridge com segredo configurado. Corpo: {"organizationId":123,"eventIds":[456]}; até 50 IDs positivos distintos. Sem nomes, e-mails, contatos, currículos ou lista de participantes. Endpoints são liberados no filtro HTTP apenas para permitir a autenticação específica do controller; sem segredo correto retornam 401, sem configuração retornam 503.

Indicadores informam contas inscritas, não pessoas únicas, presença física ou contratação. Uma pessoa com duas contas independentes pode contar duas vezes. linkedJobRecords/linkedCourseReferences são referências cadastradas, não promessa de disponibilidade atual. Falha de uma fonte retorna UNAVAILABLE e total null; não transforma ausência de resposta em zero.

## Segurança, falhas e consistência

Consultas corporativas sempre usam organização autorizada. Chaves compostas impedem campanha de outra associação e vaga de outra empresa no vínculo. Escritas usam transações no banco proprietário, bloqueios e revisões, sem transação distribuída. Auditoria corporativa registra identificador do ator, evento, ação, revisão e horário, sem credenciais ou dados de participantes.

Inscrição consulta a disponibilidade pública imediatamente antes da transação local e registra source_checked_at. A situação remota pode mudar entre essa consulta e a gravação: é um registro de interesse, não reserva atômica de capacidade. GET posterior reconsulta o evento; inscrições não são apagadas automaticamente quando o evento termina. Inscrições são eliminadas por cascade na exclusão da conta local. A política operacional de retenção de inscrições e auditoria precisa ser definida pelo responsável antes da produção; não foi criado prazo legal presumido nem rotina de exclusão destrutiva.

Os acessos HTTP reutilizam RadarBridge com limite de concorrência e timeout. Configurar radar.internal-api-url (RADAR_INTERNAL_API_URL) e radar.bridge-secret (RADAR_BRIDGE_SECRET), usando segredo forte com pelo menos 32 caracteres, rede interna e HTTPS em produção. O backend nunca encaminha JWT do aluno ou professor para essas consultas. Em deployments separados, o gateway precisa encaminhar cada contrato para seu backend responsável; os três bancos continuam com credenciais e migrações próprias.

Cursos são resolvidos progressivamente, com orçamento de processamento entre chamadas; uma chamada em andamento ainda pode alcançar o timeout do bridge. Em indisponibilidade ou orçamento esgotado, sourceStatus=UNAVAILABLE e a página pode ser parcial. O cliente deve oferecer retentativa da mesma página antes de avançar o cursor; nenhuma disponibilidade é inventada.

## Migrações e arquivos

- Empresas: V19__digital_job_fairs.sql — job_fairs, job_fair_companies, job_fair_listings, job_fair_courses, job_fair_sessions, job_fair_audit, índices e vínculos compostos adicionais em tabelas existentes.
- Oportunidades: V22__private_job_fair_registrations.sql — inscrições privadas da conta profissional.
- Capacita: V22__private_job_fair_registrations.sql — inscrições privadas da conta educacional.
- Novas classes em backend/src/main/java/com/trampolim/api/fairs: FairRules, FairService, FairManagementController, FairCompanyController, FairPublicController, FairRegistrationStore, FairRegistrationConfig, FairRegistrationApi, CandidateFairController, StudentFairController, FairCountsController e FairErrors.
- SecurityConfig: liberação somente das consultas públicas e dos dois contratos internos com autenticação própria.
- Testes: FairRulesTest, FairPostgresTest; atualização das versões esperadas em RadarMigrationPostgresTest e RecruitmentMigrationPostgresTest.
- README: referência a este documento.

As migrações são aditivas e preservam os registros anteriores. Executar backup independente dos três bancos e homologar antes de atualizar o ambiente operacional. Não foram executadas migrações no schema de uso real nesta entrega.

## Validação

Execução final em 11/11/2026: Maven verify concluído com BUILD SUCCESS; 247 testes registrados, 246 aprovados, zero falhas/erros e um ensaio opcional de carga desativado (BackendScalePostgresTest). Os 15 testes específicos do feirão passaram (13 de integração PostgreSQL/HTTP/concorrência e 2 de regras). A regressão incluiu autenticação, recuperação de senha, isolamento JWT, Hub, recrutamento, comunicação, videoaulas/mídia, biblioteca, avaliações, planejamento e Radar. FFmpeg/FFprobe locais foram configurados para a execução. O JAR de produção foi gerado.

Migrações verificadas em bancos vazios e atualização de históricos existentes pelos testes IndependentFlywayPostgresTest, RadarMigrationPostgresTest e RecruitmentMigrationPostgresTest. As primeiras execuções encontraram ausência de full_name em fixtures institucionais; os dados sintéticos foram corrigidos sem relaxar restrições. A revisão também acrescentou rejeição explícita de status ausente e aceite por organização suspensa.

Evidências: [log completo] (evidência local não versionada) e [isolamento] (evidência local não versionada), com realDataUsed=false e verifyExitCode=0. Verificação git diff --check dos arquivos rastreados alterados nesta etapa e inspeção de espaços finais dos arquivos novos sem problemas. Não houve alteração de frontend; lint, testes de navegador e build frontend não foram executados nesta etapa exclusivamente backend. Não foi realizado teste de carga representativo, medição de capacidade nem ensaio de restauração nesta rodada.

Reprodução local:

~~~powershell
# Configure CAPACITA_FFMPEG e CAPACITA_FFPROBE com seus executáveis locais.
.\ops\scripts\Test-BackendSecurity.ps1 -EvidenceName job-fairs-regression-local
# Somente o módulo:
.\ops\scripts\Test-BackendSecurity.ps1 -EvidenceName job-fairs-focused-local -TestPattern 'Fair*Test'
~~~

O harness ops/scripts/Test-BackendSecurity.ps1 cria schemas aleatórios isolados nos três PostgreSQL Docker locais e os remove ao terminar. Envio de e-mail e workers externos são desativados no teste. Todos os usuários, cursos, empresas e eventos dos testes são sintéticos e ficam somente nesses schemas.

## Limites e preparação de produção

Entrega exclusivamente backend: não adiciona telas de feirão ao frontend nem notificações de novas atividades. Não ativa SMTP, WhatsApp, cobrança ou serviços pagos. Não cria transmissão de palestras nem mede presença/contratações. Organizadores fora do modelo atual de associações exigem extensão explícita de permissões; não foram concedidos acessos implícitos.

Antes da produção: homologar migrações com backup/restauração; configurar URL interna e segredo, HTTPS/rede; definir responsáveis e permissões institucionais, retenção e atendimento de titulares; dimensionar infraestrutura e medir carga representativa. Paginação/índices/limites estão implementados, mas não comprovam capacidade para milhares de participantes concorrentes. Não houve implantação nem certificação jurídica de conformidade.
