# Desempenho mobile — revisão de 07/11/2026

## Escopo e situação inicial
A versão local foi usada como fonte de verdade, preservando as alterações em andamento e o backend do Plano de Empregabilidade Individual. Esta revisão não altera APIs, dados, autenticações nem bibliotecas. A etapa anterior está documentada em DESEMPENHO_MOBILE_2026-11-06.md; seus ganhos não são atribuídos novamente a esta revisão.

A auditoria confirmou code splitting por telas, carregamento sob demanda de mapa/PDF/editor docente/reprodutor, fontes locais com preload, avatars com dimensões reservadas, lotes de 20 anúncios, carregamentos com indicação de conexão lenta e vídeo sem reprodução automática. Essas medidas foram preservadas. Não houve justificativa para trocar bibliotecas ou acrescentar infraestrutura.

## Medição inicial
Build de produção servido localmente com gzip, Chromium/Playwright e web-vitals. Cache frio por contexto, CPU 4x mais lenta, latência de 150 ms, download de 1,6 Mbps e upload de 0,75 Mbps. Homepage e dashboards de candidato, empresa e aluno, em 375 e 1440 px, três repetições por combinação: 24 amostras iniciais.

As APIs usam fixtures sintéticas isoladas com espera de 120 ms; não se mede latência do backend nem capacidade de usuários simultâneos. INP representa interações roteirizadas, não p75 de usuários reais. Sem dados pessoais operacionais, serviços pagos ou geolocalização precisa. As mesmas condições foram usadas após o ajuste. Nenhum teste de regressão foi executado simultaneamente às coletas de desempenho.

| Tela | LCP inicial 375 px | INP inicial | CLS inicial |
|---|---:|---:|---:|
| Institucional | 2,072 s | 112 ms | 0 |
| Oportunidades | 2,812 s | 72 ms | 0,0114 |
| Empresas | 2,340 s | 152 ms | 0 |
| Capacita | 2,320 s | 176 ms | 0,0350 |

## Mudanças desta revisão
- App.tsx: consulta do diagnóstico iniciada em paralelo à validação do perfil, usando o mesmo token de Oportunidades e o mesmo AbortController. Seu resultado só é consumido depois de validar o perfil. Falha no diagnóstico não bloqueia o perfil; sincronização de diagnóstico pendente continua ocorrendo somente após resposta válida sem diagnóstico salvo.
- App.tsx: antecipação do módulo já existente de Oportunidades somente quando a rota selecionada indica esse dashboard. Importar código não concede acesso a dados. A leitura direta da rota evita colocar o estado de navegação entre as dependências da validação de sessão e repetir requisições a cada troca de página.
- App.tsx: mensagem de validação da sessão identificada como status acessível.
- scripts/measure-performance.mjs: seleção opcional por PERF_AREA, PERF_WIDTH e PERF_RUNS; registro do elemento LCP e dos tempos de recursos para investigar a cadeia crítica, sem conteúdo operacional. Usar somente com fixtures de medição: os detalhes do elemento podem conter texto e não devem ser enviados como telemetria de usuários reais.
- e2e/mobile-bootstrap.spec.ts: testes de consultas simultâneas, diagnóstico recebido antes da validação do perfil, sessão rejeitada, preservação das outras trilhas, falha parcial e sincronização de diagnóstico pendente.

As consultas necessárias não aumentam em quantidade na entrada do dashboard. A mudança reduz a dependência sequencial entre elas, mas não deve ser apresentada como redução comprovada do LCP. A primeira comparação após a mudança apresentou LCP de Oportunidades de 2,844 s no celular, contra 2,812 s inicialmente: não houve melhora consistente. A investigação do elemento LCP identificou o parágrafo principal do hero; o custo de carregamento/execução dos recursos e renderização continua relevante.

## Comparação final

| Tela | Largura | LCP antes → final | INP antes → final | CLS antes → final |
|---|---:|---:|---:|---:|
| Institucional | 375 px | 2,072 → 1,960 s | 112 → 88 ms | 0,0000 → 0,0000 |
| Institucional | 1440 px | 2,036 → 2,008 s | 104 → 96 ms | 0,0000 → 0,0000 |
| Oportunidades | 375 px | 2,812 → 2,864 s | 72 → 72 ms | 0,0114 → 0,0114 |
| Oportunidades | 1440 px | 2,992 → 2,888 s | 88 → 96 ms | 0,0001 → 0,0001 |
| Empresas | 375 px | 2,340 → 2,356 s | 152 → 160 ms | 0,0000 → 0,0000 |
| Empresas | 1440 px | 2,460 → 2,412 s | 192 → 192 ms | 0,0000 → 0,0000 |
| Capacita | 375 px | 2,320 → 2,336 s | 176 → 176 ms | 0,0350 → 0,0350 |
| Capacita | 1440 px | 2,428 → 2,404 s | 128 → 128 ms | 0,0270 → 0,0270 |

Medianas de três amostras por célula. O LCP de Oportunidades não melhorou de forma consistente e continua acima de 2,5 s nas duas larguras. Não se atribuem as pequenas variações nas demais trilhas à otimização específica de Oportunidades. Todas as medianas de CLS ficaram abaixo de 0,1; INP máximo observado foi 208 ms, portanto a meta de 200 ms não foi atendida em todas as interações. Não há comprovação de atendimento aos Web Vitals de produção.

