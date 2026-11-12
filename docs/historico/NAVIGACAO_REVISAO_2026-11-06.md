# Revisão da navegação — 06/11/2026

Fonte de verdade: projeto local em main, base 96e8c48. A única entrada não versionada anterior à revisão era o arquivo vazio Trampolim, preservado.

## Diagnóstico

A Sidebar e a Topbar já estavam implementadas e integradas aos ambientes autenticados. Recriar esses componentes duplicaria a navegação. A revisão manteve o layout existente e identificou três lacunas: o histórico do navegador podia deixar o menu sobreposto aberto; Escape não recolhia a expansão de tablet; uma categoria recolhida podia esconder o destino selecionado ao voltar.

Foram inspecionados App, accountNavigationModel, AccountNavigation, CompanyPortal, CompanyDashboard, CapacitaPage, CapacitaModuleNav, LearningPortal, WorkspaceNavigation, WorkspaceSlots e os menus de Carreira, Radar, Talentos futuros e Hub. A configuração Spring Security e o filtro JWT foram consultados sem alterações.

## Páginas e permissões preservadas

| Ambiente | Organização e destinos reais | Autorização existente |
| --- | --- | --- |
| Oportunidades | Principal: oportunidades, vagas, cursos. Carreira: diagnóstico, radar, projetos, talentos futuros, premium. Conta: perfil. Administração: catálogo, conteúdos Capacita, gestão SaaS. Ecossistema: início e demais trilhas. | Administração aparece apenas para contas já autorizadas; telas administrativas possuem guarda de acesso. Associação encaminha ao ambiente empresarial com sua própria sessão. |
| Empresas | Principal: visão geral, anúncios e publicação. Recrutamento: candidaturas, passaportes recebidos, talentos futuros e desafios. Gestão: métricas e planos. Conta: perfil público e equipe. Hub: indicadores, empresas, programas, campanhas, identidade e contratos. | Publicação, candidaturas, passaportes e planos seguem canRecruit; equipe segue isOwner; gestão institucional segue manager. |
| Capacita | Áreas: visão geral, aprendizagem, conteúdos por etapa, preparação profissional, conta. Módulos: cursos, trilhas adaptativas, passaporte, biblioteca, avaliações, desempenho, observatório, estágio, programas, dúvidas e apoio. | Gerenciamento de cursos e caixa de dúvidas recebidas dependem de account.teacher. A URL não concede esse papel. Notificações e preferências reutilizam os componentes autenticados existentes. |

### Rotas

- Oportunidades: #oportunidades/<view>, com os valores existentes welcome, details, opportunities, diagnostic, catalog-courses, catalog-jobs, radar, career, future, premium, admin, admin-capacita e admin-saas. Mantidos #oportunidades, #diagnostico e submódulos já existentes.
- Empresas: #empresas e destinos #empresa-visao-geral, #empresa-divulgacoes, #empresa-candidaturas, #empresa-metricas, #empresa-planos, #empresa-perfil, #empresa-equipe, #empresa-carreira, #empresa-futuros e #empresa-passaportes. Aliases anteriores continuam tratados pelo roteamento atual.
- Capacita: #capacita/<área> e #capacita/learning/<módulo>, com account.teacher validando o modo de professor. Mantidos os fluxos de login, cadastro, recuperação e links públicos.
- Módulos internos que utilizavam estado próprio mantêm esse contrato; não foram inventadas páginas ou URLs para preencher menus.

Inventário e arquitetura da implementação original: [NAVIGACAO_SAAS.md](NAVIGACAO_SAAS.md).

## Alterações desta revisão

1. AppLayout fecha menus transitórios em hashchange/popstate, inclusive ao voltar pelo navegador; a rolagem do fundo é liberada e o foco sai do menu fechado.
2. Escape recolhe o menu expandido no tablet e devolve o foco ao hambúrguer. O comportamento modal nativo do celular permanece preservado.
3. Mudança da página atual recolhe o menu transitório também quando a navegação acontece por callbacks internos.
4. SidebarNavigation revela a categoria da nova página ativa ao mudar de destino. O usuário ainda pode recolher manualmente categorias, inclusive a atual, até outra mudança de destino.
5. Testes incluem sete novos cenários e utilizam respostas sintéticas com o formato real de privacidade do perfil. Evidências passam a usar um diretório configurável, evitando sobrescrever capturas de revisões anteriores.

