# Dashboards regionais — Trampolim

Entrega local em 4 de novembro de 2026. Aprimoramento dos painéis existentes, sem reconstrução, novos bancos ou novas regras de autenticação.

## Oportunidades

- Nova seção “Seu próximo passo”, com ações baseadas em dados disponíveis: contato, município, resumo profissional e existência do diagnóstico.
- Completar ou revisar o perfil não exige experiência anterior.
- Acesso ao diagnóstico existente; quando já disponível, o atalho consulta o diagnóstico, sem iniciar automaticamente outro teste.
- Busca no município informado: consulta a referência oficial existente, valida correspondência única e aplica o código IBGE. Localidade ambígua ou indisponibilidade produz orientação explícita, sem presumir município.
- Atalho regional usa a região de referência configurada e informa seu nome. Quando não corresponde ao município de referência, a ação é apresentada como exploração de uma região, sem afirmar proximidade não comprovada.
- Busca remota limpa a restrição territorial e aplica a modalidade Remoto. Estágios aplicam o tipo de contrato na área de busca selecionada. O usuário pode alterar todos os filtros no catálogo existente.
- Atalhos para cursos e Radar de Competências, usando a própria sessão de Oportunidades.
- Histórico com os cinco acompanhamentos mais recentes registrados na conta; a etapa “salva” é distinguida de candidatura enviada. A edição completa continua no perfil existente.
- Resumo trata falhas parciais: perfil e histórico disponíveis não são descartados se somente uma consulta falhar. Dados desconhecidos aparecem como indisponíveis, não como números inventados.
- Referências de plataformas não são apresentadas como novas parcerias confirmadas.

## Empresas

- Ações rápidas: publicar vaga, abrir rascunho de estágio por R$ 50,00, analisar candidaturas, gerenciar anúncios e consultar indicadores.
- Nenhuma publicação ou pagamento é confirmado pelo novo atalho. Revisão, aprovação, cobrança e confirmação confiável permanecem no fluxo existente.
- Local real da vaga continua sendo informado pelo empregador: não é preenchido automaticamente com a sede da empresa.
- Indicadores derivados dos registros da própria empresa: rascunhos, anúncios aguardando pagamento e candidaturas recebidas ainda sem análise. Falha ou ausência de acesso apresenta “—”, sem presumir zero.
- Anúncios filtrados por município/local informado, cargo/área/requisito e situação. Filtros combináveis e limpeza.
- Candidaturas filtradas pelo local da vaga e por termos mencionados na mensagem enviada pelo candidato, além dos filtros anteriores de etapa e oportunidade.
- A interface esclarece que local da vaga não significa residência do candidato e que uma menção textual não comprova competência. Evidências autorizadas continuam nos Passaportes recebidos.
- Exportação existente acompanha a seleção filtrada. Não foram criados novos campos pessoais ou acesso a currículos privados.
- Papel de analista mantém sua navegação limitada; não recebe novos atalhos de publicação ou candidaturas identificáveis.
- Corrigido o contraste do seletor de etapa da candidatura, identificado na verificação de acessibilidade.

## Capacita

- Painel “Organize seu aprendizado” com acesso ao catálogo, retomada de aula em andamento, progresso e competências, avaliações, materiais, trilhas, mural e orientações de estágio/primeiro emprego.
- Retomada utiliza o histórico real e mantém a autorização de curso/aula. A abertura malsucedida não força a exibição da aula pelo novo atalho.
- No modo de professor: “Ensinar e acompanhar”, com criação de curso, dúvidas recebidas, avaliações, biblioteca e escolha de curso para acompanhar alunos.
- Acesso de professor continua condicionado à identidade retornada pelo backend; não se criam novos papéis nem outro login.
- Categorias são extraídas dos cursos da página atual e usam a pesquisa já existente. Não representam ranking de demanda regional nem parcerias educacionais.
- O histórico passou a carregar separadamente da pesquisa/paginação de cursos. Digitar uma busca não repete a consulta de progresso; falha do catálogo não elimina histórico disponível e vice-versa.
- Avaliações, biblioteca, desempenho e mural continuam utilizando os componentes e carregamento sob demanda existentes.
- Atende estudantes e pessoas em qualificação profissional sem criar classificações pessoais adicionais ou exigir novos dados sensíveis.

