# Desafios profissionais de empresas — backend

## Auditoria e reutilização

O projeto já tinha company_challenges, rubricas ponderadas, company_challenge_submissions e company_challenge_reviews no banco Empresas; inscrições privadas e outbox career_delivery_jobs no banco Oportunidades; competências educacionais e Passaporte no Capacita. Foram preservados os contratos públicos de catálogo, endpoints de cadastro, submissão, retirada e avaliação, com a mudança necessária na regra de publicação: agora exige aprovação independente. Não foram criados outro login, outra tabela de desafios ou cópias de cursos/vagas.

Lacunas identificadas: publicação direta pela empresa, ausência de revisão institucional e limite por desafio, falta de ligação consentida entre resultado empresarial e Passaporte e risco de uma entrega em trânsito ser avaliada após alteração da definição. As alterações abaixo tratam essas lacunas.

## Fluxo operacional

1. Membro corporativo verificado OWNER/ADMIN/RECRUITER salva rascunho com competências existentes no catálogo, critérios, pesos que somam 100, nota mínima, prazo, condições e materiais.
2. Empresa aprovada propõe o desafio, informando estimativa de esforço (5–120 minutos) e teto de participantes (1–10000), e confirmando TODAS as salvaguardas: finalidade educacional, sem uso produtivo, direitos mantidos pelo autor, voluntariedade e ausência de garantia de contratação.
3. Administrador global revisa conteúdo, critérios, esforço, condições e potenciais conflitos. Não pode aprovar proposta de empresa da qual seja membro. Deve justificar a decisão e confirmar revisão das salvaguardas para aprovar.
4. Empresa publica a versão aprovada, sem alterar o conteúdo. Edição requer retorno a DRAFT, cancela aprovação e exige nova revisão. Após qualquer entrega recebida, conteúdo, competências, rubrica, materiais e condições ficam imutáveis, como no mecanismo anterior.
5. Candidato consente com o envio de texto e URL HTTPS opcional, usando apelido escolhido. A conta e os contatos não são enviados automaticamente. Uma única participação por candidato/desafio; até 50 entregas locais ativas por conta, conforme limite existente.
6. Outbox entrega versão 2 com acceptedRevision. O banco corporativo trava o desafio para verificar revisão, publicação, prazo e capacidade antes da inclusão; repetição do mesmo recibo não duplica participação. Vagas de participação retiradas continuam contando no teto histórico para não contornar o limite por retirada/reenvio.
7. Recrutador autorizado avalia todos e somente os critérios da rubrica aceita. O backend calcula a média ponderada. Tentativas de substituir avaliação já registrada conflitam; é possível revogar o resultado com justificativa, preservando o histórico.
8. O candidato pode gerar um código para importar o resultado aprovado no Passaporte de sua conta independente do Capacita. É necessário consentir nas duas contas. Não existe associação automática por e-mail.

O texto público de direitos esclarece que os materiais continuam pertencendo ao autor e veda uso produtivo/comercial, cessão compulsória e substituição de trabalho remunerado. O checklist não é um detector automático de abuso: a moderação humana e o canal institucional de denúncia/atendimento continuam necessários. O prazo estimado limita o esforço declarado da proposta; o sistema não cronometra nem presume o trabalho efetivo do participante. O backend não promete impedir usos externos ilícitos nem garante contratação ou validade jurídica de termos.

## Revisão e estados

status existente: DRAFT/PUBLISHED/CLOSED. review_status adicional: DRAFT/PENDING/APPROVED/REJECTED. PUBLISHED só fica público se review_status=APPROVED, salvaguardas aceitas, empresa aprovada e prazo aberto. Fechar normalmente um desafio não elimina um resultado já conquistado. Suspensão administrativa invalida sua elegibilidade e as evidências derivadas.

Cada escrita recebe a revisão esperada do recurso. Proposta/decisão incrementam revision e gravam company_challenge_moderation, sem respostas dos participantes. Alterar um desafio após uma submissão local mas antes de sua entrega corporativa causa rejeição da entrega, em vez de usar critérios diferentes. O frontend pode enviar expectedRevision na submissão para detectar também alterações ocorridas entre abrir o formulário e enviá-lo; clientes anteriores que omitem o campo continuam funcionando, com a revisão atual fixada pelo backend no momento do envio. As interfaces não foram alteradas nesta entrega backend.

## Endpoints

### Empresa — JWT Empresas

Mantidos: GET/POST /api/company/challenges; GET/PUT /{id}; GET /{id}/submissions; POST /{id}/submissions/{receipt}/review.

Novos:

