# Mapa regional de vagas e cursos — Trampolim

Implementação local em 4 de novembro de 2026, integrada ao catálogo existente de Oportunidades.

## Uso

Em Oportunidades, abra Vagas ou Cursos e selecione **Abrir mapa regional**. A lista é a visualização inicial em todos os tamanhos de tela e permanece disponível abaixo do mapa, inclusive quando a base externa ou o suporte gráfico do navegador falham.

- Zoom, movimentação, agrupamento de pontos próximos e ampliação de agrupamentos.
- Seleção por marcador ou controle textual acessível.
- Resumo do anúncio, acesso ao link original e retorno com foco ao cartão correspondente na lista.
- Enquadramento da localidade ou dos anúncios da página.
- Município, estado, região, modalidade e filtros profissionais continuam sendo os já implementados no catálogo. Alterações na busca e troca de página atualizam os pontos.
- Mover o mapa não busca automaticamente novos anúncios nem muda os filtros; isso evita consultas redundantes e uma falsa impressão de cobertura completa.
- Preferência de câmera salva na sessão, vinculada à área geográfica selecionada. Sem preferência, o território inicial de Irecê é enquadrado pelos limites oficiais, sem um marcador fictício central.

## Origem dos pontos e limites de cobertura

Os pontos vêm exclusivamente de latitude/longitude cadastradas nos anúncios do catálogo administrado. Não há geocodificação automática do texto do município, uso da sede da empresa como endereço da vaga ou conversão do limite municipal em coordenada de oportunidade.

Entram no mapa: vagas com coordenadas numéricas válidas e cursos explicitamente presenciais com coordenadas válidas. Remotos/online, cursos de outras modalidades, registros inativos, vencidos, não aprovados quando identificados e registros sem coordenadas ficam fora dos pontos. A projeção utilizada é Mercator, com limite de latitude aproximado de ±85,05°, suficiente para o território brasileiro.

A listagem original, seus favoritos, acompanhamento de candidaturas, denúncias, links e paginação foram preservados. Anúncios empresariais ainda não oferecem coordenadas no contrato atual e continuam sendo consultados pela lista. Sob filtro de distância, registros sem coordenadas não participam dos resultados e a interface informa a limitação. Não foi criada uma migração empresarial apenas para preencher o mapa.

O mapa representa a página carregada, não todo o estoque regional. O endpoint existente utiliza páginas de 20 anúncios na interface, com limite máximo de 50 na API. O adaptador do mapa também limita a 50 pontos e remove IDs duplicados. Não é possível interpretar ausência de marcador como ausência de oportunidades na região.

## Distância e cursos presenciais

O controle de distância foi reutilizado em vagas e cursos por meio de LocationRadiusFields. O usuário pode escolher o município manualmente sem usar GPS. Somente o botão de autorização da referência aproximada solicita acesso ao aparelho; abrir o mapa não faz essa solicitação.

Quando autorizado para o raio, o ponto de referência é arredondado a duas casas decimais, enviado apenas à consulta existente e não salvo nos filtros. Não é adicionado como marcador. O cálculo continua no backend existente antes da paginação e representa distância em linha reta, não percurso rodoviário. Não se inventa distância onde faltam coordenadas.

O formulário administrativo já possuía latitude/longitude no modelo e no contrato do backend. Os campos agora também ficam disponíveis para cursos, com orientação para cadastrar somente o local real confirmado do curso presencial. Revisão e aprovação existentes continuam obrigatórias.

## Tecnologia, carregamento e desempenho

