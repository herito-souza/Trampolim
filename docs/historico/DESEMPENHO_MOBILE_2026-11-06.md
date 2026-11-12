# Trampolim — desempenho web e experiência mobile

Coleta iniciada em 06/11/2026; validação concluída em 07/11/2026.

## Escopo e linha de base
Foi usada a versão local atual, incluindo melhorias anteriores, sem substituir o projeto pelo repositório remoto. Auditoria encontrou rotas principais já lazy, fontes locais, imagens de cursos com dimensões/lazy loading, reprodução com preload=none e mapa/PDF sob demanda. Não havia justificativa para trocar React, Vite, MapLibre ou PDF.js.

A linha de base foi medida antes de modificar a aplicação. O roteiro antigo de medição precisou ser atualizado para a rota atual de Oportunidades e para uma resposta válida do resumo educacional. Medições interrompidas durante esse ajuste não foram usadas na comparação.

Método comum antes/depois:
- Build de produção servido localmente com gzip; Chromium/Playwright e web-vitals da versão instalada no projeto.
- Viewports 375×900 e 1440×900, deviceScaleFactor=1; contexto novo e cache desativado a cada execução.
- Rede simulada: 1,6 Mbps de download, 0,75 Mbps de upload, latência 150 ms; CPU 4× mais lenta.
- Três execuções por cenário; quatro áreas, dois tamanhos, 24 amostras antes e 24 finais.
- APIs interceptadas com dados sintéticos identificados e espera fixa de 120 ms. Não mede banco, servidor real ou tempo de internet dos moradores da região.
- Observação inicial de 6,5 segundos, seguida das mesmas ações por área (expandir card/buscar; consultar cursos; gerenciar anúncios; buscar cursos/trocar seção), antes de finalizar os registros de Web Vitals.
- Durante as medições não foram executados testes de navegador em paralelo. O build servido permaneceu congelado em cada fase.
- Os dados têm variação de execução e são amostras de laboratório, não p75 de campo, CrUX ou garantia em qualquer smartphone. INP depende das interações amostradas, não cobre todos os fluxos da plataforma.

## Resultados comparativos
Medianas de três execuções. JavaScript inicial corresponde à soma dos corpos gzip dos arquivos JS realmente solicitados antes das interações; não inclui fontes, CSS, cabeçalhos e APIs.

| Área | Largura | LCP, segundos: antes → depois | INP, ms: antes → depois | CLS: antes → depois | JS inicial, KB gzip: antes → depois |
| --- | ---: | ---: | ---: | ---: | ---: |
| Institucional | 375 | 2,080 → 1,996 | 96 → 88 | 0,000 → 0,000 | 89.7 → 82.1 |
| Institucional | 1440 | 2,112 → 2,016 | 112 → 104 | 0,000 → 0,000 | 89.7 → 82.1 |
| Oportunidades | 375 | 2,864 → 2,876 | 72 → 72 | 0,007 → 0,002 | 99.0 → 98.7 |
| Oportunidades | 1440 | 3,056 → 2,968 | 88 → 96 | 0,000 → 0,000 | 99.0 → 98.7 |
| Empresas | 375 | 2,616 → 2,396 | 144 → 168 | 0,098 → 0,000 | 124.3 → 104.3 |
| Empresas | 1440 | 2,848 → 2,448 | 184 → 192 | 0,049 → 0,000 | 124.3 → 104.3 |
| Capacita | 375 | 2,436 → 2,348 | 176 → 176 | 0,206 → 0,035 | 118.2 → 105.5 |
| Capacita | 1440 | 2,572 → 2,468 | 160 → 136 | 0,052 → 0,027 | 118.2 → 105.5 |

O arquivo principal passou de 286186 para 254708 bytes sem compressão e de 86389 para 78867 bytes gzip. No painel empresarial mobile, requisições iniciais totais passaram de 26 para 18; APIs iniciais passaram de seis chamadas para quatro (uma consulta duplicada de associações e a consulta comercial foram retiradas do caminho inicial). Cursos, vídeos, mapas e PDFs não foram eliminados.

Oportunidades permanece com LCP mediano acima de 2,5 s nos dois tamanhos: não houve melhora relevante no mobile (2,864 → 2,876 s). A retirada do questionário da entrada ajuda as outras áreas, mas a rota de Oportunidades ainda carrega o módulo compartilhado de recomendações. Há uma requisição de chunk adicional; o total de JS dessa rota ficou quase estável. O ganho não deve ser apresentado como universal.

