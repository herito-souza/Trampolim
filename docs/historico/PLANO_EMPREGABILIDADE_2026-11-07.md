# Plano de Empregabilidade Individual — Oportunidades

## Auditoria e decisão de arquitetura
A versão local já possui diagnóstico persistido em app_users.diagnostic_data, preferências/declarações/comprovações do Radar, requisitos estruturados de vagas, política explicável de compatibilidade e contratos HTTP com Empresas/Capacita. Também existem orientações fixas de primeiro emprego em career_first_job_steps e aprendizagem adaptativa dentro do Capacita. Esses módulos foram preservados.

O novo plano combina dados desses recursos sem substituir o diagnóstico ou duplicar o motor de compatibilidade. Precisa de persistência própria para objetivos estruturados, revisões, etapas por critério e histórico de gerações: as três notas fixas da jornada de primeiro emprego não representam esses relacionamentos. Não foram alteradas as tabelas ou credenciais de outras trilhas.

## Fontes reutilizadas
- Diagnóstico salvo: disponibilidade e identificação da trilha de interesses. Respostas/pontuações não são convertidas em níveis de competência nem copiadas integralmente para o plano.
- RadarService: preferências voluntárias, experiência e escolaridade reconhecidas no perfil, competências declaradas, comprovações importadas com consentimento, validação de evidências e MatchEngine/política existentes.
- Catálogo público local: vagas ativas/aprovadas e não vencidas, com requisitos estruturados quando cadastrados.
- Empresas: contrato público /api/public/companies/radar-jobs, sem acesso JDBC ao banco corporativo.
- Capacita: contrato público /api/public/capacita/radar/training e contrato protegido de validação de evidências já usado pelo Radar. Nenhuma senha/sessão estudantil é sincronizada.

Não são usados IA generativa, biometria, raça, gênero, saúde, deficiência, idade, nacionalidade ou outros atributos pessoais para classificar candidatos. O texto livre da ocupação serve à busca voluntária de títulos/áreas, sem inferir personalidade, capacidade ou proficiência. As competências precisam existir no catálogo padronizado ativo.

## Regras do motor, versão 1
1. O usuário revisa seus objetivos. Campos: ocupação, área, cidade, modalidade, até dez competências com níveis de 1 a 4 e até três vagas de referência.
2. Preferências existentes do Radar são sugeridas quando ainda não existe plano. Salvar objetivos não altera silenciosamente essas preferências: o plano pode recomendar sua revisão no endpoint existente.
3. Vagas explicitamente escolhidas são consultadas novamente. Se nenhuma for escolhida, busca-se uma amostra limitada usando ocupação/área: até três resultados por fonte, no máximo três análises, na ordem catálogo local e Empresas. Não é varredura completa nem ranking do mercado regional. Cidade/modalidade não são inferidas como filtros geográficos precisos; as análises mantêm os critérios e preferências do Radar.
4. Requisitos profissionais continuam calculados pelo MatchEngine. Obrigatórios, desejáveis, experiência, formação, preferências, pesos, revisões e justificativas permanecem nas análises armazenadas. Requisitos sem estrutura não geram proficiência, nota inventada ou cursos deduzidos do texto da vaga.
5. Metas de competência escolhidas pelo usuário passam pela mesma análise do Radar, sem criar vaga fictícia ou linha falsa no cache de vagas. Declarações não satisfazem requisitos que exigem comprovação.
6. Lacunas repetidas viram uma etapa por competência, usando o nível máximo solicitado. Em caso de indisponibilidade da validação de evidências, recomenda-se revisar/revalidar a comprovação, sem afirmar desconhecimento nem recomendar repetição de cursos a partir dessa incerteza.
7. Até quarenta lacunas são priorizadas: metas explícitas do usuário, competências obrigatórias e código do catálogo como desempate determinístico. A limitação é indicada por truncated.
8. Cursos só vêm de ofertas publicadas pelo contrato educacional. São agrupados por curso/módulo, sem duplicar a mesma recomendação por várias competências ou vagas. Há no máximo vinte módulos recomendados. A cobertura registra nível oferecido e desejado: curso de nível inferior é identificado como contribuição parcial, não resolução completa da lacuna.
9. Podem ser propostas etapas de revisão de objetivo, diagnóstico, preferências, formação/experiência, desenvolvimento/revalidação de competências, consulta de capacitação e busca de oportunidades. Ausência de curso/vaga gera orientação de revisão/busca, não material fictício.
10. Todas as etapas possuem chave estável, critérios versionados, hash canônico e recurso relacionado quando houver. Mapas são ordenados no hash para não perder progresso por ordem de serialização entre JVMs.

