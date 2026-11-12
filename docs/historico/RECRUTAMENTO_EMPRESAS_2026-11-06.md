# Trampolim Empresas — gerenciamento de processos seletivos

## Diagnóstico
A versão local já publica oportunidades e recebe candidaturas em company_applications. O painel lista candidaturas, filtra vaga/etapa e envia PUT de status. Há cinco estados anteriores: RECEIVED, IN_REVIEW, CONTACTED, ACCEPTED e DECLINED. O GET legado aceita paginação opcional, mantendo lista completa quando limit é omitido.

Não existiam histórico de mudanças, notas internas ou versão para detectar telas desatualizadas. O módulo CompanySelectionService já registra entrevistas e contratações declaradas/confirmadas com consentimento: foi preservado e não duplicado. Nenhum dado de Oportunidades ou Capacita é consultado pelo novo serviço.

## Modelo e estados
Continua sendo usado o mesmo registro da candidatura, com revision incremental compartilhada entre alterações de etapa e notas.

| Código | Apresentação | Observação |
| --- | --- | --- |
| RECEIVED | Recebido | Estado inicial |
| IN_REVIEW | Em análise | Triagem humana |
| INTERVIEW | Entrevista | Não comprova que foi realizada |
| ACCEPTED | Selecionado | Não significa contratação confirmada |
| DECLINED | Não selecionado | Código existente preservado |
| CLOSED | Encerrado | Reabrir exige confirmação expressa |
| CONTACTED | Contatado (legado) | Preservado; não é convertido em entrevista |

Atenção aos dados antigos: o painel anterior apresentava DECLINED como “Encerrada”. Esse código foi preservado para não reinterpretar registros sem evidência. Sua distinção histórica entre encerramento e não seleção pode exigir revisão humana; os totais representam códigos operacionais, não motivos de rejeição comprovados.

O fluxo é simples e flexível: permite avançar, corrigir ou retornar etapas com versão válida. Sair de CLOSED exige reopen=true. Encerrar uma candidatura não encerra a vaga nem outras candidaturas. A mudança de etapa não cria vínculos empregatícios ou competências automaticamente.

## Endpoints
Todos requerem Bearer JWT emitido por Empresas, conta ativa/verificada e papel OWNER ou RECRUITER da empresa determinada pelo backend. Mantém-se o titular como OWNER mesmo sem vínculo em company_members. ADMIN/ANALYST e administradores de associação não ganham acesso privado por meio desta alteração. IDs de recursos de outras empresas retornam 404. Não existe parâmetro companyId controlado pelo cliente.

Base: /api/company/recruitment

| Método | Caminho | Função |
| --- | --- | --- |
| GET | /stages | Códigos, rótulos e identificação do estado legado |
| GET | /applications | Lista paginada; filtros listingId e status combináveis |
| GET | /applications/{id} | Candidatura e versão atual, sem notas |
| PUT | /applications/{id}/stage | Alterar etapa com controle de versão |
| GET | /applications/{id}/history | Histórico paginado sem texto das notas |
| GET | /applications/{id}/notes | Observações privadas ativas, paginadas |
| POST | /applications/{id}/notes | Adicionar observação interna |
| DELETE | /applications/{id}/notes/{noteId} | Remover texto da observação |
| GET | /indicators | Totais reais por etapa; filtros listingId/status |

Paginação: page começa em 0 (máximo 1000); size padrão 25, de 1 a 100. Resposta contém items, page, size, totalElements e hasNext. Ordem estável por criação e ID; histórico usa ID decrescente. Cada página usa consulta de total e consulta limitada, sem buscar notas por candidato (sem N+1). Leituras compostas usam snapshot transacional para manter contagem e itens consistentes. Vagas específicas são verificadas antes de filtrar, evitando inferir dados de outra empresa.

A lista preserva candidaturas de vagas e inscrições de cursos já existentes (campo kind); listingId permite selecionar a oportunidade específica sem duplicar inscrições.

### Alterar etapa
Exemplo de corpo JSON do PUT:

```json
{
  "status": "INTERVIEW",
  "expectedRevision": 0,
  "requestKey": "2ce180f0-855a-4a68-b0cc-0d5589b4abcc",
  "reopen": false
}
```

reopen é opcional e só precisa ser true para sair de CLOSED. Reutilize o mesmo UUID e o mesmo corpo ao repetir uma tentativa cuja resposta não chegou. Um novo comando precisa de novo UUID e da versão atual. A resposta contém status, revision, noteId (quando aplicável) e replayed.

