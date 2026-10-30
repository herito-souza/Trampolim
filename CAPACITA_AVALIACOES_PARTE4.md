# Capacita — avaliações, desempenho e competências (Parte 4)

## Entrega e arquitetura preservada

Implementadas mini provas objetivas, correção no servidor, histórico de tentativas, painel de desempenho e competências verificadas. A versão local na branch `main` foi a fonte de verdade. As alterações não commitadas das Partes 1, 2 e 3 foram preservadas. A Parte 5 não foi iniciada.

As funcionalidades usam o login e os papéis educacionais existentes, exclusivamente `capacitaJdbcTemplate` e transações `capacitaTransactionManager`. Não há banco novo, compartilhamento de senha/token nem leitura direta dos bancos de Empresas ou Oportunidades.

## Mini provas e publicação

Professor responsável pelo curso ou administrador educacional pode criar e editar avaliações vinculadas ao curso, disciplina/assunto ou módulo. A prova contém título, assunto, descrição, instruções, questões, alternativas, alternativa correta, peso por questão, explicações, nota mínima, máximo de tentativas, prazo opcional, exigência de estudo e obrigatoriedade para conclusão.

Limites validados no backend: 1–30 questões, 2–6 alternativas por questão, 1–100 pontos por questão, nota mínima de 1–100%, 1–10 tentativas e até 20 competências por avaliação. Somente `MCQ` é aceito nesta etapa. A identificação do tipo de questão e a estratégia de correção permitem evolução posterior; questões discursivas não foram implementadas.

Rascunho, publicação e retirada de publicação são efetivos no backend. Cada publicação cria uma versão com cópia integral das questões e critérios. Versões anteriores não são editadas pela API. Uma tentativa já iniciada continua referenciando a versão original, mesmo se o professor publicar outra. O vínculo de curso/disciplina/módulo é imutável depois da criação; para outro contexto, criar outra avaliação. Revisões desatualizadas são rejeitadas para evitar sobrescrita de outra edição.

Retirar a avaliação ou o curso de publicação impede iniciar e enviar novas respostas enquanto estiver indisponível. O aluno mantém acesso ao próprio histórico e resultados já registrados. Não existe exclusão de tentativas ou reatribuição manual de notas pela interface desta etapa.

## Fluxo do aluno e correção

O aluno consulta instruções e critérios antes de iniciar. Iniciar consome uma tentativa; chamadas repetidas ou concorrentes retomam a mesma tentativa em andamento. É possível salvar respostas parcialmente e retornar pelo histórico. Há revisão das respostas e confirmação explícita antes do envio definitivo. Falha de envio permite tentar novamente com a mesma identificação; não há envio automático.

O backend valida a propriedade da tentativa, o prazo original, a publicação, a conclusão exigida das aulas e cada identificador de questão/alternativa. Não aceita nota, aprovação, peso ou resposta correta vindos do aluno. As respostas corretas e explicações não aparecem na consulta pública ao aluno antes da correção. Após o envio, explicações e acertos/erros aparecem apenas quando o professor habilitou feedback; o gabarito estruturado continua reservado à gestão autorizada.

Correção: somar os pesos das questões respondidas corretamente; nota percentual = pontos obtidos / pontos possíveis × 100. A aprovação compara os valores sem arredondamento intermediário. A nota exibida é arredondada em duas casas. O critério registrado identifica a estratégia `EXACT_SINGLE_CHOICE_WEIGHTED_V1`, mínimo, exigência de estudo, estudo comprovado no envio, máximo de tentativas e prazo.

Tentativas armazenam aluno, versão, número, respostas, pontuação, aprovação, critérios e horários. Depois de corrigida, a tentativa não pode ser alterada: o mesmo envio retorna o resultado existente; outra chave ou respostas diferentes são rejeitadas. Transações e restrições únicas protegem o limite de tentativas e a emissão única de cada competência por aluno/avaliação/identificador de competência, inclusive sob concorrência.

O professor responsável não pode realizar sua própria avaliação como aluno. Professores de outros cursos não podem consultar gabaritos privados ou resultados individuais. O relatório autorizado mostra nome do aluno, tentativa, pontuação e situação, sem contatos ou currículo. A API permite consultar a tentativa detalhada em modo de gestão mediante a mesma autorização.

## Estudo e conclusão

A opção de exigir estudo verifica se existe ao menos uma videoaula publicada e validada no contexto e se todas estão concluídas no registro confiável da Parte 1. Cada vídeo continua exigindo 90% de progresso validado pelo backend. Abrir uma página ou marcar uma opção no formulário de avaliação não comprova esse estudo.

O dashboard apresenta cursos iniciados/publicados, cursos concluídos, aulas e módulos concluídos, tentativas corrigidas/aprovadas, notas no histórico, progresso por curso e competências conquistadas. Os indicadores usam dados reais da própria conta, sem métricas demonstrativas.

