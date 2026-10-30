# Trampolim Capacita — acessibilidade e validação final das cinco etapas

Data: 29/10/2026. Fonte de verdade: projeto local, branch `main`. As alterações locais anteriores foram preservadas. Não houve implantação em produção, mudança em serviços externos, commit ou push nesta etapa.

## Resultado das cinco etapas

| Etapa | Funcionalidade operacional |
| --- | --- |
| 1 — Aprendizagem | Curso → Disciplina → Módulo → Videoaula; papéis educacionais; vídeos de 180–300 segundos validados no servidor; reprodução privada e progresso confiável |
| 2 — Comunicação | Dúvidas privadas por texto/áudio/vídeo, gravação opcional, respostas individuais e em lote com confirmação/idempotência, histórico e notificações SSE persistidas |
| 3 — Biblioteca | PDFs privados validados, versões, vínculos a várias aulas, editor simples, leitor com páginas/zoom/texto selecionável e alternativa textual |
| 4 — Avaliação | Mini provas objetivas, correção exclusivamente no backend, tentativas preservadas, desempenho e conquistas verificadas com compartilhamento consentido |
| 5 — Inclusão e auditoria | Preferências persistentes, melhorias de foco/contraste/mídia, canal privado de apoio, verificação administrativa de experiência inclusiva, prazo adicional autorizado e validação integrada |

O conjunto está conectado ao backend e ao PostgreSQL do Capacita. Os relatórios das [videoaulas](CAPACITA_VIDEOAULAS_PARTE1.md), [mural](CAPACITA_MURAL_PARTE2.md), [biblioteca](CAPACITA_BIBLIOTECA_PARTE3.md) e [avaliações](CAPACITA_AVALIACOES_PARTE4.md) detalham as respectivas APIs e critérios. Este documento consolida a operação e as evidências finais; não certifica conformidade integral WCAG ou LGPD.

## Implementações de acessibilidade e inclusão

### Preferências que efetivamente alteram a experiência

“Acessibilidade e preferências” aparece no Capacita, incluindo login, cadastro e verificação compartilhada. Permite fonte de 100/112/125/150%, alto contraste, redução de animações, espaçamento ampliado, velocidade preferida, legendas quando disponíveis e início dos vídeos sem som. As preferências de vídeo são aplicadas à videoaula e ao vídeo de mensagens do mural. Controles nativos permanecem disponíveis.

Visitantes persistem as escolhas neste navegador. Contas autenticadas podem salvá-las no próprio banco Capacita ao alterá-las. A consulta da conta não copia dados de outras plataformas; falhas de conexão mantêm o uso local com mensagem acessível. Nenhum diagnóstico, classificação de deficiência ou informação médica é requerido para os recursos gerais.

A ampliação respeita as unidades relativas e o tamanho padrão do navegador. Ao sair do Capacita, a apresentação de outras áreas é restaurada. Navegação rápida usa foco programático sem modificar a rota. As instruções explicam Tab, Shift+Tab, Enter, Espaço e setas; não são interceptadas letras soltas durante a digitação.

### Teclado, foco e semântica

- Atalhos para conteúdo/formulário mantêm a página correta. Foi corrigida a alteração indevida do hash ao usar o atalho dos conteúdos.
- Janelas de detalhes/vaga recebem foco, mantêm a navegação dentro da janela, tornam o restante inerte, fecham com Escape e devolvem o foco ao controle de origem.
- Ferramentas de estudo recebem foco ao abrir e o devolvem ao fechar. Curso, módulo e resultado de avaliação possuem alvos claros de foco.
- Erros de autenticação e publicação de vídeo recebem foco e indicação `alert`. Resultados e confirmações usam mensagens textuais, sem depender apenas de cor.
- Notificações incluem anúncio de contagem não lida, histórico, estado de conexão e controles com nomes acessíveis.
- Campos tiveram contraste corrigido; textos secundários e o rodapé foram revisados. O alto contraste preserva indicação visual de seleção e foco.
- Controles têm foco visível e alvos ampliados; formulários usam rótulos e agrupamentos semânticos. A prova mantém questões em fieldsets e alternativas nativas de rádio.

### Vídeo, mural e documentos

