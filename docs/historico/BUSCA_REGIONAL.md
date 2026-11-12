# Busca regional e profissional — Trampolim

Entrega local de 4 de novembro de 2026, compatível com a etapa territorial e a homepage anteriores.

## Funcionalidades

- Município por código IBGE, estados derivados do catálogo nacional e regiões configuráveis, sem limitar a plataforma a Irecê. A homepage sugere o município de referência configurado; o catálogo inicia na região configurada ou na preferência pública salva.
- Modalidades presencial, híbrida, remota e opções existentes preservadas.
- Cargo/palavra-chave, área, empresa/plataforma, contratação, estágio, Aprendiz/Jovem Aprendiz quando informado, escolaridade, experiência, remuneração e data de publicação.
- Homepage com filtros combinados, busca pública de empregos/estágios ou cursos publicados por empresas, limpeza, ampliação para todas as localidades e preservação do rascunho na sessão.
- Catálogos autenticados de empregos e cursos preservados. Novos filtros no servidor atuam antes da paginação; datas desconhecidas e requisitos não cadastrados não são inventados.
- Setores Comércio, Serviços, Agronegócio, Saúde, Educação, Administração, Tecnologia, Logística e Indústria. Correspondências explícitas com os nomes já armazenados, sem inferir setor a partir do texto livre e sem ordenar setores por suposta demanda local. A taxonomia e a lógica do diagnóstico não foram alteradas.
- Limpeza e chips dos filtros no catálogo. Parâmetros profissionais e de localização persistem no navegador; rascunhos de empregos e cursos são separados. Não se persistem coordenadas.

## Dados disponíveis e limites importantes

1. Escolaridade e experiência em meses utilizam company_radar_requirements.definition, no banco Empresas. Escolaridade selecionada significa aceitar requisito mínimo menor ou igual ao nível informado; experiência significa requisito em meses menor ou igual ao disponível. Valores ausentes não são tratados como zero. Não há equivalência automática com licenças ou formação específica.
2. O catálogo administrado não possui escolaridade nem experiência em meses estruturadas: esses filtros não retornam anúncios dessa fonte. O nível de experiência textual existente permanece disponível no catálogo; nas empresas somente “Sem experiência” pode ser comprovado pelo campo numérico igual a zero. Júnior/Pleno/Sênior não são inferidos de meses.
3. Raio somente no catálogo com latitude/longitude cadastradas. Autorização explícita pelo botão, referência arredondada a duas casas decimais, uso apenas na consulta e sem salvamento nos filtros. Distância em linha reta aproximada, não deslocamento rodoviário. A seleção territorial continua combinada ao raio; para pesquisar além dela, amplie a localidade. Sem coordenadas não há estimativa. Anúncios empresariais não participam quando raio ou acessibilidade estiver ativo e a interface informa o motivo.
4. Remuneração: valores ou intervalos explícitos em reais (ex.: 2500; R$ 1.500,00 a R$ 2.000,00). Interseção da faixa anunciada com a faixa pesquisada; “a combinar”, formatos ambíguos e intervalos invertidos são excluídos. Não há conversão de remuneração horária/anual nem estimativa salarial.
5. Data de publicação usa catalog_items.published_at ou career_listing_publications.published_at. Não substitui uma data desconhecida pela criação do registro. Registros antigos sem publicação rastreável não participam de filtros de data.
6. Curso público na homepage significa anúncio empresarial publicado; o conteúdo educacional do Capacita continua em sua própria trilha. Nenhuma conta, sessão ou banco foi unificado.
7. A ordenação por nome/recomendação existente atua na página do catálogo; a paginação geral permanece por ID descendente. Não se apresenta uma contagem total fictícia.

## APIs preservadas e extensão aditiva

Endpoints permanecem:

- GET /api/public/geography/settings — acrescenta estados, derivados dos municípios oficiais.
- GET /api/public/geography/municipalities — pesquisa limitada existente.
- GET /api/public/companies/regional-opportunities — novos parâmetros opcionais; kind=job (padrão) ou course.
- GET /api/opportunities/{job|course}/regional — catálogo autenticado.

Além dos parâmetros anteriores: state, contractType, experienceLevel, educationLevel (1–5), maxExperienceMonths (0–600), minimumSalary, maximumSalary, publishedDays (1–365), workSchedule, provider. No catálogo: accessibleOnly e o conjunto radiusKm/latitude/longitude (raio entre 1 e 500 km). Parâmetros inválidos retornam 400; consultas SQL parametrizadas. Região, município e estado não concedem permissões.

Publicação, aprovação empresarial, validade, encerramento e exigência de pagamento de estágio continuam nas regras existentes. O tipo course reutiliza a condição pública de publicação, alterando somente o tipo de anúncio. Nenhuma informação privada adicional foi adicionada ao contrato de resposta.

## Desempenho e privacidade

- Debounce de 300 ms para consultas reativas; homepage só pesquisa ao enviar.
- Páginas limitadas (máximo 50 na API; 20 por página na interface) e cursor reiniciado ao mudar filtros.
- AbortController cancela consultas antigas, inclusive no catálogo autenticado; resultados antigos não sobrescrevem a pesquisa atual.
- Cache de consultas públicas em memória: até 40 entradas e validade de 30 segundos, com cópias de resposta, sem tokens ou perfis. Não há cache compartilhado de dados privados. Mudanças públicas podem levar até 30 segundos para aparecer nesse cache.
- Busca de municípios também reutiliza o cache público. Erros não são armazenados e podem ser repetidos pelo usuário.
- Reutilizados índices territoriais e de publicação existentes. O cálculo de raio faz pré-seleção por latitude antes da distância; filtros por remuneração textual e aliases podem exigir varredura dos anúncios elegíveis. Não foi realizada certificação de carga para milhares de usuários nesta entrega; medições em produção devem orientar índices adicionais/normalização futura.
- Bancos independentes e migrações existentes preservados; nenhuma nova tabela, migração ou credencial é necessária.

