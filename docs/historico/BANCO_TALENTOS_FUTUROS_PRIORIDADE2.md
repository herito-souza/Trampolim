# Banco de Talentos Futuros — Prioridade 2

## Situação inicial e escopo

Fonte de verdade: projeto local `C:\Users\Windows\trampolim-main`, branch `main`, com alterações locais extensas preservadas. Não houve reset, limpeza, commit, push ou implantação externa. Antes da alteração foram inspecionados empresas, membros, vagas, publicação/pagamentos, perfis, Radar, catálogo educacional, notificações e exportação/exclusão de contas.

A versão local já tinha autenticações e bancos independentes, catálogo canônico de competências, requisitos estruturados, pontuação configurável, conquistas consentidas e APIs públicas de cursos e vagas. Notificações educacionais pertencem a usuários do Capacita e os alertas antigos de Oportunidades são voltados ao catálogo; não foram usados para misturar identidades ou bancos. O novo histórico de avisos fica em Oportunidades.

Sequência realizada: migrações e contratos → previsões empresariais → preparação com Radar → agregação protegida → processamento durável de avisos → interfaces → testes e documentação. Apenas a Prioridade 2 foi desenvolvida.

## Acesso e funcionalidades

**Empresas:** menu **Talentos futuros**, com cadastro de rascunho, edição, competências, publicação, suspensão, arquivamento, indicadores e vínculo com vagas reais já publicadas. O painel usa o padrão de componentes e navegação existente. Proprietário, administrador corporativo e recrutador podem editar a própria empresa; analista pode consultar. O backend verifica conta ativa/verificada, vínculo, papel e propriedade do recurso em cada operação.

Campos: cargo, descrição, área, quantidade estimada, formação e experiência desejadas em texto, região, modalidade e intervalo de datas previsto. O editor reutilizado do Radar registra competências, níveis, comprovação exigida e critérios estruturados de experiência/formação. Textos livres não são transformados automaticamente em requisitos calculáveis.

Fluxo de situação: `DRAFT`, `PUBLISHED`, `SUSPENDED`, `ARCHIVED`. É necessário salvar o rascunho e ao menos uma competência antes de publicar; a empresa precisa estar aprovada. Atualizações usam revisão otimista. Previsões vencidas, suspensas, arquivadas ou de empresas não aprovadas não entram no catálogo dos candidatos. Datas de disponibilidade de previsões usam o calendário de São Paulo.

Uma previsão permanece identificada como **estimativa, não vaga aberta ou promessa de contratação**. A publicação de uma vaga real continua no fluxo anterior, incluindo pagamento de estágio quando aplicável. Nenhuma previsão cria automaticamente um anúncio ou cobrança.

**Candidato:** menu **Talentos futuros**, com busca de cargos/áreas previstos, filtros de região/modalidade, interesse autorizado, análise da preparação, planos de estudo, evolução e avisos de vagas reais. Cada interesse corresponde a uma previsão de área/ocupação escolhida pelo candidato. Conta, perfil ou contato não são enviados à empresa.

O candidato autoriza separadamente:
1. Participação nos indicadores agregados da previsão.
2. Recebimento de avisos internos de vagas reais correspondentes.

Pode atualizar autorizações ou retirar o interesse. A retirada remove planos, histórico de preparação e avisos daquela previsão na sua conta. O interesse em outra previsão ou as sessões das outras trilhas não são afetados.

## Arquitetura e dados

| Banco | Migração nova | Responsabilidade |
|---|---|---|
| Empresas | `V12__future_talent_demands.sql` | Previsões, critérios, estados, vínculo histórico com registros reais de vagas |
| Oportunidades | `V15__future_talent_interests_and_notifications.sql` | Consentimentos, preparação, planos, evolução, fila e histórico de avisos |
| Capacita | Nenhuma | Continua dono de cursos, contas educacionais, progresso e conquistas; consultado pelas APIs existentes |

Novas tabelas de Empresas: `company_future_demands`, `company_future_job_links`.

Novas tabelas de Oportunidades: `future_talent_interests`, `future_preparation_snapshots`, `future_preparation_daily`, `future_training_plans`, `future_delivery_state`, `future_notification_jobs`, `future_talent_notifications`, `future_talent_audit`.

O cache existente do Radar passa a aceitar a fonte `future`, mantendo `catalog` e `company`. A migração substitui somente a restrição desse discriminador, sem apagar registros ou alterar o histórico já aplicado. Chaves estrangeiras locais mantêm a integridade e a exclusão de dependentes; não existem chaves estrangeiras ou acesso direto a repositories privados de outros bancos.

