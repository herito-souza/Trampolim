# Trampolim Hub — Prioridade 3

Entrega local de 30/10/2026. Escopo: associações empresariais. A Prioridade 4 não foi iniciada. Este documento registra o que está implementado e distingue a fundação comercial de uma cobrança recorrente efetiva.

## Auditoria e decisões

A versão local, na branch `main`, já continha alterações não commitadas das etapas anteriores. Elas foram preservadas. A auditoria identificou React 19/Vite 8/TypeScript/Tailwind 4, Java 21/Spring Boot/Spring Security/JDBC/JPA/Flyway e três bancos PostgreSQL independentes. Não houve substituição de stack, unificação dos bancos, recriação de autenticação ou publicação no GitHub.

Já existiam organizações, vínculos com empresas, membros institucionais, convites de usuários, administração global, cobrança de estágios de R$ 50, Radar, demandas futuras, cursos e progresso. Faltavam a identidade pública institucional administrável, programas vinculados a cursos, campanhas, contratos institucionais operacionais, limites, convites de empresas e métricas de participação autorizadas por instituição. As tabelas comerciais legadas em Oportunidades não tinham planos nem assinaturas cadastrados na consulta inicial. A nova operação institucional permanece em Empresas; não acessa essas tabelas para compartilhar dados privados.

A implementação reutiliza `enterprise_organizations`, `enterprise_organization_members`, `enterprise_organization_companies`, convites e auditoria existentes. Reutiliza também `PlatformAccess`, os JWT de cada trilha, a API pública de vagas, `RadarBridge`, as regras de conclusão do painel educacional e os componentes visuais existentes. Somente APIs com contratos específicos transportam dados educacionais e de candidatos.

## Funcionalidades e acesso

- Administração global em **Oportunidades → Gestão SaaS → Trampolim Hub — administração institucional**: cadastro, revisão, ativação/suspensão, convites de gestores/consultores, aprovação de vínculos empresariais, planos sem preços, contratos e utilização.
- Gestores entram com sua conta independente de **Empresas**, após aceitar o convite de membro institucional. O painel mostra módulos de indicadores, empresas associadas, programas, campanhas, identidade e contrato/utilização.
- Usuários `ASSOCIATION_MEMBER` consultam os dados autorizados do seu ambiente; não editam perfil, programas, campanhas ou contratos. Gestores usam `ASSOCIATION_ADMIN`. Ser proprietário de empresa associada não concede esses papéis.
- Cada instituição tem nome, nome legal, identificação legal opcional, responsável, contato, endereço público, apresentação, texto, cor e logotipo. A ativação pelo novo Hub exige nome legal, contato e responsável. Publicar a página exige contato, apresentação e endereço público válido.
- Página pública em `/?hub=ENDERECO`: apenas instituição ativa e página publicada. Não divulga identificação legal nem o nome do responsável; exibe os contatos institucionais autorizados, campanhas vigentes e até 20 programas disponíveis. Não aceita HTML executável.
- Programas: rascunho/publicado/arquivado, período e até 20 cursos realmente publicados no Capacita. Cursos não são copiados. Arquivamento de programa existente não depende da disponibilidade posterior do curso remoto. Campanhas têm texto simples, período e situação.
- Convite de empresa: gestor gera link para o e-mail do responsável. A pessoa deve criar/usar uma conta de Empresas, ser responsável/administrador corporativo e aceitar com o mesmo e-mail. O responsável visualiza o nome da associação em consulta autenticada antes de confirmar. O vínculo nasce pendente de revisão global, sem criar vaga, contrato ou permissão institucional. A repetição do aceite é idempotente.
- Indicadores com filtro por período e empresa autorizada: empresas ativamente vinculadas, vagas públicas disponíveis criadas no período, previsões publicadas, contratações estimadas, ocupações/competências necessárias e programas. Exportação JSON do relatório já consultado, com escopo, data e avisos.

## Participação, privacidade e semântica dos indicadores

**Candidatos:** Oportunidades → Talentos futuros → Participação institucional. O consentimento institucional é separado do interesse em uma previsão. A associação recebe somente contagens em faixas de pessoas distintas que autorizaram aquela instituição e têm interesse nas previsões de empresas ativamente vinculadas. Preparação e lacunas resolvidas usam evidências da versão atual da demanda e da política, verificadas nas últimas 48 horas. Evidências antigas não são inventadas nem recalculadas em massa ao abrir o painel.

**Alunos:** Capacita → Aprendizagem → Programas institucionais. `/?hubProgram=ID#capacita` abre o programa após o acesso educacional. O aluno escolhe autorizar aquela instituição e a relação de cursos apresentada. Cursos acrescentados depois não entram silenciosamente no consentimento: é necessário novo aceite. A retirada é independente das outras trilhas e não apaga progresso de aprendizagem.