Não foram removidos outros menus nesta revisão: os menus horizontais internos já haviam sido incorporados ao registro compartilhado. Menus de páginas públicas, ações contextuais, paginação e formulários foram preservados.

## Estrutura mantida

- Desktop a partir de 1200 px: expandida por padrão, com preferência de recolhimento preservada por trilha.
- Tablet de 768 a 1199 px: ícones inicialmente, expansão sobreposta sem reduzir novamente a área de conteúdo.
- Celular abaixo de 768 px: dialog modal, overlay, foco contido, fechamento por fora/Escape/seleção e bloqueio da rolagem de fundo.
- Topbar sticky: hambúrguer, identificação da trilha, título específico quando aplicável, notificações existentes e menu da conta. O nome da trilha não é repetido ao lado do logo; a foto de perfil existente continua funcionando.
- Categorias expansíveis, aria-current, nomes acessíveis, tooltips nativos nos ícones recolhidos, alvos de navegação de 44 px e movimento reduzido.
- Sem bibliotecas novas, downloads adicionais, alterações no backend, APIs, bancos ou regras de negócio.

## Validação

| Verificação executada | Resultado |
| --- | --- |
| Lint | Aprovado, inclusive após o ajuste final |
| TypeScript | Aprovado via tsc -b no build |
| Unitários frontend | 15 aprovados |
| Suíte completa Playwright | 170 aprovados, zero falhas/ignorados (6,1 min) |
| Regressão final de navegação | 46 aprovados, zero falhas/ignorados (1,3 min) |
| Build de produção final | Aprovado |
| git diff --check | Sem erros |

Após a suíte completa, a regra de revelação do item ativo foi refinada para contemplar mudanças dentro da mesma categoria. A matriz de navegação inteira foi repetida nesse estado final (46 casos); os 170 casos completos não foram repetidos após esse refinamento. Os 46 são parte da suíte, e não devem ser somados aos 170 como testes distintos.

Cobertura: seis perfis em 320, 375, 768, 1024 e 1440 px; página ativa; categorias; preferência de recolhimento; tablet; modal móvel; teclado e foco; Escape; overlay; histórico; URLs diretas; atualização; logout; sessão recusada; notificações; preservação da foto; fluxos de cursos, materiais, avaliações, busca, candidaturas e mapa. Ações por contexto foram preservadas. Axe não encontrou violações nos estados examinados pelas asserções existentes; não é certificação WCAG.

As capturas de desktop e celular foram conferidas visualmente. O build mantém o aviso preexistente de chunks grandes de mapa/PDF; nenhuma biblioteca foi adicionada ou substituída. Não foram realizadas novas medições de Web Vitals nesta revisão de comportamento.

Evidências: [validação estruturada] (evidência local não versionada), [suíte completa] (evidência local não versionada), [regressão final] (evidência local não versionada), [Empresas desktop] (evidência local não versionada), [Capacita celular] (evidência local não versionada) e [menu móvel] (evidência local não versionada).

Para repetir na pasta frontend:

~~~powershell
npm run lint
npm test
npm run build
npm run test:e2e -- --workers=2
~~~

## Arquivos modificados

- frontend/src/components/workspace/AppLayout.tsx — histórico, Escape, fechamento e revelação da categoria ativa.
- frontend/e2e/workspace-navigation.spec.ts — novos cenários e destino configurável das capturas.
- NAVIGACAO_REVISAO_2026-11-06.md — este relatório.
- README.md — referência ao relatório, após a conclusão.
- ops/navigation-2026-11-06/ — evidências de navegação com dados sintéticos.

## Limites da validação

Os testes de navegador usam Chromium e APIs interceptadas, com dados sintéticos. Não comprovam conformidade integral WCAG ou LGPD nem substituem homologação com NVDA/VoiceOver e aparelhos físicos. A segurança do backend foi preservada, mas sua suíte não foi repetida nesta revisão exclusivamente de navegação. Não foi realizada implantação nem publicação automática no GitHub.
