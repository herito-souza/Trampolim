# Homepage regional — Trampolim

Implementação local em 4 de novembro de 2026.

## Alterações

- Hero com título e descrição solicitados, duas ações e comunicação da atuação inicial em Irecê e região.
- Ação Encontrar oportunidades leva à busca pública na própria homepage; a ação de diagnóstico abre o diagnóstico existente em Oportunidades, inclusive após carregamento da página sob demanda.
- Busca por cargo/área, município oficial e modalidade. A referência inicial vem da configuração territorial (atualmente Irecê); outros municípios e todas as localidades estão disponíveis. Nenhuma permissão de geolocalização é solicitada.
- Resultados reais da API pública de empresas, incluindo empregos e estágios aptos para divulgação, com paginação e link para a empresa. Não há cadastro de dados demonstrativos.
- A busca só carrega os resultados após envio; componente de resultados carregado sob demanda. Consulta de municípios reaproveita busca limitada e com atraso entre digitações.
- Preservados os três cards, interações de toque/teclado/hover, menus, rodapé e identidade visual. Menu se adapta à largura disponível.
- Estados de carregamento, erro e ausência de anúncios. Se a configuração territorial falhar, mantém a busca textual tradicional com aviso explícito.
- Autenticação, sessões, permissões e bancos das três trilhas preservados. Nenhuma migração nova nesta entrega.

## Arquivos desta entrega

- frontend/src/pages/EcosystemHome.tsx
- frontend/src/features/geography/HomepageSearch.tsx (novo)
- frontend/src/features/account/CompanyOpportunities.tsx (texto contextual opcional; comportamento do catálogo mantido)
- frontend/src/pages/Home.tsx (posicionamento do diagnóstico ao entrar por âncora)
- frontend/src/App.tsx (ação para o diagnóstico existente)
- frontend/e2e/homepage-regional.spec.ts (novo)
- HOMEPAGE_REGIONAL.md (este relatório)

## Reutilização

RibbonStrip, Footer, CompanyOpportunities, MunicipalitySearch, TerritoryProvider e API territorial existente. A busca pública não acessa o catálogo privado de candidatos nem envia credenciais às consultas públicas. A preferência territorial salva contém apenas a localidade e não vincula contas.

## Verificação

- Compilação TypeScript/Vite e lint executados com sucesso.
- Sete testes unitários do frontend aprovados.
- Três novos cenários Playwright aprovados: 360, 768 e 1440 pixels. Cobrem município inicial, pesquisa por cargo, troca de município por teclado, modalidade, ausência de envio de credenciais, estado vazio, ausência de rolagem horizontal e abertura do diagnóstico.
- Axe sem violações nas regras WCAG A/AA verificadas nesses três cenários; inspeção visual da captura mobile realizada. Isso não equivale a certificação WCAG integral nem substitui testes com leitores de tela reais.
- Testes de navegador utilizam respostas controladas da API; não representam medição de desempenho de produção nem confirmação da disponibilidade de vagas reais.

## Operação e limites

Necessário backend com os endpoints territoriais da etapa regional anterior. O anúncio depende das regras existentes de publicação, aprovação, pagamento e validade; esta entrega não as altera. Busca pública exibe anúncios empresariais, enquanto o catálogo administrado de candidatos continua no ambiente autenticado.

Não foi feita implantação ou publicação no GitHub. As alterações regionais anteriores e os arquivos locais em andamento foram preservados.

## Regressão completa

A suíte de navegador executou 82 cenários: 81 passaram inicialmente; um teste institucional ainda procurava o título antigo. A expectativa foi atualizada para o novo título solicitado, sem remover verificações. Em seguida, os dez cenários do ecossistema e os três novos da homepage foram reexecutados: 13 aprovados. Assim, todos os cenários foram validados, com essa reexecução documentada. O arquivo frontend/e2e/ecosystem-journey.spec.ts também foi modificado exclusivamente nessa expectativa de título.

Não foram repetidos testes do backend nesta entrega exclusivamente frontend. A compilação, o lint, os sete testes unitários e a verificação de espaços do Git passaram. Capturas desktop e mobile foram inspecionadas visualmente.