Não há troca de senha, token ou perfil individual. Não são enviados currículos, contatos, respostas, notas individuais ou candidaturas. O próprio usuário pode exportar suas autorizações nos mecanismos de exportação de conta existentes.

Grupos menores que cinco são suprimidos; os demais são mostrados em faixas. Conclusões exigem pelo menos cinco contribuidores distintos, mesmo quando poucos alunos concluíram muitos cursos. Contagem de curso concluído exige curso publicado/não bloqueado, professor ativo, ao menos uma aula publicada/validada, todas essas aulas concluídas e as avaliações obrigatórias aprovadas, conforme o painel educacional existente. Adesão ao programa não é conclusão, contratação ou certificado.

O período filtra criação de vagas/previsões e ingresso na coorte de participantes. As conclusões representam a situação atual da coorte de adesão; não são uma série histórica de conclusões ocorridas dentro do período. Indicadores educacionais abrangem a instituição e não são atribuídos à empresa selecionada no filtro. Contratações confirmadas ficam indisponíveis, pois não há fonte de comprovação. Catálogo geral de materiais públicos, no painel básico anterior, está identificado como geral e não como resultado institucional.

Candidatos são agregados sobre até 200 previsões mais recentes do filtro; o retorno contém cobertura e aviso quando o total exceder esse escopo. Metadados de programas/cursos são consultados em lote. Se uma API remota falhar, o indicador informa indisponibilidade em vez de apresentar zero como resultado real. A privacidade por faixas não é uma promessa de anonimização formal/differential privacy; a homologação institucional deve considerar filtros combinados e riscos de inferência.

## Planos, contratos e limites

Não foram definidos preços, iniciadas cobranças ou simulados pagamentos. Planos registram nome, código, periodicidade administrativa (licença/mensal/anual), situação e capacidades. Contratos vinculam associação/plano/referência/período, com revisão otimista e situações rascunho/ativo/suspenso/encerrado. Ativação requer confirmação de contrato autorizado. Há apenas um contrato não encerrado por instituição. A resposta explicita que nenhum pagamento foi registrado.

Sem contrato ativo no período, os limites técnicos padrão são: 500 vínculos empresariais ativos/pendentes, 200 pessoas institucionais distintas, 100 programas e 30 campanhas não arquivados. Não representam plano comercial gratuito nem preço contratado. Contrato ativo aplica seus próprios limites. Dados anteriores acima de um novo limite permanecem; novas inclusões são bloqueadas até regularização. Arquivamento/encerramento libera capacidade. Papéis adicionais da mesma pessoa não consomem outro assento.

Triggers e bloqueio da linha da organização protegem inclusões concorrentes, inclusive pelos endpoints institucionais anteriores. Contrato suspenso ou ativo fora do período impede alterações adicionais do Hub, mantendo consultas, histórico e recursos gratuitos anteriores. A cobrança existente de estágio permanece independente, com confirmação do provedor no fluxo anterior. Faturamento recorrente depende de decisão comercial e integração futura; SMTP e Mercado Pago não estão homologados neste ambiente.

## Bancos e migrações

| Banco | Migração adicionada | Responsabilidade |
|---|---|---|
| Oportunidades | `db/migration/V16__institutional_candidate_consent.sql` | `hub_candidate_consents`, FK para usuário local e referência remota da associação |
| Empresas | `db/migration-empresas/V13__institutional_hub.sql` | Perfis, planos, contratos, programas, referências de cursos, campanhas, convites empresariais, índices e triggers de capacidade |
| Capacita | `db/migration-capacita/V17__institutional_program_participation.sql` | Participação educacional e escopo de cursos autorizados, FKs somente para dados locais |

Tabelas novas em Empresas: `hub_profiles`, `hub_institutional_plans`, `hub_contracts`, `hub_programs`, `hub_program_courses`, `hub_campaigns`, `hub_company_invites`. Capacita: `hub_program_participations`, `hub_participation_courses`. Nenhuma FK ou consulta SQL conecta bancos privados de trilhas distintas.

A migração cria o perfil inicial não publicado para associações existentes e prepara perfis de novas associações. Não remove registros nem substitui migrações antigas. Flyway mantém histórico separado. Migrações testadas em banco vazio e atualização a partir das versões anteriores. Consulta local após a suíte encontrou Oportunidades V16, Empresas V13 e Capacita V17. Não foi executada implantação em produção.

## APIs e segurança

