# Observatório de Empregabilidade — indicadores preparados, versão 2

## Diagnóstico da implementação anterior

RegionalObservatory já oferecia vagas atualmente abertas criadas no período, previsões, competências demandadas, fontes agregadas de Oportunidades/Capacita, contratações confirmadas e captura diária por UF. Havia cache de 60 segundos, controle de concorrência e limiar de dez participantes. As consultas, porém, eram executadas nas tabelas operacionais durante a requisição. O filtro de período de vagas usava created_at, não primeira publicação; a geografia era UF informada ou sede empresarial. Conclusões educacionais eram a situação atual de uma coorte de consentimento, não uma série temporal de conclusões.

Esta entrega acrescenta uma API v2 preparada, com contratos explícitos, sem substituir silenciosamente as definições anteriores. O endpoint antigo continua disponível por compatibilidade, identificado por sourceMode=LEGACY_DYNAMIC_COMPATIBILITY e preparedIndicatorsEndpoint. Suas consultas ainda são dinâmicas, limitadas e armazenadas em cache; os dashboards precisam migrar para a v2 para utilizar exclusivamente a camada preparada. A privacidade de contratações/tempo médio do legado foi reforçada para exigir identificadores distintos e diversidade de empresas, não apenas quantidade de registros.

## O que os indicadores representam

São registros observados na Trampolim. Não representam todo o mercado de trabalho de Irecê, não são dados oficiais do IBGE, não extrapolam amostras e não comprovam vínculo trabalhista. O catálogo IBGE é usado apenas para validar códigos e rótulos territoriais, nunca para inventar estatísticas.

Um anúncio não equivale à quantidade de postos: headcount não é somado. Uma candidatura não equivale a pessoa única. E-mails normalizados são um identificador técnico de candidatura, não identificação civil; contas educacionais independentes também não garantem pessoas físicas distintas. Resultados de fontes diferentes não são somados como se fossem o mesmo público.

## Arquitetura

Cada banco proprietário possui sua própria geração de agregados. Empresas prepara vagas, presença empresarial e recrutamento. Capacita prepara conclusões e conquistas acadêmicas. O banco Oportunidades não é consultado diretamente pelos agregadores; os dados de candidaturas/confirmacões já pertencem ao módulo corporativo pelos fluxos existentes.

Os lotes usam transação REPEATABLE_READ, bloqueio consultivo por banco/schema e geração UUID. Os dados só se tornam visíveis após commit completo. A atualização ocorre em segundo plano, nunca como efeito colateral do GET. Falha reverte o lote e preserva a geração anterior. Duas gerações são mantidas; requisições usam leitura consistente para não misturar gerações durante a troca.

O lote Empresas registra municípios realmente presentes nos anúncios a partir do catálogo territorial existente. Código inválido ou ausente não é preenchido por nome livre ou pela cidade da sede. Nesse caso, o anúncio permanece no grupo sem município reconhecido; somente uma UF-base empresarial válida pode ser usada no recorte estadual. Código válido do anúncio prevalece sobre a UF da sede. Dessa forma, uma empresa sediada na Bahia pode publicar anúncio de São Paulo sem atribuí-lo incorretamente à Bahia.

O lote usa observatory_vacancy_cells para contagens por mês, município, UF e área e observatory_company_presence para contar empresas DISTINTAS em territórios combinados. Não soma contagens municipais de empresas, pois a mesma empresa pode anunciar em mais de uma cidade. Nenhum identificador empresarial individual é devolvido pelo dashboard.

Dados pessoais não são armazenados nos agregados: apenas valores e quantidade de identificadores/contribuintes por célula. As contagens brutas de células pequenas permanecem internas e não são expostas em API. A leitura entre bancos usa RadarBridge e contrato interno autorizado, com limite de concorrência, timeout e segredo; não encaminha sessão ou senha do usuário.

## Métricas e fórmulas

O contrato legível por máquina está em GET /api/public/career/observatory/definitions.

