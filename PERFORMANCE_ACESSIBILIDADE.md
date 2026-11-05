# Performance, responsividade e acessibilidade

## Escopo e arquitetura

O trabalho desta etapa altera somente o frontend. Preserva as três autenticações, APIs, bancos e regras de conclusão de aprendizagem. Não executa migrações nem implantação.

- Fontes Barlow Condensed e DM Sans mantidas, agora hospedadas localmente com licenças SIL OFL, subconjunto latino e preload dos dois arquivos prioritários. Origens em `frontend/src/assets/fonts/sources.json`.
- Painéis empresariais, recursos de carreira e ferramentas educacionais divididos em módulos carregados quando acessados. Mapa e leitor de PDF permanecem sob demanda.
- `Feedback.tsx` compartilha carregamento, mensagens e nova tentativa, com regiões acessíveis. Formulários usam cores da plataforma, controles com altura mínima de 44px, foco existente e quebra de textos longos.
- Imagens de cursos têm dimensões reservadas, carregamento tardio e decodificação assíncrona.
- Vídeo com `preload="none"`, sem autoplay. Captura de câmera continua apenas após ação explícita do usuário. Prévias de arquivos do mural não são baixadas antecipadamente.
- Falhas de conexão e espera por dados possuem mensagens. A gravação de progresso tenta novamente sem substituir a reprodução; usa a sessão e a validação existentes. Não há conclusão offline ou atribuição de competências pelo navegador. O trecho ainda não confirmado pode ser perdido ao fechar a página.

## Reprodução das medições

Na pasta frontend:

```powershell
npm ci
npm run build
node scripts/measure-performance.mjs after
npm run lint
npm test
npm run test:e2e -- --workers=2
```

O script inicia seu próprio servidor local na porta 4180 e mede a versão compilada com gzip. Não executar testes pesados ou recompilar simultaneamente. `web-vitals` é dependência de desenvolvimento usada apenas pelo instrumento, sem telemetria dos usuários.

As evidências estão em `ops/performance-before.json` e `ops/performance-after.json`. São três amostras por cenário (33 por versão), Chromium, cache desativado, 150ms de latência, 1,6Mbps download, 0,75Mbps upload e CPU 4x. APIs usam respostas sintéticas com atraso de 120ms: não medem backend real. Os valores representam mediana das interações roteirizadas em laboratório, não p75 de produção, CrUX ou garantia para qualquer celular. Uma primeira amostra pode incluir custos frios adicionais do navegador e rede externa.

## Limites e produção

- Validar os objetivos LCP 2,5s, INP 200ms e CLS 0,1 com usuários reais e distribuição por dispositivo/conectividade. O laboratório não substitui essa evidência.
- Configurar compressão Brotli/gzip e cache longo somente para arquivos com hash; HTML deve revalidar. O servidor do instrumento demonstra essa política, não configura a hospedagem de produção.
- Streaming adaptativo requer transcodificação, múltiplas qualidades, manifestos e distribuição apropriada. A infraestrutura atual oferece arquivo de vídeo/Range; esta etapa evita downloads antecipados, mas não inventa HLS ou adaptação de bitrate.
- O chunk MapLibre continua grande e isolado. Não é carregado na listagem inicial; seu aviso de compilação permanece visível.
- Concorrência, PostgreSQL, filas, armazenamento/CDN, consultas e disponibilidade dos serviços exigem validação de infraestrutura separada. Não há nova alegação de capacidade para milhares de usuários baseada nesses testes de frontend.
- Acessibilidade automatizada e fluxos de teclado não certificam conformidade integral. Leitura manual com NVDA/VoiceOver, aparelhos físicos, documentos e legendas de cada professor ainda precisam de validação editorial e assistiva.

## Arquivos desta etapa

