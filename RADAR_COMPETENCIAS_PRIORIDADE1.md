# Radar de competências — Prioridade 1

## Auditoria e decisões

Implementação sobre a versão local em `C:\Users\Windows\trampolim-main`, branch `main`. Havia alterações locais extensas das etapas educacionais e de escalabilidade, incluindo arquivos ainda não rastreados; foram preservadas. Não houve commit, push, reset ou substituição pelo GitHub.

Confirmados React/TypeScript/Vite/Tailwind, Java 21/Spring Boot/Security, JWT por trilha, PostgreSQL e Flyway com três conexões e transações independentes. Reutilizados perfil, experiências, formações, catálogo, empresas/membros/vagas, avaliações versionadas, tentativas e conquistas do Capacita. Não existia uma taxonomia comum comprovada nem requisitos estruturados suficientes para um match explicável. O diagnóstico e os textos livres continuam disponíveis, sem inferir comprovações ou condições eliminatórias automaticamente.

Sequência executada: catálogo e contratos → critérios e cálculo → APIs públicas de metadados → autorização entre contas → interfaces → testes integrados e regressão. Somente a Prioridade 1 foi desenvolvida.

## Recursos e acesso

- Oportunidades, após login: menu **Radar de competências**. Declarações com nível 1–4, sugestões para revisão, preferências profissionais, análises por vaga, lacunas, fórmula e critérios eliminatórios separados.
- Empresas: **Vagas e cursos**, seção de critérios do Radar. Proprietário/recrutador edita somente vagas da sua empresa. As demais permissões não recebem acesso a dados privados de candidatos.
- Administração de Oportunidades: catálogo, sinônimos, pesos e requisitos de vagas do catálogo. Validação também no servidor.
- Capacita: editor de avaliações permite vincular competências ao catálogo e declarar o nível avaliado. A aprovação gera uma conquista vinculada à versão aplicada. Em **Meu desempenho e competências**, o aluno autoriza uma conquista para o Radar ou revoga a autorização.
- Recomendações contêm apenas cursos publicados, não bloqueados, de responsáveis ativos, com avaliação publicada vinculada à competência. Sem catálogo disponível, há mensagem de indisponibilidade; sem material cadastrado, estado vazio. Não foram criados cursos ou vagas fictícios na aplicação.
- Cursos recomendados abrem o módulo no Capacita, com autenticação independente quando necessária.

## Pontuação

| Fator | Peso inicial |
|---|---:|
| Competências obrigatórias | 50 |
| Competências desejáveis | 15 |
| Experiência geral | 10 |
| Escolaridade concluída | 10 |
| Cidade de trabalho desejada | 5 |
| Modalidade | 5 |
| Preferência de área | 5 |

`pontuação = 100 × soma(peso × crédito) / soma(pesos aplicáveis)`.

Pesos são configuráveis, versionados, não negativos e somam 100, com peso profissional positivo. Competências recebem a média de `min(nível demonstrado / nível solicitado, 1)` em cada grupo. Declarações não atendem critérios que exigem avaliação comprovada. O nível comprovado vem de uma conquista aprovada, explicitamente vinculada e autorizada, revalidada no Capacita.

Experiência geral usa meses completos aproximados e une períodos sobrepostos. Não equivale a experiência em uma área específica. Datas inválidas são sinalizadas. Ausência de experiências requer confirmação para ser considerada zero, caso contrário é desconhecida. Escolaridade só considera níveis reconhecidos e concluídos, sem inferir licença profissional. Localidade usa a cidade voluntariamente desejada, sem endereço residencial; em vagas remotas/online esse fator não se aplica. Modalidade e área só se aplicam quando há preferência voluntária e critério da vaga.

Requisitos profissionais sem informação recebem zero e indicação de pendência. Fatores não exigidos não entram no denominador. Sem competência, experiência mínima ou formação estruturada, a nota é **nula**, mesmo quando existem preferências. Requisitos eliminatórios ficam separados; não há recusa automática de candidaturas. O candidato pode revisar seus dados no perfil. Os requisitos originais continuam legíveis e sujeitos à revisão humana.

O contrato do cálculo não contém nome, e-mail, foto, idade, sexo, raça, religião, deficiência, estado civil ou dados de apoio educacional. A administração deve revisar a pertinência dos requisitos e dos sinônimos. Isso não constitui certificação de ausência de vieses em decisões humanas.

