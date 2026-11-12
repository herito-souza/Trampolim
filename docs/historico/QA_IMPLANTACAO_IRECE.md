# Auditoria de preparação para implantação — Trampolim

Data: 05/11/2026. Fonte de verdade: checkout local em `C:\Users\Windows\trampolim-main`, branch main. A comparação com o upstream configurado apresentou 0 commits de diferença, mas existem várias alterações locais não commitadas de etapas anteriores. Não houve fetch, publicação no GitHub ou implantação nesta auditoria.

## Parecer

**Apto para continuidade de homologação controlada. Liberação pública integral não autorizada tecnicamente até encerrar as pendências operacionais abaixo.** Testes de software não comprovam conformidade jurídica integral com a LGPD, capacidade para milhares de usuários, acessibilidade integral ou segurança absoluta.

## Arquitetura e escopo

React 19, TypeScript 6, Vite 8, Tailwind 4; Java 21/Spring Boot 4.1; PostgreSQL, JPA/JDBC e Flyway. Oportunidades, Empresas e Capacita preservam bancos, contas, JWT, audiências e emissores próprios. Integrações públicas e autorizadas não dão acesso direto aos bancos privados de outra trilha.

Foram preservadas as alterações anteriores e funcionalidades existentes. Os bancos de desenvolvimento foram iniciados via Docker; seus volumes não foram apagados. Testes de integração criam schemas aleatórios e os removem ao concluir. Não houve migração dos dados de usuários, cobrança ou envio de e-mail real.

## Correções comprovadas

1. **Falha de rede na busca:** a interface exibia `Failed to fetch`. O serviço público agora mostra orientação em português, mantém o cancelamento por AbortSignal e permite nova tentativa sem guardar a falha no cache. Testes verificam recuperação e preservação dos filtros.
2. **Webhook e ciclo de publicação:** confirmação repetida podia reabrir vaga encerrada ou pausada. Confirmação agora publica somente vaga aguardando pagamento, não expirada, com prazo de inscrição válido e empresa aprovada. Falha de pagamento preserva o encerramento/pausa em vez de reiniciar o ciclo.
3. **Resposta JSON de pagamentos:** testes revelaram que o cliente HTTP padrão do Spring 7 tentava converter uma resposta para o modelo JsonNode da versão anterior de Jackson. A leitura agora recebe texto e faz conversão explícita com a biblioteca já usada pelo módulo. O corpo do evento usa mapa de dados compatível. Não houve troca de bibliotecas.
4. **Legibilidade em 320 px:** os botões dos cards institucionais dividiam palavras no meio mesmo sem overflow da página. A disposição agora se adapta à largura do card, e o teste verifica que os termos dos rótulos não se quebram entre linhas.
5. **Configuração local do frontend:** arquivos .env locais passam a ser ignorados pelo Git, preservando o exemplo público. Não foi encontrada uma credencial real nessa pasta; a mudança é preventiva e não altera o runtime.
6. **Chaves de exemplo em produção:** além das chaves públicas de desenvolvimento já bloqueadas, a inicialização recusa placeholders REPLACE_ dos templates. O teste existente foi ampliado para as três trilhas, sem imprimir valores na mensagem.
7. **Data da evidência de desempenho:** o instrumento deixa de fixar a data de 04/11 e calcula a data em America/Sao_Paulo.

Os testes de pagamento usam HTTP simulado localmente com assinatura calculada para dados sintéticos e PostgreSQL real. **Não equivalem a homologação com o Mercado Pago.** Nenhuma transação real foi criada.

## Matriz de cenários

| Cenário | Evidência |
|---|---|
| Candidato de Irecê encontra oportunidades locais | dashboard, busca territorial e regional-resilience |
| Morador de Lapão pesquisa Irecê | regional-resilience verifica código 2914604 sem gravar o perfil ou usar sessão Capacita |
| Estudante procura estágio regional | territory-journey e integração territorial PostgreSQL; somente publicação apta aparece |
| Empresa publica/gerencia anúncio | dashboard e testes HTTP corporativos; pagamento real permanece pendente |
| Professor publica conteúdo | learning, library e assessment; upload real de fixture e autorização do docente |
| Aluno assiste pelo celular | player, mídia com Range, retomada e falha/nova tentativa de progresso |
| Ampliar para municípios próximos ou outras regiões | search-regional, territory e catálogo geográfico |
| Buscar trabalho remoto | dashboard e filtros regionais; a busca remota pode retirar a limitação municipal |
| Rede lenta e falhas de API | 350ms, 400kbps/150kbps no roteiro de pesquisa, 503, interrupção de transporte e recuperação |
| Sessões e dados privados | isolamento JWT, autorização por recurso/organização, exportação, exclusão e compartilhamento consentido |