## Reutilização e arquivos

Novos:

- frontend/src/features/dashboard/DashboardActions.tsx — apresentação compartilhada de ações, com títulos, descrições e controles de teclado/foco.
- frontend/src/features/dashboard/CandidateNextStep.tsx — atalhos profissionais e resolução do município pelo serviço público existente.
- frontend/e2e/dashboard-journey.spec.ts — oito cenários novos de interface.
- DASHBOARDS_REGIONAIS.md — este relatório.

Modificados:

- frontend/src/features/account/OpportunitiesPage.tsx
- frontend/src/features/account/CompanyDashboard.tsx
- frontend/src/features/learning/LearningPortal.tsx
- README.md — referência a este relatório.

Reutilizados: api autenticada, AccountNavigation, AccountHeaderActions, TerritoryProvider, catálogo regional, LearningPortal, CapacitaModuleNav, LessonPlayer, PerformancePanel, LibraryPanel, AssessmentPanel, QuestionBoard, QuestionNotifications e fluxos empresariais existentes.

## Dados, segurança e limites

- Nenhuma alteração no backend, entidades, migrações, credenciais ou separação de bancos nesta entrega.
- Cada painel usa os endpoints autenticados de sua própria trilha. A consulta pública de municípios não recebe token de candidato.
- A preferência territorial e os filtros de pesquisa não transferem identidade entre contas. Os testes verificam a preservação das sessões das demais trilhas.
- Permissões reais continuam sendo impostas pelo backend; verificações de interface não substituem autorização por recurso.
- Os filtros empresariais atuam sobre os registros já disponibilizados pelo endpoint da empresa. Não foram implementados novo índice global de pessoas, descoberta de candidatos sem candidatura/consentimento ou comprovação de habilidades a partir de texto.
- Indicadores não representam contratações confirmadas, pessoas únicas ou estatísticas regionais agregadas. Nenhuma vaga, empresa, aluno, parceiro ou contratação foi inventado para preencher os painéis.
- Não foi realizada certificação de carga, validação em aparelhos físicos ou certificação integral WCAG. A verificação de responsividade e acessibilidade desta entrega utiliza navegador automatizado, teclado, Axe e inspeção visual.
- Dados sintéticos nos testes são fixtures locais de automação, não registros cadastrados na aplicação para apresentação comercial.

## Execução

Atualize o frontend em execução ou recompile-o. O backend e as configurações existentes das três trilhas continuam sendo necessários. Não há novas variáveis de ambiente, serviços externos ou migrações.

Não houve publicação no GitHub ou implantação em produção. As alterações locais das etapas anteriores foram preservadas.

## Testes executados e resultados

- Frontend completo: 93 cenários Playwright aprovados na execução final, incluindo os oito novos cenários de dashboards e as regressões de autenticação, perfil, busca, Empresas, videoaulas, biblioteca, avaliações, mural e notificações.
- Oito testes unitários existentes do frontend aprovados.
- Compilação TypeScript/Vite aprovada; lint aprovado sem advertências; git diff --check sem problemas.
- Novos cenários em 390 e 1440 pixels: próximo passo com dados reais do perfil, histórico, município IBGE, busca remota sem restrição local, filtros combinados de anúncios/candidaturas, abertura de rascunho de estágio, progressão do aluno, ferramentas do professor, papel de analista e falhas de carregamento sem métricas fictícias.
- Testes verificam os tokens enviados aos endpoints de cada ambiente e preservação da sessão de outra trilha. Não substituem testes de autorização reais do backend, que não foi modificado nem reexecutado nesta etapa exclusivamente frontend.
- Axe sem violações nas regras WCAG A/AA verificadas nos novos cenários, inclusive WCAG 2.1 A para nomes acessíveis. Corrigidos contraste do seletor de etapa e associação entre rótulo visível, nome acessível e descrição dos atalhos. Os botões também foram exercitados por teclado.
- Capturas de tela mobile de Oportunidades e seleção empresarial inspecionadas visualmente; testes verificam ausência de rolagem horizontal da página.

Comandos na pasta frontend: npm run build; npm run lint; npm test; npm run test:e2e -- --workers=2.
