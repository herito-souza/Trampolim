# Jornada Primeiro Emprego — 13/11/2026

## Reaproveitamento auditado

Oportunidades já tinha `GET/PUT /api/career/first-job`, os registros privados `career_first_job_steps`, oportunidades públicas filtradas por regime/UF/flag `firstExperience`, orientação legal, simulador textual, cursos do Capacita via API autorizada e status pessoais de acompanhamento. Esta entrega aprimora a experiência existente; não cria catálogo, vaga, empresa, curso ou novo fluxo de candidatura.

## Mudanças

- A jornada agora pergunta explicitamente se primeiro emprego é o objetivo atual. A resposta pode ser atualizada e fica privada na conta Oportunidades.
- Quando selecionada, mostra ações para perfil/currículo, diagnóstico, cursos, simulador e busca de anúncios de primeira experiência por meio dos destinos já existentes.
- Orientação de currículo sugere projetos escolares, atividades comunitárias, voluntariado e cursos somente quando forem reais, sem inventar experiência ou resultados.
- A barra de progresso usa apenas informações observadas na trilha: perfil/currículo existente, diagnóstico preenchido, entrevista concluída e salva, e status pessoal de candidatura. O texto distingue registros do próprio candidato de etapas confirmadas pela empresa.
- Recomendações Capacita continuam limitadas aos conteúdos publicados retornados pela API pública autorizada; progresso individual não é consultado entre bancos.
- A verificação de requisitos diferencia estágio, aprendizagem e emprego. Idade é opcional e processada apenas na resposta de `POST /api/career/eligibility`; o contrato informa `storedPersonalData:false` e `automaticEligibility:false`. O resultado sempre requer validação humana. Não se coleta diagnóstico de deficiência para avaliar exceção legal.
- Fontes legais visíveis são fontes oficiais: [Lei do Estágio](https://www.planalto.gov.br/ccivil_03/_ato2007-2010/2008/lei/l11788.htm), [MTE — Aprendizagem Profissional](https://www.gov.br/trabalho-e-emprego/pt-br/assuntos/aprendizagem-profissional), [MTE — Combate ao Trabalho Infantil](https://www.gov.br/trabalho-e-emprego/pt-br/assuntos/inspecao-do-trabalho/areas-de-atuacao/Combate-trabalho-infantil). Resumos atualizados em 13/11/2026; não são parecer jurídico.

## Dados e compatibilidade

`career_first_job_steps` reutilizada para persistir preferência `FIRST_JOB_GOAL` (`YES`/`NO`) além dos três planejamentos já existentes. Migração aditiva Oportunidades V30 estende a restrição do tipo de etapa, preservando as linhas antigas. `GET /api/career/first-job` agora também retorna o objetivo atual e indicadores reais existentes. As APIs públicas de oportunidades e orientações foram preservadas. Nenhum banco Empresas ou Capacita foi alterado.

## Regras e salvaguardas

- A preferência não é inferida pelo diagnóstico, idade ou características pessoais: a pessoa escolhe.
- Oportunidades sinalizadas como primeira experiência são apenas anúncios assim marcados pela empresa; não há promessa de compatibilidade ou contratação.
- Estágio depende dos requisitos educacionais e do termo legal, e não é tratado como vínculo de emprego regular.
- Aprendizagem profissional tem regra geral de idade e exceções que dependem de análise; o sistema não concede elegibilidade automaticamente.
- A regra geral de trabalho inicia aos 16 anos, salvo aprendizagem a partir dos 14; menores de 18 têm proteções específicas, incluindo restrição a trabalho noturno, perigoso e insalubre.
- A idade não é armazenada pelo novo verificador. O site não substitui a confirmação do anúncio, da entidade formadora, da escola, do empregador ou de orientação qualificada.

## Arquivos

- `backend/src/main/java/com/trampolim/api/career/CareerCoachingController.java`
- `backend/src/main/java/com/trampolim/api/career/CareerEligibilityController.java`
- `backend/src/main/resources/db/migration/V30__first_job_goal_preference.sql`
- `backend/src/test/java/com/trampolim/api/career/CareerEligibilityControllerTest.java`
- `frontend/src/features/career/CareerPanel.tsx`
- `docs/TRAMPOLIM.md` e este relatório

## Validação

Verificações executadas: lint frontend aprovado; typecheck (`tsc --noEmit`) aprovado; build frontend de produção aprovado; testes frontend, 30 aprovados. `mvnw -DskipTests compile` aprovou e compilou os 224 fontes Java principais. Os testes JUnit novos e existentes não puderam ser executados: a fase testCompile tenta compilar os 94 arquivos de teste do repositório e falha por classes/pacotes ausentes em testes legados (entre eles `TrailTokenService`, `com.trampolim.api.user`, `BackendApplication`, `RadarBridge` e outros), antes da execução dos testes selecionados. `git diff --check` não encontrou erros de whitespace; apenas avisos de conversão LF/CRLF dos arquivos modificados.

A migração precisa ser executada pelo Flyway em banco de desenvolvimento/homologação; não foi aplicada a banco real nesta alteração. Não foi executado teste end-to-end visual ou consulta de elegibilidade na interface. O serviço de elegibilidade backend permanece sempre consultivo e o resumo legal é apresentado na área existente; não há decisão automática de aptidão para emprego, estágio ou aprendizagem.
