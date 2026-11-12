# Meu Plano de Empregabilidade — interface de Oportunidades

## Auditoria e decisão
A versão local já contém o motor do plano, a migração V20 de Oportunidades, o Radar, o diagnóstico, catálogos, candidaturas, sidebar/topbar e encaminhamento de curso/módulo ao Capacita. A interface usa esses recursos; não foi criado outro diagnóstico, motor de compatibilidade ou sistema de cursos.

Nova rota protegida pela sessão existente: #oportunidades/plan. Acesso pela categoria Carreira e desenvolvimento e pela seção Seu próximo passo. A rota participa da leitura inicial, atualização, histórico do navegador e seleção ativa. O componente é carregado sob demanda: chunk próprio de 28,60 kB (8,15 kB gzip) no build desta entrega. Isso não representa uma medição de LCP/INP/CLS nem prova de capacidade para milhares de usuários.

## Experiência implementada
- Objetivos: ocupação, área, município e modalidade, com preferências existentes sugeridas somente quando o plano ainda não existe. É necessário informar pelo menos ocupação, área, competência ou referência de vaga, conforme o backend. Campos complementares são opcionais.
- Até dez metas de competência com SkillPicker existente, nível desejado e opção de exigir comprovação. O formulário impede duplicação da mesma competência. Referências de vagas existentes são preservadas e podem ser retiradas.
- Salvar objetivos e gerar recomendações são ações explícitas e separadas. Visitar a página não gera plano nem consome a cota de geração. Objetivos não alteram silenciosamente as preferências do Radar.
- Resumo de competências declaradas e comprovações vinculadas via Radar. Vínculos não são apresentados como certificados ou como evidências eternamente válidas; a revalidação ocorre nas análises existentes.
- Lacunas, cursos e oportunidades vêm exclusivamente do snapshot retornado pelo motor. A página não deduz lacunas a partir de texto livre, não cria ofertas e não refaz o cálculo de compatibilidade.
- Oportunidades mostram pontuação quando disponível, requisitos eliminatórios separadamente e explicações dos fatores. Pontuação ausente não é convertida em zero. São amostras históricas da geração, não garantia de disponibilidade ou contratação.
- Etapas ordenadas com acesso ao recurso correspondente, critérios e níveis, cobertura parcial de cursos e acompanhamento: ainda não comecei, em andamento, conclusão declarada ou não seguir por enquanto.
- Progresso: número de etapas atuais declaradas concluídas dividido pelo total atual de etapas. Dispensar uma etapa não a conclui; registros de etapas removidas não entram na contagem. Não é percentual de empregabilidade nem certificação.
- Próxima ação: prioriza uma etapa em andamento, depois uma ainda não iniciada. Com snapshot desatualizado, orienta atualizar o plano antes de continuar a registrar progresso.
- Histórico sob demanda, cinco gerações por página, com cursor do servidor e até vinte gerações recentes conforme a retenção existente. Consultar histórico não substitui o plano atual.
- Estados de carregamento, falha, ausência de dados, acesso negado, fontes parciais, seleção limitada de recomendações e necessidade de reavaliar critérios.

## APIs existentes utilizadas
Todas as chamadas privadas usam exclusivamente o token de Oportunidades:
- GET /api/employability-plan — plano e revisão atual.
- PUT /api/employability-plan/objectives — objectives e expectedRevision.
- POST /api/employability-plan/refresh — expectedRevision e requestKey UUID.
- PUT /api/employability-plan/steps/{key} — state e expectedRevision.
- GET /api/employability-plan/generations?limit=5&before=... — histórico paginado.
- GET /api/radar/profile — competências e comprovações da própria conta.
- GET /api/public/skills?q=...&limit=40 — catálogo público do SkillPicker existente.

Não houve contrato novo, alteração no backend, migração ou acesso direto a outros bancos nesta entrega frontend.

## Consistência, privacidade e navegação entre trilhas
Atualizações carregam a revisão recebida do servidor. Erro 409 mantém o snapshot e o formulário local; bloqueia novas gravações até o usuário optar por recarregar a versão salva. A ação avisa que descartará a edição local. Nenhuma atualização otimista concede progresso antes da confirmação do servidor.

O mesmo UUID de geração é preservado para repetir uma tentativa com falha de rede, enquanto a revisão não mudar. Operações simultâneas por cliques repetidos são bloqueadas no componente; idempotência e autorização continuam validadas pelo backend. Requisições são canceladas na saída da página e dados da conta não são guardados em cache compartilhado ou no navegador. Rascunhos de formulário ficam apenas em memória e não sobrevivem à saída da página; objetivos/progresso salvos são recuperados pela API.

Para cursos, reutiliza-se a intenção já consumida por LearningPortal: trampolim_capacita_course_intent em sessionStorage, contendo somente courseId/moduleId públicos. O usuário é encaminhado ao Capacita existente. Nenhum token, senha, perfil ou diagnóstico é copiado. Sem conta/sessão estudantil, o usuário utiliza o login independente do Capacita. O curso e o módulo são novamente consultados com a autorização educacional apropriada. Sem suporte a sessionStorage, permanece possível navegar ao catálogo.

Botões de diagnóstico, Radar, perfil/candidaturas e vagas abrem os módulos já existentes. A ocultação de navegação não substitui o controle de acesso do servidor.