- `frontend/src/styles/fonts.css`, `frontend/src/assets/fonts/*`, `frontend/index.html`, `frontend/src/index.css`: fontes, contraste, controles e regras de reflow.
- `frontend/src/App.tsx`, `frontend/src/features/account/CompanyDashboard.tsx`, `frontend/src/features/learning/LearningPortal.tsx`: divisão de carregamento e uso de painéis reutilizáveis.
- `frontend/src/components/ui/Feedback.tsx`: estados compartilhados de carregamento, erro e nova tentativa.
- `frontend/src/features/learning/LessonPlayer.tsx`, `MediaRecorderInput.tsx`, `frontend/src/features/hub/HubPublicPage.tsx`: mídia, progresso, imagens e conectividade.
- `frontend/scripts/measure-performance.mjs`, `frontend/package.json`, `frontend/package-lock.json`, `ops/performance-*.json`: instrumento e evidências.
- `frontend/e2e/dashboard-journey.spec.ts`, `homepage-regional.spec.ts`, `learning-journey.spec.ts`, `accessibility-journey.spec.ts`: matriz de larguras, teclado, verificações automatizadas e reprodução sob demanda.
- `README.md` e este documento.

O checkout já continha alterações de etapas anteriores. Esta lista identifica o escopo desta etapa, sem atribuir todas as alterações locais a esta otimização.

## Referências

