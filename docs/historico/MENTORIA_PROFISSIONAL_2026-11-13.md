# Mentoria profissional — 13/11/2026

## Escopo implementado

Foi criado um módulo de encontros de mentoria humana no Trampolim Oportunidades, separado do recurso existente de orientação de carreira automatizada. A experiência está em `#oportunidades/career/mentoring` e reutiliza o JWT e o usuário autenticado de Oportunidades.

O fluxo cobre candidatura do mentor, consentimento registrado para publicar o perfil se aprovado, fila de revisão administrativa, catálogo público somente com perfis aprovados, filtro por especialidade, disponibilização de horários, solicitação por candidato, confirmação ou recusa, cancelamento, registro de conclusão, detalhes privados do encontro, histórico, avaliação, denúncia moderada e retirada do perfil público pelo próprio mentor. A retirada também cancela encontros futuros não realizados e mantém o histórico privado. Os números do painel administrativo são agregados a partir dos registros do módulo. Nenhuma conta fictícia ou conteúdo de mentor foi semeado.

## Isolamento e público

Todos os registros ficam no PostgreSQL de Oportunidades e usam `app_users`. Não há consulta direta ao banco Empresas ou Capacita. Assim, uma pessoa que só tenha conta Capacita ainda não participa deste módulo: precisa entrar ou criar sua conta independente em Oportunidades. Não há vínculo automático de identidade, sincronização de senha ou compartilhamento de sessão. Uma integração futura de estudantes requer contrato de API, consentimento e desenho específico; esta entrega não a simula.

O MVP permite encontros individuais apenas a pessoas que declaram ter 18 anos ou mais, tanto para mentoria quanto para solicitação. A confirmação é autodeclarada, não equivale a validação de identidade/idade por documento. Nenhuma data de nascimento ou documento é coletado. Menores não podem usar encontros individuais nesta versão enquanto não houver salvaguardas verificadas, como processo apropriado de consentimento e supervisão. Isso é uma limitação de produto deliberada, não uma mudança do público geral das trilhas.

## APIs

As rotas privadas ficam sob `/api/career/mentoring` e exigem o token de Oportunidades:

| Verbo e caminho | Uso | Acesso |
| --- | --- | --- |
| `GET /me` | Estado do perfil de mentor e sinalizador administrativo | Conta autenticada |
| `PUT /mentor-profile` | Criar/atualizar candidatura com declaração 18+ | Conta autenticada |
| `DELETE /mentor-profile` | Retirar perfil do catálogo e cancelar encontros futuros ainda não realizados | Titular |
| `GET /availability/mine`, `POST /availability`, `DELETE /availability/{id}` | Consultar, oferecer e retirar horários próprios | Mentor aprovado |
| `POST /sessions`, `GET /sessions` | Solicitar horário e consultar encontros próprios | Conta autenticada/participante |
| `POST /sessions/{id}/confirm`, `/decline`, `/cancel`, `/complete` | Confirmar, recusar, cancelar ou registrar conclusão | Participante conforme estado e papel |
| `PUT /sessions/{id}/details` | Salvar informação/link privado, somente HTTPS se houver URL | Mentor da sessão confirmada |
| `GET /sessions/{id}/history` | Consultar eventos de uma sessão | Participante da sessão |
| `PUT /sessions/{id}/rating` | Avaliar sessão concluída | Pessoa que solicitou |
| `POST /reports` | Denunciar perfil/sessão | Conta autenticada |
| `GET /admin/mentors`, `PUT /admin/mentors/{id}/review` | Revisar cadastros de mentores | `PlatformAccess.isSuperAdmin` |
| `GET /admin/reports`, `PUT /admin/reports/{id}` | Tratar denúncias com justificativa | `PlatformAccess.isSuperAdmin` |
| `GET /admin/metrics` | Totais agregados do módulo | `PlatformAccess.isSuperAdmin` |

O catálogo sem autenticação fica em `GET /api/public/career/mentoring/mentors` e `GET /api/public/career/mentoring/mentors/{uuid}/availability`. Só exibe nome profissional, apresentação, experiência resumida, especialidades, contagem de horários e localidade pública geral. Não há telefone, e-mail ou endereço residencial no catálogo.