O plano não envia candidaturas, matricula estudantes, concede competências, emite certificados ou confirma contratações.

## Persistência e atualização
- Um plano atual por usuário de Oportunidades.
- revision é compartilhada por objetivos, geração e progresso; mutações exigem expectedRevision. Conflitos retornam 409.
- Objetivos novos invalidam a geração ativa e desativam suas etapas. O histórico anterior continua acessível; é necessário gerar novamente.
- Geração é explícita por POST. GET lê o snapshot salvo e calcula apenas uma impressão dos dados locais relevantes; não consulta outras trilhas nem refaz o match remoto.
- Impressão inclui objetivos, diagnóstico, declarações, registros de comprovação, preferências, datas profissionais, formação e revisão da política; o mês atual também entra porque experiência em andamento evolui com o tempo.
- Se os dados locais da pessoa/política observados mudarem durante o cálculo ou houver edição concorrente do plano, o resultado não substitui silenciosamente uma versão mais recente.
- Chamadas HTTP são feitas antes da transação que grava o plano. Não se mantém bloqueio de plano/banco durante espera por outras trilhas.
- Regenerar preserva progresso somente quando chave e critérios continuam iguais; mudanças relevantes reiniciam a etapa. Etapas retiradas ficam inativas e não aceitam atualização.
- Até vinte gerações e duzentas etapas inativas são mantidas por usuário. DELETE do plano apaga objetivos, etapas e gerações em cascata; exclusão da conta também remove esses dados. A auditoria operacional mínima existente não contém objetivos ou pontuações e continua segundo sua política própria.

## Progresso declarado, não comprovação
Estados permitidos: NOT_STARTED, IN_PROGRESS, DECLARED_DONE e DISMISSED. O registro inclui data de atualização. A API informa quantidade de etapas ativas e declaradas concluídas; não apresenta esses valores como percentual de aprovação acadêmica ou de empregabilidade.

Uma etapa marcada DECLARED_DONE não muda competências do Radar, histórico acadêmico, notas, evidências ou candidaturas. Competências verificadas continuam dependendo das integrações autorizadas existentes.

## Contratos HTTP
Base: /api/employability-plan. Exige JWT de Oportunidades e conta pessoal verificada. Não há parâmetro userId para consultar/alterar plano de outra pessoa; o proprietário é sempre resolvido a partir da sessão. Tokens de Empresas/Capacita não são aceitos.

| Método | Rota | Finalidade |
| --- | --- | --- |
| GET | /api/employability-plan (sem barra final) | Plano atual, snapshot, impressão local/progresso; sem plano retorna exists=false e revisão 0 |
| PUT | /objectives | Criar/revisar objetivos com controle de versão |
| POST | /refresh | Gerar ou atualizar recomendações |
| PUT | /steps/{key} | Alterar progresso declarado de etapa ativa do usuário |
| GET | /generations | Histórico com cursor before e limit de 1 a 20 (padrão 10) |
| DELETE | /api/employability-plan?expectedRevision=N | Excluir o plano atual e dados relacionados |

### Salvar objetivos

```json
{
  "expectedRevision": 0,
  "objectives": {
    "occupation": "Assistente administrativo",
    "area": "Administração",
    "city": "Irecê, BA",
    "modality": "Presencial",
    "skills": [{"skillCode": "excel", "level": 2, "requireEvidence": false}],
    "jobs": []
  }
}
```

