# Busca universal por trilha — 12/11/2026

## Implementação

- Botão acessível na Topbar e atalho `Ctrl+K`/`Cmd+K` abrem uma paleta de busca contextual.
- Resultados de páginas e ações vêm dos grupos já registrados na Sidebar. A busca filtra os destinos apresentados ao perfil atual; não cria permissões nem contorna as validações das páginas e APIs.
- Resultados agrupados por área, seleção destacada, navegação por setas/Home/End, Enter para abrir e Escape para fechar. O foco retorna ao botão de pesquisa.
- Painel modal responsivo, com botão compacto em celular, estado vazio, erro, carregamento e instruções por teclado.
- A busca de conteúdo espera 300 ms após a digitação, só consulta a partir de dois caracteres, cancela requisições obsoletas e limita a lista exibida.
- Ao selecionar uma vaga, curso ou material, abre-se a tela de busca já existente com o termo selecionado preenchido.

## Fontes por ambiente e proteção

- Oportunidades: `/public/companies/regional-opportunities?q=…&limit=8` e `/opportunities/course/regional?q=…&limit=8`. Usa a preferência geográfica atual e resultados ativos/públicos do catálogo. A seleção abre o catálogo de vagas/cursos existente.
- Empresas: `/company/listings`, protegido pela sessão empresarial e autorização existente. A pesquisa reduz o retorno no cliente a título, área/localidade e tipo/situação; contatos, detalhes de candidaturas e informações internas não são exibidos. O provedor só fica ativo no painel da empresa, não no painel da associação.
- Capacita: `/capacita/contents` para materiais públicos da etapa selecionada e `/capacita/learning/courses` com a sessão Capacita e `manage` restrito ao papel docente conhecido. O servidor continua responsável pela autorização; a busca não inclui alunos, progresso individual ou dúvidas privadas.

Não houve endpoint novo, mudança de contrato, autenticação ou banco de dados.

## Arquivos

- `frontend/src/components/workspace/UniversalSearch.tsx`
- `frontend/src/components/workspace/universalSearchModel.ts`
- `frontend/src/components/workspace/AppLayout.tsx`
- `frontend/src/components/workspace/workspace.css`
- `frontend/src/App.tsx`
- `frontend/src/features/account/CatalogPage.tsx`
- `frontend/src/features/account/CompanyPortal.tsx`
- `frontend/src/features/account/CompanyDashboard.tsx`
- `frontend/src/features/learning/LearningPortal.tsx`
- `frontend/src/pages/CapacitaPage.tsx`
- `frontend/tests/universal-search.test.mjs`
- `frontend/e2e/universal-search.spec.ts`

## Validação

- `npm run lint`: aprovado.
- `npm test`: 27/27 testes passaram, incluindo busca sem distinção de acentos/maiúsculas.
- `npx tsc --noEmit --incremental false -p tsconfig.app.json`: aprovado.
- `git diff --check`: aprovado; Git mostrou apenas avisos de conversão LF/CRLF em arquivos modificados existentes.
- Playwright e build completo não foram concluídos: o ambiente rejeita escrita em `frontend/node_modules/.vite-temp`, `node_modules/.tmp` e pasta de resultados (`EPERM`). O spec E2E foi criado, mas não se declara validação visual automatizada como aprovada.
