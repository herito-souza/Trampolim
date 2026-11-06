# Navegação SaaS — Sidebar e Topbar

Implementação local de 05/11/2026. Fonte de verdade: checkout local, branch main, base d2321cb. Nenhum banco, contrato de API, dependência, regra de autenticação ou cobrança foi alterado.

## Diagnóstico anterior

Oportunidades combinava AccountNavigation, cabeçalhos por página e estados de página no armazenamento local. Empresas tinha navegação horizontal do portal, navegação do painel e submenus de Hub, talentos e carreira. Capacita combinava o menu de áreas com módulos educacionais, modo professor e cabeçalhos próprios. A quantidade de controles crescia e repetia navegação em diferentes alturas. Alguns destinos internos não preservavam o módulo no histórico do navegador.

Foram reutilizados os callbacks de navegação, componentes das páginas, permissões retornadas pelo servidor, sessão de cada trilha, notificações persistidas/SSE do Capacita e preferências de acessibilidade existentes. Os formulários, ações contextuais, paginação, mídias e fluxos de candidatura continuam nas páginas originais.

## Estrutura implementada

- AppLayout: estrutura compartilhada; Sidebar por categorias, Topbar sticky, página ativa, perfil e saída da trilha atual.
- WorkspaceNavigation: registra os menus dos módulos na Sidebar. Fora do layout mantém navegação autônoma compatível. Não duplica os menus dentro do conteúdo.
- WorkspaceSlots: omite os cabeçalhos repetidos apenas dentro do layout e transporta a instância existente de notificações para a Topbar.
- NavIcon: SVGs locais; nenhuma biblioteca adicionada.
- accountNavigationModel: destinos reais, restauração de URLs e configuração de permissões do menu de Oportunidades.
- Preferências do Capacita: controles originais no menu da conta, com a mesma persistência e aplicação global.

Os menus antigos de páginas internas foram consolidados na Sidebar. A homepage institucional, logins, cadastros e páginas públicas mantêm seus layouts. Estados de carregamento e de acesso negado continuam usando as verificações existentes antes de mostrar dados privados.

## Comportamento responsivo

| Largura | Estado inicial | Interação |
| --- | --- | --- |
| Abaixo de 768 px | Fechada | Dialog modal nativo, overlay, bloqueio da rolagem do fundo; fecha ao selecionar, tocar fora ou pressionar Escape |
| 768–1199 px | Faixa de ícones | Expansão sobreposta, sem diminuir novamente o conteúdo; botão continua acessível |
| 1200 px ou mais | Expandida | Recolhe para ícones, redistribui espaço e preserva escolha por trilha no navegador |

Ícones recolhidos possuem nome acessível e identificação ao passar o mouse. Categorias podem ser recolhidas; página ativa usa aria-current. Botão hambúrguer informa aria-expanded/aria-controls. Tab, Shift+Tab e Escape funcionam no menu móvel; foco volta ao acionador ao cancelar. Ao navegar, o conteúdo recebe foco. Há link de salto ao conteúdo, foco visível, alvos mínimos de 44 px nos controles de navegação, movimento reduzido e áreas seguras de dispositivos móveis. O link de salto não altera a rota.

A decoração full-bleed das páginas foi limitada à largura disponível no layout, incluindo a propriedade translate do Tailwind v4, para evitar deslocamento e corte do conteúdo à esquerda.

## Destinos reais por ambiente

### Oportunidades

Principal: Oportunidades, Vagas, Cursos. Carreira: Diagnóstico, Radar, Carreira e projetos, Talentos futuros, Premium. Conta: Perfil profissional. Administração e Conteúdos Capacita/Gestão SaaS aparecem apenas para os perfis já autorizados. Associação, quando disponível, encaminha ao ambiente corporativo, que exige sua própria sessão.

URLs privadas: #oportunidades/opportunities, #oportunidades/catalog-jobs, #oportunidades/catalog-courses, #oportunidades/diagnostic, #oportunidades/radar, #oportunidades/career, #oportunidades/future, #oportunidades/details, #oportunidades/premium e destinos administrativos existentes. URLs legadas #oportunidades e #diagnostico continuam válidas. Submódulos de carreira preservam #oportunidades/career/<módulo>. Favoritos, candidaturas e configurações permanecem nos fluxos existentes, sem criar páginas artificiais.