Oportunidades consulta previsões e vagas públicas de Empresas via HTTP. Empresas solicita apenas indicadores agregados de Oportunidades por contrato servidor a servidor, com `X-Trampolim-Bridge`. A chave é validada no backend, não enviada ao navegador. A autoria e a conta do candidato nunca fazem parte dessa resposta.

A política de disponibilidade de vaga foi extraída de `CompanyRadarController` para uma condição compartilhada, preservando: empresa aprovada, vaga publicada, prazo válido e estágio pago. Ela é reutilizada no vínculo, nos eventos e na abertura de avisos. Nenhuma confirmação de pagamento é simulada.

## Preparação e capacitação

Reutilizados `RadarService`, `MatchEngine`, catálogo padronizado, níveis, pesos e revalidação de conquistas do Capacita. O cálculo continua explicável, sem utilizar características pessoais protegidas. A empresa publica seus critérios e o candidato pode revisar os próprios dados no Radar.

O acompanhamento guarda uma referência inicial de lacunas e a situação atual por interesse. Mudanças na revisão de requisitos ou na política de pesos reiniciam a referência, evitando contabilizar remoção de critérios como conquista. Cada dia mantém uma observação; o histórico diário tem retenção de 90 dias e limpeza em lotes. Os indicadores de preparação usam análises da revisão atual, política atual e idade máxima de 48 horas.

A classificação **preparação compatível** exige nota de pelo menos 80%, todas as competências obrigatórias atendidas e ausência de pendências ou critérios eliminatórios não demonstrados. É um indicador orientativo, não aprovação em seleção. Competência declarada e comprovada continuam diferenciadas.

Se a origem das comprovações estiver indisponível, a análise indica o problema e não substitui o histórico salvo por uma evolução fabricada. A revalidação usa a integração consentida já desenvolvida na Prioridade 1; não é criado outro vínculo de identidade.

Cursos e módulos vêm da API pública de metadados educacionais já existente, limitada a conteúdos publicados vinculados às competências. Não são cadastrados cursos fictícios. Sem curso relacionado, há estado vazio; com falha de integração, há mensagem de indisponibilidade.

Iniciar um plano valida novamente a recomendação no servidor, registra curso/módulo/títulos reais e abre o módulo no Capacita. Os estados são `IN_PROGRESS` e `STOPPED`. Trata-se de **início de plano declarado pelo candidato**, não matrícula, presença ou conclusão comprovada. Nenhuma conta educacional é criada automaticamente. A conclusão de estudos e competências comprovadas seguem as regras existentes do Capacita e sua autorização de compartilhamento.

Análises são atualizadas por solicitação do candidato e, em segundo plano, em lotes de até 20 interesses por ciclo, agendados para revisão diária. Fontes expiradas ou suspensas não geram resultados inventados.

## Indicadores e proteção de privacidade

Indicadores próprios da empresa: número de demandas, ocupações, contratações estimadas nas previsões publicadas vigentes, competências solicitadas, previsões com vaga real vinculada, vagas efetivamente vinculadas e quantas ainda estão abertas. Estimativas são rotuladas como tal.

Indicadores de interessados: dimensão do grupo autorizado, análises atuais, níveis de preparação, início declarado de planos, lacunas frequentes, competências atendidas/comprovadas e pessoas com lacunas resolvidas. Há histórico agregado diário quando existe amostra suficiente.

Proteções implementadas:
- Grupos com menos de cinco interessados não recebem estatísticas detalhadas.
- Contagens de candidatos são exibidas em faixas de cinco (por exemplo, 5–9), sem total exato.
- Células de competências, lacunas e evolução com menos de cinco participantes são suprimidas.
- Nenhum identificador de usuário, nome, e-mail, currículo, contato, recibo de comprovação ou resposta individual é enviado ao painel empresarial.
- Critérios revistos, observações antigas e políticas anteriores não são apresentados como preparação atual.

Essas medidas reduzem exposição e inferência em grupos pequenos; não são promessa de anonimato matemático ou substituto de uma avaliação institucional de privacidade para produção.

**Contratações confirmadas** permanecem sem valor (`null`), com explicação. A aplicação não dispõe de comprovação de vínculo empregatício. Aceite de candidatura, início de plano e previsão de contratação não são apresentados como contratação confirmada.

## Avisos de vagas reais