A operação usa transação e bloqueio de linha da candidatura, verifica a versão e grava alteração/histórico atomicamente. Versão antiga ou UUID reutilizado com outros dados/autor retorna 409. Após 409, recarregue e revise os dados; não repita automaticamente com versão nova. Uma repetição idêntica retorna o resultado original e não cria outro evento. Esse resultado pode ser anterior ao estado atual: recarregue a candidatura antes de iniciar outra ação.

### Observações internas
POST recebe body (1 a 2.000 caracteres), expectedRevision e requestKey. DELETE recebe expectedRevision e requestKey na query string e exige UUID da nota no caminho. A nota deve pertencer à candidatura autorizada. Remover apaga imediatamente seu texto, preservando apenas o registro mínimo da operação. As notas não são retornadas pelo GET da candidatura, pelo histórico, pelas listagens públicas ou pela listagem legada. Clientes devem renderizar as notas como texto, sem inserir HTML recebido diretamente na página. Evite diagnósticos de saúde, raça, religião, vida sexual e outros dados sensíveis ou critérios discriminatórios em textos livres; nenhum desses atributos é utilizado em pontuação, filtros ou decisões automáticas.

## Compatibilidade
GET /api/company/applications e seu formato de lista continuam disponíveis. PUT /api/company/applications/{id}/status continua recebendo status sozinho e respondendo o mesmo objeto de status; suas alterações agora passam pelo serviço transacional e pelo histórico. Também aceita expectedRevision e requestKey opcionais em conjunto para clientes que migrarem gradualmente.

Limite deliberado: clientes legados que enviam apenas status não informam a versão que viram. Seus comandos são serializados e auditados, mas conservam a intenção de última gravação. A detecção de edição obsoleta depende do envio da versão; o novo endpoint a exige. Para painéis novos, use exclusivamente a API versionada. O painel atual foi preservado; esta entrega de backend não inclui novas telas para notas/histórico nem migração do painel para paginação obrigatória.

## Migração V17 — somente Empresas
Arquivo: backend/src/main/resources/db/migration-empresas/V17__recruitment_workflow.sql.

- Amplia a restrição de status sem renomear registros existentes.
- Adiciona revision com padrão 0.
- Cria company_recruitment_notes e company_recruitment_events, com vínculos e exclusão em cascata pela candidatura.
- Registra BASELINE na data da migração, preservando a etapa existente sem inventar transições, autores ou datas anteriores.
- Trigger registra RECEIVED para novas candidaturas, inclusive as recebidas pela API pública existente.
- Índices para etapa/paginação, consultas por empresa e limpeza de retenção.
- Índices únicos de versão e chave de requisição por candidatura garantem consistência.

Atualize as instâncias escritoras de forma coordenada: a migração sozinha não registra os UPDATEs de uma versão antiga do backend que ainda escreva diretamente o status sem o novo serviço.

Os bancos de Oportunidades e Capacita não recebem novas migrações nesta entrega. Não se altera o histórico Flyway anterior. Aplicação em banco operacional deve ocorrer no processo normal de atualização, com backup e janela adequada: a alteração de restrição e a criação de índices podem exigir bloqueios. Os testes só aplicam a migração em schemas descartáveis.

## Privacidade e retenção
Defaults operacionais configuráveis, sujeitos à política aprovada pelo controlador de dados (não equivalem a aconselhamento jurídico):

- RECRUITMENT_NOTES_RETENTION_DAYS=180: notas vencidas deixam de ser lidas imediatamente, independentemente do agendador.
- RECRUITMENT_HISTORY_RETENTION_DAYS=730: prazo de conservação dos eventos posteriores à linha de base e dos registros mínimos de notas removidas.
- RECRUITMENT_RETENTION_ENABLED=true: rotina por hora, início após um minuto, limpa no máximo 1.000 registros por categoria por execução, com SKIP LOCKED para múltiplas instâncias.
- O conteúdo vencido é apagado; o tombstone mínimo permite idempotência até o prazo do histórico. A linha BASELINE/RECEIVED inicial e a candidatura permanecem até a política de exclusão da candidatura/empresa. Pedidos idempotentes fora do prazo de retenção podem retornar conflito por versão antiga.
- A rotina não apaga candidaturas, anúncios, pagamentos ou resultados de contratação. Prazos desses registros e de backups devem seguir a política já estabelecida e a finalidade/base de tratamento, com exclusão controlada. Não foi criada exclusão anônima por e-mail que permitisse apagar candidaturas alheias.
- Solicitações de acesso, correção e exclusão devem ser atendidas pela empresa responsável mediante verificação de identidade e regras aplicáveis. Não conceder acesso global a currículos ou notas de terceiros.
- Logs/eventos de etapa não contêm nome, contato ou texto da observação. O hash de idempotência é interno e não é retornado na API. Identificação do autor é por ID corporativo, anulada pela chave estrangeira quando seu acesso é excluído.