## Referências oficiais e expansão

Códigos e catálogo municipal: [API de Localidades do IBGE](https://servicodados.ibge.gov.br/api/docs/localidades), fotografia local documentada na etapa anterior. Região inicial: [Divisão Territorial da Bahia](https://www.ba.gov.br/cultura/314/divisao-territorial-da-bahia), consultada nesta etapa em 04/11/2026. O território oficial contempla os 18 municípios solicitados e mais Ipupiara e Itaguaçu da Bahia. A configuração mantém os 20, sem impedir buscas nacionais.

Novas regiões podem ser configuradas pelo arquivo GEOGRAPHY_REGIONS já existente. Estados derivam dos dados oficiais. Não há lista fixa de empresas, vagas ou cursos nem ranking regional fabricado.

| Município | UF | Código IBGE |
|---|---|---|
| América Dourada | BA | 2901155 |
| Barra do Mendes | BA | 2903003 |
| Barro Alto | BA | 2903235 |
| Cafarnaum | BA | 2905305 |
| Canarana | BA | 2906204 |
| Central | BA | 2907608 |
| Gentio do Ouro | BA | 2911303 |
| Ibipeba | BA | 2912400 |
| Ibititá | BA | 2913101 |
| Ipupiara | BA | 2914109 |
| Irecê | BA | 2914604 |
| Itaguaçu da Bahia | BA | 2915353 |
| João Dourado | BA | 2918357 |
| Jussara | BA | 2918506 |
| Lapão | BA | 2919157 |
| Mulungu do Morro | BA | 2922052 |
| Presidente Dutra | BA | 2925600 |
| Uibaí | BA | 2932408 |
| São Gabriel | BA | 2929255 |
| Xique-Xique | BA | 2933604 |

## Diagnóstico e busca expressa

A implementação do diagnóstico, seus resultados e a regra de pontuação não foram modificados. Uma busca explícita por cargo ou área no catálogo não descarta anúncios apenas por terem pontuação zero nesse diagnóstico; as recomendações e a ordenação por afinidade continuam utilizando os mecanismos existentes. Isso permite explorar outros setores voluntariamente.

## Arquivos desta entrega

Backend (pacote geography): RegionalQuery.java, SearchRefinements.java (novo), RegionalCompanyController.java, RegionalCatalogController.java e TerritorialCatalog.java. Teste ampliado: TerritorialHttpPostgresTest.java.

Frontend: App.tsx; features/account/CatalogPage.tsx e CompanyOpportunities.tsx; features/geography/territory.ts, TerritoryProvider.tsx, TerritorySelector.tsx, MunicipalitySearch.tsx e HomepageSearch.tsx. Novos componentes/utilitários: SearchRefinementFields.tsx, searchOptions.ts e publicSearch.ts.

Testes: e2e/search-regional.spec.ts (novo, três cenários), tests/publicSearch.test.mjs (novo), e2e/territory-journey.spec.ts (localizador das referências atualizado para continuar verificando a ordem correta após a inclusão dos novos filtros). Documentação: BUSCA_REGIONAL.md e link no README.md.

## Validação executada

- Backend completo: 146 testes, 0 falhas, 0 erros e 0 ignorados. Inclui PostgreSQL real, esquemas isolados, filtros combinados antes do cursor, estado, salário com intervalo, escolaridade/experiência, publicação, curso público, validação de parâmetros e raio com coordenadas conhecidas.
- Navegação completa: 85 cenários aprovados. Após ajustes finais dos rótulos de cursos e apresentação, os 10 cenários diretamente afetados de homepage, busca e território foram reexecutados e aprovados.
- Frontend unitário: oito testes aprovados, incluindo cache público, expiração, limite de entradas, cópia de resposta e cancelamento.
- Build TypeScript/Vite e lint aprovados, sem advertências. git diff --check sem problemas.
- Responsividade: 390/1440 pixels nos novos filtros, 360/768/1440 na homepage e os demais cenários mobile existentes. Axe sem violações nas regras WCAG A/AA verificadas nos novos cenários; captura mobile inspecionada visualmente. Não é certificação integral nem teste com leitor de tela físico.
- As respostas sintéticas dos testes de navegador são exclusivamente fixtures de teste. Os testes de API exercitam bancos PostgreSQL locais, não produção.

## Execução e pendências

Reinicie o backend com a versão atual e atualize o frontend. Não há novas variáveis obrigatórias, credenciais ou migrações nesta entrega. O backend deve estar atualizado para aplicar os novos parâmetros opcionais. As migrações territoriais V18 (Oportunidades) e V15 (Empresas) já pertencem à etapa anterior; Capacita permanece independente.

Não foi realizada implantação nem publicação no GitHub. Limites reais dependem da cobertura dos dados publicados. Nenhum estágio, curso ou empresa foi inventado para preencher estados vazios. Testes de carga em infraestrutura de produção e ampliação da cobertura de metadados continuam sendo evoluções futuras.