Todas as medianas de INP ficaram abaixo de 200 ms, mas houve amostras de 240 ms no Capacita mobile; as medianas empresariais de INP também aumentaram, embora continuem abaixo de 200 ms. Os intervalos mínimo/máximo estão no JSON comparativo. Não se afirma que todas as interações atingem a meta. Todas as medianas finais de CLS ficaram abaixo de 0,1.

Nenhuma das amostras finais registrou erro JavaScript ou carregamento de mapa, worker PDF ou arquivo MP4 na entrada/interações escolhidas. Abrir esses recursos explicitamente continua trazendo seus arquivos pesados.

## Otimizações implementadas
1. Leitura do resultado salvo do diagnóstico isolada em diagnosticStorage.ts. Perguntas, pontuação, validação do resultado e recomendações foram preservadas; exportações antigas continuam disponíveis. Os construtores de dados das perguntas foram marcados como puros, sem alterar seu resultado. Não foi criado outro diagnóstico.
2. Ferramentas da associação passam a carregar sob demanda em Empresas; App fornece a consulta de organizações já feita para navegação. Snapshot fica apenas na memória da sessão atual, identificado por token, com cancelamento de respostas antigas. Não foi adicionado cache persistente de dados privados.
3. Histórico de interesse comercial é consultado somente ao abrir Planos e parcerias, preservando operações existentes. O resumo empresarial não espera essa chamada.
4. Formulários de curso/disciplina/módulo e administração docente foram extraídos para CourseManagement.tsx; editor e reprodutor chegam apenas quando usados, protegidos pelos limites de carregamento/erro existentes. Regras e permissões não mudaram.
5. Skeleton empresarial que aparecia acima dos botões e depois desaparecia foi substituído por mensagem com altura reservada. Resumo educacional mantém a geometria dos dados durante a espera, com indicação de carregamento em vez de números fictícios. Isso reduziu os deslocamentos observados.
6. Listas empresariais renderizam lotes de 20 registros, com botão acessível para ampliar. Filtros, contadores e CSV continuam usando todo o conjunto carregado, inclusive registros ainda não renderizados. Trata-se de limite de trabalho no DOM, não nova paginação de servidor; endpoints existentes foram preservados.
7. LoadingStatus informa espera prolongada após três segundos e falta de conexão. Timers/listeners são removidos ao desmontar, e a linha auxiliar já reserva espaço. Fluxos existentes de erro e nova tentativa continuam disponíveis.
8. Avatar possui dimensões explícitas e decoding assíncrono, preservando a origem autenticada em memória. Imagens de cursos continuam lazy e com dimensões. Nenhuma biblioteca de ícones ou fonte externa foi adicionada; o sistema SVG existente foi mantido.
9. Uma tentativa de antecipar manualmente o código de Oportunidades piorou o LCP no ensaio intermediário e foi retirada. Não foi mantida apenas por parecer uma otimização.

## Funcionalidades preservadas e segurança
- Autenticações e bancos independentes, regras de permissão e contratos HTTP sem alterações.
- Fotos continuam autenticadas, sem token na URL; não foi adicionado service worker ou cache compartilhado para dados privados.
- Sem autoplay de videoaulas. preload=none já existente foi preservado; gravação de câmera só começa mediante ação do usuário no fluxo existente.
- Preferências de acessibilidade, foco/teclado, notificações e progresso educacional foram preservados. Textos de carregamento não fingem sucesso nem conexão restaurada.
- Sem alteração de backend, banco, pagamentos, publicação de dados ou implantação.

## Arquivos desta etapa
Criados:
- frontend/src/features/diagnostic/diagnosticStorage.ts
- frontend/src/features/learning/CourseManagement.tsx
- frontend/src/components/ui/ProgressiveList.tsx
- frontend/src/components/ui/useProgressiveList.ts
- frontend/e2e/mobile-performance.spec.ts
- ops/mobile-performance-2026-11-06/comparison.json e este relatório