## Separação, consentimento e contratos

Nenhum novo serviço acessa diretamente os bancos privados das outras trilhas. Não há sincronização de credenciais, contas ou sessões.

1. Candidato autenticado gera um código aleatório em Oportunidades, válido por 15 minutos; apenas SHA-256 é armazenado.
2. Em sua conta independente do Capacita, seleciona conquista própria padronizada e confirma o envio para a conta que gerou o código.
3. Capacita mantém recibo PENDING durável, envia somente código da competência, nível, curso, data e critério por HTTP servidor a servidor com segredo.
4. Oportunidades consome o desafio uma vez, sob bloqueio transacional, vinculando a evidência ao dono do desafio. Recibo estável evita duplicações em novas tentativas após falhas.
5. Capacita confirma ACTIVE. Em cada análise, Oportunidades revalida os recibos pela API. Revogação, exclusão, conta inativa ou indisponibilidade impedem o crédito comprovado. O candidato também pode retirar a evidência em Oportunidades.

Links públicos antigos de conquistas não são aceitos como prova de titularidade. Competências antigas sem código e nível canônicos não são convertidas por aproximação. Conquistas e certificados formais permanecem conceitos distintos.

Principais APIs:

- `GET /api/public/skills`, `/resolve`: taxonomia pública; sinônimos globais normalizados únicos.
- `/api/radar/profile`, `/skills`, `/preferences`, `/link-challenges`, `/matches/{source}/{id}`, `/jobs`: somente conta verificada de Oportunidades, com escopo próprio.
- `/api/radar/admin/skills`, `/admin/policy`, `/admin/catalog/{id}/requirements`: administração autorizada.
- `/api/company/listings/{id}/radar`: critérios, com propriedade e revisão otimista.
- `/api/public/companies/radar-jobs`: apenas vagas publicadas, empresa aprovada, prazos válidos e estágio pago. Nenhum contato privado ou candidato é retornado.
- `/api/capacita/learning/radar/exports`: autorização, histórico e revogação pelo aluno titular.
- `POST /api/integrations/radar/award`, `/api/integrations/capacita/radar/evidence`: segredo de servidor obrigatório, comparação em tempo constante. Não utilizar tokens de usuário para esses contratos.
- `GET /api/public/capacita/radar/training`: somente metadados públicos, sem respostas de provas, arquivos privados ou dados de alunos.

## Bancos e migrações

| Banco | Migração nova | Estruturas |
|---|---|---|
| Oportunidades | V14 | skill_concepts, skill_terms, candidate_skill_declarations, candidate_radar_preferences, radar_catalog_requirements, radar_scoring_policy, radar_claim_challenges, candidate_skill_evidence, radar_match_cache, radar_audit |
| Empresas | V11 | company_radar_requirements, índice de vagas publicadas |
| Capacita | V16 | skill_code/skill_level nas conquistas; learning_radar_exports; integridade de titularidade; índice GIN das competências de avaliações |

Migrações aditivas, sem apagar histórico ou dados existentes. Só o catálogo de 13 competências genéricas é inicializado; nenhum resultado de avaliação é inventado. As referências privadas locais usam FK/cascade. Não existe FK entre bancos. Exportação de conta inclui dados do Radar; exclusão preserva os mecanismos existentes e elimina dependentes locais. Se a conta de origem for removida, a revalidação deixa de reconhecer o recibo.

Testes criam schemas temporários nos três bancos reais e removem somente seus próprios schemas. As novas migrações também são verificadas sobre as versões anteriores preservando credenciais sintéticas existentes. As migrações foram aplicadas e suas versões consultadas também nos bancos locais: Oportunidades 14, Empresas 11 e Capacita 16. Reinicie o backend em execução para carregar as novas classes; produção exige backup e procedimento normal de atualização. Não foi realizada implantação externa.

## Desempenho e operação