- `/api/admin/hub/**`: JWT de Oportunidades e autorização global de `PlatformAccess`.
- `/api/empresas/hub/organizations/{id}/**`: JWT de Empresas, conta ativa/verificada e papel naquela associação ativa. Todas as operações verificam o tenant no backend.
- `/api/empresas/hub/company-invites/accept`: responsável corporativo autenticado; e-mail, validade e organização validados; token aleatório armazenado somente como hash.
- `/api/public/hub/**`, `/api/public/capacita/hub/courses/**`: contratos públicos limitados a dados publicados.
- `/api/hub/**`: apenas conta de candidato da trilha correspondente.
- `/api/capacita/hub/**`: apenas conta educacional da trilha correspondente.
- `/api/integrations/hub/candidates` e `/learning`: comunicação de servidor com `X-Trampolim-Bridge`, validada com o segredo existente; nenhum segredo chega ao navegador.

Logotipos: PNG/JPEG até 1 MB, dimensões máximas 2048×2048, decodificação e nova codificação PNG removendo metadados/conteúdo anexado. SVG é rejeitado. Arquivos ficam fora do banco, em diretório por instituição com nome UUID. Leitura privada exige papel institucional; leitura pública exige página publicada e instituição ativa. Upload verifica autorização e tamanho antes do multipart; até dois processamentos simultâneos. A cor escolhida precisa de contraste mínimo 4,5:1 com `#0b1421`. Texto é escapado pelo React. As respostas privadas usam os mecanismos de cache seguro do Spring Security; não existe cache de indicadores compartilhado entre instituições.

Eventos de perfil, logotipo, situação, programas, campanhas, planos, contratos, convites e consentimentos são registrados nas auditorias existentes. Mensagens de conflito não expõem SQL ou dados de outros ambientes. A exclusão de conta usa os FKs locais para retirar os consentimentos; nomes institucionais e referências não sincronizam identidades.

## Execução e configuração

Preserve os arquivos de ambiente atuais. Nova variável opcional: `HUB_LOGO_DIRECTORY`, padrão `storage/hub-logos`, já documentada em `backend/.env.example`. Em produção use caminho persistente com permissões restritas; múltiplas réplicas precisam de volume compartilhado adequado ou adaptação para armazenamento de objetos. Arquivos não acompanham automaticamente o backup do PostgreSQL.

Reutilizados: `RADAR_BRIDGE_SECRET` (segredo gerenciado, pelo menos 32 caracteres, não enviar pelo chat), `RADAR_INTERNAL_API_URL` (rota interna confiável para a API), `FRONTEND_URL` e as configurações separadas dos três bancos/JWT. O Hub não necessita outro login nem outro banco.