A videoaula também é testada com o navegador alternando offline/online, preservando a URL da reprodução e mostrando/removendo o aviso de conexão. Isso não promete armazenamento de vídeo completo nem conclusão de aula offline.

O navegador usa respostas sintéticas claramente identificadas; os testes HTTP Java exercitam API e PostgreSQL reais com fixtures isolados. Não foi executada uma única jornada completa navegador → infraestrutura de produção → provedor externo. Um timeout inicial ao testar o servidor Vite de desenvolvimento sob 3G levou à separação correta: rede lenta na pesquisa após abrir a interface, e desempenho de abertura avaliado pela compilação de produção documentada separadamente. Não se interpreta o download de módulos do Vite dev como desempenho de produção.

## Segurança e privacidade

- Chaves distintas, issuer/audience/trilha e expiração JWT; validação de versão/revogação e testes de token de uma trilha rejeitado em outra.
- Hash BCrypt; regras de senha, recuperação por trilha e limitação de requisições cobertas pelos testes existentes.
- Autorização no backend para empresa, associação, professor, aluno e recursos privados; não depende apenas da interface.
- Isolamento de banco protegido contra URLs repetidas; migrações independentes e indisponibilidade parcial testadas.
- Uploads possuem limites, autorização e processamento real de mídia/PDF; arquivos ficam fora do PostgreSQL. Isso não equivale a certificação antivírus de todo arquivo.
- Progresso, avaliações e competências são validados pelo backend; testes verificam fraude, duplicidade, permissões e evidências.
- Logs operacionais evitam corpo, query string, credenciais e identidades; testes do filtro e revisão de código. Não é auditoria de todos os logs de uma hospedagem futura.
- Dados pessoais e compartilhamentos são limitados por finalidade e autorização. São controles técnicos relacionados à privacidade; bases legais, avisos, contratos, retenção institucional, atendimento a titulares e regras para menores exigem revisão própria.
- Tokens no navegador continuam sujeitos a comprometimento em caso de XSS. Não houve mudança de armazenamento de sessões nesta etapa. HTTPS, controle de scripts/dependências e revisão de política de conteúdo permanecem necessários.
- Não houve pentest externo, varredura completa de CVEs Java/imagens ou validação de toda a cadeia de fornecimento. Npm audit não substitui essas verificações.

## Expansão regional

O catálogo local contém códigos oficiais de municípios brasileiros e a região inicial vem de `geography.regions`/`GEOGRAPHY_REGIONS`. `defaultRegion` e `referenceMunicipality` são configuráveis. A lista inicial de 20 municípios é recorte territorial, não limite nacional; há seleção por estado, município e todas as localidades. Nomes ambíguos não são automaticamente atribuídos a uma cidade.

Novo teste carrega arquivo temporário com referência São Paulo (3550308) e região sintética de QA, preservando Irecê no catálogo. Demonstra troca da configuração sem alterar código, contas ou bancos. Atualizar o catálogo oficial quando necessário, validar códigos e documentar a origem; isso é atualização de dados, não reconstrução.

Para expansão estadual: estabelecer responsáveis por cada configuração, revisar busca e municípios com usuários locais, manter observabilidade por região sem expor localização residencial, limitar consultas e medir distribuição real de dados. A região de trabalho é a do anúncio, não inferida da sede da empresa.

## Limpeza e manutenção

A verificação TypeScript usa noUnusedLocals/noUnusedParameters e o lint foi executado. Não houve evidência suficiente para excluir componentes, bibliotecas ou estilos globais com segurança; nada foi removido por mera aparência de desuso. Overrides de tema/acessibilidade não foram tratados como duplicação acidental.

Foi identificada consulta repetida ao dashboard empresarial entre CompanyPortal e CompanyDashboard. Está registrada como melhoria posterior: reaproveitar a resposta por sessão com ciclo de atualização explícito, sem cache global de permissões. Mensagens de transporte em outros fluxos autenticados ainda podem ser uniformizadas; a correção desta etapa trata a busca pública. Contratos legados de listagem sem paginação, relatórios agregados e envio/processamento síncrono de arquivos merecem atenção em crescimento. Os testes de orçamento de consultas e índices não substituem carga de produção.

## Impedimentos e pendências de liberação