O recrutador escolhe uma vaga já publicada e confirma o vínculo com a previsão publicada. O backend rejeita vagas de outra empresa, encerradas, não aprovadas, vencidas ou estágios sem pagamento confirmado. O vínculo é único por previsão/vaga e não duplica anúncios.

1. Empresas mantém o vínculo durável no seu banco.
2. Oportunidades consulta eventos públicos paginados por cursor e persiste cada evento e o avanço do cursor na mesma transação local.
3. A produção dos eventos é serializada em Empresas até o commit, impedindo que a sequência do cursor pule um vínculo de transação ainda não confirmada.
4. Uma fila durável de Oportunidades distribui avisos em lotes de até 500 destinatários, revalidando a disponibilidade da vaga.
5. Somente interesses já existentes no momento do vínculo, ainda presentes e com aviso autorizado recebem a notificação.
6. Restrição única `(usuário, previsão, vaga)` evita duplicações em reprocessamento, reconexão ou clique repetido.
7. O candidato consulta histórico paginado, contador de não lidos, marcação como lido e conteúdo da vaga real. A disponibilidade é revalidada antes de abrir a vaga.

O processo usa um executor próprio com um único lote em execução ou pendente, sem bloquear o agendador compartilhado das notificações educacionais. Contas não verificadas são filtradas antes de limitar o lote de revisão, evitando impedir a atualização de outros candidatos. A limpeza de histórico é tentada mesmo quando a integração de vagas está indisponível.

O processo padrão é disparado a cada 60 segundos, após espera inicial de 30 segundos; há até duas etapas de entrega de 500 pessoas por ciclo, além da captura de até 100 eventos e revisão de até 20 preparações. Volume alto pode criar fila; não há promessa de aviso instantâneo. O navegador consulta os avisos a cada 30 segundos quando visível e recupera o histórico ao voltar a ficar online. Há atualização manual.

Os avisos desta etapa são **internos à plataforma**, sem envio por e-mail, SMS, WhatsApp ou push. Não dependem de SMTP. O processo de candidatura é separado e usa o fluxo público já existente, com seu consentimento próprio.

## APIs principais

Empresas autenticadas:
- `GET/POST /api/company/future-demands`
- `GET/PUT /api/company/future-demands/{id}`
- `GET/PUT /api/company/future-demands/{id}/requirements`
- `GET /api/company/future-demands/{id}/preparation`
- `GET/POST /api/company/future-demands/{id}/vacancies`
- `GET /api/company/future-indicators`

Candidato autenticado em Oportunidades:
- `GET /api/future/demands`, `/interests`
- `PUT/DELETE /api/future/demands/{id}/interest`
- `GET /api/future/demands/{id}/preparation`
- `GET/PUT /api/future/demands/{id}/plans`
- `GET /api/future/notifications`
- `PUT /api/future/notifications/{id}/read`
- `GET /api/future/vacancies/{id}`

Integração controlada:
- `POST /api/integrations/future/summary`: segredo de servidor obrigatório, sem resposta identificável.
- `GET /api/public/companies/future-demands`, `/{id}`: somente previsões disponíveis.
- `GET /api/public/companies/future-job-events`: somente metadados de vagas reais disponíveis.
- `GET /api/public/companies/future-vacancies/{id}`: descrição pública, sem contatos privados.
- As APIs de competências, cursos e comprovações da Prioridade 1 foram reutilizadas.

## Segurança, limites e operação

Autorização, validação de dados, escopo por recurso, revisão otimista e auditoria são controlados no backend. O frontend oferece estados de carregamento, erro, ausência de dados e botões coerentes com o papel; ocultação não substitui a autorização.

Paginação de previsões e avisos: padrão 20, máximo 50. Até 500 previsões não arquivadas por empresa, 200 interesses por candidato, 40 competências e planos por previsão/interesse e cem vínculos de vagas por previsão. Processamento periódico usa lotes, transações, bloqueios com `SKIP LOCKED`, tentativas idempotentes e cursores persistidos. O cache do Radar e os clientes HTTP com limites já existentes foram preservados.

Índices cobrem empresa/previsão, feeds publicados, interesse por demanda/usuário, interessados que autorizaram aviso, preparação por data, fila pendente, histórico do usuário e limpeza diária. Os contadores `future.talent.poll.failures` e `future.talent.poll.duration` estão na instrumentação operacional protegida já existente.

A exportação de conta de Oportunidades inclui interesses, planos, observações e avisos próprios. Exclusão da conta remove dependentes locais por cascade. Retirada de interesse remove seus dependentes e avisos daquela previsão. Vínculos públicos de vaga e registros operacionais sem identidade de candidato podem permanecer. Definir política institucional de retenção de auditoria e eventos concluídos antes da produção.