Videoaulas oferecem controles nativos e botões explícitos de reprodução/pausa e deslocamento de dez segundos, velocidade com nome acessível, posição/duração e progresso textual. Professores já podem fornecer WEBVTT e transcrições; foram acrescentadas orientações sobre falantes, sons e descrições de gráficos/demonstrações. A interface informa quando esses recursos não foram fornecidos.

O mural pode ser utilizado exclusivamente por texto. Câmera e microfone permanecem opcionais; descrições, transcrições e legendas fornecidas pelo autor são aceitas sem fingir revisão automática. Os reprodutores de mensagens receberam identificação acessível. A captura mantém alternativa de upload e liberação dos dispositivos ao sair.

O leitor PDF mantém camada de texto e alternativa textual fora do canvas, navegação, zoom, abertura em visualizador compatível e download autorizado. Professores recebem orientação para títulos, ordem de leitura, texto selecionável, descrições de imagens e contraste. Texto extraído automaticamente é identificado como não verificado; documentos sem texto/estrutura detectada informam a limitação. O editor simples não produz uma certificação PDF/UA.

### Apoio educacional privado e experiência inclusiva dos professores

“Solicitar apoio educacional” permite descrever voluntariamente o apoio desejado. O formulário orienta a não enviar laudos, diagnósticos ou dados de saúde. Exige confirmação do atendimento privado, conserva histórico e permite exclusão. Apenas o autor e a administração educacional podem ler/administrar o pedido; professores não recebem esses textos automaticamente.

A administração responde, acompanha situação e atualiza os pedidos pelo próprio painel. Não há disparo de e-mail de apoio nesta entrega. A limpeza padrão remove pedidos encerrados há 90 dias, configurável e desativável; o usuário pode excluir seu texto antes disso. Esse é um parâmetro operacional, não um prazo legal presumido.

O diretório de professores com experiência inclusiva exibe somente resumo aprovado e data de verificação manual. A administração registra uma referência privada, deve verificar uma fonte real e pode revogar. A própria pessoa não pode validar sua qualificação; outro administrador precisa fazê-lo. Revogação do papel/conta também remove a disponibilidade no diretório. Não foram cadastradas qualificações reais sem comprovação, nem presumidas competências de professores não listados.

### Avaliações inclusivas

As questões continuam sendo corrigidas no servidor e conservam nota mínima, pesos, máximo de tentativas e versão original. Há revisão, feedback textual e salvamento parcial para pausas dentro do prazo.

Professor responsável ou administração pode autorizar um prazo final individual, posterior ao original, para uma avaliação publicada que possua prazo. A autorização é privada, auditada e não exige um diagnóstico no formulário. Não altera questões, nota ou número de tentativas e não concede uma competência por si só. O servidor aplica o prazo efetivo também ao iniciar, retomar, salvar e enviar uma tentativa já aberta, e registra o critério aplicado na correção.

## Evidências e limites de acessibilidade