### Empresas

Principal: Visão geral, Vagas e cursos, Publicar vaga conforme permissão. Recrutamento: Candidaturas/Passaportes recebidos conforme permissão, Talentos futuros e Desafios e impacto. Gestão: Métricas, Planos e parcerias conforme permissão. Conta: Perfil público e Equipe apenas para proprietário. Hub registra seus módulos reais e restringe identidade/contratos a gestores autorizados.

Preservadas as URLs #empresas e #empresa-<página> e aliases legados do painel. Download/exclusão da conta permanecem no menu de perfil. Convites e compartilhamentos corporativos mantêm suas URLs e exigem sessão empresarial.

### Capacita

Áreas: Visão geral, Aprendizagem, Conteúdos por etapa, Preparação profissional, Minha conta. Aprendizagem: cursos/videoaulas, trilhas, avaliações, biblioteca, progresso/competências, passaporte, dúvidas, programas e demais módulos já existentes. Professor: Gerenciar cursos e Dúvidas recebidas somente quando autorizados pelo servidor. Não há segundo login nem novos papéis.

URLs: #capacita/<área> e #capacita/learning/<módulo>. O modo de gerenciamento utiliza ?mode=teacher dentro do fragmento e só é aplicado se a conta possuir autorização. #capacita continua compatível. Filtros, aulas, progresso e notificações mantêm seus mecanismos originais.

## Sessões e notificações

O layout não lê nem transfere credenciais entre trilhas. Sair afeta somente a sessão do ambiente atual. Links entre ambientes passam pelo roteamento e pelos controles já existentes. Ocultar um link não substitui autorização do backend.

Capacita reutiliza o indicador real de não lidas, histórico, marcação de leitura e stream de notificações. Oportunidades/Empresas disponibilizam acesso às atualizações existentes sem fabricar contagem ou criar um novo serviço de notificações. Não há requisições de identidade adicionais geradas pelo componente de navegação.

## Verificação e execução

Na pasta frontend:

~~~powershell
npm run lint
npx tsc -b
npm test
npm run test:e2e -- --workers=2
npm run build
~~~

A suíte workspace-navigation cobre seis perfis em 320, 375, 768, 1024 e 1440 px, estado persistido, histórico/URLs diretas, restrições administrativas, saída, foco, overlay, Topbar sticky, preferências ampliadas e ausência de requisições de identidade redundantes. Testes anteriores foram adaptados para acionar os menus reais; as verificações de permissões foram mantidas no novo local.

Os testes de navegador usam contas/dados sintéticos e APIs interceptadas. Testes HTTP/PostgreSQL do backend devem complementar essa evidência; não representam teste de produção ou carga.

### Resultados executados

| Verificação | Resultado |
| --- | --- |
| Lint (oxlint) | Aprovado |
| TypeScript (tsc -b) | Aprovado, como parte do build |
| Unitários frontend | 15 aprovados |
| Playwright — suíte completa congelada | 160 aprovados, sem falhas ou testes ignorados; 6,3 min, 2 workers |
| Backend selecionado | 21 aprovados, sem falhas/erros/ignorados; Maven BUILD SUCCESS |
| Build de produção | Aprovado |
| git diff --check | Sem erros de whitespace |

Os 39 novos cenários de navegação complementam os 121 anteriores. A execução final foi feita sem editar arquivos; uma tentativa anterior foi interrompida por recarga do servidor de desenvolvimento. Os testes anteriores não foram removidos.

Backend: JwtAuthenticationFilterTrailIsolationTest, TrailTokenServiceTest, AuthControllerTrailIsolationTest, LearningHttpPostgresTest e HubHttpPostgresTest. Foram usados três PostgreSQL locais e schemas temporários isolados, com migrações existentes e limpeza dos próprios dados de teste. Não houve nova migração nem alteração dos dados operacionais. A suíte inteira de backend não foi repetida: a mudança é exclusiva do frontend.

Axe não encontrou violações nos estados efetivamente examinados pelos testes. Foram exercitados Tab, Shift+Tab, setas, Escape, foco/restauração, overlay, ampliação de fonte/espaçamento, URLs diretas, voltar/atualizar, menus por perfil, notificações e recusa de sessões. Isso não substitui avaliação humana com tecnologias assistivas.