| Prioridade | Pendência | Critério de saída |
|---|---|---|
| P0 — liberação pública | HTTPS/domínio, segredos exclusivos, origens CORS e bancos privados na infraestrutura final | configuração de produção revisada e smoke tests autenticados; não usar exemplos de desenvolvimento |
| P0 — continuidade dos dados | backup de três bancos e mídia, restauração, RPO/RTO e responsáveis | ensaio em destino isolado com integridade e recuperação comprovadas |
| P0 para fluxo completo de contas | SMTP e confirmação/recuperação por e-mail | homologar envio/recebimento, expiração e redefinição nas três trilhas |
| P0 para estágios pagos | credenciais Mercado Pago, URL HTTPS do webhook e homologação | pagamento sandbox, repetição, falha, conciliação e publicação testados; não simular pagamento real |
| P1 | armazenamento privado e capacidade de distribuição/processamento | testar mídia, Range, cotas, retomada e falhas no ambiente final |
| P1 | observabilidade, alertas, retenção e rotina operacional | monitoramento ativo, responsáveis e simulação de falha |
| P1 | segurança de dependências Java/imagens e teste externo | avaliar achados e corrigir impedimentos antes de abrir ao público |
| P1 | acessibilidade e usabilidade com pessoas reais | NVDA/VoiceOver, aparelhos físicos, conectividade regional, documentos e legendas reais |
| P1 | tempo de carregamento autenticado | reduzir LCP de candidato/empresa e comprovar métricas de campo; medições locais anteriores não bastam |

Um piloto fechado pode usar fixtures e integrações explicitamente desativadas, sem fingir pagamentos ou e-mails. A produção integral depende dos critérios acima. Nenhuma implantação foi executada nesta etapa.

## Arquivos desta auditoria

- frontend/src/features/geography/publicSearch.ts
- frontend/.gitignore
- frontend/tests/publicSearch.test.mjs
- frontend/e2e/regional-resilience.spec.ts
- frontend/src/pages/EcosystemHome.tsx
- frontend/e2e/homepage-regional.spec.ts
- frontend/e2e/learning-journey.spec.ts
- frontend/scripts/measure-performance.mjs
- backend/src/main/java/com/trampolim/api/company/CompanyPaymentController.java
- backend/src/test/java/com/trampolim/api/company/CompanyPaymentWebhookPostgresTest.java
- backend/src/main/java/com/trampolim/api/security/TrailTokenService.java
- backend/src/test/java/com/trampolim/api/security/TrailTokenServiceTest.java
- backend/src/test/java/com/trampolim/api/geography/TerritorialCatalogTest.java
- Este relatório, ops/GUIA_HOMOLOGACAO_IRECE.md, README.md e evidências em ops/qa-2026-11-05/.

Não há nova tabela ou migração nesta auditoria. Alterações locais de etapas anteriores permanecem preservadas e não são atribuídas a esta etapa.

## Resultados de desempenho — medição atual de 05/11

33 amostras (três por cenário), compilação de produção local com gzip, cache desativado, latência de 150ms, download 1,6Mbps, upload 0,75Mbps e CPU do navegador 4x. APIs sintéticas com atraso de 120ms. Executado sem os testes Java/e2e concorrentes. LCP/INP em milissegundos. São interações roteirizadas de laboratório, não CrUX, p75 de usuários reais ou medição do backend real.

| Cenário | LCP mediana (mín.–máx.) | INP mediana | CLS mediana | Requisições | KiB transferidos |
|---|---:|---:|---:|---:|---:|
| homepage 320px | 1992 (1984–2028) | 88 | 0.0000 | 7 | 175.5 |
| homepage 375px | 1980 (1968–2144) | 88 | 0.0000 | 7 | 175.5 |
| homepage 768px | 2076 (2020–2176) | 96 | 0.0001 | 7 | 175.5 |
| homepage 1024px | 2064 (2032–2100) | 104 | 0.0000 | 7 | 175.5 |
| homepage 1440px | 2096 (2032–2252) | 104 | 0.0000 | 7 | 175.5 |
| candidate 375px | 2752 (2720–2836) | 64 | 0.0076 | 23 | 203.2 |
| candidate 1440px | 2816 (2764–2832) | 80 | 0.0000 | 23 | 203.2 |
| company 375px | 2792 (2776–2860) | 176 | 0.0000 | 25 | 213.8 |
| company 1440px | 2916 (2876–3464) | 200 | 0.0000 | 25 | 213.8 |
| capacita 375px | 2084 (2000–2096) | 56 | 0.0767 | 30 | 223.0 |
| capacita 1440px | 2156 (2140–2176) | 72 | 0.0439 | 29 | 222.9 |

As medianas de INP e CLS atenderam às metas nos cenários do roteiro. Homepage e Capacita atenderam também ao LCP ≤2500ms. Candidato e empresa ficaram acima dessa meta, portanto o objetivo de carregamento autenticado continua pendente. Não foi observada requisição antecipada de MP4, worker PDF ou mapa nas entradas avaliadas. Mapas e mídia continuam exigindo ensaio específico quando abertos.