- MapLibre GL JS 6.16.0, com clustering GeoJSON no worker e camadas gráficas; não são criados milhares de elementos DOM de marcadores.
- Importação dinâmica do renderizador, CSS e worker. Nenhum pedido à base cartográfica ou ao renderizador é feito na visualização inicial de lista, conforme teste de navegador.
- Um worker por mapa; destruição da instância ao fechar e atualização do source quando mudam os anúncios.
- Paginação, debounce, cancelamento, filtros e índices geográficos existentes mantidos. Não há endpoint que retorne milhares de anúncios para o mapa nem uma nova consulta por movimento da câmera.
- Limites municipais obtidos sob demanda são mantidos em cache de memória limitado a 40 áreas. Geometrias completas não são armazenadas como dados de usuários.
- Vite pré-otimiza a dependência no desenvolvimento para evitar recarregamento da página na primeira abertura do mapa.
- O build mantém o aviso de chunk acima de 500 kB para a biblioteca gráfica, sem desabilitar o aviso. O chunk lazy do renderizador mede aproximadamente 1,06 MB minificado / 289 kB gzip, e o worker separado cerca de 519 kB minificado. Esses arquivos são carregados somente após solicitação; a lista permanece como alternativa em conexões lentas.

## Cartografia e expansão geográfica