A entrada das telas não fez requisições de mapa/PDF/vídeo nos 24 cenários finais; erros JavaScript coletados: 0. O bundle principal gzip passou de 78867 para 78964 bytes: não houve redução de bundle nesta revisão, e não se afirma esse ganho. Os ganhos de code splitting anteriores foram preservados.

Evidências definitivas: ops/performance-mobile2-before-2026-11-07.json, ops/performance-mobile2-final-2026-11-07.json e ops/mobile-performance-2026-11-07/comparison.json. As coletas intermediárias e diagnósticas foram preservadas, mas não substituem o build final na tabela. A coleta final inclui o tratamento corrigido de HTTP 204 e a leitura direta da rota no aquecimento do módulo. Reconexões de notificações nos logs refletem o stream sintético encerrado após o evento ready; não são prova de polling redundante em produção.

## Resultados e verificações
- Lint final: aprovado sem avisos (ops/mobile2-lint.log).
- Unitários: 19 aprovados, zero falhas/ignorados (ops/mobile2-unit.log).
- TypeScript e build de produção finais: aprovados (ops/mobile2-final-build.log). O aviso sobre chunks de mapa/PDF grandes continua; não foi ocultado.
- Rodada geral: 185 cenários reportados, 184 sucessos e uma falha no novo teste de diagnóstico pendente. O api helper retorna undefined em HTTP 204; o ajuste inicial o confundia com erro de consulta. A correção diferencia o resultado HTTP da falha de rede e preserva o comportamento anterior de sincronização. Nenhum teste foi removido ou afrouxado.
- O executor da rodada geral permaneceu preso após reportar o último cenário, sem emitir resumo final. Sua árvore de processos foi identificada e encerrada; não se declara saída limpa dessa rodada (ops/mobile2-e2e.log). O frontend de desenvolvimento não foi encerrado.
- Reteste após a correção: 40 aprovados, zero falhas, em 1,4 minuto, com saída normal (ops/mobile2-focused.log). Inclui os quatro testes novos, account-journey, dashboard-journey e mobile-performance. A suíte inteira não foi repetida após a correção; os 40 casos não devem ser somados aos 184 como se fossem todos diferentes.
- Os testes de dashboards cobrem 320, 375, 768, 1024 e 1440 px, teclado, ausência de overflow e análise automática axe. Capturas atuais de candidato 375 px, aluno 320 px e Empresas 375 px foram inspecionadas visualmente, sem cortes laterais observados. Leitor de tela humano não foi ensaiado.
- Regressões existentes exercitaram mapa, PDF, notificações, gravação de mídia, avaliações, rotas protegidas, papéis e sessões isoladas. O reteste focado confirma também ausência de editor docente/reprodutor/mapa/PDF na entrada do aluno, lotes e filtros de anúncios e recuperação de conexão.
- git diff --check sem erros nos arquivos desta revisão; apenas avisos de normalização CRLF.

Arquivos desta revisão: frontend/src/App.tsx, frontend/scripts/measure-performance.mjs, frontend/e2e/mobile-bootstrap.spec.ts, README.md, este relatório e as evidências ops/mobile2-*, ops/performance-mobile2-*.json e ops/mobile-performance-2026-11-07/comparison.json. Alterações locais anteriores foram preservadas. Nenhuma dependência, API, banco ou migração foi alterado.

## Limitações e recomendações
- LCP de Oportunidades continua acima de 2,5 s no ensaio. Não foi alterado o conteúdo ou escondido o hero para obter um número melhor. Investigar o custo de execução/renderização com perfil de CPU em aparelho real e latência real de APIs antes de novos fracionamentos de código.
- Mapa e PDF/worker permanecem bundles grandes, porém sob demanda. As 24 amostras de cada coleta não baixaram mapa, PDF ou vídeo na entrada; isso não mede o custo quando esses recursos são abertos.
- Limite de cards no DOM não substitui paginação no servidor. Reutilizar APIs paginadas na evolução das listas e medir com volumes representativos.
- Variantes de imagem/miniaturas privadas e vídeo adaptativo precisam de suporte de armazenamento/transcodificação; não foram simulados nem introduzidos sem necessidade nesta revisão frontend.
- Configuração de produção deve manter compressão, cache imutável para assets públicos com hash, proteção das respostas privadas e HTTPS. Medições locais não incluem CDN, armazenamento nem rede regional.
- Testes de teclado, análise automática de acessibilidade e inspeção visual não equivalem a certificação integral WCAG. Validar também com leitores de tela e pessoas que utilizem tecnologias assistivas.
- Coletar Web Vitals de produção com minimização de dados e medir p75 em smartphones reais. Não há evidência para afirmar suporte a milhares de usuários a partir destes ensaios.

## Reprodução
Na pasta frontend: npm run build; node scripts/measure-performance.mjs mobile-review. Variáveis PERF_AREA (homepage, candidate, company, capacita), PERF_WIDTH (375 ou 1440) e PERF_RUNS (padrão 3) restringem a coleta quando necessário. O script usa porta local 4180 e APIs sintéticas interceptadas; não executa gravações em dados reais.

Verificações: npm run lint; npm test; npm run test:e2e -- --workers=2; npm run build. Playwright cobre larguras 320, 375, 768, 1024 e 1440 px nas suítes existentes de dashboards/navegação.