| Campo/métrica | Unidade e cálculo | Referência temporal | Limitações principais |
| --- | --- | --- | --- |
| registeredPublications | COUNT de anúncios job com primeira marca em career_listing_publications, empresa atualmente aprovada e estágio atualmente pago | Mês da primeira marca de publicação; não criação | Marca anterior à elegibilidade financeira pode existir; não é prova isolada do instante de divulgação efetiva. Anúncios sem marca não recebem data inventada. Situação financeira/empresarial atual pode alterar a série reprocessada. |
| stocksAtCapture.active_listings | Predicado público existente: PUBLISHED, empresa aprovada, prazos abertos e estágio PAID | Estoque no instante de captura, independente do período do gráfico | Não é média mensal nem estoque histórico; observedAt sempre acompanha o valor. |
| participatingCompanies | COUNT DISTINCT company_id com primeira marca de anúncio válido no período/território | Período da publicação registrada | Não equivale a todas as empresas cadastradas, associadas ou empregadoras da região. |
| APPLICATIONS | Registros company_applications em anúncios job de empresas aprovadas | Mês de created_at | Restrito à administração; não representa pessoas únicas nem candidaturas externas. |
| INTERVIEWS_REPORTED_HELD | Outcomes com interview_held=TRUE e interview_at não futuro | Mês do horário informado | Relato empresarial; não soma agendamentos como entrevistas realizadas. Restrito à administração. |
| CONFIRMED_HIRES | Outcomes com hire_date, confirmação autenticada do candidato e public_metrics_consent=TRUE | Mês de candidate_confirmed_at | Não é mês da admissão nem comprovação legal. Não inclui relatos não confirmados ou sem autorização de divulgação agregada. |
| COURSE_COMPLETIONS | Pares aluno-curso com curso publicado/não bloqueado, professor ativo, ao menos uma aula publicada READY, todas as aulas concluídas e cada avaliação obrigatória aprovada SUBMITTED | Maior data entre última aula e primeira aprovação de cada avaliação obrigatória | Situação do currículo atual, não livro histórico imutável de certificação. Mudanças no currículo podem alterar uma conclusão derivada. Cursos só com PDFs ou sem videoaulas não atendem essa regra existente. |
| VERIFIED_AWARDS | Conquistas educacionais respaldadas por tentativa submetida/aprovada consistente em aluno, avaliação e nota; sem revogação e ainda válidas | Mês de awarded_at | Registros de conquista, não nomes distintos de competências nem certificados formais. Exclui autodeclaração e evidência empresarial do Passaporte. |

Conclusões e conquistas só incluem contas ativas/verificadas com consentimento regional vigente e eventos posteriores a consent_at. A UF vem do consentimento, não de localização residencial presumida ou do município do curso. Renovar consentimento reinicia esse marco; revogação remove o participante da próxima captura e invalida imediatamente a geração disponível para consulta. Não foi acrescentada coleta de município ou localização precisa dos alunos.

publicationSeries é preenchida por mês: zero só é devolvido se há captura válida que cobre aquele mês e não há marca registrada correspondente. Estados NOT_CAPTURED, INVALIDATED, STALE, OUTSIDE_COVERAGE e UNAVAILABLE não são convertidos em zero. Uma fonte pode estar READY e outra indisponível; seus horários e estados permanecem separados.

## Filtros e geografia

- fromMonth/toMonth no formato AAAA-MM, inclusivos, entre um e doze meses, sem meses futuros.
- Capturas cobrem do primeiro dia do mês de doze meses atrás até o instante atual: treze meses de calendário, incluindo o atual parcial. Consultas fora da cobertura não retornam séries parciais como se completas fossem.
- Meses e datas derivadas usam America/Sao_Paulo. O estoque usa o predicado operacional de disponibilidade existente, inclusive suas regras de vencimento; não altera a lógica de publicação.
- state (UF), municipalityCode (IBGE) ou region (ID do catálogo configurado). Município e região são mutuamente exclusivos. UF conflitante com o território retorna 400.
- Na ausência de recorte, a UF sugerida é a da cidade de referência configurada (atualmente Irecê-BA), não uma constante que impeça expansão. A interface pode usar a cidade/território inicial do endpoint de configuração geográfica existente.
- Distribuições municipality/area são paginadas, com offset até 10000 e limit de 1 a 50. A ordem é contagem de publicações registradas decrescente e categoria como desempate. Áreas são os valores cadastrados nos anúncios, não uma taxonomia oficial de demanda regional.
- Código ausente ou inválido fica explicitamente em “Município não informado ou código não reconhecido”. O filtro de Irecê não incorpora automaticamente anúncios sem código apenas porque o texto livre menciona Irecê.