Durante os testes automatizados, o agendador é desativado por configuração do Surefire para evitar chamadas ao ambiente pessoal em paralelo; os métodos reais de captura, entrega, atualização e limpeza são executados explicitamente nos testes. A execução habilitada/desabilitada do worker também possui teste próprio. Nenhuma regra de segurança ou teste foi desativado para aprovar a entrega.

## Configuração e atualização

Reutilizadas `RADAR_INTERNAL_API_URL` e `RADAR_BRIDGE_SECRET`, exclusivamente no backend. A chave existente não foi publicada nem substituída. Para uma instalação nova, configure segredo exclusivo com pelo menos 32 caracteres e endereço interno alcançável entre as APIs. Em produção use rede privada/HTTPS e gerenciamento de segredos.

Novas opções documentadas em `backend/.env.example`:
- `FUTURE_TALENT_POLL_ENABLED=true`
- `FUTURE_TALENT_POLL_DELAY_MS=60000`

A ausência da configuração segura impede os indicadores interplataformas com erro explícito, sem expor dados. Falhas de conexão mantêm trabalhos pendentes para tentativa posterior; não marcam uma vaga, pagamento ou matrícula ficticiamente.

Antes de atualizar produção: backup independente dos bancos, revisão das migrações aditivas, configuração da integração, execução de testes e atualização do backend/frontend. Não apagar o histórico Flyway. Para reverter dados em caso de falha, use o backup validado do processo de implantação; não excluir linhas do histórico de migrações. As versões foram consultadas nos bancos locais ao final: Oportunidades V15, Empresas V12 e Capacita V16. Reinicie o backend em execução para carregar as novas classes. Nenhuma implantação externa foi realizada.

## Testes e evidências

Resultados em 29/10/2026:

| Verificação | Resultado |
|---|---|
| Lint do frontend | Aprovado, sem avisos |
| Testes unitários do frontend | 7 aprovados |
| TypeScript e build de produção | Aprovados |
| Suíte completa de navegador | 56 aprovados em 2,2 minutos |
| Navegação após atualizar o módulo empresarial | 14 cenários de ecossistema/talentos repetidos e aprovados, incluindo persistência do módulo após recarregar |
| Suíte completa do backend | 121 testes aprovados, zero falhas, erros ou ignorados; 6 min 29 s |
| Verificações finais do processamento | Três testes do worker e jornadas HTTP reais aprovados; agregação final dos relatórios: 122 casos distintos, zero falhas, erros ou ignorados |
| Banco novo e atualização | Migrações aplicadas nos três bancos de teste; atualização preserva usuários e 10.000 declarações do Radar; Capacita sem nova migração |
| Agregado sintético | 10.000 interessados; 347 ms na suíte completa e 606 ms em repetição isolada do módulo, amostras locais sem compromisso de latência |
| Página de entrega | 500 destinatários, Index Only Scan em `idx_future_interest_notify`; 0,481 ms de execução na evidência mais recente |
| Concorrência de interesse | 24 requisições HTTP reais com oito trabalhadores, 420 ms na suíte completa; apenas um interesse por usuário/previsão |
| Acessibilidade dos novos painéis | axe sem violações nos escopos verificados; teclado, computador e celular; seletor sobreposto em tela estreita corrigido |
| Git diff --check | Aprovado |

Depois da suíte completa, foram reforçados isolamento do processamento, limpeza sob falha de integração, filtro de elegibilidade da fila e exibição de evolução apenas da revisão atual. Os testes afetados foram repetidos. A verificação do mural foi repetida com FFmpeg configurado, pois a primeira chamada isolada sem essa ferramenta não executou seu cenário de mídia. A suíte completa já havia executado esse cenário com a ferramenta disponível.

Os dados de navegador são respostas sintéticas identificadas. A jornada HTTP e o ensaio de consultas usam PostgreSQL real em schemas temporários. As medições não são resultados de clientes, estatísticas de empregabilidade, teste de carga sustentada ou capacidade para 10.000 sessões simultâneas.

Novos testes: `FutureModelTest`, `FutureTalentHttpPostgresTest`, `FutureTalentWorkerTest`, `FutureQueryBudgetPostgresTest` e `future-talent-journey.spec.ts`.

