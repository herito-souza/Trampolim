# Organização do Capacita por módulos

A navegação segue os botões arredondados usados na trilha Empresas. Cada seção mostra apenas suas próprias funcionalidades, com seleção visível e navegação por Tab, Enter, setas, Home e End.

## Áreas

- Visão geral: apresentação do Capacita.
- Aprendizagem: cursos e videoaulas, biblioteca, mini provas, desempenho e competências, dúvidas e apoio educacional.
- Conteúdos por etapa: seleção de ensino médio, superior e conclusão do ensino médio, busca e materiais.
- Preparação profissional: orientações, diagnóstico para o primeiro trabalho e estágios publicados.
- Minha conta: exportação dos próprios dados, declaração de faixa etária e exclusão de conta.

Professor mantém Gerenciar cursos e Dúvidas recebidas. Administrador possui acesso separado à Administração educacional. As autorizações existentes do servidor continuam responsáveis pela proteção das operações.

## Contexto de estudo

Trocar de módulo mantém o curso, a disciplina e a aula selecionados na sessão atual. O vídeo é pausado ao sair da área de cursos ou de aprendizagem. Os atalhos de materiais, avaliações e dúvidas do curso/módulo/aula continuam disponíveis no contexto de estudo. Notificações continuam conectadas e podem abrir diretamente uma dúvida.

A organização não altera endpoints, credenciais, bancos de dados, regras de progresso ou publicação. As rotas atuais continuam compatíveis. A seleção dos módulos é local à tela, como a navegação empresarial existente; não há novos endereços públicos de cada módulo.

## Arquivos desta mudança

- `frontend/src/features/learning/CapacitaModuleNav.tsx`: componente de navegação reutilizável.
- `frontend/src/features/learning/LearningPortal.tsx`: separação das funcionalidades educacionais, preservação de contexto e pausa de mídia.
- `frontend/src/pages/CapacitaPage.tsx`: organização das áreas e conta separada.
- `frontend/e2e/capacita-modules.spec.ts`: testes de navegação e contexto em desktop e celular.
- `frontend/e2e/ecosystem-journey.spec.ts`: adaptação dos fluxos de conta e estágio à navegação nova.
- `frontend/.gitignore`: pasta de resultados temporários da validação.

## Verificação

Compilação TypeScript/Vite, lint e 7 testes unitários aprovados. O aviso preexistente de pacote JavaScript maior que 500 kB continua presente.

Suíte completa Playwright: **43 testes aprovados** (2,5 minutos), incluindo seis cenários de acessibilidade, navegação modular em 1280/390 px e regressões de aulas, biblioteca, provas, dúvidas, notificações e privacidade. Os testes de navegador usam a infraestrutura de API interceptada já adotada no projeto; nesta mudança não foram repetidos os testes de integração PostgreSQL/backend, pois não houve alterações nesses componentes. A captura móvel foi inspecionada visualmente e não apresentou corte horizontal.