No recorte municipal/regional, métricas pessoais de recrutamento não são divulgadas. Capacitação retorna MUNICIPAL_DATA_UNAVAILABLE, porque o consentimento educacional atual só tem UF. Não se mostra o número estadual sob rótulo municipal. Para acompanhar capacitação, consultar explicitamente a UF.

## Privacidade e autorização

Métricas de recrutamento pessoais usam células disjuntas de mês/UF, sem cruzamentos por cargo/empresa, sem períodos arbitrários diários e sem total combinado desses valores. Exigem valor de ao menos dez registros, ao menos dez e-mails normalizados distintos e três empresas. Cursos/conquistas exigem ao menos dez contas de alunos contribuintes no próprio indicador/mês: dez alunos consentidos sem conclusão não autorizam publicar a conclusão de uma única pessoa.

Valores elegíveis são apresentados em faixas de dez: lowerBound, upperBound e label. Por exemplo, 14 registros observados são divulgados como 10–19, não como estimativa ou intervalo de confiança. Grupos insuficientes, inclusive zero, recebem a mesma resposta SUPPRESSED; não há contagem exata de contribuintes na resposta. Não há valor médio, identidade, endereço, contato, currículo, lista de pessoas ou ranking individual.

Candidaturas e entrevistas só são acessíveis ao superadministrador global existente, e ainda com supressão. Empresas/associações não recebem automaticamente métricas operacionais de outras organizações. Seus endpoints privados anteriores seguem as próprias permissões. Indicadores públicos são globais agregados de dados empresariais já publicados ou de resultados pessoais autorizados, sem cache por tenant que possa misturar informações privadas.

Essas proteções reduzem exposição, mas não constituem prova formal de anonimização ou privacidade diferencial. E-mails e contas não garantem pessoas únicas; fontes auxiliares e observações repetidas podem gerar riscos residuais. Antes de ampliar dimensões de métricas pessoais, realizar avaliação de risco de reidentificação e finalidade/base legal. Não houve declaração de conformidade jurídica integral com a LGPD.

### Invalidação por privacidade

Cada banco tem observatory_epoch. Mudanças de consentimento/conta educacional, revogação/exclusão de conquistas e mudanças relevantes de consentimento/exclusão de candidaturas corporativas incrementam a época por triggers. Após o commit da alteração, novas leituras deixam de aceitar gerações de época anterior, sem esperar o cache expirar. Uma requisição iniciada antes da revogação pode concluir com a observação anterior e seu horário explícito. A captura trava a época para evitar publicar lote antigo depois de uma revogação concorrente; leituras conferem a época atual. O lote tem limites de espera/execução. Invalidação é conservadora e pode tornar os indicadores indisponíveis até nova captura, mesmo se a alteração não mudaria uma célula divulgável.

Agregados antigos são mantidos apenas como gerações internas até a próxima limpeza, sem acesso público direto. Excluir uma conta não implica reconstruir sua identidade entre trilhas. Alterações em vagas/progresso que não sejam invalidação de privacidade aparecem na próxima captura, como informado por observedAt.

## Endpoints

| Método/caminho | Acesso e finalidade |
| --- | --- |
| GET /api/public/career/observatory/definitions | Dicionário estático de métricas, versão, unidade, fórmula, origem e limitações |
| GET /api/public/career/observatory/indicators | Resumo, série de primeiras marcas, estoque capturado, empresas distintas e células pessoais autorizadas por UF |
| GET /api/public/career/observatory/distribution | Distribuição paginada por municipality ou area |
| GET /api/admin/observatory/indicators | Superadministrador com JWT Oportunidades; inclui candidaturas e entrevistas suprimidas |
| GET /api/admin/observatory/status | Saúde da captura corporativa: estado, observedAt, running, lastAttempt |
| POST /api/admin/observatory/refresh | 202; enfileira captura Empresas e solicita captura Capacita via integração segura; não agrega na requisição |
| GET /api/capacita/learning/admin/observatory/status | Administrador educacional, JWT Capacita; saúde da fonte local |
| POST /api/capacita/learning/admin/observatory/refresh | 202; enfileira somente captura educacional |
| POST /api/integrations/career/observatory/v2/learning | Header X-Trampolim-Bridge; lê apenas agregados educacionais preparados |
| POST /api/integrations/career/observatory/v2/learning/refresh | Mesmo segredo; enfileira captura da própria fonte |