## Execução e testes
A aplicação usa Java 21/Spring Boot e o DataSource/TransactionManager de Empresas existentes. Sem novas bibliotecas, serviços pagos ou credenciais externas.

Para executar verificações com bancos Docker locais saudáveis, usando apenas schemas temporários:

```powershell
.\ops\scripts\Test-BackendSecurity.ps1 -EvidenceName recruitment-2026-11-06
.\ops\scripts\Test-BackendSecurity.ps1 -EvidenceName recruitment-focused-2026-11-06 -TestPattern 'Recruitment*Test,CompanyAccountPostgresTest,RadarMigrationPostgresTest'
```

O novo parâmetro EvidenceName evita sobrescrever relatórios de etapas anteriores; TestPattern permite repetir casos específicos. O script valida nomes de schemas/paths antes da limpeza e não restaura em bancos existentes.

Resultados executados:

- Compilação inicial: aprovada (ops/recruitment-compile.log).
- Suíte geral inicial: 191 casos, 184 aprovados, duas falhas, cinco ignorados (ops/recruitment-verify.log). Uma falha nova, no DTO HTTP que exigia reopen mesmo omitido, foi corrigida substituindo o primitivo obrigatório por Boolean opcional. Não foram removidas verificações.
- Reteste final: 17 testes aprovados, zero falhas, zero ignorados; Maven verify com compilação, testes e empacotamento aprovado (ops/recruitment-focused.log). Inclui 9 testes de serviço/API de recrutamento, 1 de migração de candidaturas existentes, 6 de contas/permissões empresariais e 1 de atualização Flyway das três trilhas. O pacote foi gerado em backend/target; sem implantação.
- Foram confirmados POST/GET/DELETE de notas, PUT de etapa, catálogo de etapas, histórico paginado e compatibilidade do PUT legado via HTTP autenticado.
- Concorrência: dois recrutadores alterando a mesma revisão; exatamente um sucesso e um 409, com um único evento de alteração persistido.
- Atomicidade: falha sintética na inserção do histórico reverte etapa e revisão.
- Migração: banco novo e atualização V16→V17; mantém nomes, contatos, estados, datas e credenciais já existentes, sem inventar autores/transições. Nova execução do Flyway não repete a migração.
- Evidência de isolamento/limpeza: ops/recruitment-focused-2026-11-06/isolated-tests.json. Todos os fixtures são sintéticos, em schemas descartáveis; não foram cadastradas empresas no schema operacional.
- git diff --check sem erros nos arquivos verificados.

A suíte geral não está integralmente aprovada: permanece uma falha preexistente de CareerHttpPostgresTest no mentor de IA de Oportunidades (limite diário esperava 429 e recebeu 200). O código desse módulo não foi alterado. Uma consulta somente de expressões, sem tabelas de usuários, reproduziu a causa na sessão America/Sao_Paulo após 21h: CURRENT_TIMESTAMP >= (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date retorna false para uma requisição atual, pois a data UTC é reinterpretada no fuso da sessão. É necessário corrigir e testar a fronteira de dia UTC explicitamente em trabalho separado, respeitando a regra desta solicitação de não modificar as outras trilhas. Isso impede declarar a regressão global totalmente limpa.

Dos cinco casos ignorados na primeira suíte, um é o teste de carga opt-in e quatro são testes de mídia que exigem as variáveis CAPACITA_FFMPEG/CAPACITA_FFPROBE. O PATH sozinho não satisfaz suas condições. Eles não foram declarados aprovados nem desativados; o reteste focado de recrutamento não depende dessas ferramentas. A suíte completa não foi repetida após as correções locais: foram repetidos os testes afetados e os de migração/permissões empresariais.

 Os testes novos usam dados sintéticos e verificam etapas, histórico, notas, repetição idempotente, conflitos de versão, concorrência real, atomicidade, filtros, paginação, métricas, papéis, acesso entre empresas, JWT de outra trilha, retenção, HTTP e migração de candidaturas existentes.

## Arquivos da etapa
Novos: RecruitmentService.java, RecruitmentController.java, RecruitmentRetention.java; migração V17; RecruitmentPostgresTest.java e RecruitmentMigrationPostgresTest.java; este relatório.
Modificados: CompanyController.java (adaptador legado), application.properties e backend/.env.example (configuração de retenção), RadarMigrationPostgresTest.java (versão esperada 17), ops/scripts/Test-BackendSecurity.ps1 (diretório de evidências e seleção de testes) e README.md (link técnico). Todo o trabalho local anterior foi preservado.