```powershell
Set-Location C:\Users\Windows\trampolim-main\backend
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Em outro terminal:

```powershell
Set-Location C:\Users\Windows\trampolim-main\frontend
npm run dev
```

Reinicie o backend para carregar as classes novas; reiniciar apenas o frontend não atualiza as APIs. O comando de inicialização aplica as migrações pendentes das três conexões habilitadas. Não use limpeza do Docker, remoção de volumes ou `flyway clean`. Faça backup de cada banco e do armazenamento antes de atualizar ambientes com dados relevantes; restaure cópias em ambiente separado e valide antes de trocar o tráfego. Reverter código mantendo tabelas novas é preferível a apagar dados para desfazer uma versão.

## Testes e evidências

| Verificação executada | Resultado |
|---|---|
| Suíte completa do backend (`mvnw test`) | 128 casos detectados, 124 aprovados e 4 testes de mídia inicialmente condicionais por ausência das variáveis explícitas; zero falhas/erros |
| Reexecução dos 4 testes de mídia com `CAPACITA_FFMPEG`/`CAPACITA_FFPROBE` | 4 aprovados, nenhum ignorado; uploads reais, duração, áudio/vídeo, arquivos privados, reprodução e notificações |
| Verificação final e empacotamento (`mvnw -Dtest=Hub*Test package`) | 8 testes do Hub aprovados (inclui 2 novos testes de proteção do upload); JAR atualizado gerado |
| Suíte completa de navegador | 62 cenários aprovados; os 2 cenários adicionados depois foram validados na suíte final específica do Hub |
| Suíte final de navegador do Hub | 8 cenários aprovados: gestor em 1440/390 px, consulta limitada, página pública, candidatos/alunos, administração em celular e convite empresarial |
| Ajuste final de retorno do convite | Cenário reexecutado e aprovado, com sessão preservada ao voltar a Empresas |
| Frontend unitário | 7 testes aprovados |
| Lint e compilação TypeScript/Vite | Aprovados; frontend final gerado |
| Migrações PostgreSQL | Instalação vazia e atualização preservando dados aprovadas; versões locais confirmadas: 16/13/17 |
| Acessibilidade | Axe nos painéis/página pública/administração sem violações nas regras WCAG A/AA verificadas; teclado e largura móvel exercitados; imagens de tela inspecionadas |
| Concorrência e isolamento de páginas | 1.000 instituições sintéticas adicionais; 32 requisições HTTP simultâneas, até 20 registros cada, todas com tenant correto; lote em 307 ms no teste local |
| Integridade do diff | `git diff --check` sem erros de espaço em branco; alterações anteriores preservadas |

As execuções complementares cobrem 130 casos distintos do backend e 64 cenários distintos de navegador. Esses totais não representam uma única execução final de toda a suíte: as verificações gerais foram complementadas por reexecuções dos módulos afetados. Não houve falha pendente após as correções. A medida de 307 ms é do lote local de consultas paginadas; não é p95, SLO ou projeção de capacidade em produção.

Evidência resumida e sem credenciais em `ops/hub-validation-2026-10-30.json`.

Os testes HTTP criam schemas temporários nos três PostgreSQL e os removem ao terminar. Os programas, conclusões e mil instituições do teste são fixtures identificadas como sintéticas; não são conteúdo/impacto/receita apresentados em produção. A suíte de navegador usa respostas de teste para exercitar a interface, complementada pelo fluxo HTTP real entre os três bancos. Testes de tela não substituem os testes de autorização do servidor.

Comandos:

```powershell
# Backend: ferramentas reais para testes de mídia existentes
$env:CAPACITA_FFMPEG = 'CAMINHO_LOCAL_DO_FFMPEG'
$env:CAPACITA_FFPROBE = 'CAMINHO_LOCAL_DO_FFPROBE'
.\mvnw.cmd test
# Frontend, no diretório frontend
npm run lint
npm test
npm run build
npm run test:e2e -- --workers=3
```

## Arquivos desta etapa

Criados no backend: `hub/HubAccess.java`, `HubAdminController.java`, `HubCandidateController.java`, `HubCompanyInvites.java`, `HubController.java`, `HubErrors.java`, `HubLogoStore.java`, `HubLogoUploadFilter.java`, `HubPublicController.java`, `HubRules.java`, `HubService.java`; `education/HubLearningController.java`; as três migrações acima; testes `hub/HubRulesTest.java`, `HubLogoStoreTest.java`, `HubLogoUploadFilterTest.java`, `HubHttpPostgresTest.java`.

Modificados no backend: `config/SecurityConfig.java`, `organization/EnterpriseOrganizationController.java`, `organization/SaasOrganizationAdminController.java`, `user/ProfileController.java`, `education/CapacitaController.java`, `application.properties`, `.env.example`, `radar/RadarMigrationPostgresTest.java`.

Criados no frontend: `features/hub/HubPanel.tsx`, `HubAdminPanel.tsx`, `HubParticipationPanel.tsx`, `HubPublicPage.tsx`, `HubCompanyInvitation.tsx`, `HubProfileForm.tsx`, `hubTypes.ts`; `e2e/hub-journey.spec.ts`.

Modificados no frontend: `App.tsx`, `features/account/AssociationDashboardPage.tsx`, `SaasAdminPage.tsx`, `features/future/FutureTalentPanel.tsx`, `features/learning/LearningPortal.tsx`. Documentação: este relatório, `README.md` e `ops/hub-validation-2026-10-30.json`. Alterações preexistentes no Git foram mantidas e não são atribuídas a esta etapa.

## Limitações e implantação

- Nenhum preço/cliente/contrato real foi inventado. Assinaturas recorrentes, emissão fiscal e cobrança institucional automática permanecem dependentes de decisão comercial e provedor configurado. Convites são links para envio manual, sem envio SMTP simulado.
- Contratações confirmadas e uma série histórica de impacto institucional não existem sem fonte e eventos verificáveis; os indicadores atuais explicam sua coorte/cobertura.
- Há paginação com cursor nas listas novas, limite máximo 50 por requisição; a administração geral anterior passou a páginas de até 100 (interface 50) e deixou de fazer uma chamada de vínculos para cada associação ao abrir. Contratos e catálogo de planos têm limites de consulta de 50 e 100; grandes históricos comerciais exigirão paginação adicional.
- Não existe certificado de conformidade WCAG/LGPD. Houve verificações automatizadas e interação por teclado, mas ainda são necessários testes com leitores de tela e validação institucional dos procedimentos de dados/menores.
- Teste local concorrente não garante milhares de usuários simultâneos. Antes de produção, medir SLOs em infraestrutura representativa, dimensionar pools/limites, monitorar latência/erros e ocupação por organização e homologar backup/restauração. Não foi introduzido cache compartilhado entre tenants.
- Retenção de auditoria e convites expirados deve seguir política institucional definida; o Hub não apaga automaticamente registros históricos nem realiza migração destrutiva. Segredos, TLS, SMTP, provedor de pagamento e armazenamento persistente precisam de configuração segura no ambiente de implantação.
