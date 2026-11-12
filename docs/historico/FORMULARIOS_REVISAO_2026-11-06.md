# Revisão de formulários — 06/11/2026

## Diagnóstico e decisões
O cadastro usa autenticações independentes por trilha; empresas exigem dados organizacionais além das credenciais. O perfil profissional já permite completar informações por seções, sem tornar currículo, foto, experiência anterior ou formação condições de acesso. Professores são contas do Capacita autorizadas pela administração: não foi criado cadastro paralelo nem concessão automática de privilégios.

Os formulários educativos já permitem rascunhos no servidor. Videoaulas e materiais salvam metadados como rascunho antes do processamento de arquivo, com publicação validada separadamente. Avaliações dos alunos possuem ação existente de salvar respostas no servidor. Não foi criado salvamento automático local de informações privadas.

Não foi encontrada justificativa para remover campos exigidos pela aplicação. A simplificação prioriza orientação comum, identificação dos campos necessários, preenchimento progressivo das seções existentes e mensagens acionáveis; não substitui todos os formulários por assistentes em etapas.

## Alterações
- FormGuide reutilizável: contagem de campos necessários preenchidos, indicador de progresso, lista de obrigatoriedades e resumo de validação em português com links que levam ao campo.
- Ao validar, abre a seção recolhida que contém o primeiro erro e move o foco até ele. Erros nativos recebem aria-invalid e são anunciados no resumo.
- Integração em cadastro/login, perfil/experiências/formações, gerenciamento empresarial, cursos/disciplinas/módulos, permissões docentes, videoaulas, avaliações e materiais PDF.
- Cadastro mantém informações durante correções na mesma tela e informa os limites dessa preservação. Não promete recuperação após fechar ou atualizar a página.
- Controle de mostrar/ocultar caracteres com estado acessível, suporte a colar e gerenciadores de senha; política de senha existente preservada.
- Máscaras de CNPJ e telefone sem conferir legitimidade do cadastro ou descartar telefones internacionais. A validação definitiva continua no servidor.
- Upload de currículo: seletor agora acessível com Tab e foco visível, limite de 10 MB informado antes do envio, bloqueio durante operação e limpeza da seleção também após rejeição para permitir tentar o mesmo arquivo novamente.
- Cadastro mais compacto em celulares, com textos de apoio reduzidos sem remover campos.
- Seleção inicial de curso no material PDF utiliza valor vazio, permitindo validação nativa de obrigatoriedade.
- Arquivo de vídeo vazio, formato inesperado ou maior que o limite é rejeitado no cliente, limpando também a seleção anterior. Duração real continua validada pelo servidor.

## Arquivos desta etapa
- frontend/src/components/forms/FormGuide.tsx (novo)
- frontend/src/components/forms/PasswordField.tsx (novo)
- frontend/src/components/forms/formRules.ts (novo)
- frontend/src/features/account/AuthScreen.tsx
- frontend/src/features/account/ProfilePage.tsx
- frontend/src/features/account/CompanyDashboard.tsx
- frontend/src/features/learning/LearningPortal.tsx
- frontend/src/features/learning/LessonEditor.tsx
- frontend/src/features/learning/LibraryPanel.tsx
- frontend/src/features/learning/AssessmentEditor.tsx
- frontend/tests/form-rules.test.mjs (novo)
- frontend/e2e/form-guidance.spec.ts (novo)
- frontend/e2e/account-journey.spec.ts (teste de teclado no seletor de currículo)
- frontend/e2e/ecosystem-journey.spec.ts (mensagem de faixa etária alinhada ao novo resumo de validação)

Alterações anteriores já presentes nesses arquivos foram preservadas. Nenhuma alteração de banco, contrato de API, dependência ou autenticação foi necessária.

## Validação
- Lint: aprovado (ops/forms-lint.log).
- TypeScript e build de produção: aprovados (ops/forms-build.log).
- Unitários: 19 aprovados, zero falhas (ops/forms-unit.log).
- Suíte completa de navegador: 178 cenários executados; 172 aprovados, 6 falhas iniciais (ops/forms-e2e.log). Cinco eram seletores ambíguos no teste novo de exibir caracteres; um esperava o texto antigo da validação de faixa etária. Os testes foram corrigidos sem remover suas verificações.
- Reteste final das três suítes afetadas: 23 aprovados, zero falhas (ops/forms-e2e-focused.log), incluindo os seis casos anteriores, os cinco tamanhos e o seletor de currículo por teclado. Os 178 cenários distintos têm aprovação em execuções complementares; a suíte completa não foi repetida após os últimos ajustes locais de texto e upload de currículo.
- Verificação automática de acessibilidade nos cinco novos cenários: nenhuma ocorrência grave/crítica nos critérios avaliados. A suíte anterior também avaliou formulários docentes e navegação; não se trata de certificação integral.
- Revisão visual da captura de 320 px motivou redução dos textos de apoio e espaço do cabeçalho no celular. Capturas finais: ops/forms-cadastro-{320,375,768,1024,1440}.png.
- git diff --check: sem erros de whitespace nos arquivos verificados.
- O build mantém aviso anterior de chunks grandes de PDF/mapa, carregados separadamente; esta etapa não introduziu bibliotecas.

Os últimos ajustes afetaram somente cadastro, texto auxiliar e upload do perfil; seus fluxos foram repetidos no reteste final. Testes de backend não foram repetidos porque nenhum código de servidor ou banco foi alterado.

Novos cenários cobrem máscaras, número internacional, cadastro em 320/375/768/1024/1440 px, preservação de preenchimento após erro, foco no campo inválido, mostrar/ocultar caracteres, ausência de senha em armazenamento web e análise automática de acessibilidade. A suíte existente cobre fluxos empresariais e educacionais com APIs simuladas e dados identificados de teste.

## Limites e produção
- Rascunhos existentes dependem de autenticação e sucesso da gravação no servidor; não há recuperação universal de formulários após atualização/fechamento.
- A contagem indica preenchimento, não validade jurídica ou aprovação dos dados.
- As máscaras não validam situação cadastral de empresas.
- Extensão, tamanho e MIME no navegador são conveniências; validação de conteúdo e autorização continuam obrigatórias no backend existente.
- Não foram utilizados dados pessoais reais, implantadas mudanças ou executadas migrações nesta etapa.
- Testes automáticos de acessibilidade não comprovam conformidade integral WCAG ou substituem avaliação com usuários e leitores de tela reais.