Modificados, preservando alterações locais anteriores:
- frontend/src/App.tsx
- frontend/src/features/diagnostic/recommendations.ts
- frontend/src/features/account/CompanyPortal.tsx
- frontend/src/features/account/CompanyDashboard.tsx
- frontend/src/features/dashboard/CompanyOverview.tsx
- frontend/src/features/learning/LearningPortal.tsx
- frontend/src/features/learning/LearningOverview.tsx
- frontend/src/features/learning/types.ts
- frontend/src/components/ui/Feedback.tsx
- frontend/src/components/workspace/ProfileAvatar.tsx
- frontend/scripts/measure-performance.mjs
- README.md

## Testes e evidências
- Lint: aprovado sem avisos (ops/mobile-lint.log).
- Unitários: 19 aprovados (ops/mobile-unit.log), incluindo os testes existentes do diagnóstico.
- TypeScript e build de produção: aprovados (ops/mobile-after-build.log).
- Ensaios focados iniciais: 33 aprovados e uma falha na expectativa de contagem em modo de desenvolvimento. O teste novo foi ajustado para considerar a montagem/limpeza dupla do StrictMode; a contagem em produção é medida separadamente. Nenhuma verificação funcional foi removida.
- Suíte completa final de navegador: 180 aprovados, zero falhas, em 7,2 minutos (ops/mobile-e2e.log). Inclui teclado, acessibilidade automática, permissões, rotas diretas, notificações, biblioteca, avaliações, vídeos e larguras 320/375/768/1024/1440 px.
- Verificação adicional: 3 aprovados, zero falhas (ops/mobile-slow-validation.log), incluindo um novo cenário de atraso de chunk, aviso após três segundos, contexto offline/online e conclusão do carregamento. Os outros dois casos repetem regressões da suíte principal; são 181 cenários distintos aprovados em execuções complementares. Não foi repetida a suíte inteira após acrescentar esse teste.
- Última alteração de fonte após a suíte foi somente remoção de linha em branco no fim de arquivo. Build e lint finais foram repetidos, sem alterar os arquivos de produção medidos.
- git diff --check aprovado; avisos de normalização CRLF não são erros de whitespace.
- Novos testes verificam limite inicial de 20 cards, expansão via teclado, filtro encontrando registro 45, ausência de overflow e ausência de módulos docentes/reprodutor/mapa/PDF na entrada do aluno.
- Dados brutos: ops/performance-mobile-before-debug.json (375 px, nome de coleta diagnóstica), ops/performance-mobile-before-desktop-2026-11-06.json e ops/performance-mobile-final-2026-11-06.json. Os rótulos não indicam build de desenvolvimento: todas essas medições usam dist de produção.
- Comparação com medianas e extremos: ops/mobile-performance-2026-11-06/comparison.json. Logs das tentativas intermediárias foram preservados; não foram usados como resultado final.

## Limites e próximos passos
- A meta de LCP de Oportunidades continua pendente. Investigar a sequência perfil/diagnóstico, carregamento de fontes e separar mais o conteúdo do questionário dos cálculos compartilhados; validar cada mudança sob o mesmo roteiro antes de adotá-la. Não trocar uma requisição grande por uma cadeia de pequenos arquivos sem medir.
- O mapa (~1 MB de JS sem gzip) e o leitor PDF/worker continuam grandes, porém sob demanda. Não se justifica removê-los ou trocar bibliotecas somente pelo aviso do build. Monitorar uso real e comportamento em conexão oscilante ao abrir esses recursos.
- Avatar original pode ser grande; dimensões e decoding não diminuem bytes do arquivo. Miniaturas privadas no servidor e variantes responsivas exigem trabalho próprio no armazenamento/API e não foram fingidas nesta entrega.
- Reprodução adaptativa de vídeo depende de variantes/transcodificação e distribuição apropriadas. Não foi introduzida infraestrutura externa nem afirmado streaming adaptativo inexistente.
- Listas de anúncios/candidaturas ainda recebem o conjunto previsto pelas APIs atuais. O novo limite de DOM não substitui paginação obrigatória no servidor; quando o volume real justificar, integrar os endpoints paginados sem falsificar totais agregados.
- Implantação deve preservar gzip/Brotli para ativos públicos, cache imutável de arquivos com hash e respostas privadas sem cache compartilhado. Validar HTTPS, região do servidor, latência de APIs, disponibilidade de armazenamento e observabilidade em etapa de infraestrutura.
- Necessário medir em smartphones reais e coletar Web Vitals com minimização de dados para observar p75 e variação regional. Não há prova de suporte a milhares de usuários ou certificação integral WCAG/LGPD baseada nesses testes.