Evidência bruta: `ops/performance-qa-2026-11-05.json`; resumo em `ops/qa-2026-11-05/performance-summary.json`. Resultados de 04/11 foram preservados e não apresentados como nova medição. A próxima evolução deve analisar a cascata de módulos e consultas autenticadas sem cache global de permissões ou flexibilização de segurança.

Nos testes PostgreSQL desta auditoria, o curso sintético de 20 disciplinas, 200 módulos, 1.000 aulas publicadas e 200 rascunhos manteve três consultas de estrutura, comparadas a 221 no caminho antigo reproduzido. As medianas locais foram 336.698µs no caminho atual e 18.323.042µs no antigo; o host executava outros testes, usando conexões novas por consulta. Não equivalem à latência de produção com pool nem demonstram capacidade concorrente.

A migração de índice preservou 20.000 cursos sintéticos e o plano usou idx_learning_courses_public_feed (consulta local: Execution Time 0,122ms). A agregação de talentos foi ensaiada com 10.000 candidatos sintéticos e página limitada a 500; ver o plano preservado. Não foi executado novo ensaio de milhares de usuários simultâneos, resistência prolongada ou distribuição de vídeo em escala.


## Resultado final dos testes

| Verificação | Resultado executado |
|---|---|
| Oxlint | Aprovado, sem erros |
| TypeScript | Aprovado; tsc -b explícito e novamente no build |
| Unitários frontend | 13 aprovados |
| E2E frontend final isolado | 121 aprovados, dois workers, 5,2 minutos |
| Backend completo — Maven verify | 151 aprovados, zero falhas/erros/ignorados, JAR produzido |
| Após o reforço final de placeholders JWT | 25 testes de segurança/autenticação/isolamento/senhas repetidos e aprovados; verify e empacotamento aprovados |
| Migrações/integração PostgreSQL | Executadas na suíte completa, incluindo instalação vazia, atualização de fixtures e três históricos independentes |
| Dependências JavaScript | npm audit: zero vulnerabilidades conhecidas reportadas na execução |
| Compose dos bancos | Configuração validada, sem expor valores interpolados |
| Conjunto local de duas APIs/proxy | Pendente: ops/.env.local não existe; não houve validação da configuração final nem subida do conjunto |
| Proxy Nginx | nginx -t aprovado em contêiner descartável sem rede; não é implantação completa |
| Higiene do diff | git diff --check sem erros de whitespace; aviso de conversão CRLF existente nos arquivos npm |

A suíte completa backend foi executada antes do último endurecimento de placeholders; depois dele foram repetidos os 25 testes diretamente relacionados, sem alegar uma segunda execução completa de 151. A suíte final de navegador inclui as três novas jornadas regionais/conectividade e a alternância offline/online no player. Não houve desativação de testes, regras ou asserções.

Uma execução intermediária de 121 cenários, em paralelo com testes Java/SQL pesados, teve 119 aprovações e dois timeouts gráficos de mapa. A repetição integral isolada passou 121/121 com os mesmos limites e verificações. Isso é evidência de sensibilidade do ensaio gráfico aos recursos do host; não prova uma causa universal para todo timeout. O primeiro roteiro 3G sobre Vite dev também excedeu o tempo de abertura, e uma expectativa do seletor foi ajustada ao contrato real (`city`, com código conferido na API), sem alterar a aplicação para satisfazer o teste. Essas execuções não foram apresentadas como aprovadas.

Foram revisadas visualmente capturas de 320px, preservando o antes/depois da correção dos botões. Testes incluem 320, 375, 768, 1024 e 1440px, fluxo por teclado, ampliação de texto e verificações axe nas telas auditadas. Não foram realizados testes com leitores de tela por usuários ou em aparelhos físicos. A configuração do conjunto local foi revisada como código, mas a validação com seu arquivo de ambiente está pendente porque ops/.env.local não existe. A configuração foi revisada em relação ao CI, mas o GitHub Actions remoto não foi executado; a máquina usou Node 24.18.0, enquanto o pipeline declara Node 22.

A conferência por leitura encontrou zero schemas desta execução remanescentes nos três bancos. Os arquivos em ops/qa-2026-11-05 registram suites, resumo, checksum do JAR, planos SQL, metadados npm, verificação limitada de padrões de chaves e capturas sintéticas. A verificação de chaves não cobre todo o histórico Git ou todos os formatos de credencial.

**Decisão técnica:** funcionalidades centrais estão validadas para homologação regional controlada. Não recomendo abrir a operação pública integral enquanto SMTP, pagamento sandbox, infraestrutura HTTPS/segredos, restauração, monitoramento e demais critérios de liberação não forem comprovados no ambiente final. A expansão territorial é suportada por configuração e dados; expansão de volume exige dimensionamento, ensaios de carga e operação próprios.