- [Core Web Vitals: medição em laboratório e em campo](https://web.dev/articles/vitals).
- [Biblioteca de medição web-vitals](https://github.com/GoogleChrome/web-vitals).
- WCAG 2.2 AA orienta as verificações; axe não substitui auditoria humana.

## Validação executada — 04/11/2026

- Compilação TypeScript/Vite aprovada; aviso de tamanho do MapLibre mantido, módulo continua sob demanda.
- Oxlint sem erros.
- 12 testes unitários aprovados.
- 118 testes Playwright aprovados (4,5 minutos, dois workers). Após o ajuste final de carregamento de Empresas, os 37 cenários afetados de dashboards, ecossistema e carreira foram repetidos e aprovados (1,3 minuto); compilação e lint repetidos com sucesso.
- Matriz 320, 375, 768, 1024 e 1440px: homepage, candidato, empresa, aluno e professor; filtros, formulários, navegação por teclado e ausência de overflow da página. Verificações axe nos cenários auditados sem violações detectadas; isso não comprova conformidade integral.
- Testes de ampliação de texto, preferências, foco, legendas fornecidas e transcrição preservados. Revisão visual de capturas de candidato e empresa em 320px.
- Videoaula testada sem download do MP4 antes da reprodução, retomada, falha de confirmação do progresso e nova tentativa usando a mesma sessão e URL de vídeo, sem conceder conclusão indevida.
- Corrigida corrida no debounce inicial: o temporizador podia reiniciar a paginação mesmo sem alteração da busca. Agora só reinicia quando o texto pesquisado muda. O teste de paginação e a suíte completa passaram após a correção.
- Execuções intermediárias apresentaram falhas que não foram omitidas: três cenários passaram na repetição; uma fixture nova interceptava consulta geral de progresso e foi restringida ao endpoint da aula; a falha real de paginação acima foi corrigida. A validação final foi repetida integralmente, sem desativar verificações.
- Nenhum teste backend ou de carga foi executado nesta etapa, pois não houve alteração de backend ou banco. Não houve migração.


A medição intermediária está preservada em `ops/performance-after-initial.json`: revelou que as ações rápidas inseridas acima da apresentação empresarial ainda causavam CLS elevado. O painel agora monta a apresentação e as ações após obter a empresa, utilizando o estado de carregamento compartilhado enquanto aguarda. Os números finais devem ser consultados em `performance-after.json`.

## Antes e depois — medianas por cenário

LCP e INP em milissegundos, CLS sem unidade; transferência em KiB, incluindo interações do roteiro. A variação das três amostras está nos JSONs.

| Cenário | LCP antes → depois | INP antes → depois | CLS antes → depois | Requisições | KiB transferidos |
|---|---:|---:|---:|---:|---:|
| homepage 320px | 1916 → 1968 | 96 → 80 | 0.0896 → 0.0000 | 12 → 7 | 184.2 → 175.5 |
| homepage 375px | 1860 → 1944 | 88 → 88 | 0.0682 → 0.0000 | 12 → 7 | 184.2 → 175.5 |
| homepage 768px | 1896 → 1968 | 88 → 88 | 0.0068 → 0.0001 | 12 → 7 | 184.2 → 175.5 |
| homepage 1024px | 1904 → 1968 | 96 → 96 | 0.0068 → 0.0000 | 12 → 7 | 184.2 → 175.5 |
| homepage 1440px | 1932 → 1988 | 104 → 104 | 0.0339 → 0.0000 | 12 → 7 | 184.2 → 175.5 |
| candidate 375px | 2524 → 2720 | 64 → 64 | 0.0422 → 0.0116 | 28 → 23 | 212.0 → 203.1 |
| candidate 1440px | 2512 → 2736 | 80 → 80 | 0.1171 → 0.0000 | 28 → 23 | 212.0 → 203.1 |
| company 375px | 2448 → 2764 | 168 → 168 | 0.7037 → 0.0000 | 30 → 25 | 228.9 → 213.8 |
| company 1440px | 2552 → 2852 | 208 → 184 | 0.3307 → 0.0000 | 30 → 25 | 228.9 → 213.8 |
| capacita 375px | 2004 → 2048 | 56 → 48 | 0.1025 → 0.0767 | 38 → 30 | 240.6 → 223.0 |
| capacita 1440px | 2000 → 2136 | 72 → 64 | 0.0497 → 0.0439 | 37 → 29 | 240.6 → 222.8 |

### Interpretação

As medianas de INP e CLS ficaram dentro das metas em todos os cenários. Homepage e Capacita também ficaram dentro da meta de LCP. Candidato e empresa continuam acima de 2500ms: a meta de carregamento não está concluída. Houve redução de requisições e transferência, e melhora de estabilidade visual; não houve melhora de LCP nesta rodada. Redução de bytes não significa melhora de todos os tempos.

A estabilização de Empresas removeu a apresentação intermediária que recebia ações acima dela: CLS caiu a zero, mas o conteúdo principal aguarda os dados e o LCP aumentou. A próxima evolução deve reduzir o caminho crítico dos módulos e consultas autenticadas, preservando a validação de sessão e permissões. Medir a cascata real de API em homologação antes de modificar o backend ou introduzir cache privado.

Não foram observadas requisições do mapa, worker PDF ou MP4 nos cenários iniciais do instrumento. A contagem é limitada aos padrões documentados no script; testes de navegação complementam a verificação de vídeo sob demanda.

### Próximos passos priorizados

1. Reduzir o LCP autenticado: analisar paralelismo seguro de carregamento de módulos e dados de perfil/empresa, sem expor dados antes da autorização. Repetir a medição após cada mudança.
2. Aplicar compressão e cache de arquivos estáticos na hospedagem; coletar métricas de campo com política de privacidade adequada.
3. Validar com NVDA/VoiceOver, aparelhos físicos e usuários em conexão limitada; revisar PDFs, legendas e transcrições reais.
4. Preparar distribuição de mídia e qualidades adaptativas somente após definir armazenamento, transcodificação e custos. Validar capacidade do backend com carga e monitoramento separados.

### Tamanho dos módulos JavaScript

| Módulo | Antes (bytes) | Depois (bytes) | Gzip antes/depois |
|---|---:|---:|---:|
| Entrada principal | 285644 | 279012 | 85269 / 83733 |
| CompanyPortal | 100614 | 70745 | 22369 / 16355 |
| CapacitaPage | 86636 | 71427 | 21788 / 18451 |

A divisão transfere código para arquivos sob demanda; não representa eliminação de funcionalidades nem diminuição equivalente do total de todos os arquivos. A política de cache de produção depende da hospedagem.