| Método/caminho | Corpo ou finalidade |
| --- | --- |
| POST /api/company/challenges/{id}/proposal | expectedRevision, estimatedMinutes, participantLimit, educationalOnly, noProductiveUse, authorRetainsRights, voluntary, noHiringGuarantee |
| GET /api/company/challenges/{id}/indicators | received, withdrawn, evaluated, approved; somente a própria empresa |
| POST /api/company/challenges/{id}/submissions/{receipt}/review/revoke | reason com pelo menos 20 caracteres; revogação definitiva |
| POST /api/company/passport/professional/verify | token de compartilhamento destinado ao e-mail autenticado do recrutador |

Criar diretamente com status PUBLISHED agora retorna 409. Salve DRAFT primeiro. A estrutura anterior de Challenge continua sendo utilizada.

### Administração — JWT Oportunidades e PlatformAccess.isSuperAdmin

| Método/caminho | Finalidade |
| --- | --- |
| GET /api/admin/professional-challenges | Fila/lista paginada before e limit (1–50) |
| GET /api/admin/professional-challenges/{id} | Definição e últimas 50 entradas de revisão; não lista respostas privadas |
| POST /api/admin/professional-challenges/{id}/decision | expectedRevision, approved, reason (mínimo 20), safeguardsReviewed |
| POST /api/admin/professional-challenges/{id}/suspend | expectedRevision e reason (mínimo 20) |

A autenticação administrativa existente não foi substituída nem compartilhada com Empresas. A verificação de conflito considera participação corporativa do mesmo responsável.

### Participante — JWT Oportunidades

Mantidos: /api/career/challenges, /challenges/{id}, POST /challenges/submissions, GET /challenges/submissions/mine, GET/DELETE /challenges/submissions/{receipt}, prefixados por /api/career.

POST de submissão mantém receiptId, challengeId, alias, body (20–12000 caracteres), projectUrl opcional e consent. Adicionado expectedRevision opcional. receiptId deve ser mantido em retentativas; critérios/notas não são enviados pelo participante. A URL de evidência aceita somente HTTPS sem credenciais; o servidor não baixa nem executa o recurso externo. Não foi introduzido upload binário neste módulo.

Novos:

- POST /api/career/challenges/submissions/{receipt}/passport-link: {"consent":true}. Código aleatório de 256 bits, válido por 15 minutos para a primeira vinculação; limite de 10 emissões/hora por conta. Guardado somente como hash.
- DELETE /api/career/challenges/passport-links/{credentialId}: revoga o vínculo originário da própria conta.

O código é secreto: deve ser colado apenas na própria conta do Capacita, não publicado. Sua posse autoriza a vinculação consentida. Ele não identifica nem sincroniza senhas entre contas.

### Passaporte — JWT Capacita

| Método/caminho | Finalidade |
| --- | --- |
| POST /api/capacita/learning/passport/professional | token e consent; importar evidência empresarial |
| GET /api/capacita/learning/passport/professional | offset/limit, até 20 por página |
| GET /api/capacita/learning/passport/professional/{id} | Própria evidência revalidada |
| DELETE /api/capacita/learning/passport/professional/{id} | Revogar credencial local |
| GET/POST /api/capacita/learning/passport/professional-shares | Consultar/criar compartilhamento: credentialId, recipientEmail, days (1–30), consent |
| DELETE /api/capacita/learning/passport/professional-shares/{id} | Revogar compartilhamento |

O endpoint anterior /api/capacita/learning/passport/credentials continua retornando as conquistas educacionais; inclui professionalEvidenceCount para descoberta dos registros profissionais, que possuem endpoints próprios e tipo distinto. Não foram criadas tentativas acadêmicas fictícias para inserir resultados de recrutadores na tabela de avaliações educacionais. O tipo é COMPANY_ASSESSED_PROFESSIONAL_EVIDENCE, com formalCertificate=false.

Cada resultado traz empresa emissora, desafio, competências avaliadas, rubrica, nota, identificador da avaliação e data; não inclui resposta, URL de projeto, apelido, contato ou perfil pessoal do candidato. Trata-se de evidência avaliada pela empresa, não certificado formal nem equivalência automática a diploma. Não é importada automaticamente como competência acadêmica ou avaliação oficial do Capacita.

A importação reserva primeiro um identificador local durável e depois chama a origem por API. Falha de rede permite retomar com o mesmo código sem duplicar o resultado. Uma única vinculação ativa por recibo é garantida no banco originário. Revogação no Capacita impede novas utilizações locais; o titular também pode revogar o vínculo em Oportunidades. Para vincular novamente uma origem já vinculada, revogue antes seu vínculo originário; não há reativação silenciosa de credenciais revogadas. Limite de 100 registros por conta e 100 compartilhamentos simultaneamente ativos; limites são guardados com bloqueio da conta.

