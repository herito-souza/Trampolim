# Regionalização — Irecê e região — primeira entrega

Implementação aprovada da base territorial e da consistência de filtros, preservando a arquitetura local e as três autenticações. Iniciada em 30/10/2026 e finalizada/validada nesta retomada de 04/11/2026. Não representa implantação em produção nem publicação automática no GitHub.

## Comportamento entregue

- Referência inicial Irecê/BA, código IBGE 2914604. O recorte padrão usa os vinte municípios do Território de Identidade Irecê, explicitamente descrito na configuração.
- Opções de pesquisar a região, todas as localidades ou um município brasileiro, sem exigir GPS ou alterar o endereço do perfil.
- Opção de incluir oportunidades remotas/online de outras localidades.
- Preferência pública de busca persistida no navegador. Ela não vincula contas, não contém coordenadas e não concede acesso a organizações.
- Catálogo administrado e anúncios de Empresas recebem os mesmos filtros de território, palavra-chave, cidade, área e modalidade. Os filtros avançados exclusivos do catálogo administrado permanecem identificados como tal; eles não são apresentados como filtros universais.
- Catálogos regionais usam consulta no servidor e paginação por cursor: vinte itens por página em Oportunidades; o Capacita mantém o padrão existente de vinte e quatro estágios por lote. O servidor limita requisições a cinquenta itens e não envia o catálogo nacional completo ao navegador.
- No celular, anúncios publicados aparecem antes da exploração de profissões. As referências gerais continuam acessíveis em seção expansível, separadas da contagem de anúncios.
- Busca textual sem distinção entre acentos/maiúsculas: Irece/Irecê e Lapao/Lapão. O campo livre de cidade é uma busca textual; o seletor de município usa o código oficial.
- A seção de preparação profissional do Capacita usa o mesmo registro publicado de estágio de Empresas, com filtro de estágio, prazo, aprovação e pagamento confirmados. O valor comercial de R$ 50,00 não foi alterado.
- Trocar a região reinicia a paginação. Respostas de uma consulta antiga são descartadas e não devem substituir a seleção atual.
- Falha ou espera superior a oito segundos na configuração territorial libera o modo tradicional com mensagem explícita de indisponibilidade da seleção regional. Nesse modo não se afirma que Irecê foi filtrada.

## Dados oficiais e limites da associação legada

O arquivo `backend/src/main/resources/geography/municipalities.json` contém 5.571 registros retornados pela API de localidades do IBGE na coleta de 30/10/2026, com código, nome e UF; inclui a URL da origem e a data da coleta. A aplicação consulta essa referência local, sem chamada ao IBGE durante a navegação. Não foram cadastradas empresas, cursos ou vagas de demonstração na aplicação.

A configuração padrão está em `backend/src/main/resources/geography/regions.json`. As vinte localidades são identificadas por código oficial, conferidas contra a referência municipal. A composição inicial segue a página oficial de divisão territorial da Bahia, sem presumir distância rodoviária ou deslocamento viável.

Registros antigos não são regravados automaticamente. Quando não há código municipal, o filtro aceita correspondência exata normalizada do nome com UF; o nome sem UF só é aceito quando não é ambíguo na referência nacional. Complementos como bairro, múltiplas cidades ou texto “a combinar” não são adivinhados. A UF/sede da empresa não é utilizada como substituto do local da vaga.

Há correção explícita e autorizada do município do anúncio. O texto anterior é preservado para revisão. O resultado público inclui a identificação municipal padronizada para exibição; o código corrigido é a referência da seleção por município. A edição não publica o anúncio, não confirma pagamento e não muda suas demais regras.

As referências gerais de profissões/portais do diagnóstico permanecem identificadas como exploração profissional; não constituem vagas locais confirmadas ou estatísticas de emprego.

## Arquitetura e permissões

A referência geográfica é pública, compartilhada como arquivo de dados e contratos HTTP; nenhuma tabela privada foi compartilhada entre os bancos.

- Oportunidades: migração **V18**, `catalog_items.municipality_code` opcional, função de normalização e índices de consulta.
- Empresas: migração **V15**, `company_listings.municipality_code` opcional, função de normalização e índices de consulta.
- Capacita: **nenhuma migração nova**. A área de estágios consome a API pública de Empresas, mantendo sua autenticação e seu banco independentes.

Nenhuma tabela ou coluna existente foi apagada. Não houve preenchimento automático dos códigos de registros legados. As validações de código municipal são executadas contra a referência oficial no backend; as colunas também têm restrição de formato.

Apenas administrador da plataforma pode atribuir município ao catálogo administrado. Em Empresas, apenas membro ativo e verificado com papel proprietário/recrutador da empresa responsável pode editar a vaga. Alterações empresariais registram auditoria. Tokens de outras trilhas continuam rejeitados nos endpoints privados. Um filtro regional nunca substitui esse controle de acesso.

