# Parcerias com instituições de ensino

## Escopo entregue

- O Trampolim Hub passa a aceitar organizações `EDUCATIONAL` junto às associações, reaproveitando o banco Empresas, os programas do Hub e o catálogo real de cursos do Capacita.
- A administração global pode cadastrar instituições, revisar os dados legais e do representante, verificar, reprovar ou devolver o cadastro para revisão. Instituições não são ativadas por uma simples mudança de estado: a verificação exige ação administrativa explícita.
- O convite para representantes usa a autenticação Empresas já existente e os papéis `EDUCATION_ADMIN`, `CONTENT_EDITOR` e `PARTNER_MANAGER`. O administrador gerencia a instituição; editores podem administrar programas e eventos; o papel de consulta é somente leitura.
- Instituições verificadas podem divulgar programas vinculados a cursos publicados e publicar eventos por meio do módulo de feiras/eventos existente. Os eventos continuam sendo registros únicos, com inscrição e dados públicos tratados pelos fluxos existentes.
- Páginas institucionais e programas educacionais públicos só aparecem quando a instituição estiver verificada, ativa e tiver publicado seu perfil.
- O painel mostra apenas participações e conclusões educacionais agregadas. A participação continua exigindo consentimento do estudante; nenhum endpoint institucional foi criado para consultar notas, perfis, candidaturas ou histórico individual.

## Dados e migração

Foi adicionada a migração `V28__educational_institution_partnerships.sql` ao banco Empresas. Ela acrescenta estado e observação de verificação, data e responsável pela decisão, cria perfis do Hub para instituições já cadastradas e atualiza o gatilho para novas organizações educacionais. Instituições existentes começam como `PENDING_REVIEW`; nenhuma é considerada verificada automaticamente. Os bancos Oportunidades e Capacita não foram modificados.

## Fluxos e APIs

- Administração global: `GET/POST /api/admin/hub/organizations`, `GET/PUT /api/admin/hub/organizations/{id}/profile`, `PUT /api/admin/hub/organizations/{id}/verification`.
- Organizações para a sessão Empresas: `GET /api/empresas/institutional/organizations`.
- Área autenticada e controlada por papel: `/api/empresas/hub/organizations/{id}/...`.
- Eventos institucionais: endpoints já existentes em `/api/empresas/hub/organizations/{id}/job-fairs`.
- Catálogo institucional público: endpoints existentes em `/api/public/hub/...`; o catálogo de eventos já existente passou a incluir instituições verificadas.

## Revisão e segurança

A validade de um identificador legal não verifica uma instituição por si só. A plataforma registra uma decisão administrativa explícita e não consulta bases externas. O processo operacional deve conferir documentos e representante por canal aprovado. Identificadores e observações da revisão não são expostos nas páginas públicas.

Os dados de estudantes permanecem no banco Capacita. O Hub recebe somente contagens agregadas pelo contrato interno já existente, com consentimento revogável e supressão de grupos pequenos. Não há compartilhamento individual com representantes institucionais. Não foi criado fluxo de acesso especial para menores; os controles atuais de participação e consentimento permanecem aplicáveis.

## Configuração e pendências

- Aplicar a migração Flyway do banco Empresas durante a atualização normal do backend.
- A instituição precisa ser cadastrada/verificada pela administração global e ter um usuário convidado para acessar o painel.
- O processo é uma revisão administrativa, não uma integração automática com Censo Escolar, MEC, CNPJ ou outra base externa.
- A publicação de eventos reutiliza o módulo de eventos existente. A publicação de vagas de estágio continua sendo feita por empresas verificadas no módulo Empresas; a instituição pode divulgar eventos e programas e relacionar-se com as oportunidades públicas já existentes, sem criar cópias de vagas.

## Validação

Adicionado cenário de integração ao teste PostgreSQL do Hub para cobrir instituição pendente, bloqueio de ativação sem verificação, verificação administrativa, isolamento de papel institucional e publicação de evento. O resultado de execução será registrado após rodar as verificações no ambiente disponível.