Exemplo de consulta (substitua pelos meses realmente desejados e cobertos):

~~~text
/api/public/career/observatory/indicators?fromMonth=2026-09&toMonth=2026-10&municipalityCode=2914604
/api/public/career/observatory/distribution?fromMonth=2026-09&toMonth=2026-10&state=BA&dimension=municipality&limit=20&offset=0
~~~

Contrato interno educacional:

~~~json
{"fromMonth":"2026-09","toMonth":"2026-10","state":"BA"}
~~~

Sem segredo válido retorna 401; integração não configurada não é substituída por dados locais de outro banco. Os filtros de JWT por trilha e PlatformAccess/LearningAccess existentes continuam obrigatórios. O endpoint público anterior /api/public/career/observatory e seus proxies existentes permanecem compatíveis; sua semântica antiga não deve ser confundida com os novos campos v2.

## Eficiência e operação

- Web v2: somente tabelas preparadas, período limitado, índices por geração/UF/mês/município, paginação e plano de consulta inspecionável. Não faz contagens nas tabelas de vagas, candidaturas ou progresso durante o GET.
- Concorrência de leitura corporativa limitada a quatro requisições analíticas, com 30 consultas por minuto por chave de cliente entre resumo/distribuição. Reutiliza o tratamento de proxy confiável da aplicação; configurar corretamente o encaminhamento do IP no ambiente de produção.
- Leitura SQL com timeout de três segundos. Captura com statement_timeout de trinta segundos e lock_timeout de três segundos. Falhas não tornam um lote parcial visível.
- Um lote local por vez e bloqueio consultivo no PostgreSQL evitam duplicação entre instâncias. A fila em memória não persiste por reinício; a próxima execução periódica recupera a atualização. Não foi introduzida fila externa ou Redis sem necessidade medida.
- Snapshot com mais de duas horas é STALE e não apresenta valores como atuais. Próxima captura bem-sucedida recupera a fonte.
- Atualização padrão a cada quinze minutos por fonte. Não existe promessa de tempo real; a primeira leitura antes da captura retorna NOT_CAPTURED.

Configuração: OBSERVATORY_V2_REFRESH_MS=900000; usa o controle existente OBSERVATORY_CAPTURE_ENABLED (ou configuração career.observatory-capture-enabled). RADAR_INTERNAL_API_URL e RADAR_BRIDGE_SECRET continuam sendo os contratos de integração; usar segredo forte, rede privada/HTTPS e roteamento correto em implantação separada. Não registrar corpos contendo segredos em logs. SMTP, WhatsApp e pagamentos não são necessários para os indicadores.

## Banco e arquivos

Empresas V21__prepared_employment_observatory.sql: observatory_epoch, observatory_runs, observatory_personal_cells, observatory_municipalities, observatory_vacancy_cells e observatory_company_presence; índices temporais em fontes e triggers de invalidação. Capacita V24__prepared_learning_observatory.sql: as três primeiras tabelas locais, índices e invalidação de sua própria fonte. Não foi alterada entidade JPA nem schema do banco Oportunidades. Migrações aditivas, sem apagar dados ou alterar históricos Flyway aplicados.

Novos arquivos backend/src/main/java/com/trampolim/api/observatory: SnapshotBatch.java, EmploymentObservatory.java, LearningObservatory.java, ObservatoryController.java, LearningObservatoryController.java, ObservatoryErrors.java e MetricDefinitions.java.

Alterados: career/RegionalObservatory.java (privacidade e metadados do legado), application.properties e backend/.env.example (intervalo). Testes novos em test/java/com/trampolim/api/observatory/ObservatoryPostgresTest.java; versões esperadas atualizadas em RadarMigrationPostgresTest e RecruitmentMigrationPostgresTest. README referencia este relatório. Nenhuma tela, autenticação, contrato de pagamento ou documento de usuário foi substituído.

## Testes e resultados

Execução final em 11/11/2026: Maven verify terminou com BUILD SUCCESS; 266 testes registrados, 265 aprovados, zero falhas/erros e um ensaio opcional de carga desativado (BackendScalePostgresTest). Tempo da suíte/build: 7min31s. Os nove testes novos do Observatório passaram. FFmpeg/FFprobe locais habilitados para a regressão de mídia.