- Progresso percentual: proporção de videoaulas publicadas/validadas concluídas.
- Módulo concluído: pelo menos uma videoaula válida, todas concluídas e aprovação nas avaliações publicadas obrigatórias daquele módulo.
- Curso concluído: pelo menos uma videoaula válida, todas concluídas e aprovação em todas as avaliações publicadas obrigatórias do curso, incluindo as de disciplina/módulo.
- Uma aprovação anterior na mesma avaliação satisfaz sua obrigatoriedade; publicar outra versão não reavalia nem invalida tentativas anteriores.

Os indicadores de curso/módulo refletem o catálogo atualmente publicado, com até 200 cursos iniciados listados. O histórico de tentativas, inclusive de avaliações retiradas de publicação, é mantido separadamente. Adicionar novas aulas ou obrigações pode mudar o indicador atual de conclusão; isso não reescreve notas e conquistas já registradas. Listas gerais e relatórios exibem até 200 entradas recentes; competências próprias, até 500. Não foi implementada uma certificação de curso nesta etapa.

Prazos são absolutos, armazenados como instante e exibidos no horário do navegador. A tentativa mantém seu prazo original; editar a avaliação não muda os critérios de uma tentativa já aberta. Não há temporizador individual adicional ou nova tentativa após o prazo original de uma tentativa em andamento. Para aplicar critérios diferentes sem esse vínculo, o professor pode criar outra avaliação.

## Minhas Competências

As competências são definidas na avaliação vinculada a um módulo e copiadas para sua versão. Somente uma tentativa aprovada gera conquistas; o aluno não possui operação de criação/atribuição de competência.

Cada conquista contém UUID verificável, nome, descrição, curso/módulo de origem, professor responsável, data, nota, critério e vínculo interno à tentativa que a comprovou. A primeira conquista da competência naquela avaliação é preservada; aprovações repetidas não duplicam nem sobrescrevem o registro.

Em “Meu desempenho e competências” → “Minhas Competências”, o aluno pode consultar detalhes, destacar conquistas e definir sua ordem de apresentação. Alterar apresentação não altera a comprovação. Curso concluído, avaliação aprovada, competência conquistada e certificado formal são diferenciados na interface; não são emitidos certificados formais.

## Compartilhamento e preparação para Oportunidades

Há dois mecanismos opcionais, com consentimento explícito e auditoria:

1. Exportar em JSON somente as competências destacadas da própria conta. A API exige sessão Capacita. A exportação não envia dados a outra plataforma.
2. Gerar link de verificação de uma conquista por 30 dias. O token possui 256 bits de aleatoriedade; somente seu hash é armazenado. Gerar outro link invalida o anterior, e o aluno pode revogar o compartilhamento. Expiração, revogação, exclusão ou bloqueio de conta impedem novas consultas.

O registro compartilhado contém somente os dados daquela conquista, sem e-mail, nome pessoal do aluno, credenciais, outras competências, respostas ou histórico privado. A identidade pessoal não é comprovada pelo link, limitação explicitada na página. A rota do frontend é `?competencia=<token>`; a verificação usa `/api/capacita/learning/competencies/shared/{token}`, sem cabeçalho de autenticação, com resposta sem cache e política de referrer restritiva.

Essa API e o JSON versionado preparam uma integração futura consentida com Oportunidades. Não foi implementada importação automática, vinculação de contas ou mudança no perfil privado do Oportunidades. Um consumidor futuro deve verificar a origem/autorização da conquista; um arquivo JSON, isoladamente, não é prova criptográfica de autenticidade. Não registrar tokens completos nos logs de proxy/acesso e manter HTTPS em produção.

## Banco, autorização e privacidade

Nova migração exclusiva do Capacita: `backend/src/main/resources/db/migration-capacita/V13__assessments_and_verified_competencies.sql`.

| Tabela | Finalidade |
| --- | --- |
| learning_assessments | Contexto, rascunho, publicação e revisão |
| learning_assessment_versions | Definições publicadas preservadas |
| learning_assessment_attempts | Respostas, tentativas, correção e critérios |
| learning_competency_awards | Conquistas comprovadas, apresentação e compartilhamento |
| learning_assessment_audit | Eventos sem gabaritos ou respostas nos logs |

Restrições garantem a versão pertencente à avaliação, uma tentativa aberta por aluno/avaliação, numeração única e vínculo da conquista a uma tentativa do mesmo aluno. As rotas verificam papel, professor responsável e propriedade dos registros no backend. Nenhum parâmetro `manage` concede permissão por si só.

A exportação pessoal existente do Capacita foi ampliada para incluir tentativas e competências da própria conta, sem gabaritos, hashes de compartilhamento ou credenciais. As novas relações respeitam a exclusão de conta existente, com cascata dos dados do aluno e anonimização do ator nos eventos de auditoria. Contas responsáveis por cursos continuam sujeitas à proteção de exclusão da Parte 1.

Nenhuma migração antiga foi editada. A V13 foi testada em schema vazio e confirmada como aplicada no banco Capacita local. Nenhuma tabela ou migração foi acrescentada a Empresas/Oportunidades.

## Arquivos desta etapa

Novos:

- `backend/src/main/java/com/trampolim/api/education/AssessmentService.java`
- `backend/src/main/java/com/trampolim/api/education/AssessmentController.java`
- `backend/src/main/java/com/trampolim/api/education/LearningPerformanceService.java`
- `backend/src/main/resources/db/migration-capacita/V13__assessments_and_verified_competencies.sql`
- `backend/src/test/java/com/trampolim/api/education/AssessmentPostgresTest.java`
- `backend/src/test/java/com/trampolim/api/education/AssessmentHttpPostgresTest.java`
- `frontend/src/features/learning/assessmentTypes.ts`
- `frontend/src/features/learning/AssessmentEditor.tsx`
- `frontend/src/features/learning/AssessmentRunner.tsx`
- `frontend/src/features/learning/AssessmentPanel.tsx`
- `frontend/src/features/learning/PerformancePanel.tsx`
- `frontend/src/features/learning/SharedCompetency.tsx`
- `frontend/e2e/assessment-journey.spec.ts`
- Este relatório.

Modificados:

- `backend/src/main/java/com/trampolim/api/config/SecurityConfig.java`: libera somente a consulta do link consentido; demais operações continuam autenticadas.
- `backend/src/main/java/com/trampolim/api/education/CapacitaController.java`: exportação pessoal.
- `frontend/src/features/learning/LearningPortal.tsx`: avaliações e desempenho integrados ao estudo.
- `frontend/src/App.tsx`: página de verificação compartilhada.
- `frontend/e2e/library-journey.spec.ts`: aguarda explicitamente o término do carregamento do PDF por até 15 segundos sob carga, preservando as verificações de páginas/texto/zoom.
- `README.md`: guia da etapa.

## Configuração e uso

Não há novas dependências ou credenciais externas. Reutiliza bancos, autenticação, permissões, Java e configuração existentes. Reiniciar backend e frontend; o Flyway aplica a V13 ao Capacita. Manter backup do banco e as configurações de produção das etapas anteriores.

Professor: Capacita → Mini provas → Gerenciar avaliações → Criar mini prova. Selecionar contexto, escrever questões/alternativas/gabarito, configurar critérios e competências do módulo, publicar. “Resultados dos alunos” consulta os registros autorizados. Para retirar, editar Situação da avaliação e salvar.

Aluno: Mini provas → Ver avaliação → Iniciar ou retomar tentativa → responder → Revisar respostas → confirmar → Enviar avaliação. Para sair antes, salvar respostas para depois. Histórico e conquistas ficam em “Meu desempenho e competências”.

O administrador e os professores continuam usando contas Capacita existentes/confirmadas e permissões da Parte 1. Não foram criadas contas ou avaliações reais automaticamente, nem inseridas competências demonstrativas em perfis reais.

## Validação e limites

Os novos testes cobrem correção ponderada e nota mínima exata; gabarito ausente no payload do aluno; propriedades falsificadas de nota/aprovação ignoradas; limite de tentativas; prazo vencido; respostas incompletas/inválidas; histórico preservado; versão nova sem recorreção; estudo obrigatório; conclusão de curso/módulo; concessão somente após aprovação; tentativa concorrente e envio duplicado sem duplicação de conquista; propriedade de registros; professor de outro curso; token de outra plataforma; exportação, consentimento e revogação.

PostgreSQL real é usado em schemas temporários removidos ao final. Playwright verifica publicação no computador/celular, teclado, revisão, confirmação, falha e repetição do envio, painel, destaque, ordem, download JSON e link consultado sem credenciais. Dados sintéticos existem somente nos testes. A conferência visual usa viewport móvel de 390 px; aparelhos físicos e carga de produção não foram homologados.

Resultados finais em 29/10/2026:

| Verificação | Resultado |
| --- | --- |
| Backend completo | 91 testes, zero falhas, zero erros, zero ignorados |
| Navegação Playwright completa | 35 testes aprovados |
| Testes unitários do frontend | 7 aprovados |
| Lint | Aprovado |
| TypeScript e build Vite | Aprovados |
| Empacotamento Maven | Aprovado |
| Migração V13 no Capacita local | Aplicada com sucesso |
| git diff --check | Sem erros de whitespace |

Os testes Playwright usam respostas de API controladas para validar as telas e os fluxos; os testes de integração HTTP executam o backend e o PostgreSQL reais. Na primeira execução completa do navegador, um teste da biblioteca atingiu o limite de espera enquanto o worker PDF ainda carregava. A espera passou a acompanhar explicitamente esse estado, mantendo as verificações; a reexecução completa passou nos 35 testes. O bundle inicial continua acima de 500 kB, aviso já existente nas etapas anteriores; isso não impede o build.

A infraestrutura de pagamentos/SMTP das etapas anteriores não é necessária para corrigir avaliações ou gerar conquistas, mas o fluxo de confirmação de novas contas continua dependendo de sua configuração operacional. Permanecem fora desta etapa: discursivas, certificados formais, importação no Oportunidades, proctoring e validação de identidade acadêmica. As alterações ficam locais, sem commit/push automático.
