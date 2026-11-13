# Métricas de produto — 13/11/2026

## Auditoria

O produto já possuía fontes operacionais para candidaturas, histórico de recrutamento, entrevistas realizadas, contratações confirmadas, conclusão de cursos e competências aprovadas. O Trampolim Capacita já emite `course.completed.v1`; o registro usa os critérios existentes de aulas e avaliações. As tabelas de contas armazenavam `created_at` e `last_login_at`, mas o último login não permitia calcular coortes de retorno. O Trampolim não registra eventos confiáveis de início/abandono do diagnóstico, busca, publicação de vaga ou jornada de curso.

## Entrega e acesso

- Endpoint administrativo agregado: `GET /api/admin/product-analytics/{OPORTUNIDADES|EMPRESAS|CAPACITA}?fromMonth=AAAA-MM&toMonth=AAAA-MM`.
- Filtros geográficos opcionais em Empresas: `state` ou `municipalityCode` (formato IBGE de sete dígitos); não podem ser usados juntos. Métricas de candidato/aluno não recebem recorte geográfico porque os perfis não têm município IBGE validado.
- A rota exige superadministrador. A saída contém séries mensais e fórmulas, sem e-mail, nome, currículo, empresa individual, candidatura ou curso identificável.
- Janela de consulta limitada a 36 meses. Grupos com menos de cinco participantes/empresas têm valor e tamanho suprimidos. Métricas com denominador inferior a cinco também são ocultadas.
- A captura é própria, sem pixels ou SDK externo. `product_usage_days` registra somente um identificador interno por trilha e dia em que o backend emite uma sessão autenticada; chave estrangeira apaga os registros quando a conta é removida. Não coleta páginas visitadas, busca digitada, conteúdo, endereço IP ou dados de dispositivo.

## Fórmulas e fontes

| Métrica | Fórmula e fonte |
|---|---|
| Novas contas | Contas criadas no mês, agrupadas pela trilha e sem juntar identificadores entre bancos. Oportunidades filtra contas de pessoa. |
| Cadastro confirmado | Contas da coorte de criação com e-mail verificado e aceite de termos presente no momento da consulta. Como não existe histórico de verificação de e-mail, é o estado atual da coorte, não uma série histórica da data de confirmação. |
| Ativação em até 7 dias | Conta verificada com primeiro registro de sessão autenticada até sete dias após a criação. A coorte só é considerada madura após sete dias. |
| Retenção D7 / D30 | Entre usuários com primeiro registro de sessão no mês, percentual com outra sessão entre os dias 7–13 ou 30–36, respectivamente. Mede retorno por emissão de sessão, não toda atividade com token já aberto. |
| Primeira candidatura | Candidato distinto pelo e-mail normalizado cuja primeira candidatura conhecida na base Empresas foi registrada no período. O e-mail só é usado dentro da consulta; não é retornado nem associado à conta de Oportunidades. |
| Candidaturas | Registros consentidos de candidatura a vagas de emprego, pela data de envio; não dependem de a vaga continuar aberta hoje. |
| Candidaturas por vaga | Média de candidaturas consentidas por anúncio com registro de publicação no mês, incluindo anúncios sem candidatura; grupo de candidatos abaixo de cinco é suprimido. |
| Entrevistas realizadas | Candidaturas com `interview_held=true` e data de entrevista registrada, agrupadas pelo mês dessa data. |
| Contratações confirmadas | Registros com data de contratação, confirmação do candidato e consentimento para métricas públicas. Ausência de retorno ou mudança de etapa nunca é contratação. |
| Mediana de dias até contratação | Mediana de dias entre o envio da candidatura e a data de contratação confirmada, na mesma base consentida. |
| Empresas ativas | Empresa atualmente aprovada com usuário autorizado que emitiu sessão no mês. Em filtro municipal, exige vaga publicada naquela localidade. |
| Vagas com contratação confirmada | Número de anúncios distintos vinculados a contratação confirmada. Não estima quantidade de postos preenchidos quando um anúncio possui múltiplas posições. |
| Cursos concluídos | Eventos `course.completed.v1` idempotentes no Outbox, emitidos após os critérios pedagógicos atuais. O histórico está sujeito à retenção configurada do Outbox (padrão atual: 365 dias). |
| Competências verificadas | Conquistas de avaliação não revogadas, pela data de emissão. Conclusão de curso não gera competência automaticamente. |

## Limitações e indisponibilidades explícitas

- Abandono de diagnóstico, busca, publicação e trilha educacional permanece indisponível: não há evento de início/abandono suficiente. Progresso parcial ou ausência de retorno não é classificado como abandono.
- Métricas de retorno subestimam usuários que continuam ativos com sessão já aberta sem emitir novo token. A captura atual observa cadastro/login que emite sessão; não instrumenta cada página ou ação.
- Coortes anteriores à migração de `product_usage_days` não têm histórico de uso reconstruível. O campo `usageTrackingFrom` informa a primeira data capturada.
- A fonte de primeiras candidaturas é a base Empresas, que registra o e-mail do candidato; não existe identidade comum entre os bancos e não é feita consulta cruzada a Oportunidades.
- A publicação histórica de vagas utiliza `career_listing_publications`, instrumentada a partir de sua implantação; anúncios antigos sem esse registro não entram na média por vaga.
- A confirmação de contratação exige consentimento específico para métricas públicas, portanto a contagem pode ser menor que o total de contratações registradas sem esse consentimento.
- Os resultados descrevem apenas registros da Trampolim. Não representam estatística oficial nem o mercado regional completo.

## Alterações de banco

Migrações aditivas, independentes por banco:

- Oportunidades: `V28__product_analytics_usage_days.sql`.
- Empresas: `V27__product_analytics_usage_days.sql`, com índices de primeira candidatura e data de contratação.
- Capacita: `V29__product_analytics_usage_days.sql`, com índice para eventos de conclusão de curso.

Não foram criados identificadores globais nem tabelas agregadas compartilhadas. Consultas limitam-se a 36 meses e usam índices por período.

## Testes e validação

- Testes unitários cobrem supressão de coortes menores que cinco, divulgação a partir do limite e supressão de taxas com denominador pequeno.
- Compilação Maven do backend validada.
- Execução das migrações e consultas PostgreSQL não realizada neste ambiente; Docker não estava executando.
- Teste global Maven permanece limitado por erros de compilação em classes de teste preexistentes que referenciam símbolos ausentes. O teste focado deve ser executado após corrigir essa configuração ou no pipeline do projeto.