## Componentes e arquivos
Criados:
- frontend/src/features/planning/planTypes.ts — tipos dos contratos existentes.
- frontend/src/features/planning/planModel.ts — progressão declarada, próxima etapa, cópia de objetivos e destinos existentes.
- frontend/src/features/planning/PlanObjectives.tsx — revisão progressiva de objetivos/metas.
- frontend/src/features/planning/PlanJourney.tsx — linha de etapas e critérios.
- frontend/src/features/planning/PlanInsights.tsx — competências, lacunas e oportunidades analisadas.
- frontend/src/features/planning/EmployabilityPlanPage.tsx — integração autenticada, atualizações, histórico e estados de falha.
- frontend/tests/employability-plan.test.mjs — quatro testes unitários do modelo de apresentação.
- frontend/e2e/employability-plan.spec.ts — cenários de navegador com fixtures identificadas, sem dados operacionais.
- Este relatório.

Modificados:
- frontend/src/App.tsx — importação lazy, rota, destino e título da topbar.
- frontend/src/features/account/accountNavigationModel.ts — registro da rota e item da sidebar.
- frontend/src/features/account/AccountNavigation.tsx — destino e alternativa de navegação fora do layout compartilhado.
- frontend/src/features/dashboard/CandidateNextStep.tsx — acesso ao plano no dashboard existente.
- README.md — referência desta documentação.

Reutilizados: AppLayout/sidebar/topbar, autenticação e api/ApiError de Oportunidades, SkillPicker, FormGuide, LoadingStatus, InlineNotice, estilos/níveis do Radar, navegação de conta, encaminhamento existente ao LearningPortal. Nenhuma dependência adicionada e nenhum menu antigo duplicado.

## Testes e evidências
- Lint final aprovado sem avisos (ops/employability-ui-lint.log). TypeScript e build de produção aprovados (ops/employability-ui-build.log). Avisos existentes de tamanho dos chunks de mapa/PDF continuam visíveis; esses módulos permanecem sob demanda.
- 23 testes unitários aprovados, incluindo quatro novos testes do modelo do plano (ops/employability-ui-unit.log).
- Quatorze cenários focados reportados como aprovados após corrigir seletores dos testes. O executor que havia iniciado o servidor local permaneceu no encerramento no Windows e foi encerrado após a suíte completa, sem afetar o servidor de desenvolvimento. A suíte completa reutilizou o servidor de teste e encerrou normalmente. Foram usadas as listas pelo nome acessível e o botão real de curso, sem enfraquecer verificações funcionais. A primeira tentativa tinha erro de sintaxe no arquivo de teste e não executou casos. Os logs intermediários foram preservados.
- Suíte final completa: **203 testes aprovados**, zero falhas, em 7,4 minutos, com encerramento normal (ops/employability-ui-e2e.log). Inclui os 185 cenários anteriores e 18 cenários novos do plano. Não se somam as execuções focadas como casos distintos. Os quatro cenários adicionais foram: falha de leitura, preservação de edição após erro, negação pelo backend e carregamento sob demanda da página.
- Responsividade: 320, 375, 768, 1024 e 1440 px; teclado, ausência de overflow, análise axe do main com tags WCAG A/AA até 2.2 e capturas de tela nos testes novos. Foram inspecionadas visualmente as capturas de 320 e 1440 px; não foram observados cortes laterais. Cópias duráveis: ops/employability-ui-2026-11-07/plan-320.png e plan-1440.png. Capturas de página inteira incluem a topbar sticky na posição de rolagem do teste. Não foi realizado ensaio humano com leitor de tela.
- git diff --check passou nos arquivos acompanhados desta entrega; somente avisos de normalização CRLF. Os arquivos novos não contêm linhas com espaços em branco no final.
- Testes frontend interceptam contratos HTTP com fixtures sintéticas isoladas. Não constituem homologação de produção nem substituem os 29 testes backend do plano/Radar/migrações executados na etapa anterior.

## Configuração e limites
Requer o backend atualizado com os endpoints do plano e a V20 de Oportunidades aplicada pelo processo de implantação existente. VITE_API_URL, configuração segura do Radar e integrações educacionais permanecem as já documentadas. Não foi iniciada implantação nem aplicada migração ao banco operacional.

A busca automática do motor continua sendo uma amostra limitada por ocupação/área; município e modalidade são objetivos revisáveis e as preferências do Radar seguem independentes. Não foi adicionada seleção de novas referências de vaga por ID no formulário: referências pré-existentes são mantidas/retiradas e o motor continua sua busca quando não há referências explícitas.

A disponibilidade de vagas/cursos pode mudar após gerar o snapshot. Acesse os módulos existentes para confirmar. Fontes indisponíveis são sinalizadas. Não há atualização periódica automática de recomendações, emissão de certificado pelo frontend ou compartilhamento automático de contas. A consulta do histórico apresenta resumo das gerações, sem restaurar versões antigas.

Acessibilidade automatizada e teclado não equivalem a validação integral com leitores de tela/pessoas com deficiência. Reprodução real em aparelhos físicos, avaliação com tecnologias assistivas e medições de campo continuam recomendadas. Esta entrega não inclui novos resultados de Web Vitals.

## Executar as verificações
Na pasta frontend: npm run lint; npm test; npm run build. Para testes específicos: npm run test:e2e -- e2e/employability-plan.spec.ts --workers=2. Para a suíte completa: npm run test:e2e -- --workers=2. As fixtures são locais aos testes e não cadastram dados no banco operacional.