## Regras de segurança e funcionamento

- Os encontros, solicitações, mensagens e detalhes são visíveis somente aos participantes; filas e denúncias são restritas à administração da plataforma.
- O backend valida papéis, estado, titularidade, status do mentor, duração dos horários e limites. O banco impede dois pedidos ativos para o mesmo horário; o bloqueio da agenda serializa inclusões concorrentes do mesmo mentor.
- Horários precisam ter 30–120 minutos, começar entre 24 horas e 90 dias no futuro e não se sobrepor. Uma solicitação não é considerada confirmada antes da resposta do mentor; pedidos sem confirmação expiram perto do início.
- Há limite por conta para solicitações e denúncias. Cancelamento, recusa, conclusão e detalhes geram histórico. O registro de conclusão é declaração de um participante, não prova de presença.
- Os limites de curto prazo usam o limitador em memória já adotado pelo backend. Em implantação com múltiplas réplicas, a contagem por minuto/hora é por instância; deve ser substituída ou complementada por um controle distribuído antes de depender dela como limite global. O limite diário de solicitações é serializado no banco por conta.
- Avaliações são privadas e não formam selo público. Aprovação administrativa significa autorização operacional do perfil no Trampolim, não verificação de identidade, qualificação profissional ou endosso institucional.
- Links online aceitam HTTPS; não há provedor de videoconferência, pagamento, gravação ou canal externo contratado. O sistema não grava áudio/vídeo automaticamente.
- A exclusão do registro de conta pode apagar histórico associado por cascata. Não foi criada retenção automática de sessões ou denúncias; prazo operacional e procedimento de descarte devem ser definidos antes da produção e alinhados com as obrigações aplicáveis.

## Persistência

A migração `backend/src/main/resources/db/migration/V31__career_mentoring_program.sql` cria perfis, especialidades, horários, sessões, eventos de histórico, avaliações e denúncias. Inclui índices, unicidade de horário ativo e liberação de horário futuro quando um pedido é removido pela exclusão da conta. Não altera as migrações nem bancos de Empresas e Capacita.

## Limitações e operação

- Notificações são consultadas ao entrar/atualizar a área; esta entrega não adiciona push, e-mail ou WhatsApp.
- O sistema não integra contas independentes do Capacita com Oportunidades.
- O aceite 18+ é uma declaração. Se o produto exigir verificação forte de idade, isso precisa de decisão, base legal, minimização e implementação própria.
- As filas administrativas são limitadas a 100 registros por consulta e ainda não possuem paginação.
- Configurar e executar Flyway em Oportunidades é necessário para disponibilizar as rotas. A equipe precisa designar administradores de plataforma para revisar perfis e denúncias.

## Validação desta entrega

- `npm run lint`: aprovado após implementar a interface.
- `npm run build`: aprovado; o Vite gera aviso preexistente de chunks grandes para mapa/PDF, sem falha de build.
- `mvnw -DskipTests compile`: compilação Java concluída com sucesso.
- `mvnw -Dtest=CareerMentorshipServiceTest test`: não chegou a executar testes. A compilação da suíte de teste falha antes da execução por referências não resolvidas em diversos testes do checkout (incluindo classes base como `BackendApplication`, `SkillCatalog` e `TrailTokenService`); a falha também afeta os testes preexistentes. Não foi possível validar a migração contra PostgreSQL nesta execução.
- Foram adicionados testes unitários para exigir autodeclaração 18+, consentimento de perfil público e bloquear a fila administrativa a contas comuns; eles permanecem sem execução até corrigir a compilação da suíte de testes.

## Próxima fase

Antes de ampliar para menores ou contas exclusivamente Capacita, validar salvaguardas, supervisão, consentimento e desenho de integração entre trilhas. Antes de produção, definir verificação proporcional de mentores, política de conservação/exclusão, atendimento de denúncias e rotina de notificação aos participantes. As declarações legais e de proteção de menores devem ser revisadas por assessoria qualificada.