### APIs internas — segredo, nunca JWT de outra trilha

POST, header X-Trampolim-Bridge:

- /api/integrations/career/challenges/evidence: version=1, receiptId; apenas resultado elegível, sem resposta.
- /api/integrations/career/challenges/passport/redeem: token, credentialId; consumo idempotente ligado ao identificador do Capacita.
- /api/integrations/career/challenges/passport/check: credentialId; revalida vínculo, conta originária, retirada e avaliação.
- /api/integrations/capacita/passport/professional/verify: token, recipientEmail; usado somente pela API corporativa que deriva o destinatário da sessão autenticada.

A entrega interna existente /api/integrations/career/challenges/submit agora usa version=2 e acceptedRevision, além dos campos anteriores. Endpoints de recibo/retirada permanecem na versão 1. Sem segredo válido a API interna não libera resultado. O segredo é o já utilizado RadarBridge; configure RADAR_BRIDGE_SECRET (mínimo 32 caracteres) e RADAR_INTERNAL_API_URL no gateway interno. Use TLS/rede privada em produção, nunca coloque segredos ou códigos nos logs.

## Proteção, revogação e falhas

Empresas só consulta seus desafios e respostas. Oportunidades só consulta recibos próprios. Capacita só apresenta seus registros e vínculos. Aprovação administrativa não permite consultar indiscriminadamente as respostas privadas. As contagens não incluem nomes, contatos ou ranking público de participantes.

Verificações e compartilhamentos consultam a origem ao acessar. Resposta retirada, avaliação revogada, vínculo revogado, empresa suspensa ou moderação inválida deixam de produzir evidência válida; fonte indisponível resulta UNAVAILABLE, sem usar snapshot antigo como comprovação. Não há sincronização direta entre bancos nem transação distribuída. A comunicação mínima é síncrona e limitada pelo RadarBridge; o envio de evidências continua usando o outbox e as retentativas existentes. Falhas permanentes de entrega são registradas como REJECTED; não se reenvia automaticamente uma participação contra outra revisão.

Não houve integração paga, mensagens reais, IA obrigatória ou implantação. O armazenamento de respostas segue o mecanismo existente; arquivos externos não são baixados. O participante deve evitar enviar dados pessoais/sensíveis ou materiais de terceiros. Retirada apaga corpo, apelido e URL na origem e enfileira a retirada corporativa; a validação do Passaporte deixa de aceitar a origem retirada antes mesmo de terminar a entrega dessa retirada. Exclusão da conta segue os cascades/outbox existentes. A política operacional de retenção e atendimento precisa ser definida pelo responsável; não foi criado prazo legal presumido ou exclusão destrutiva.

## Migrações e compatibilidade

- Empresas V20__professional_challenge_governance.sql: colunas de revisão, esforço, capacidade, salvaguardas; histórico de moderação; justificativa de revogação; accepted_revision nas respostas.
- Oportunidades V23__professional_passport_links.sql: vínculos consentidos, índice único de vinculação ativa por recibo e accepted_revision local.
- Capacita V23__professional_passport_evidence.sql: learning_professional_credentials e learning_professional_shares, com FKs para a própria conta/registro.

Dados anteriores são preservados. Desafios antigos publicados recebem review_status=DRAFT e não são exibidos até revisão humana; não foram aprovados automaticamente. O administrador/empresa deve recolher para rascunho, confirmar salvaguardas, revisar e republicar. Entregas antigas e suas avaliações permanecem, mas só viram evidência se a definição for aprovada e os critérios originais continuarem íntegros. Jobs antigos ainda pendentes com Delivery versão 1 são rejeitados, pois não é seguro atribuir retroativamente uma versão aceita. Não se apagam esses registros; a equipe deve analisar os casos legados e obter nova confirmação em fluxo apropriado, sem editar payloads para forçar aprovação. Não foi criada conversão automática insegura para esse legado.

As migrações são aditivas. Em produção, realizar backup independente dos três bancos, homologar atualização e publicar backend/worker compatíveis na mesma janela. Nenhuma migração foi executada no schema de dados reais nesta tarefa.

## Arquivos

Criados:

- career/ProfessionalChallengeService.java e ProfessionalChallengeController.java.
- career/CareerPassportLinkController.java.
- education/ProfessionalPassportService.java e ProfessionalPassportController.java.
- As três migrações acima.
- test/career/ProfessionalChallengePostgresTest.java.

Alterados sob backend/src/main/java/com/trampolim/api:

- career/CompanyCareerService.java: publicação aprovada, direitos, versão aceita e teto de participação.
- career/CareerProjectsController.java: envio com revisão fixada e consulta do registro.
- company/CompanyPassportController.java: verificação corporativa do compartilhamento profissional.
- education/PassportService.java e AdaptivePassportErrors.java: descoberta do novo tipo e erros protegidos.
- config/SecurityConfig.java: contrato interno de verificação, protegido no controller pelo segredo.

Testes adaptados: CareerHttpPostgresTest.java (fluxo agora moderado), RadarMigrationPostgresTest.java e RecruitmentMigrationPostgresTest.java (novas versões, sem retirar verificações). README aponta este relatório. Nenhuma interface frontend ou autenticação foi substituída.

## Testes e resultados

Validação final em 11/11/2026: Maven verify, BUILD SUCCESS, 257 testes registrados, 256 aprovados, zero falhas/erros e um teste opcional de carga desativado (BackendScalePostgresTest). Tempo total da suíte e build: 7min15s. JAR de produção gerado. FFmpeg/FFprobe locais habilitados para a regressão de mídia.

Os 10 testes de ProfessionalChallengePostgresTest passaram: aprovação obrigatória e salvaguardas; rejeição/suspensão; capacidade sob concorrência e idempotência; rubrica calculada no servidor e imutabilidade de resultados; dois consentimentos e destinatário/revogação de compartilhamento; retirada; indisponibilidade e retomada com o mesmo identificador; revogação da avaliação; decisões concorrentes; rejeição de revisão diferente durante a entrega. CareerHttpPostgresTest também passou com o fluxo moderado e os demais recursos anteriores. A regressão completa incluiu videoaulas, biblioteca, avaliações, Passaporte acadêmico, Hub, feirão, Radar, planejamento, notificações, recrutamento e isolamento JWT.

Migrações validadas em instalações vazias e atualização dos históricos existentes, incluindo preservação de contas anteriores nos testes de Flyway. A compilação inicial encontrou uma expressão de validação inválida, corrigida. A primeira execução de integração encontrou acesso indevido a campo de dependência no proxy transacional e um teste legado ainda publicando diretamente; ambos foram corrigidos, sem desativar verificações. A execução focada seguinte passou e a regressão final incluiu as proteções adicionais de revisão e revogação.

Evidências: [log final] (evidência local não versionada) e [isolamento dos dados] (evidência local não versionada), realDataUsed=false e verifyExitCode=0. git diff --check dos arquivos rastreados e inspeção de espaços finais dos arquivos novos sem problemas. Nenhum teste frontend foi executado, pois a entrega é backend. Não houve ensaio de carga, medição de capacidade para milhares de pessoas, restauração de backup ou implantação nesta rodada.

Execução local com schemas aleatórios descartáveis e dados sintéticos; serviços externos reais desativados. O harness preserva os schemas de uso real e remove os temporários ao terminar.

~~~powershell
.\ops\scripts\Test-BackendSecurity.ps1 -EvidenceName professional-challenges-local -TestPattern 'ProfessionalChallengePostgresTest,CareerHttpPostgresTest,RadarMigrationPostgresTest,RecruitmentMigrationPostgresTest'
# Regressão completa; configure CAPACITA_FFMPEG e CAPACITA_FFPROBE para mídia real:
.\ops\scripts\Test-BackendSecurity.ps1 -EvidenceName professional-challenges-regression-local
~~~

## Limitações e próximos passos

A participação continua usando a conta Oportunidades e seus endpoints de respostas. O Capacita recebe o resultado apenas por vinculação explícita; não foi criado um segundo fluxo de submissão educacional ou conta automática em outra trilha.

Entrega backend. Telas de proposta/revisão, importação e compartilhamento profissional precisam de etapa frontend; não há botões demonstrativos nesta entrega. Não são enviadas notificações novas nem ativados SMTP/WhatsApp. A política pedagógica e a moderação humana precisam de responsáveis treinados, procedimento para denúncias e revisão dos termos; aprovação técnica não equivale a conformidade jurídica. A UI antiga de publicação deve tratar o 409 e conduzir ao fluxo de revisão quando essa interface for desenvolvida.

Revalidação online implica dependência das APIs originárias e custo de chamadas proporcional à página; foram impostos limites, mas não foram medidos milhares de participantes nem realizado teste prolongado de carga nesta tarefa. Se houver volume demonstrado, considerar endpoint agregado autorizado de revalidação antes de cache de resultados sensíveis. Não ativar automaticamente equivalência acadêmica, sincronização de perfis ou recomendações de contratação a partir dessas evidências.