## APIs

| Operação | Caminho | Acesso |
|---|---|---|
| Configuração territorial | `GET /api/public/geography/settings` | Público |
| Busca limitada de municípios | `GET /api/public/geography/municipalities?q=irece&state=BA&limit=20` | Público |
| Catálogo regional | `GET /api/opportunities/{kind}/regional` | Sessão Oportunidades |
| Anúncios empresariais regionais | `GET /api/public/companies/regional-opportunities` | Público |
| Município do catálogo | `GET/PUT /api/admin/catalog/{id}/municipality` | Administrador |
| Município da vaga empresarial | `GET/PUT /api/company/listings/{id}/municipality` | Proprietário/recrutador autorizado |

Filtros compartilhados dos feeds: `region` ou `municipalityCode` (exclusivos entre si), `includeRemote`, `q`, `city`, `area`, `modality`, `before`, `limit`. A API empresarial também aceita `internshipsOnly=true`. A resposta contém `items`, `nextCursor` e observação sobre cobertura. Endpoints anteriores continuam disponíveis para compatibilidade.

`PUT .../municipality` recebe `{ "municipalityCode": "2914604" }`. `null` remove somente essa associação opcional. Valores desconhecidos, código inválido, região inexistente e páginas excessivas são rejeitados. O navegador não recebe chave interna de integração nem envia token de candidato aos feeds públicos empresariais.

## Configuração e expansão

O padrão é `geography.regions=classpath:geography/regions.json`. Para uma cobertura administrada externamente, configure por exemplo:

```text
GEOGRAPHY_REGIONS=file:C:/Trampolim/config/regioes.json
```

Copie o modelo JSON, mantenha `defaultRegion`, `referenceMunicipality`, `source` e a lista de regiões com `id`, `name`, `description` e `municipalityCodes`. Cada região aceita até seiscentos municípios; é possível cadastrar várias regiões. Todos os códigos devem existir no catálogo local e não podem estar duplicados na mesma região. Configurações inválidas são rejeitadas em vez de produzirem recortes silenciosamente errados.

Reinicie o backend após alterar o arquivo externo. A adição de regiões não exige alteração em componentes ou migração de dados privados. A atualização da referência municipal do IBGE é uma atualização controlada do arquivo de dados, com origem/data e revisão dos códigos; não se depende de uma API externa disponível em cada carregamento.

Inicie os três bancos como no ambiente local existente e reinicie o backend para aplicar V18/V15 nos históricos corretos. Faça backup antes da atualização de ambiente compartilhado. O frontend utiliza as rotas novas quando a configuração territorial está disponível. Em caso de backend anterior/indisponível, apresenta o modo tradicional explicitamente.

Atribuição de municípios:

1. Administração → catálogo → editar publicação → município do local da atividade.
2. Empresas → Desafios e impacto → Resultados de seleção → requisitos complementares → identificador da vaga → município do local da atividade.

A segunda entrada reutiliza o formulário empresarial existente de informações complementares. A configuração de município não altera cobrança, regime de contratação ou disponibilidade da vaga.

## Componentes reutilizados

Identidade visual, estilos de campos/botões, navegação da conta, navegação modular do Capacita, autenticação, permissões, cadastro/publicação, auditoria empresarial e fluxo de candidatura foram mantidos. Novos componentes geográficos reutilizam esses padrões:

- `TerritoryProvider` e contexto público, sem vínculo com sessões.
- `TerritorySelector`, utilizado no catálogo e nos estágios do Capacita.
- `MunicipalitySearch`, reutilizado para busca e atribuição.
- `MunicipalityAssignment`, reutilizado em administração e Empresas.
- Consulta geográfica adiada e controle de respostas antigas.

## Validação

Os resultados finais executados estão em `ops/territory-validation-2026-11-04.json`. A regressão anterior de 30/10/2026 tinha 146 testes de backend e 77 cenários de navegador aprovados. A validação final desta retomada registra separadamente os testes efetivamente executados em 04/11/2026, incluindo os cenários adicionais do Capacita e de indisponibilidade.

Os testes de integração usam PostgreSQL real com schemas isolados e dados sintéticos. As verificações de navegador usam respostas de teste, explicitamente separadas dos dados operacionais. Não se simulam contratações, cobertura econômica ou ofertas reais.

Verificações: referências oficiais, acentos, nomes ambíguos, expansão para outra UF, filtros nas duas fontes, paginação, autorizações, preservação do texto legado, pagamento/publicação/expiração, proteção das sessões, retomada da busca após falha, interface em 390/1440 px, teclado e análise automática de acessibilidade nos escopos definidos.

