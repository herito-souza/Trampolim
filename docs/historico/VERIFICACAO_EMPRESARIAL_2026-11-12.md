# Verificação empresarial, antifraude e moderação — backend

## Auditoria e escopo

Já existiam companies.verification_status (PENDING/APPROVED/REJECTED/SUSPENDED), aprovação administrativa, autorização por empresa, pagamento de estágio, denúncias do catálogo legado e notificações corporativas persistidas. Foram reutilizados cadastros, estados operacionais, PlatformAccess, autenticações independentes, APIs internas RadarBridge e o mecanismo communication_events/deliveries.

Lacunas: cadastro corporativo aceitava somente o formato numérico de 14 posições sem cálculo do DV; a aprovação antiga não exigia critérios/documentação objetiva; não havia distinção entre autorização operacional e selo comprovado; denúncias do catálogo legado não cobriam o registro corporativo independente; não existiam análise de duplicações, recurso de retenção de anúncio e proteção unificada nos diferentes caminhos de publicação.

Os cadastros existentes e suas aprovações foram preservados. Nenhum registro antigo ganhou selo automaticamente e nenhum CNPJ antigo foi apagado ou corrigido por inferência. A migração preenche apenas fingerprints técnicos de anúncios, sem reclassificar retroativamente todos como fraude ou alterar pagamento. Anúncios antigos podem ser reanalisados explicitamente pelo administrador.

## CNPJ e avaliação das fontes externas

Cnpj valida formato e dígitos verificadores, inclusive CNPJ alfanumérico. Aceita pontuação de apresentação e normaliza para maiúsculas; não remove silenciosamente caracteres arbitrários. O cálculo usa o procedimento da Receita (ASCII menos 48, módulo 11). Não comprova existência, situação ativa, representação ou legitimidade do anúncio. O formulário recebeu somente o ajuste de teclado de CNPJ para permitir letras em celulares.

Referências consultadas em 12/11/2026:

- [Receita Federal — documentação técnica do DV](https://www.gov.br/receitafederal/pt-br/centrais-de-conteudo/publicacoes/documentos-tecnicos/cnpj).
- [Receita Federal — CNPJ alfanumérico](https://www.gov.br/receitafederal/pt-br/acesso-a-informacao/acoes-e-programas/programas-e-atividades/cnpj-alfanumerico).
- [Serpro — contratação da Consulta CNPJ](https://apicenter.estaleiro.serpro.gov.br/documentacao/consulta-cnpj/pt/como_contratar/).
- [Serpro — autenticação, ambientes e exemplos](https://apicenter.estaleiro.serpro.gov.br/documentacao/consulta-cnpj/pt/quick_start/).
- [Conecta Gov — escopo e requisitos de acesso](https://www.gov.br/conecta/catalogo/apis/consulta-cnpj).

Serpro é uma alternativa oficial contratável, com autorização/credenciais e condições comerciais próprias; não se presumiu acesso gratuito ou contratação existente. Conecta Gov tem escopo institucional específico, não foi tratado como API pública irrestrita para empresas privadas. Nenhum scraper, API de terceiros não autorizada ou consulta a um CNPJ real foi ativado. Dados de demonstração/trial não podem conceder selo.

Nesta entrega a conferência é manual, feita por administrador independente com fonte oficial/autorizada, referência documental e data registradas. Automação externa permanece pendente de contratação, revisão de termos/custos e homologação. Não há botão ou resultado simulado de consulta automática.

## Verificação e selo

O status operacional APPROVED continua significando autorização empresarial existente para publicar, sujeito às demais regras. O selo novo significa somente “Cadastro empresarial revisado”, não garantia de idoneidade, segurança de toda vaga, contratação ou chancela da Receita.

Fluxo: empresa solicita revisão → REQUESTED → UNDER_REVIEW opcional → APPROVED/REJECTED. Solicitações abertas são idempotentes, no máximo três novas por empresa/dia. Perfil alterado invalida a solicitação pendente anterior e exige revisão da versão atual. Administrador não pode decidir para empresa da qual seja responsável ou membro.

Aprovação exige:

1. CNPJ sintaticamente válido, dados obrigatórios completos e responsável ativo com e-mail verificado.
2. Confirmação humana de situação cadastral ativa em fonte oficial/autorizada.
3. Correspondência da razão social.
4. Comprovação da representação empresarial.
5. Confirmação independente do contato.
6. Referência da consulta/documentação, data ISO-8601 dos últimos trinta dias e justificativa administrativa de ao menos vinte caracteres.

Uma decisão APPROVED representa a confirmação conjunta desses critérios, aplicada pelo serviço; ficam registrados perfil examinado (hash), referência, horário da consulta, revisor, justificativa, revisão e validade. Nenhum documento sensível é publicado nem há novo upload documental: armazenar somente referência mínima ao procedimento autorizado.

Selo válido por noventa dias a partir da decisão. Somente a decisão concluída mais recente pode sustentar a prova: uma rejeição posterior impede reutilizar aprovação antiga pelo endpoint legado. Sua consulta exige empresa operacionalmente aprovada, responsável ainda ativo/verificado, caso aprovado vigente e fingerprint do cadastro atual igual ao examinado. Alterar razão social, nome público, CNPJ, proprietário/e-mail do responsável, contato ou localização invalida a correspondência. Expiração/perfil alterado removem o selo, sem suspender automaticamente toda a empresa. Resposta pública usa Cache-Control: no-store e não devolve CNPJ, fonte documental, contato ou identidade do revisor.

A rota administrativa antiga de aprovação não concede APPROVED sem uma revisão comprovada vigente. Rejeição/suspensão administrativa exige justificativa e mantém trilha de auditoria, notificação e possibilidade de nova revisão. Isso altera intencionalmente o antigo atalho de aprovação; a interface de gestão precisará adotar o novo fluxo.

## Regras de anúncios e limites

Regras determinísticas observam somente conteúdo e atividade de publicação, não características protegidas de candidatos ou empregadores. Código versionado na migração Empresas V22; não utiliza IA generativa ou classificação criminal.

| Sinal | Critério inicial | Tratamento |
| --- | --- | --- |
| CANDIDATE_PAYMENT_REQUEST | Expressões normalizadas como taxa de inscrição, pagar para se candidatar/participar ou depósito antecipado | REVIEW |
| SENSITIVE_CREDENTIAL_REQUEST | Solicitação de senha bancária, envio de senha, código de autenticação/cartão ou CVV | REVIEW |
| SHORTENED_EXTERNAL_LINK | URL externa usando encurtadores bit.ly/tinyurl.com | REVIEW; servidor não segue o link |
| DUPLICATE_COMPANY_LISTING | Mesmo fingerprint normalizado de conteúdo na mesma empresa, descrição com ao menos 80 caracteres e outro anúncio não encerrado criado nos últimos trinta dias | REVIEW |
| HIGH_PUBLICATION_VELOCITY | Dez publicações efetivadas na última hora, seguidas de nova tentativa de publicação ainda sem aprovação humana específica | REVIEW |

Fingerprint considera título, descrição, requisitos, área, local, URL, benefícios, pré-requisitos, salário, contato e cursos aceitos. O fingerprint usa hash para comparação técnica, não assinatura de segurança ou prova de fraude. Não são compartilhados IDs/conteúdos privados de anúncios de outras empresas. Duplicidade entre empresas diferentes, similaridade semântica e obfuscação avançada não foram implementadas; são limitações explícitas.

Estados do anúncio: CLEAR, REVIEW e HELD. Ao tentar publicar conteúdo em REVIEW/HELD, a publicação fica PAUSED. Não é exclusão nem bloqueio irreversível. REVIEW é sinalização, não acusação; citações legítimas e frases negadas podem gerar falso positivo e precisam de revisão humana. O proprietário recebe os códigos e pode recorrer. HELD é decisão humana revisável.

A proteção está em trigger do banco Empresas: alcança publicação normal, alterações de conteúdo, renovação e confirmação de pagamento. Estágio pode ficar PAID e continuar PAUSED; pagamento não apaga moderação nem se transforma em publicação aprovada. Liberação ALLOW grava o fingerprint analisado e limpa a retenção, mas NÃO publica automaticamente: a empresa precisa publicar novamente com pagamento/prazo/aprovação válidos. Alterar o conteúdo invalida a liberação específica e pode gerar nova sinalização.

Novo anúncio pelo endpoint corporativo: padrão de vinte criações por janela móvel de 24 horas, com bloqueio da linha empresarial para evitar corrida. TRUST_MAX_CREATED_PER_DAY configura limite entre 1 e 1000 (valores fora disso são limitados). Não altera preços. Importações administrativas/SQL de manutenção não são consideradas publicação por esse endpoint; não é proteção contra um administrador com acesso direto ao banco.

Anúncios legados não são reclassificados em massa. O administrador pode executar rescan de um registro. Regras podem deixar passar fraude ou sinalizar anúncio legítimo; não substituem equipe de moderação, canal de suporte e monitoramento.

## Denúncias e privacidade

As denúncias corporativas possuem IDs próprios, distintos dos itens do catálogo legado. Candidatos usam Oportunidades; alunos usam Capacita; cada origem autentica e armazena o relato em seu próprio banco. O banco Empresas recebe somente recibo opaco, referência da vaga, motivo, descrição consentida e identificação da trilha — não recebe ID de conta, nome, e-mail, currículo ou sessão do denunciante.

Motivos: PAYMENT_REQUEST, IDENTITY_MISUSE, MISLEADING, DUPLICATE, OTHER. Descrição opcional de até mil caracteres, com aviso para não incluir dados sensíveis. Uma denúncia por conta/vaga e limite de cinco novas por conta/24 horas. Recibo e payload iguais não duplicam; tentativa de sobrescrever conflita. A quantidade de relatos não suspende vaga ou empresa automaticamente, evitando bloqueio por brigada de denúncias.

A entrega local é persistida antes da chamada HTTP; falha transitória permanece PENDING, com retentativas controladas e limite de dez tentativas, sem mensagens externas. Falha permanente fica FAILED; o autor pode tentar novamente respeitando o próximo horário permitido. O worker processa até vinte pendências e vinte exclusões por origem a cada ciclo. Chamadas repetidas de múltiplas instâncias são idempotentes no destino. Não há garantia de entrega exatamente uma vez.

Somente administração global consulta texto dos relatos. A empresa não recebe identidade, texto livre ou contagem dos denunciantes; recebe apenas sinalização/decisão de seu anúncio e mensagem genérica de revisão. O autor consulta status OPEN/RESOLVED/DISMISSED/WITHDRAWN pela API autenticada própria, sem observações internas da moderação.

Retirada apaga a descrição local e enfileira exclusão no destino; exclusão da conta também gera solicitação de apagamento por trigger antes do cascade. Empresas apaga descrição/decisão pessoal do relato e conserva tombstone mínimo de recibo para impedir reaparecimento em entrega atrasada. Operação não remove provas de outros relatos ou a própria vaga. A política operacional de retenção de casos, referências, auditorias e tombstones precisa ser definida pelo responsável antes de produção; não foi inventado prazo legal ou rotina destrutiva de limpeza.

## Moderação e recursos

Fila de casos, anúncios sinalizados, denúncias e recursos com paginação. Decisões exigem razão e revisão esperada; mudanças concorrentes retornam 409 em vez de sobrescrever uma decisão. Empresa pode abrir um recurso por anúncio retido; repetição devolve o mesmo recurso aberto, no máximo cinco novos por empresa/dia. Liberação ou manutenção da retenção resolve os recursos abertos do anúncio e fica auditada. Nova revisão pode reverter um HELD.

Status RESOLVED de denúncia significa tratamento administrativo, não confirmação de fraude. Decidir relato e moderar anúncio são operações distintas, autorizadas e registradas. Aprovação/rejeição cadastral também é independente da validação de cada vaga.

## Notificações

Foi adicionado TRUST_REVIEW ao mecanismo corporativo existente de comunicação. Eventos persistidos têm chave idempotente e são entregues somente a responsáveis/recrutadores autorizados da empresa, inclusive para permitir ciência/recurso quando ela estiver suspensa. Preferências, limites de frequência, caixa de entrada e marcação de leitura existentes são preservados. A mensagem não contém identidade do denunciante, documento de verificação ou teor de acusações.

Notificação interna não significa e-mail/WhatsApp enviado. Nenhum canal pago, provedor externo ou mensagem real foi ativado. Nesta etapa o autor de denúncia consulta o status; não foi criado push entre trilhas que compartilhe sua identidade.

## Endpoints

### Empresas — JWT Empresas

| Método/caminho | Finalidade |
| --- | --- |
| POST/GET /api/empresas/trust/verification | Solicitar/consultar revisões próprias |
| GET /api/company/listings/{id}/trust | Estado, sinais e trust_revision do próprio anúncio |
| POST /api/company/listings/{id}/trust/appeal | reason (20–1000 caracteres) |

GET público /api/public/companies/{slug}/verification retorna somente selo vigente e seu escopo. O frontend ainda precisa apresentar o selo e os fluxos novos; não se deve usar verification_status=APPROVED como substituto.

### Administração — JWT Oportunidades e superadministrador

Base /api/admin/company-trust:

| Método/caminho | Contrato |
| --- | --- |
| GET /cases | before/limit (1–50) |
| GET /cases/{id} | Dados cadastrais e referência de revisão, somente administrador |
| POST /cases/{id}/start | expectedRevision |
| POST /cases/{id}/decision | expectedRevision, approved, registryActive, legalNameMatches, representativeConfirmed, contactConfirmed, sourceReference, checkedAt, reason |
| GET /listings | Fila de sinais/relatos abertos, before/limit |
| POST /listings/{id}/scan | Reanalisar anúncio existente |
| POST /listings/{id}/decision | expectedRevision, action ALLOW/HOLD, reason (20–1000) |
| GET /appeals | Recursos abertos, before/limit |
| GET /reports | Anúncios com relatos abertos, before/limit |
| GET /listings/{id}/reports | before UUID e limit; dados privados de moderação |
| POST /reports/{receipt}/decision | expectedRevision, action RESOLVED/DISMISSED, reason |
| GET /companies/{id}/history | Auditoria paginada |

Rota anterior /api/admin/empresas/{companyId}/verification mantém status/reason, exige prova vigente para APPROVED e justificativa de ao menos vinte caracteres. Não é atalho para criar selo.

### Relatos privados

Bases /api/opportunities/company-trust (JWT Oportunidades) e /api/capacita/opportunities/company-trust (JWT Capacita):

- POST /listings/{id}/reports: reason, details, consent=true.
- GET /reports: últimos cem próprios.
- GET /reports/{receipt}: registro próprio e status de análise disponível.
- DELETE /reports/{receipt}: retirar e enfileirar apagamento.
- POST /reports/{receipt}/retry: retomar FAILED no horário permitido, mantendo o recibo.

Não há parâmetro para escolher a conta de outro denunciante. Conta empresarial não pode usar sessão de Empresas nesses endpoints pessoais.

### Contratos internos

POST /api/integrations/company-trust/reports, /reports/status e /reports/withdraw, protegidos por X-Trampolim-Bridge e segredo forte existente. Dados em records CompanyTrustService.Report/Withdrawal. Não aceitam JWT como substituto do segredo. Nenhuma chamada consulta diretamente o banco privado de outra trilha.

## Configuração e implantação

TRUST_MAX_CREATED_PER_DAY=20; TRUST_REPORT_DELIVERY_ENABLED controla o worker de entrega, herdando o controle operacional existente quando não definido. O mecanismo de notificações segue COMMUNICATIONS_WORKER_ENABLED/configuração existente. RADAR_INTERNAL_API_URL e RADAR_BRIDGE_SECRET precisam de roteamento interno correto e segredo forte, HTTPS/rede privada em produção. As verificações cadastrais manuais não dependem de novas credenciais externas.

Migrações aditivas: Empresas V22__company_trust_and_listing_moderation.sql; Oportunidades V24__private_company_listing_reports.sql; Capacita V25__private_company_listing_reports.sql. Fazer backups independentes, homologar atualização e implantar backend/worker compatíveis antes de ativar novos fluxos. Não foram executadas migrações no schema operacional de dados reais nesta tarefa.

Novas estruturas: company_trust_cases, company_trust_audit, company_listing_publish_events, company_listing_reports, company_report_tombstones, company_listing_appeals, campos de risco/revisão nos anúncios e, nas origens pessoais, listing_trust_reports/listing_report_erasure.

Arquivos novos em backend/src/main/java/com/trampolim/api/trust: Cnpj, CompanyTrustService, CompanyTrustController, ListingReportGateway, ReportGatewayConfig, ReportApi, CandidateReportController, StudentReportController e TrustErrors. Alterados cadastros AuthController/IndependentTrailAuthController, validação da importação LegacyCompanyMigrationService, CompanyController, CompanyVerificationController, SecurityConfig, CommunicationTemplates/CommunicationStore e configurações. Ajuste mínimo no teclado de CNPJ em AuthScreen.tsx. Testes CnpjTest e CompanyTrustPostgresTest; expectativas Flyway atualizadas sem remover verificações anteriores.

## Validação

Validação em 12/11/2026:

- Compilação e Maven verify/build de produção aprovados.
- Suíte completa: 282 testes registrados, 281 aprovados, zero falhas/erros e um ensaio opcional de carga não habilitado. Incluiu cadastros, autenticação, isolamento JWT, pagamentos, recrutamento, mídia, avaliações, Hub, desafios, feirão e Observatório.
- Rodada posterior de 41 testes focados aprovada, com ajuste da serialização de anúncio por bloqueio consultivo e cenário determinístico de edição versus moderação. Nenhuma verificação foi desativada.
- Rodada final do módulo: 18 testes aprovados (15 de integração e três de CNPJ), cobrindo também que rejeição mais recente invalida aprovação anterior e que normalização não transforma caracteres não ASCII em outro identificador.
- Frontend: lint, 23 testes unitários e build/typecheck aprovados após o ajuste de teclado. Avisos preexistentes de tamanho de módulos de mapa/PDF permanecem; Playwright não foi executado nesta alteração mínima.
- Migrações verificadas em bancos vazios e atualização de históricos, preservando os registros anteriores dos cenários de Flyway.
- git diff --check e inspeção de espaços finais dos novos arquivos sem problemas.

[Resumo versionável das evidências](../evidencias/verificacao-empresarial-2026-11-12.json). Logs brutos ficam somente em ops/company-trust-*.log no computador, não são publicados no repositório. Todos os registros são sintéticos e foram criados em schemas descartáveis; os comprovantes de revisão dos testes são explicitamente sintéticos, sem consulta externa real.

A primeira tentativa de integração precisou iniciar o Docker. A primeira suíte focada encontrou duas fixtures inadequadas (campos obrigatórios ausentes e denúncia de anúncio nunca publicado), corrigidas sem afrouxar validações. A revisão final identificou e tratou a ordem de bloqueios em edição/moderação e a seleção da prova mais recente. Esses ajustes receberam retestes específicos depois da regressão ampla.

Não houve teste representativo de carga, homologação de fornecedor cadastral, envio real, restauração de produção ou implantação.

~~~powershell
.\ops\scripts\Test-BackendSecurity.ps1 -EvidenceName company-trust-local -TestPattern 'CnpjTest,CompanyTrustPostgresTest,CompanyAccountPostgresTest,CommunicationPostgresTest,RadarMigrationPostgresTest,RecruitmentMigrationPostgresTest'
~~~

## Pendências e limites

Telas de verificação, selo, denúncia, recursos e filas administrativas precisam ser integradas ao frontend. Não há interface demonstrativa simulando essas ações. A interface antiga de aprovação deve tratar 409 e conduzir ao novo procedimento quando atualizada.

Automação de fonte oficial depende de decisão, contrato, autorização/custos/termos e homologação; não está ativa. A evidência manual precisa de operadores treinados e procedimentos claros de validação/contestação. Fontes cadastrais e selos não garantem que um anúncio é seguro.

As regras atuais não detectam todo golpe, similaridade semântica ou identidades obtidas ilicitamente; podem gerar falsos positivos. Não há suspensão irreversível ou exclusão automática por score. Limites locais não demonstram capacidade para milhares de usuários; aplicar testes representativos, monitoramento, retenção e avaliação de privacidade antes de produção. Não houve implantação, certificação jurídica integral ou contratação externa.