Cenários novos: publicação registrada versus criação; estoque atual versus fluxo no período; falta de marco histórico; município oficial e desconhecido; expansão para São Paulo; empresas distintas em mais de uma cidade; estágio não pago; captura com falha preservando geração anterior; limiares por identificador e empresa; proibição de dados pessoais/publicação de métricas operacionais sem autorização; conclusão exigindo todas as aulas e avaliação; competência com evidência válida; revogação de consentimento invalidando geração; NOT_CAPTURED/STALE/UNAVAILABLE sem zero fictício; autenticação entre trilhas; paginação; leitura preparada sem depender da tabela operacional de vagas.

As primeiras tentativas encontraram uma expressão de JSON na fixture Java e uma candidatura sintética repetida no mesmo anúncio. As fixtures foram corrigidas sem remover restrições, desativar testes ou mudar regras de negócio. A regressão completa também passou por desafios profissionais, Passaporte, feirão, Hub, Radar, notificações, recrutamento, avaliações e demais módulos existentes. Migrações de bancos vazios e atualização de históricos foram validadas pelos testes de Flyway; as contas de teste preexistentes nos cenários de atualização foram preservadas.

Evidências: [log completo] (evidência local não versionada), [isolamento] (evidência local não versionada) com realDataUsed=false e verifyExitCode=0, e [plano real da consulta preparada] (evidência local não versionada). O EXPLAIN ANALYZE/BUFFERS da fixture pequena (um anúncio, duas células mensais) registrou planejamento de 0,161 ms e execução de 0,313 ms; acessou somente agregados. Isso é evidência funcional do plano local, não benchmark representativo, comparação antes/depois ou comprovação de capacidade em produção. Outro teste renomeou temporariamente a tabela operacional no schema descartável e confirmou que os GETs preparados continuavam lendo os valores já capturados.

Verificação git diff --check dos arquivos rastreados e inspeção de espaços finais dos arquivos novos sem problemas. A redação dos rótulos de privacidade foi refinada após a suíte, sem mudança de fórmulas; o pacote final foi recompilado separadamente. Não houve teste frontend, teste de carga representativo, restauração ou implantação nesta etapa backend.

Os testes usam PostgreSQL local real, schemas aleatórios descartáveis e empresas, alunos, vagas e respostas sintéticos. Não utilizam dados reais ou estatísticas externas. As migrações não foram aplicadas ao schema operacional de usuários.

~~~powershell
.\ops\scripts\Test-BackendSecurity.ps1 -EvidenceName observatory-local -TestPattern 'ObservatoryPostgresTest,RadarMigrationPostgresTest,RecruitmentMigrationPostgresTest'
# Regressão completa; configure CAPACITA_FFMPEG e CAPACITA_FFPROBE para mídia:
.\ops\scripts\Test-BackendSecurity.ps1 -EvidenceName observatory-regression-local
~~~

## Pendências e evolução

Migrar a interface do Observatório para os novos endpoints requer uma etapa frontend. O endpoint legado continua com consultas dinâmicas para preservar seus recursos anteriores de previsões/lacunas/programas; esta entrega não declara que todo acesso analítico antigo foi eliminado. Não foram implementadas equivalências silenciosas entre suas métricas e as novas.

Dados municipais ausentes devem ser corrigidos no cadastro da vaga, não inventados pelo analytics. Séries anteriores às primeiras marcas reais de publicação/capturas não podem ser reconstruídas como fatos. Uma eventual coleta educacional municipal precisa de finalidade, consentimento e controle próprios; não faz parte desta entrega.

O lote educacional reavalia o currículo atual e pode custar mais com grande histórico. Há limites e índices, mas ainda será necessário medir volume representativo em homologação; se os limites forem atingidos, avaliar incrementalidade com invalidação por mudança de currículo antes de adicionar infraestrutura. Não foram demonstrados milhares de usuários concorrentes nem conformidade jurídica integral. Definir responsáveis por dados, acompanhamento de lotes inválidos/stale, política de retenção e revisão de privacidade antes de implantação. Fazer backup e homologar migrações; não houve implantação em produção.