Base padrão: [OpenFreeMap](https://openfreemap.org/) usando dados do OpenStreetMap. A instância pública informava uso gratuito, sem chave ou cadastro, na consulta desta entrega; não foi contratado plano, suporte ou serviço pago. Os créditos automáticos da base foram preservados.

Os enquadramentos iniciais derivam de [malhas oficiais do IBGE](https://servicodados.ibge.gov.br/api/docs/malhas?versao=3), e não de coordenadas estimadas para vagas. O arquivo officialViewports.json contém fonte, data de consulta, limites de 20 municípios da região inicial, Bahia, Brasil e união regional. Os limites foram calculados como mínimos/máximos das geometrias oficiais. Não constituem endereços ou pontos de oportunidades.

Municípios e estados fora do recorte inicial podem obter limites pela API oficial de malhas, sem tokens e somente após abrir o mapa. O acesso CORS do endpoint municipal foi verificado com São Paulo (HTTP 200 e Access-Control-Allow-Origin: *). Falha ou demora usa os anúncios disponíveis ou o enquadramento nacional e mantém a lista. Regiões configuradas com limites locais conhecidos usam a união desses limites; outras usam o município de referência configurado como enquadramento inicial. O usuário pode enquadrar os resultados reais. Alterações futuras do recorte inicial devem atualizar o snapshot de enquadramento quando necessário, preservando a fonte e a data.

A seleção de municípios e regiões continua seguindo o catálogo oficial e GEOGRAPHY_REGIONS da etapa territorial. Não existe restrição de dados do mapa a Irecê.

## Configuração

Opcional no frontend:

```dotenv
VITE_MAP_STYLE_URL=https://tiles.openfreemap.org/styles/liberty
```

É o padrão, sem chave. Pode ser substituído por um estilo HTTPS ou da própria origem, inclusive infraestrutura própria. O endereço é público no navegador: não incluir segredos. Caso se escolha outra base, revisar os créditos e os termos correspondentes. Reiniciar o Vite ou refazer o build após mudar variáveis do frontend.

Para hospedagens com CSP restritiva, permitir o worker e os assets estáticos locais, as conexões HTTPS da base escolhida (incluindo tiles, sprites e fontes) e, para enquadramentos não incluídos no snapshot, servicodados.ibge.gov.br. A política deve ser definida pelo ambiente de hospedagem; nenhuma política de segurança foi desativada nesta entrega.

A lista funciona sem acesso à base externa ou ao IBGE. O mapa visual requer suporte gráfico do navegador e conexão com a base. Não há garantia de disponibilidade do provedor externo; a configuração permite hospedar a cartografia em infraestrutura própria.

## Privacidade e segurança

- Sem mudanças em autenticação, permissões, rotas privadas, bancos ou migrações.
- Nenhum dado residencial de candidato é utilizado como ponto do mapa.
- O renderizador não recebe token, perfil ou contexto de sessão. O GeoJSON do worker contém apenas ID, tipo e geometria dos anúncios elegíveis.
- A base externa recebe requisições de cartografia e o IP necessário ao tráfego normal, o que é informado antes de abrir o mapa. Não se promete anonimato de rede.
- Sem Authorization em requisições externas; cookies não são enviados entre origens pelo renderizador. A consulta de limites oficiais utiliza credentials: omit.
- Seleção e resumo usam texto React, sem HTML de popup montado a partir de campos do anúncio. Novos links aceitam somente HTTP/HTTPS.
- A câmera salva é uma preferência pública de navegação no mapa, não uma sessão compartilhada nem um histórico de localização GPS.

## Arquivos

Criados:

- frontend/src/features/maps/mapModel.ts
- frontend/src/features/maps/mapViewports.ts
- frontend/src/features/maps/officialViewports.json
- frontend/src/features/maps/OpportunityMapView.tsx
- frontend/src/features/maps/RegionalMap.tsx
- frontend/src/features/maps/regionalMap.css
- frontend/src/features/geography/LocationRadiusFields.tsx
- frontend/e2e/map-journey.spec.ts
- frontend/tests/mapModel.test.mjs
- ops/map-live-validation-2026-11-04.png
- MAPA_REGIONAL.md

Modificados:

- frontend/src/features/account/CatalogPage.tsx
- frontend/src/features/account/AdminCatalogPage.tsx
- frontend/package.json e package-lock.json
- frontend/vite.config.ts
- frontend/.env.example
- README.md

Não houve alterações no backend, novas entidades, migrações ou credenciais.

## Testes executados

- Suíte completa de navegação: **102 cenários aprovados**, incluindo os nove novos cenários do mapa.
- Unitários do frontend: **12 aprovados**, incluindo os quatro novos testes de validação, elegibilidade, limites de volume, geometria e segurança de links.
- Build TypeScript/Vite aprovado, com o aviso de tamanho do chunk lazy documentado; lint sem advertências. Dependência instalada com auditoria npm sem vulnerabilidades reportadas naquele momento.
- Testes do mapa executam MapLibre e clustering reais com renderização gráfica de software e base controlada: celular 390 px e desktop 1440 px, vagas/cursos, seleção de ponto, teclado, troca de filtros, retorno/foco à lista, preservação de outra sessão, câmera salva, falta de WebGL e falha da base.
- Testes adicionais verificam raio de cursos com autorização e referência arredondada, além do envio das coordenadas de curso ao contrato administrativo existente.
- Axe sem violações nas regras WCAG A/AA verificadas nos novos cenários; não equivale a certificação integral ou validação com todos os leitores de tela/aparelhos físicos.
- Verificação separada com a base real OpenFreeMap: mapa pronto, 12 respostas HTTP 200 do provedor, sem Authorization externo. Captura da região real inspecionada visualmente e guardada em ops/map-live-validation-2026-11-04.png. Nenhuma vaga fictícia foi cadastrada para essa verificação.
- Não foi repetida a suíte do backend, pois o backend não foi modificado. Os testes de navegador utilizam respostas controladas da aplicação e não representam teste de carga de produção.

Comandos na pasta frontend: npm run build; npm run lint; npm test; npm run test:e2e -- --workers=2.

## Pendências e limites reais

Para ampliar a cobertura de pontos, responsáveis autorizados precisam cadastrar coordenadas reais de locais de trabalho e cursos presenciais. Coordenadas empresariais e cursos do Capacita sem local físico não foram inventados nem adicionados automaticamente. Uma futura extensão do contrato empresarial poderá incluir coordenadas confirmadas do local da vaga, mantendo autorização e isolamento próprios.

Não foram implementadas busca automática por viewport, geocodificação de endereços residenciais, download offline de tiles, certificado de carga ou contratação de infraestrutura. Não houve implantação em produção nem publicação no GitHub. Todas as alterações locais anteriores foram preservadas.

Referências técnicas: [MapLibre GL JS](https://maplibre.org/maplibre-gl-js/docs/), [clustering](https://maplibre.org/maplibre-gl-js/docs/examples/create-and-style-clusters/) e [integração OpenFreeMap](https://openfreemap.org/quick_start/).
