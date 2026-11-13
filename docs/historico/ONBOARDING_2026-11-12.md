# Onboarding por perfil — 12/11/2026

## O que foi implementado

- Tutorial contextual de três passos para Candidato, Administrador de Oportunidades, Proprietário/Administrador de empresa, Recrutador/Analista, Aluno, Professor e Administrador do Capacita.
- O tutorial de primeiro acesso é preparado somente após cadastro concluído; login não cria onboarding pendente.
- Cada andamento é guardado localmente por trilha e e-mail normalizado, sem guardar credenciais nem compartilhar estado entre ambientes.
- Ações do tutorial abrem destinos já existentes: perfil, diagnóstico, vagas, cursos, candidaturas, publicações, painel da empresa, dúvidas recebidas, progresso e administração Capacita quando autorizada.
- “Agora não” pausa e guarda o passo atual. “Pular tutorial” impede abertura automática repetida. “Ver tutorial inicial” no menu do perfil reinicia o guia, inclusive depois da conclusão.
- O guia é um painel não modal: permanece disponível sem bloquear a navegação ou os controles da página. Tem progresso, botões com rótulos objetivos, foco por teclado, sem exigir hover e layout móvel.

## Arquivos

- `frontend/src/components/workspace/OnboardingGuide.tsx`
- `frontend/src/components/workspace/onboardingModel.ts`
- `frontend/src/components/workspace/AppLayout.tsx`
- `frontend/src/App.tsx`
- `frontend/src/features/account/CompanyPortal.tsx`
- `frontend/src/features/learning/LearningPortal.tsx`
- `frontend/src/pages/CapacitaPage.tsx`
- `frontend/tests/onboarding-model.test.mjs`
- `frontend/e2e/onboarding.spec.ts`
- `docs/historico/ONBOARDING_2026-11-12.md`

Não houve alteração de autenticação, APIs ou banco de dados.

## Validação

- `npm run lint`: aprovado.
- `npm test`: 26 testes aprovados, incluindo três testes novos para isolamento por trilha/identidade, persistência dos estados e tolerância a dados locais inválidos.
- `npx tsc --noEmit --incremental false -p tsconfig.app.json`: aprovado.
- `npm run build`: não concluiu; o TypeScript em modo build não conseguiu gravar `node_modules/.tmp/*.tsbuildinfo` (EPERM).
- Playwright: não iniciou. Vite não conseguiu criar seu arquivo temporário em `node_modules/.vite-temp` e o executor não conseguiu criar a pasta de resultados (EPERM). Não há resultado de teste visual automatizado nesta validação.
- Revisão manual com leitor de tela não executada.

## Limites

O estado de onboarding fica no armazenamento local do navegador e não sincroniza entre dispositivos. Se o navegador limpar os dados locais, o guia pode voltar a ser oferecido para um estado marcado como pendente, mas o acesso aos recursos não depende dele. A identidade dos papéis vem dos dados já retornados pelos respectivos ambientes; permissões e APIs existentes continuam sendo a autoridade.
