# Simulador de entrevistas — 13/11/2026

## Implementado

- Fluxo de simulação textual para Oportunidades, configurável por área, ocupação, nível (início, júnior, pleno e sênior) e tipo (geral, primeiro emprego, comportamental e técnica).
- Perguntas geradas por modelos de texto versionáveis no código, sem inferir perfil ou características pessoais.
- Feedback local disponível sem IA. O feedback opcional do provedor existente utiliza critérios limitados a clareza, estrutura, pertinência e exemplos concretos no texto; não representa previsão de contratação.
- Consentimento separado para enviar uma resposta ao provedor. O envio contém tipo, nível, área, ocupação, pergunta e resposta; `CareerRules.redact` remove padrões comuns de e-mail, telefone e segredos. O formulário orienta a não incluir dados pessoais ou confidenciais. O pedido usa `store:false`, não grava o texto bruto no histórico de chamadas e não é repetido automaticamente após resultado incerto.
- Histórico privado opcional. Por padrão, uma sessão nova não é salva para retomada. Se o titular optar, ela aparece na listagem; cada registro possui ação de exclusão. Rascunhos não consentidos são apagados após 24 horas e sessões não consentidas são excluídas quando terminam.
- Recomendações consultam somente o endpoint público autorizado do Capacita e mostram conteúdo publicado retornado pela API; indisponibilidade e ausência de resultados são estados distintos.
- Ensaio opcional de voz utiliza gravação temporária no navegador, com consentimento explícito e limite de dois minutos. Não há upload, transcrição, análise de fala ou avaliação de sotaque.
- Fronteira `CareerAiProvider` separa o serviço do provedor. O adaptador OpenAI existente é o único implementado/configurado nesta entrega; não há failover nem segunda integração.

## Rotas e dados

- `POST /api/career/interviews`: inicia sessão com `area`, `occupation`, `experienceLevel`, `interviewType` e `saveHistory`.
- `GET /api/career/interviews` e `GET /api/career/interviews/{id}`: consultam somente sessões do titular; a listagem inclui apenas aquelas cujo histórico foi autorizado.
- `POST /api/career/interviews/{id}/answers`: salva a resposta no registro próprio e devolve orientação. Requer autenticação de Oportunidades e controle de revisão concorrente.
- `GET /api/career/interviews/{id}/training`: consulta conteúdos Capacita publicados por integração interna autorizada.
- `DELETE /api/career/interviews/{id}`: exclusão pelo próprio titular.
- A migração Oportunidades V29 adiciona nível, tipo e `save_history`; registros preexistentes recebem defaults compatíveis. Nenhuma tabela ou configuração dos bancos Empresas/Capacita foi alterada para esta função.

## Limites e privacidade

O limite de simulações é dez por usuário/dia. A orientação IA reaproveita os limites já existentes do mentor de carreira: padrão de 20 chamadas por usuário/dia, 1.000 global/dia, orçamento de tokens reservados de 40.000 por usuário/dia e 1.000.000 global/dia, três chamadas por usuário/minuto, duas simultâneas e saída máxima padrão de 600 tokens (máximo configurável de 2.000). Essas cotas são compartilhadas com outros usos do serviço de IA. Uma falha com resultado incerto consome a reserva, para evitar duplicação de cobrança.

IA continua desabilitada por padrão (`MENTOR_AI_ENABLED=false`). Habilitação exige configuração do segredo no servidor e aceite explícito por resposta. O texto fornecido ainda pode conter informação pessoal que não seja reconhecida pelos padrões automáticos; o consentimento e a instrução de minimização não eliminam esse risco. O áudio fica temporariamente na memória/blob URL do navegador e é apagado pelo controle da interface ou ao desmontar a página; o navegador pode manter cópia temporária até liberar recursos.

## Estimativa indicativa de custo

Hipótese: três respostas por simulação, cada uma com 1.000 tokens de entrada e 600 de saída. Na tabela pública do GPT‑5 mini, US$ 0,25 por milhão de tokens de entrada + US$ 2 por milhão de saída, resulta em **US$ 0,00435 por simulação**. Com GPT‑5.4 mini, a US$ 0,75/US$ 4,50 por milhão de tokens de entrada/saída, a mesma hipótese resulta em **US$ 0,01035**. Não são preços contratados nem custos medidos; prompts, comprimentos, câmbio, impostos e preços podem variar. O cálculo real só é registrado quando `MENTOR_INPUT_USD_PER_MILLION` e `MENTOR_OUTPUT_USD_PER_MILLION` estão configuradas.

A configuração de referência ainda usa `gpt-5-mini`; a documentação oficial consultada marca este modelo como depreciado. Não se alterou automaticamente o modelo, pois a troca pode afetar custo e comportamento. Antes de ativar em produção, selecione modelo suportado e atualize valores de custo conforme contrato/tabela vigente. Referências oficiais: [GPT‑5 mini](https://developers.openai.com/api/docs/models/gpt-5-mini) e [GPT‑5.4 mini](https://developers.openai.com/api/docs/models/gpt-5.4-mini).

## Arquivos desta implementação

- Frontend: `frontend/src/features/career/CareerPanel.tsx`, `frontend/src/features/career/InterviewAudioPractice.tsx`.
- Backend: `backend/src/main/java/com/trampolim/api/career/CareerCoachingController.java`, `CareerAiService.java`, `CareerInterviewQuestions.java`, `CareerInterviewRetention.java`, `CareerAiProvider.java`, `OpenAiCareerAiProvider.java`, `CareerAccountController.java`.
- Banco/configuração: `backend/src/main/resources/db/migration/V29__career_interview_context.sql`, `application.properties`, `.env.example`.
- Teste de domínio: `backend/src/test/java/com/trampolim/api/career/CareerInterviewQuestionsTest.java`.

## Validação e pendências

Validações executadas: `npm run build` concluiu; `npm run lint` concluiu; `npm test` passou nos 30 testes. `mvnw -DskipTests compile` compilou os 224 fontes principais. `mvnw -Dtest=CareerInterviewQuestionsTest test` não chegou a executar a classe: a compilação dos 93 fontes de teste falhou por referências ausentes em testes legados (incluindo `TrailTokenService`, pacote `user`, `BackendApplication`, `RadarBridge` e outros). Portanto o build principal está verificado, mas o teste JUnit dedicado e os testes de integração/migração não estão validados neste checkout.

Não foi configurado provedor alternativo, transcrição, serviço pago de voz, nem armazenamento persistente de gravações. IA depende de credencial, modelo suportado, avaliação do fornecedor e configuração de preço/limites. O Capacita pode estar offline e, nesse caso, recomendações não são exibidas. A migração ainda precisa ser executada pelo Flyway em ambiente compatível antes da implantação.