Evidências: [resultados estruturados](ops/navigation-2026-11-05/validation.json), [log Playwright final](ops/navigation-2026-11-05/playwright-final.log) e capturas sintéticas [Oportunidades desktop](ops/navigation-2026-11-05/candidate-1440.png), [Empresas desktop](ops/navigation-2026-11-05/owner-1440.png), [Capacita desktop](ops/navigation-2026-11-05/student-1440.png), [Capacita celular](ops/navigation-2026-11-05/student-375.png), [menu móvel](ops/navigation-2026-11-05/drawer-375.png).

### Problemas corrigidos

- Menus de páginas duplicados e longas barras horizontais: registrados na Sidebar compartilhada.
- Endereços internos sem restauração adequada: preservação de estados reais no fragmento e sincronização do histórico.
- Decoração de largura total deslocada pelo translate do Tailwind v4: largura/posicionamento adaptados ao layout.
- Sidebar de tablet cobrindo o hambúrguer quando expandida: posicionamento abaixo da Topbar.
- Preferências do Capacita atrás do menu fixo: controles originais transportados para o perfil.
- Retorno de foco em painéis carregados sob demanda: restauração ao acionador visível ou conteúdo.
- Contraste insuficiente de um texto do perfil no novo fundo: opacidade ajustada.
- Testes que procuravam os menus no local anterior: seletores atualizados, com verificações de permissões incluindo links ocultos.

### Desempenho

Medição concluída em build de produção local, viewport de 375 px: 3 execuções por área (12 amostras), cache frio, latência de 150 ms, download de 1,6 Mbps e CPU desacelerada em 4 vezes. APIs sintéticas com atraso de 120 ms; não é medição do backend ou de usuários reais. Medianas:

| Área | LCP (ms) | INP amostral (ms) | CLS |
| --- | ---: | ---: | ---: |
| Institucional | 2044 | 96 | 0.000 |
| Oportunidades | 2864 | 72 | 0.015 |
| Empresas | 2908 | 168 | 0.025 |
| Capacita | 2300 | 176 | 0.000 |

Nenhuma amostra baixou mapa, worker de PDF ou vídeo desnecessariamente no carregamento inicial. Oportunidades e Empresas ficaram acima do objetivo de LCP de 2,5 s nessa simulação; permanece necessária otimização/medição em infraestrutura de homologação e produção. INP é uma amostra de interações do roteiro, não p75 de campo. Os dados não permitem prometer capacidade para milhares de usuários.

O JavaScript de entrada ficou em aproximadamente 86,4 kB gzip (registro anterior de QA: 83,8 kB). O build preserva os chunks separados de mapa/PDF e seu aviso de tamanho já existente. Nenhuma dependência nova.

[Dados completos da medição](ops/performance-sidebar-2026-11-05.json). Para repetir na pasta frontend, após npm run build: PERF_WIDTH=375 seleciona somente essa largura no script scripts/measure-performance.mjs; sem a variável, mantém todos os cenários originais.

## Limitações

Não foi feita homologação humana com NVDA/VoiceOver nem testes em aparelhos físicos nesta sessão. Axe e teclado automatizado não comprovam conformidade integral WCAG 2.2 AA. O layout não resolve, por si só, os requisitos de infraestrutura, pagamentos/SMTP ou implantação documentados em QA_IMPLANTACAO_IRECE.md. A implantação em produção é uma etapa separada da publicação do código no GitHub.

## Arquivos criados e modificados

Lista integral: [manifesto de arquivos](ops/navigation-2026-11-05/changed-files.json). O arquivo preexistente e não relacionado Trampolim foi preservado.

### Implementação