Revisão 0 cria o plano; atualizações usam a revisão atual. skills/jobs são listas obrigatórias, podendo estar vazias. source de uma vaga é catalog ou company, com id positivo; referências duplicadas são rejeitadas. Campos opcionais de texto podem ser vazios. requireEvidence é opcional, falso por padrão.

### Gerar recomendações

```json
{"expectedRevision": 1, "requestKey": "7b146915-7302-4345-9fde-75fe9b285207"}
```

A resposta contém replayed, generatedRevision, generation e plan. Repetições idênticas da mesma chave não geram novo histórico. Reutilizar a chave com outra revisão retorna 409. A resposta repetida informa a geração original e o plano atual; este pode ter sido editado depois. Para a próxima mutação, use plan.revision.

Há limite de dez tentativas por hora por usuário/processo e cinco gerações confirmadas por hora na auditoria persistida, além dos limites de integração existentes. Uma geração pode exigir consultas sequenciais a fontes limitadas; o frontend deve apresentar carregamento e impedir reenvios com novas chaves. Uma nova decisão precisa de novo UUID.

### Registrar progresso

```json
{"state":"IN_PROGRESS","expectedRevision":2}
```

Use a key retornada em snapshot.steps. Critérios desatualizados ou plano alterado retornam 409; etapa inexistente/inativa do usuário retorna 404. Não existe estado cliente VERIFIED/APPROVED/HIRED. Após erro, recarregue o plano e revise a intenção antes de reenviar.

### Campos e interpretação
- objectives: objetivo atual; snapshot.objectives é o objetivo utilizado naquela geração.
- snapshot.ruleVersion, analyses, sources, steps.criteria: critérios/fatores/fontes auditáveis.
- snapshot.partial: alguma fonte ou evidência não estava disponível.
- snapshot.truncated: limites explícitos de lacunas/módulos atingidos.
- localInputsChanged: alteração dos dados locais observados desde a geração ou necessidade de gerar pela primeira vez.
- sourceFreshness=SNAPSHOT_NOT_LIVE: vagas, cursos e evidências representam as consultas feitas na geração. Esse campo nunca significa catálogo atual em tempo real. Vagas podem fechar e cursos podem sair de publicação sem notificação imediata ao plano; use refresh antes de agir e confira a fonte ao acessá-la.
- progress: estados declarados, com step_key, criterion_hash e updated_at. O histórico usa os campos generation, produced_revision, created_at e snapshot.

A interface deve distinguir indisponibilidade de ausência de resultados e não converter falha de fonte em “nenhum curso/vaga existe”. Os conteúdos são texto/dados; não renderize HTML fornecido por títulos/descrições sem tratamento apropriado.

## Migração necessária
V20__individual_employability_plan.sql, apenas no diretório Flyway de Oportunidades:
- individual_employability_plans;
- individual_employability_steps;
- individual_employability_runs;
- índice para cota de geração na auditoria já existente.

São criações aditivas, com FKs e exclusão em cascata, sem apagar/recriar usuários, diagnóstico ou tabelas existentes. Os históricos anteriores do Flyway permanecem. A versão esperada no teste de atualização passou de 19 para 20 em Oportunidades; Empresas/Capacita continuam nas versões locais anteriores (17/20).

Não foi aplicada migração no schema operacional. Ao atualizar a aplicação, seguir o processo de backup/implantação existente e permitir execução normal do Flyway. A criação do índice em auditoria pode exigir janela adequada se essa tabela tiver grande volume.

## Arquivos criados e modificados
Criados no pacote backend/src/main/java/com/trampolim/api/planning/: EmployabilityPlanModel, EmployabilityPlanEngine, EmployabilityPlanService, EmployabilityPlanController e PlanHashes. Nova migração V20 no banco de Oportunidades.