Não se declara conformidade integral de acessibilidade, homologação em aparelhos físicos ou capacidade para milhares de usuários a partir desses cenários.

### Resultados executados em 04/11/2026

- Backend: **146 testes aprovados**, sem falhas, erros ou testes ignorados. Inclui integração territorial e atualização dos três históricos Flyway com preservação de usuários.
- Frontend: TypeScript/Vite compilados, lint sem avisos e **7 testes unitários aprovados**. Pacote executável do backend atualizado após os testes.
- Navegador: **79 cenários aprovados na suíte completa final**, incluindo quatro cenários territoriais, a apresentação dos anúncios antes das referências gerais, reinício da paginação do Capacita e busca de contingência.
- O teste existente do diagnóstico foi estabilizado para esperar o indicador de cada uma das dez perguntas antes de clicar. Isso respeita a transição da interface, sem alterar a lógica de negócio ou remover verificações.
- A atualização do contexto React eliminou o aviso de recarga de desenvolvimento. A consulta territorial adiada descarta respostas antigas; a preferência pública mantém as credenciais das trilhas intactas.

## Próximas etapas do plano regional

Esta primeira entrega consolida a base territorial e a busca. Continuam como evoluções posteriores:

- Reconciliação humana dos municípios ambíguos e das localidades sem código.
- Ampliação municipal/territorial do observatório: o painel anterior por UF não foi convertido em estatística municipal nesta etapa.
- Preferências de mobilidade intermunicipal no Radar, sem presumir viabilidade de deslocamento pela proximidade.
- Ampliação da taxonomia setorial/diagnóstico legado com dados reais, incluindo educação e agronegócio.
- Otimização adicional de PDF/vídeo para conexões lentas, medições de rede e testes presenciais com participantes da região.
- Padronização geográfica de endereços institucionais e ofertas presenciais fora do catálogo já integrado, quando seus fluxos forem evoluídos.

A regionalização não comprova que o acervo atual cobre todos os municípios ou setores; isso depende de empresas/ofertas reais cadastradas e conferidas.

## Fontes

- [API de localidades do IBGE — documentação](https://servicodados.ibge.gov.br/api/docs/localidades)
- [Origem do catálogo municipal](https://servicodados.ibge.gov.br/api/v1/localidades/municipios?orderBy=nome)
- [Divisão territorial da Bahia](https://www.ba.gov.br/cultura/314/divisao-territorial-da-bahia)

## Arquivos desta etapa

- `README.md`
- `REGIONALIZACAO_IRECE_ETAPA1.md`
- `backend/src/main/java/com/trampolim/api/catalog/OpportunityAlertService.java`
- `backend/src/main/java/com/trampolim/api/config/SecurityConfig.java`
- `backend/src/main/java/com/trampolim/api/geography/RegionalCatalogController.java`
- `backend/src/main/java/com/trampolim/api/geography/RegionalCompanyController.java`
- `backend/src/main/java/com/trampolim/api/geography/RegionalQuery.java`
- `backend/src/main/java/com/trampolim/api/geography/TerritorialCatalog.java`
- `backend/src/main/java/com/trampolim/api/geography/TerritorialController.java`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/db/migration-empresas/V15__municipality_reference_and_regional_feed.sql`
- `backend/src/main/resources/db/migration/V18__municipality_reference_and_regional_feed.sql`
- `backend/src/main/resources/geography/municipalities.json`
- `backend/src/main/resources/geography/regions.json`
- `backend/src/test/java/com/trampolim/api/geography/TerritorialCatalogTest.java`
- `backend/src/test/java/com/trampolim/api/geography/TerritorialHttpPostgresTest.java`
- `backend/src/test/java/com/trampolim/api/radar/RadarMigrationPostgresTest.java`
- `frontend/e2e/territory-journey.spec.ts`
- `frontend/src/features/account/AdminCatalogPage.tsx`
- `frontend/src/features/account/CatalogPage.tsx`
- `frontend/src/features/account/CompanyOpportunities.tsx`
- `frontend/src/features/account/api.ts`
- `frontend/src/features/career/CareerExtras.tsx`
- `frontend/src/features/geography/MunicipalityAssignment.tsx`
- `frontend/src/features/geography/MunicipalitySearch.tsx`
- `frontend/src/features/geography/TerritoryProvider.tsx`
- `frontend/src/features/geography/TerritorySelector.tsx`
- `frontend/src/features/geography/territory.ts`
- `frontend/src/features/geography/territoryContext.ts`
- `frontend/src/features/geography/useRegionalQuery.ts`
- `frontend/src/main.tsx`
- `frontend/src/pages/CapacitaPage.tsx`
- `ops/territory-validation-2026-11-04.json`

- `backend/.env.example`

- `frontend/e2e/account-journey.spec.ts`