- `frontend/src/App.tsx`
- `frontend/src/components/sections/Header.tsx`
- `frontend/src/components/ui/RibbonStrip.tsx`
- `frontend/src/components/workspace/AppLayout.tsx`
- `frontend/src/components/workspace/NavIcon.tsx`
- `frontend/src/components/workspace/WorkspaceNavigation.tsx`
- `frontend/src/components/workspace/WorkspaceSlots.tsx`
- `frontend/src/components/workspace/workspace.css`
- `frontend/src/components/workspace/workspaceContext.ts`
- `frontend/src/features/account/AccountNavigation.tsx`
- `frontend/src/features/account/AdminCapacitaPage.tsx`
- `frontend/src/features/account/AdminCatalogPage.tsx`
- `frontend/src/features/account/AssociationDashboardPage.tsx`
- `frontend/src/features/account/CatalogPage.tsx`
- `frontend/src/features/account/CompanyDashboard.tsx`
- `frontend/src/features/account/CompanyPortal.tsx`
- `frontend/src/features/account/OpportunitiesPage.tsx`
- `frontend/src/features/account/PremiumPage.tsx`
- `frontend/src/features/account/ProfilePage.tsx`
- `frontend/src/features/account/SaasAdminPage.tsx`
- `frontend/src/features/account/WelcomeScreen.tsx`
- `frontend/src/features/account/accountNavigationModel.ts`
- `frontend/src/features/career/CareerPanel.tsx`
- `frontend/src/features/future/FutureTalentPanel.tsx`
- `frontend/src/features/hub/HubPanel.tsx`
- `frontend/src/features/learning/AccessibilityPreferences.tsx`
- `frontend/src/features/learning/CapacitaModuleNav.tsx`
- `frontend/src/features/learning/LearningPortal.tsx`
- `frontend/src/features/learning/QuestionNotifications.tsx`
- `frontend/src/features/learning/accessibilityContext.ts`
- `frontend/src/features/learning/accessibleFocus.ts`
- `frontend/src/features/radar/RadarPanel.tsx`
- `frontend/src/pages/CapacitaPage.tsx`

### Testes

- `frontend/e2e/accessibility-journey.spec.ts`
- `frontend/e2e/account-journey.spec.ts`
- `frontend/e2e/adaptive-passport-journey.spec.ts`
- `frontend/e2e/assessment-journey.spec.ts`
- `frontend/e2e/capacita-modules.spec.ts`
- `frontend/e2e/career-journey.spec.ts`
- `frontend/e2e/dashboard-journey.spec.ts`
- `frontend/e2e/discussion-journey.spec.ts`
- `frontend/e2e/ecosystem-journey.spec.ts`
- `frontend/e2e/future-talent-journey.spec.ts`
- `frontend/e2e/hub-journey.spec.ts`
- `frontend/e2e/learning-journey.spec.ts`
- `frontend/e2e/library-journey.spec.ts`
- `frontend/e2e/navigation-helpers.ts`
- `frontend/e2e/performance-behavior.spec.ts`
- `frontend/e2e/radar-journey.spec.ts`
- `frontend/e2e/territory-journey.spec.ts`
- `frontend/e2e/workspace-navigation.spec.ts`
- `frontend/tests/workspaceNavigation.test.mjs`

### Documentação e medição

- `README.md` e `NAVIGACAO_SAAS.md`.
- `frontend/scripts/measure-performance.mjs`: usa a navegação real e permite selecionar a largura, mantendo os cenários originais.
- `ops/performance-sidebar-2026-11-05.json` e `ops/navigation-2026-11-05/`: resultados, log e capturas sintéticas.

## Atualizações do cabeçalho e foto de perfil

O nome da trilha não é repetido à direita do logo quando coincide com o texto abaixo de Trampolim. Títulos específicos de páginas continuam visíveis.

Em Oportunidades, Perfil profissional permite enviar, trocar e remover foto JPG, PNG ou WEBP de até 5 MB. Reutiliza os endpoints privados POST/GET/DELETE /profile/photo já existentes, sem migração ou alteração do backend. A foto aparece imediatamente no menu da conta e é recuperada após atualização da página. O ícone padrão permanece quando não há imagem ou ocorre falha de carregamento. O token é enviado no cabeçalho de autorização, nunca no endereço da imagem. URLs temporárias em memória são liberadas ao substituir a foto ou encerrar a sessão. A foto não é publicada no perfil compartilhado.

Validação posterior à foto: 3 cenários novos aprovados (envio/persistência/remoção em 375 e 1440 px, formatos/tamanho e falha de envio), além dos 8 cenários anteriores de account-journey aprovados. Lint, TypeScript e build de produção aprovados novamente. O resultado de 160 testes documentado acima corresponde à execução completa anterior à adição da foto; não foi repetida toda a suíte após essa adição.