Referência: [WCAG 2.2 do W3C](https://www.w3.org/TR/WCAG22/). Foram combinados axe-core com tags A/AA de WCAG 2.0/2.1/2.2, navegação por teclado, inspeção da árvore de acessibilidade do navegador, revisão do código e conferência visual das telas móveis. A validação não se limitou a uma pontuação automática.

Foram auditados 16 estados/superfícies: entrada, login, cadastro, alto contraste com fonte/espaçamento ampliados, avaliações, desempenho, biblioteca, mural, apoio, formulários de curso/prova/PDF do professor, dúvidas recebidas, administração do apoio, reflow e videoaula. Os relatórios finais desses cenários registram **zero violações automáticas detectadas**.

Em 15 desses relatórios há uma categoria `incomplete` de contraste: o analisador não resolve alguns fundos em gradiente e trechos curtos da marca. Não foi convertida em “aprovação automática”. Foi feita revisão visual das cores e cálculo manual conservador usando o extremo do gradiente; razões calculadas incluem texto principal 15,37:1, identificação azul 8,17:1, marca laranja 9,38:1, texto de botão azul 8,62:1 e alto contraste 21:1. A revisão não substitui a avaliação de novos conteúdos/imagens.

Testes adicionais verificam ampliação de texto a 200%, reflow a 320 px, preferências a 150% com espaçamento em tela de 390 px, retenção/restauração de foco, Escape/Tab em janelas, alternativas por teclado, velocidade/mudo/legendas aplicados ao elemento de vídeo e presença da transcrição. O leitor de PDF, os testes da prova e o fluxo real também exercitam controles por teclado.

**Não houve homologação com fala de NVDA, JAWS ou VoiceOver, nem sessões com usuários de tecnologias assistivas.** NVDA não estava disponível no ambiente consultado. A árvore acessível e a execução por teclado foram verificadas, mas não equivalem a testar esses leitores reais. Essa homologação e a revisão dos conteúdos publicados permanecem necessárias antes de declarar conformidade integral AA.

Os professores precisam fornecer legendas/transcrições/descrições adequadas e versões acessíveis dos materiais. Não se declara que todos os vídeos ou PDFs existentes possuem esses recursos. Referências para autoria: [mídias acessíveis](https://www.w3.org/WAI/media/av/), [legendas](https://www.w3.org/WAI/media/av/captions/) e [transcrições](https://www.w3.org/WAI/media/av/transcripts/).

## Auditoria de segurança e privacidade

| Item | Verificação/medida |
| --- | --- |
| Autenticação | Reutilização de JWT próprio do Capacita, emissor/audiência/assinatura/validade e versão de sessão; tokens de Empresas/Oportunidades não concedem acesso privado |
| Papéis e recursos | Verificações no backend para dono de curso, professor, administração, autor de dúvida, tentativa, conquista e pedido de apoio |
| Reprodução | Foi corrigida a possibilidade de emitir ticket com versão nova durante logout concorrente: emissão agora usa a versão do JWT recebido e validade limitada também à expiração dele |
| Progresso | Sessão revogada não pode registrar progresso; saltos e crédito duplicado continuam bloqueados |
| Uploads | Tipos/duração/tamanho reais, limites antes do parsing, cotas, concorrência limitada e processamento privado; não há acesso direto a arquivos por nome do usuário |
| PDFs | Inspeção/regravação PDFBox, rejeição de recursos ativos e processo Java com heap/prazo limitados; não equivale a uma garantia antivírus |
| Notificações | Persistência, SSE autenticado, autorização por destinatário, expiração, revogação, reconexão e recuperação offline |
| Provas e conquistas | Correção no servidor, definições preservadas, gabarito privado, idempotência e concessão somente por aprovação comprovada |
| Apoio | Texto privado, consentimento, limite de envios, exclusão e retenção configurável; sem taxonomia/algoritmo de deficiência |
| Dados pessoais | Preferências, apoio e prazos individuais incluídos na exportação própria; exclusão preserva as relações de cascata/anonimização de auditoria |
| Segredos e logs | Arquivos locais de ambiente/armazenamento ignorados no Git; busca direcionada não encontrou chaves com os padrões consultados em `frontend/src`/`backend/src/main`; artefatos de teste ignorados |
| Dependências | `npm audit --omit=dev`: zero vulnerabilidades reportadas na consulta executada. Não é uma análise completa de dependências Java, pentest ou certificação da cadeia de fornecimento |

A faixa etária existente permanece a partir de 14 anos, declarada pelo usuário, sem comprovação automática. Não foram introduzidos diagnósticos obrigatórios nem compartilhamento de dados de saúde. Operação com adolescentes exige avaliação institucional do melhor interesse, base legal, comunicação apropriada, responsabilidades e políticas de retenção. A implementação técnica não determina, sozinha, conformidade jurídica. Referência oficial: [orientação da ANPD sobre crianças/adolescentes](https://www.gov.br/anpd/pt-br/assuntos/noticias/anpd-divulga-enunciado-sobre-o-tratamento-de-dados-pessoais-de-criancas-e-adolescentes).

## Arquitetura, entidades e migrações

Frontend: React/TypeScript, Vite e Tailwind existentes. Painéis e editor são carregados sob demanda, com fallback de carregamento e erro sem desmontar a videoaula. Preferências e foco são componentes compartilhados dentro do Capacita, sem impor a apresentação às outras áreas.

Backend: Java 21/Spring Boot, autorização existente e acesso JDBC/transações nomeadas do Capacita. As outras plataformas mantêm seus bancos e credenciais separados. Não houve alteração nas migrações já aplicadas.

| Migrações | Estrutura |
| --- | --- |
| V1–V8 | Contas educacionais, catálogo, progresso, recuperação/verificação, privacidade/administração e declaração etária existentes |
| V9–V10 | Papéis, cursos/disciplinas/módulos/aulas, progresso, reprodução, moderação e proteção de envios |
| V11 | Dúvidas, mensagens, lotes, mídias, tickets, notificações e auditoria |
| V12 | Materiais, versões PDF, associações a aulas e auditoria da biblioteca |
| V13 | Avaliações/versionamento, tentativas, critérios, competências e consentimento de compartilhamento |
| V14 — nova nesta etapa | `student_accessibility_preferences`, `learning_support_requests`, `learning_inclusive_teachers`, `learning_assessment_accommodations`, `learning_inclusion_audit` |

Preferências, pedidos e prazos têm vínculo exclusivo com a conta Capacita e regras de exclusão. Referências administrativas de verificação não são retornadas no diretório. A V14 foi aplicada pelo Flyway somente ao Capacita local e testada desde banco/schema vazio. Nenhuma tabela foi acrescentada a Empresas/Oportunidades.

Novas rotas privadas: `/api/capacita/learning/inclusion/preferences`, `/requests` e `/teachers`, com operações apropriadas de consulta/alteração/exclusão; `/api/capacita/learning/assessments/{id}/accommodations` autoriza prazo individual. A permissão é validada no servidor, inclusive quando a interface esconde controles.

## Armazenamento e notificações

`CAPACITA_MEDIA_ROOT` deve apontar para volume privado persistente. Vídeos normalizados, PDFs e mensagens usam UUIDs e referências no banco, não grandes arquivos PostgreSQL. PDFs ficam em `library/`; mídias do mural, em `discussion/`. Não servir esse diretório diretamente por um servidor estático.

FFmpeg/FFprobe verificam duração e normalizam vídeo/áudio. PDFBox executa em JVM separada com 192 MiB de heap e prazo padrão de 45 segundos. Os limites de concorrência evitam processamento ilimitado, mas exigem dimensionar CPU/memória e armazenamento para a carga esperada. Em múltiplas instâncias, os arquivos precisam de volume compartilhado protegido ou adaptador de armazenamento de objetos; não foram introduzidos serviços externos nesta entrega.

SSE usa token no cabeçalho, não na URL; consulta eventos persistidos em intervalos de dois segundos, heartbeat e renovação de canal. Há limites de dois canais por conta e 500 por instância por padrão. Configurar proxy sem buffering SSE e com timeout compatível, HTTPS e CORS restrito aos domínios autorizados. A recuperação offline vem do banco; não depende de e-mail nem de eventos simulados.

As retenções anteriores foram preservadas: conversas encerradas não são removidas automaticamente por padrão; materiais excluídos têm 30 dias por padrão; mídias sem referência entram em limpeza após 24 horas. Pedidos de apoio encerrados têm 90 dias por padrão. Políticas institucionais precisam ser definidas antes de produção, incluindo backups. Prazos operacionais não foram apresentados como obrigações legais.

## Variáveis e execução

As variáveis anteriores estão em `backend/.env.example` e nos relatórios das quatro etapas: conexões independentes, JWTs, `CAPACITA_ADMIN_EMAILS`, `CAPACITA_MEDIA_ROOT`, `CAPACITA_FFMPEG`, `CAPACITA_FFPROBE`, limites de mídia/perguntas/PDF e retenção. Nova variável: `CAPACITA_SUPPORT_CLOSED_RETENTION_DAYS=90`; zero desativa essa limpeza automática.

`TRAMPOLIM_TEST_CACHE_DIR` é usada pelo servidor de desenvolvimento dos testes integrados, para não compartilhar o cache do Vite com outros servidores. Não é uma credencial nem uma configuração exigida em produção. O PDF.js foi incluído na pré-otimização de dependências para evitar recarga tardia do ambiente ao abrir o leitor em desenvolvimento.

Para execução local, usar janelas separadas:

```powershell
Set-Location C:\Users\Windows\trampolim-main\backend
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location C:\Users\Windows\trampolim-main\frontend
npm ci
npm run dev
```

O administrador educacional inicial continua sendo a conta Capacita confirmada configurada nas etapas anteriores. A configuração não cria contas. Conceder os papéis aos professores pelo painel; não copiar credenciais de outra plataforma. SMTP e pagamentos continuam dependendo das configurações externas já documentadas, sem simulação de operações reais.

Para produção: gerar o build estático do frontend e o JAR do backend, configurar os bancos/segredos/CORS/SMTP/HTTPS/volume privado, aplicar Flyway no processo de implantação autorizado e coordenar backup do banco com o volume de mídia. Usar mecanismos nativos de `pg_dump`/`pg_restore` com arquivos e snapshots de armazenamento; testar a restauração em ambiente separado. Não foi executada restauração nem implantação de produção nesta auditoria.

## Execução dos testes

```powershell
Set-Location C:\Users\Windows\trampolim-main\frontend
npm run lint
npm test
npm run build
npm run test:e2e
```

Backend, com os bancos locais ativos e FFmpeg/FFprobe configurados:

```powershell
Set-Location C:\Users\Windows\trampolim-main\backend
.\mvnw.cmd test
```

Para o percurso completo real, primeiro empacotar o backend e depois executar:

```powershell
Set-Location C:\Users\Windows\trampolim-main\backend
.\mvnw.cmd -DskipTests package
Set-Location ..\frontend
npm run test:e2e -- --config playwright.integrated.config.ts
```

Esse teste usa portas 4174/5097, o container local `trampolim-capacita-db` e um schema `cap_live_<UUID>` criado/removido por ele. Usa somente contas e conteúdos sintéticos nesse schema; o diretório temporário é validado antes da limpeza. O backend de teste desativa e-mail apenas nesse processo isolado; **não reproduzir essa configuração em produção**. O armazenamento e os resultados do teste são separados dos dados reais, e os artefatos de integração ficam em `frontend/test-results-integrated/`.

O percurso real cadastra as contas de teste, entra pela interface, cria curso/disciplina/módulo, publica vídeo validado de 180 segundos, gera/publica PDF e avaliação, acompanha a reprodução real a 2× até o backend comprovar 90%, troca três dúvidas com resposta individual/lote e SSE, consulta PDF, corrige a prova e verifica curso concluído, conquista, histórico e relatório do professor. Não fabrica tempo assistido, nota ou aprovação no banco.

## Resultados finais e problemas corrigidos

Resultados finais executados em 29/10/2026:

| Verificação | Resultado |
| --- | --- |
| Backend completo | **96 testes aprovados; zero falhas, erros ou ignorados** |
| Navegação/integração controlada no navegador | **41 testes aprovados**, incluindo seis jornadas de acessibilidade |
| Percurso real navegador → backend → PostgreSQL | **1 teste aprovado**, incluindo reprodução real e conclusão da videoaula, curso, prova, conquista e histórico |
| Testes unitários do frontend | **7 aprovados** |
| Axe nas 16 superfícies/estados | **Zero violações detectadas**; revisões `incomplete` de contraste registradas e analisadas separadamente |
| Lint | Aprovado, sem avisos na execução final |
| TypeScript/build e pacote Maven | Aprovados |
| npm audit de produção | Zero vulnerabilidades conhecidas reportadas na consulta executada |
| Migração | V14 aplicada com sucesso no Capacita local |
| Limpeza do teste real | Nenhum schema `cap_live_*` permaneceu após a execução |
| git diff --check | Sem erros de whitespace |

Os relatórios JSON e as capturas estão em `C:\Users\Windows\Documents\Codex\2026-10-28\ac\outputs\capacita-parte5`. `resumo-acessibilidade.json` lista resultados por superfície; `contraste-manual.json` registra as premissas/cálculos da revisão de gradiente. Houve revisão visual do desempenho em largura móvel; os números mostrados nas capturas são dados sintéticos de teste, não métricas comerciais da Trampolim.

Foram corrigidos contraste de campos/textos, navegação dos atalhos, foco em janelas/ferramentas/erros/resultados, identificação dos controles de vídeo, persistência das preferências, prazo adicional autorizado e a emissão de ticket no logout concorrente. A divisão dos painéis reduziu o bundle inicial observado de aproximadamente 691 kB para 608 kB antes de gzip; permanece o aviso de chunk acima de 500 kB. Não foi medido LCP de produção nem capacidade para milhares de usuários.

Na infraestrutura de testes, resultados e cache do Vite foram isolados para evitar colisão de artefatos e recargas entre servidores de desenvolvimento. O percurso real foi repetido até validar a conclusão com dados isolados e encerramento correto. Nenhum teste ou regra de segurança foi desativado para obter aprovação.

## Arquivos criados e modificados nesta etapa

Novos:

- `backend/src/main/resources/db/migration-capacita/V14__accessibility_support_and_accommodations.sql`
- `backend/src/main/java/com/trampolim/api/education/InclusionController.java`
- `backend/src/test/java/com/trampolim/api/education/InclusionPostgresTest.java`
- `frontend/src/features/learning/AccessibilityPreferences.tsx`
- `frontend/src/features/learning/accessibilityContext.ts`
- `frontend/src/features/learning/accessibleFocus.ts`
- `frontend/src/features/learning/InclusionSupport.tsx`
- `frontend/src/features/learning/AssessmentAccommodation.tsx`
- `frontend/src/features/learning/LearningPanelBoundary.tsx`
- `frontend/e2e/accessibility-journey.spec.ts`
- `frontend/e2e-integrated/capacita-live.spec.ts`
- `frontend/playwright.integrated.config.ts`
- Este relatório.

Modificados:

- `backend/src/main/java/com/trampolim/api/education/AssessmentService.java` e `AssessmentController.java`: prazo individual autorizado.
- `backend/src/main/java/com/trampolim/api/education/LearningPlaybackController.java`: emissão e progresso vinculados à sessão recebida.
- `backend/src/main/java/com/trampolim/api/education/CapacitaController.java`: exportação de preferências/apoio/prazos próprios.
- `backend/src/test/java/com/trampolim/api/education/LearningPostgresTest.java`: sessão real no teste de progresso.
- `backend/src/main/resources/application.properties` e `backend/.env.example`: retenção do apoio.
- `frontend/src/main.tsx`, `src/index.css`, `src/pages/CapacitaPage.tsx` e `src/features/account/AuthScreen.tsx`: preferências, contraste, foco e janelas.
- Em `frontend/src/features/learning/`: `LearningPortal`, `LessonPlayer`, `LessonEditor`, `QuestionBoard`, `QuestionComposer`, `QuestionNotifications`, `LibraryPanel`, `PdfReader`, `AssessmentPanel`, `AssessmentRunner`, `PerformancePanel` e `SharedCompetency`: navegação/mídia/autoria acessível e carregamento sob demanda.
- `frontend/vite.config.ts` e `frontend/.gitignore`: pré-otimização/cache e artefatos isolados.
- `README.md`: referência à documentação final.

## Limitações e próxima evolução

1. Homologar com pessoas que utilizem leitores de tela, navegação alternativa e diferentes dispositivos físicos; preservar uma matriz de regressão por navegador/tecnologia assistiva.
2. Revisar legendas, transcrições, descrições e PDFs de cada conteúdo real antes da publicação acessível. Disponibilidade técnica não garante qualidade de autoria.
3. Definir políticas institucionais para adolescentes, apoio, consentimento, retenção, atendimento e resposta a incidentes; realizar a avaliação jurídica pertinente ao contexto real.
4. Executar teste de carga e restauração, dimensionar banco/processamento/SSE e compartilhar o armazenamento antes de múltiplas instâncias. A execução local não prova capacidade para milhares de acessos simultâneos.
5. Ampliar paginação onde ainda existem limites de listagem das etapas anteriores; avançar no carregamento por rota e observabilidade com dados minimizados.
6. Não estão incluídos certificados formais, proctoring, OCR/revisão automática de acessibilidade, integrações institucionais de credenciais ou importação automática no Oportunidades.

Essas pendências não foram apresentadas como recursos implementados ou testes aprovados. A plataforma recebeu implementação e validação técnica das cinco etapas; conformidade integral, homologação humana e prontidão operacional de produção dependem das atividades acima.