- Vagas com cursor por ID, página padrão 20/máximo 50, busca limitada a 120 caracteres.
- Até 40 critérios por vaga, 200 declarações e 100 evidências por candidato; dez desafios/hora.
- Cache persistente por candidato/fonte/vaga, limitado a 200 entradas por candidato; impressão digital inclui dados profissionais, níveis, critérios, política e estado das evidências.
- Recalcula apenas a vaga selecionada quando a impressão digital muda, com bloqueio para evitar cálculo duplicado concorrente. Consulta a disponibilidade da vaga e validade das evidências antes de usar cache. Recomendações são consultadas novamente, refletindo retirada de publicação.
- HTTP reutilizado, conexão/leitura com limites, sem redirecionamento e sem repetição automática de mutações. Até 16 integrações simultâneas por processo, com falha explícita quando saturado. Não foram adicionadas filas ou infraestrutura assíncrona sem necessidade medida.
- Índices nas relações de usuário/competência, histórico de desafios/recibos, cache por usuário/data, feeds publicados e JSONB de competências.
- Contadores Micrometer `radar.matches.evaluated` e `radar.matches.cache.hits`, no endpoint operacional já protegido. Não contêm identidades.
- Desafios expirados antigos são removidos para o próprio usuário ao gerar outro código; auditoria e recibos seguem a retenção operacional a definir antes de produção.

## Configuração

`backend/.env` continua privado e ignorado pelo Git. Foram adicionadas ao exemplo apenas as configurações:

- `RADAR_INTERNAL_API_URL`: endereço privado da API para as integrações; padrão local `http://127.0.0.1:8080/api`. Em contêineres ajuste para o endereço alcançável pelo backend, respeitando a porta interna.
- `RADAR_BRIDGE_SECRET`: segredo aleatório exclusivo com pelo menos 32 caracteres, somente servidor. A ausência desabilita a vinculação com erro explícito. No ambiente local foi preparado um valor privado; não é exibido nem incluído no relatório.

Para produção use HTTPS/rede privada, gerenciador de segredos, rotação coordenada e acesso restrito aos endpoints de integração. Não libere o segredo no frontend. SMTP, Mercado Pago e as chaves JWT anteriores não foram substituídos e não são necessários para o cálculo determinístico. Administradores e professores continuam usando os mecanismos existentes.

## Testes e resultados

Resultados em 29/10/2026:

| Verificação | Resultado |
|---|---|
| Frontend lint | Aprovado, sem avisos |
| Frontend unitário | 7 aprovados |
| Frontend TypeScript e build de produção | Aprovados |
| Navegador, suíte completa | 50 aprovados em 2,1 minutos |
| Navegador, Radar ampliado | 5 aprovados em 17,3 s; dois casos novos além dos três já incluídos na suíte completa; 52 casos distintos validados |
| Backend, suíte completa | 113 executados; 112 passaram e um teste novo falhou na preparação de conta sintética, por ausência do aceite de termos |
| Backend, repetição após corrigir somente a preparação do teste | RadarMigrationPostgresTest aprovado; relatórios finais agregados: 113 casos, zero falhas/erros/ignorados |
| Migrações | Bancos vazios e atualização Oportunidades 13→14, Empresas 10→11, Capacita 15→16; credenciais sintéticas preservadas e segunda execução sem reaplicar |
| Concorrência do motor | 10.000 avaliações, 16 trabalhadores, 294 ms na execução completa local |
| Cache na jornada HTTP | 4 avaliações e 2 acertos de cache; alterações e revogação invalidadas |
| Acessibilidade do Radar | axe sem violações no escopo `main`, computador e celular; envio por teclado e ausência de rolagem horizontal nas larguras testadas |
| Git diff --check | Aprovado |

O teste novo de migração foi repetido isoladamente porque apenas a sua preparação mudou, sem alterar o código da aplicação após a suíte completa. A conta sintética passou a registrar o aceite obrigatório, sem remover a restrição do banco. Os testes de navegador usam respostas sintéticas identificadas; a jornada HTTP usa APIs reais e PostgreSQL real nos três bancos. O ensaio de 294 ms mede apenas o motor em memória, não a latência das APIs nem capacidade de produção.

Problemas corrigidos durante a validação: compatibilidade JSON entre cliente e servidor, retorno de listas de recomendações, textos com codificação incorreta, cancelamento de análise ao trocar de consulta e integridade do vínculo entre conquista e titular.

Testes novos: MatchEngineTest (oito cenários, incluindo 10 mil cálculos com 16 trabalhadores), RadarHttpPostgresTest (três bancos e APIs reais), RadarMigrationPostgresTest (atualização das três trilhas), radar-journey.spec.ts (computador, celular, teclado, axe, consentimento, critérios empresariais e direcionamento ao módulo).