A jornada HTTP usa três bancos PostgreSQL reais com schemas temporários, contas sintéticas e APIs reais. Cobre criação/edição, empresa não aprovada, propriedade, papéis, tokens de outras trilhas, revisão otimista, Radar, cursos publicados, consentimento, faixas/supressão, declaração versus comprovação, evolução, redefinição da referência, plano inválido, atualização em segundo plano, vínculo de vaga real, estágio não pago, duplicação, autorização de avisos, leitura privada, vaga encerrada, retirada e exportação.

O teste de consulta usa dez mil candidatos sintéticos, preserva dez mil declarações existentes do Radar durante a migração, mede o agregado e registra `EXPLAIN (ANALYZE,BUFFERS)` da página de destinatários em `backend/target/audit/future-query-plan.txt`. É uma medição local de consultas, não homologação de milhares de acessos simultâneos ou compromisso de desempenho em produção.

No navegador: computador/celular, teclado, consentimentos separados, retirada, recomendações, independência de contas, abertura de aviso, edição empresarial e verificação axe no escopo novo. Ferramentas automáticas não equivalem a certificação completa WCAG ou validação manual com todas as tecnologias assistivas.

Para reproduzir: em `frontend`, `npm run lint`, `npm test`, `npm run build`, `npm run test:e2e`. Apenas os novos fluxos: `npm run test:e2e -- future-talent-journey.spec.ts`. Em `backend`, com os três bancos locais disponíveis: `.\mvnw.cmd '-Dtest=Future*Test' test`; suíte completa: `.\mvnw.cmd '-Dspring.jpa.hibernate.ddl-auto=validate' test`. Os testes anteriores de mídia exigem FFmpeg/FFprobe configurados como na documentação do Capacita. Nunca execute os testes com credenciais de produção.

## Arquivos desta entrega

Criados:
- `backend/src/main/java/com/trampolim/api/future/FutureModel.java`
- `backend/src/main/java/com/trampolim/api/future/FutureTalentService.java`
- `backend/src/main/java/com/trampolim/api/future/FutureTalentController.java`
- `backend/src/main/java/com/trampolim/api/future/FutureTalentWorker.java`
- `backend/src/main/java/com/trampolim/api/company/CompanyFutureController.java`
- `backend/src/main/resources/db/migration/V15__future_talent_interests_and_notifications.sql`
- `backend/src/main/resources/db/migration-empresas/V12__future_talent_demands.sql`
- `backend/src/test/java/com/trampolim/api/future/{FutureModelTest,FutureTalentHttpPostgresTest,FutureTalentWorkerTest,FutureQueryBudgetPostgresTest}.java`
- `frontend/src/features/future/{FutureCompanyPanel,FutureTalentPanel}.tsx`, `types.ts`
- `frontend/e2e/future-talent-journey.spec.ts`
- Este relatório.

Adaptados:
- `backend/src/main/java/com/trampolim/api/company/CompanyRadarController.java`
- `backend/src/main/java/com/trampolim/api/radar/{RadarService,RadarErrors}.java`
- `backend/src/main/java/com/trampolim/api/config/SecurityConfig.java`
- `backend/src/main/java/com/trampolim/api/user/ProfileController.java`
- `backend/src/test/java/com/trampolim/api/radar/RadarMigrationPostgresTest.java` (versões esperadas ampliadas; verificações preservadas)
- `backend/src/main/resources/application.properties`, `backend/.env.example`, `backend/pom.xml`
- `frontend/src/App.tsx`, `frontend/src/features/account/{AccountNavigation,CompanyDashboard}.tsx`
- `README.md`.

Arquivos já modificados anteriormente não foram atribuídos a esta entrega.

## Pendências e limitações reais

- Confirmação de contratação exige uma fonte de resultados e validação institucional ainda não existente. A métrica fica indisponível, sem inventar resultados.
- Início de plano é autodeclarado. Não foi criado compartilhamento adicional de matrículas ou histórico privado do Capacita; comprovações usam o consentimento existente.
- Nenhum perfil individual é disponibilizado à empresa. Um eventual recrutamento identificável exige fluxo próprio de autorização e finalidade, não incluído aqui.
- Avisos são internos e periódicos; e-mail, push e canais externos não fazem parte desta etapa.
- Pequenos grupos não recebem números detalhados. Isso é deliberado para reduzir exposição.
- Catálogo útil depende de previsões reais, critérios revisados e cursos publicados com vínculos de competências. Sem esses registros, o sistema apresenta estados vazios.
- Homologar carga sustentada, filas, limites de operação, retenção de auditoria e infraestrutura na instalação de destino. Resultados sintéticos não são estatísticas de impacto ou capacidade contratada.
- Não foi iniciada a Prioridade 3.
