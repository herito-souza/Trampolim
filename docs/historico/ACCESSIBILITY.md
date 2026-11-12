# Revisão de acessibilidade do Trampolim

O frontend já inclui idioma em português, links para pular ao conteúdo, foco visível, áreas de toque maiores em telas pequenas, suporte a movimento reduzido e avisos associados às ações. Este roteiro complementa as verificações automáticas do CI.

O CI também executa verificações automatizadas WCAG A/AA com axe nas telas de oportunidades, vagas e perfil, além de uma jornada responsiva de login, favoritos e acompanhamento. A análise automatizada não substitui testes com leitores de tela ou avaliação humana.

## Antes de cada publicação

- Navegue por início, diagnóstico, catálogo, perfil e painel administrativo usando somente Tab, Shift+Tab, Enter, Espaço e setas.
- Confirme que o foco está sempre visível e que diálogos, formulários e mensagens podem ser usados sem mouse.
- Amplie o navegador para 200% e confira se filtros, cartões, menus e formulários continuam legíveis sem rolagem horizontal da página.
- Confira contraste de texto, estados de foco, mensagens de erro e botões nos temas claro e escuro do sistema.
- Teste pelo menos uma vez com NVDA no Windows e VoiceOver no iOS; confirme títulos, rótulos, estado dos controles e avisos de carregamento/erro.
- Faça uma sessão de avaliação com pessoas com deficiência antes de mudanças grandes de navegação ou de conteúdo. Registre obstáculos e corrija-os antes de publicar.

Não colete dados pessoais durante essas avaliações. Convide participantes voluntários e peça consentimento antes de registrar comentários ou gravações.