A jornada HTTP usa uma avaliação realmente aprovada para gerar a conquista, sem inserir uma comprovação artificial diretamente. Verifica isolamento, catálogo/sinônimos, permissões, critérios, revisão otimista, recomendações publicadas, idempotência, invalidação de cache, revogação, indisponibilidade e expiração da vaga.

## Arquivos desta entrega

Novos:
- `backend/src/main/java/com/trampolim/api/radar/{RadarModel,MatchEngine,RadarBridge,SkillCatalog,SkillCatalogController,RadarService,RadarController,RadarErrors}.java`
- `backend/src/main/java/com/trampolim/api/company/CompanyRadarController.java`
- `backend/src/main/java/com/trampolim/api/education/EducationRadarController.java`
- As três migrações da tabela acima.
- `backend/src/test/java/com/trampolim/api/radar/{MatchEngineTest,RadarHttpPostgresTest,RadarMigrationPostgresTest}.java`
- `frontend/src/features/radar/{SkillPicker,RequirementsEditor,CompanyRadar,RadarAdmin,RadarPanel}.tsx`, `radarUi.ts`
- `frontend/src/features/learning/RadarExportPanel.tsx`
- `frontend/e2e/radar-journey.spec.ts`
- Este relatório.

Adaptados:
- `backend/src/main/java/com/trampolim/api/education/{AssessmentService,LearningPerformanceService,CapacitaController}.java`
- `backend/src/main/java/com/trampolim/api/user/ProfileController.java`
- `backend/src/main/java/com/trampolim/api/config/SecurityConfig.java`
- `backend/src/main/resources/application.properties`, `backend/.env.example`
- `frontend/src/App.tsx`, `frontend/src/features/account/{AccountNavigation,CompanyDashboard}.tsx`
- `frontend/src/features/learning/assessmentTypes.ts` e `frontend/src/features/learning/{AssessmentEditor,PerformancePanel,LearningPortal}.tsx`.
- `README.md`.

Os demais arquivos já modificados antes desta tarefa não fazem parte do escopo deste relatório.

## Limitações e próximas decisões

- Preenchimento real de critérios pelos recrutadores e vínculos de competências pelos professores é necessário para obter análises úteis. Não há backfill semântico automático de provas antigas.
- Níveis são definidos pelo professor responsável e comprovam o critério daquela avaliação; não representam certificação externa ou habilitação profissional.
- Sem avaliação vinculada publicada, não há recomendação fictícia de curso para a lacuna. Outras fontes de cursos requerem contratos futuros.
- Não há acesso do recrutador a pontuações privadas de candidatos, pois não foi autorizada transferência de perfis/candidaturas entre bancos. O recrutador gerencia os requisitos e sua revisão humana; compartilhamento de análise individual exigiria consentimento e contrato próprio.
- Comparação de cidade/área é literal normalizada; não calcula distância nem equivalência automática de ocupações. Experiência é geral e formações em texto desconhecido pedem correção humana.
- Validações de navegador e axe não equivalem a certificação completa de acessibilidade ou testes manuais com todas as tecnologias assistivas.
- O ensaio concorrente do motor e o cache não demonstram capacidade para milhares de sessões HTTP simultâneas. Dimensionamento, carga sustentada, filas, SLOs e limites devem ser homologados na infraestrutura de destino.
- Definir retenção de auditoria/recibos e operação de suporte antes da produção. Regras de empregabilidade e decisões humanas exigem revisão institucional.


## Reproduzir a validação

Na pasta `frontend`: `npm run lint`, `npm test`, `npm run build`, `npm run test:e2e`. Para os cinco cenários novos: `npm run test:e2e -- radar-journey.spec.ts`.

Na pasta `backend`, com os três bancos locais disponíveis: `.\mvnw.cmd '-Dspring.jpa.hibernate.ddl-auto=validate' test`. Os testes de vídeo exigem `CAPACITA_FFMPEG` e `CAPACITA_FFPROBE` apontando para ferramentas instaladas, conforme a documentação do Capacita. Para o Radar: `.\mvnw.cmd '-Dtest=MatchEngineTest,RadarHttpPostgresTest,RadarMigrationPostgresTest' test`.

Os testes do Radar usam os mesmos endereços e credenciais de desenvolvimento dos serviços PostgreSQL definidos no Compose e no CI, nunca credenciais reais de produção. Não execute esses testes contra produção.