Modificados: RadarService (reutilização da análise para metas, sem vaga/cache fictício, e leitura autenticada de preferências), RadarErrors (respostas de erro da nova API), RadarMigrationPostgresTest (versão esperada), README.md e este relatório. Nenhum contrato HTTP anterior foi removido.

Testes novos: EmployabilityPlanEngineTest e EmployabilityPlanPostgresTest. Fixtures de vagas/cursos existem apenas em schemas temporários/servidor HTTP de teste, identificados como sintéticos; não são conteúdo operacional.

## Testes e execução
Primeira execução focada: 23 testes aprovados, zero falhas/ignorados. Incluiu motor, serviço/HTTP, MatchEngine, integração do Radar e Flyway.

Verificações executadas:
- Suíte completa: 209 casos registrados, 208 aprovados, zero falhas/erros e um teste de carga opt-in não executado (ops/individual-plan-full.log). As integrações reais de mídia foram habilitadas; esse recurso não ficou ignorado por ausência de ferramenta.
- Foram aprovados os testes de geração, revisão de objetivos, competências declaradas/comprovadas, ausência de ofertas, fontes indisponíveis, deduplicação, limites, progresso, revogação de etapas, reavaliação de requisitos, permissões, HTTP, Radar e Flyway.
- Revisão final identificou risco de ordem de bloqueios entre usuário, plano e auditoria. As escritas agora bloqueiam primeiro a conta pessoal verificada e depois o plano, mantendo chamadas HTTP fora dessa transação.
- Reteste final do motor e serviço: 18 aprovados, zero falhas/ignorados (ops/individual-plan-final.log), incluindo novo teste com atualização de objetivos, progresso e geração concorrentes: exatamente um sucesso e dois conflitos 409, sem bloqueio circular ou revisão perdida.
- A suíte inteira não foi repetida após esse ajuste restrito ao serviço do plano; os testes afetados foram repetidos. A compilação e o empacotamento Maven verify passaram nas duas execuções. Não se somam os 18 casos repetidos aos 208 como se fossem todos diferentes.
- Build Java 21/Spring Boot aprovado; jar em backend/target/backend-0.0.1-SNAPSHOT.jar. Sem implantação.
- Migrações de bancos novos e atualização Flyway aprovadas na suíte, preservando os usuários existentes e seus hashes. Nenhuma migração foi aplicada ao schema operacional.
- git diff --check sem erros nos arquivos verificados.
- Evidências de isolamento e limpeza: ops/individual-plan-full-2026-11-07/isolated-tests.json e ops/individual-plan-final-2026-11-07/isolated-tests.json, com realDataUsed=false e verifyExitCode=0. Os schemas temporários foram removidos ao término.

Sem chamadas a provedores pagos, uso de dados pessoais reais ou testes de carga representativos. As fontes externas específicas do plano foram simuladas em servidor HTTP local e os contratos reais existentes também foram exercitados pelos testes de integração do Radar.

```powershell
.\ops\scripts\Test-BackendSecurity.ps1 -EvidenceName individual-plan-2026-11-07 -TestPattern 'EmployabilityPlan*Test,MatchEngineTest,Radar*PostgresTest'
```

O script usa bancos Docker locais em schemas descartáveis, sem restaurar ou sobrescrever schemas existentes. A suíte geral pode precisar das variáveis CAPACITA_FFMPEG/CAPACITA_FFPROBE para integrações de mídia; não são dependências do motor do plano.

## Configuração e limites reais
- Reutiliza RADAR_INTERNAL_API_URL e RADAR_BRIDGE_SECRET existentes. Não cria/solicita segredos novos; configure-os no ambiente, nunca no frontend. A validação de evidências exige a configuração segura existente.
- Sem IA generativa obrigatória, Redis, fila, nova biblioteca ou alteração das credenciais de outras trilhas.
- Em caso de indisponibilidade de Empresas/Capacita, fornece dados locais e status parcial quando possível. Se o banco próprio estiver indisponível, não finge salvamento.
- Esta entrega é backend. Não foi criada tela do plano nem alterado dashboard; endpoints estão preparados para integração do frontend.
- Atualizações de disponibilidade e requisitos de vagas/cursos são verificadas em refresh, não por assinatura de eventos. O contrato público educacional não fornece revisão de conteúdo; mudanças que não alterem os critérios públicos disponíveis não reiniciam, por si só, uma declaração de progresso. Não há inferência de cursos a partir de texto livre nem garantia de cobertura de todas as vagas/cursos da região.
- Não foi executado teste de carga representativo de milhares de usuários; limites de chamadas e consultas não equivalem a prova de capacidade. Antes de expandir, medir latência real das integrações, concorrência e taxa de geração.
- Conformidade jurídica, bases legais e retenção temporal devem ser validadas pelo responsável por dados. GET/histórico/DELETE permitem controle do próprio plano; não foi criada exposição pública ou acesso administrativo indiscriminado.


## Revalidação e correção na solicitação repetida — 07/11/2026
A auditoria confirmou que o motor e os seis endpoints já estavam presentes; não foi criado um fluxo paralelo. Objetivos, diagnóstico, Radar, fontes educacionais públicas, critérios, deduplicação, revisões, progresso declarado e histórico foram preservados.

Correção adicional: a exclusão do plano agora adquire primeiro o bloqueio da conta pessoal verificada e depois o do plano, seguindo a mesma ordem das outras escritas. Isso evita a inversão com atualização de progresso/objetivos e auditoria. Nenhuma nova migração foi necessária.

Arquivos alterados nesta revalidação:
- backend/src/main/java/com/trampolim/api/planning/EmployabilityPlanService.java: ordem de bloqueios na exclusão.
- backend/src/test/java/com/trampolim/api/planning/EmployabilityPlanPostgresTest.java: novo teste de exclusão concorrente com progresso, em três rodadas, verificando uma única operação vencedora, revisão consistente ou remoção em cascata; objetivos exclusivos no cenário de acesso entre usuários, com verificação de que a etapa não existe no plano do solicitante.
- Este relatório e evidências em ops/individual-plan-*-revalidation* e ops/individual-plan-delete-concurrency*.

Testes executados nesta solicitação:
- Primeiros testes unitários: 13 aprovados (motor do plano e MatchEngine).
- Primeira revalidação com PostgreSQL: 28 aprovados, zero falhas/erros/ignorados, Maven verify aprovado.
- Rodada após acrescentar concorrência: 18 aprovados e uma falha no cenário de permissões. O objetivo sintético genérico encontrava vagas SQL criadas por outros testes e gerava uma etapa SQL legítima também no plano solicitante. A fixture foi isolada por objetivo único; a validação de negação de acesso não foi removida. O novo teste de concorrência passou nessa rodada.
- Rodada final completa do escopo: 29 aprovados, zero falhas/erros/ignorados, BUILD SUCCESS (ops/individual-plan-final-revalidation.log). Inclui cinco testes do motor, 14 de serviço/HTTP/PostgreSQL, oito de MatchEngine, um de integração HTTP do Radar e um de migrações. Contagens de rodadas repetidas não são somadas como casos distintos.
- A suíte global de todo o backend não foi repetida nesta solicitação; os resultados anteriores permanecem históricos, conforme seção acima.
- Evidência final: ops/individual-plan-final-revalidation-2026-11-07/isolated-tests.json. Schemas temporários removidos ao término; dados operacionais preservados. O Docker estava desligado inicialmente e foi iniciado para executar os testes. Não houve implantação nem aplicação da V20 ao schema operacional.

Os endpoints continuam sob /api/employability-plan: GET do plano; PUT /objectives; POST /refresh; PUT /steps/{key}; GET /generations; DELETE do plano com expectedRevision. A interface do plano não faz parte desta entrega backend. Permanecem necessários os contratos/configurações existentes do Radar e a aplicação controlada das migrações na implantação; não foram criadas credenciais ou ofertas fictícias.
