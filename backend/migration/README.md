# Procedimento seguro de migração das trilhas

## Regras

- Faça a migração primeiro em uma cópia restaurável dos bancos.
- Use uma identidade de superadministrador temporária, com acesso restrito. Remova-a ao concluir.
- Não copie senhas, tokens, sessões ou confirmações de pagamento antigas.
- Preserve o banco de Oportunidades como fonte de leitura até concluir reconciliação, aceite dos usuários e validação funcional.
- Não use `docker compose down -v`, `DROP DATABASE`, `TRUNCATE` ou exclusão de tabelas para desfazer uma tentativa.

## Backup verificável em ambiente local ou de teste

Configure as variáveis `PGHOST`, `PGPORT`, `PGUSER` e `PGDATABASE` para cada conexão, e forneça a senha por um gerenciador de credenciais PostgreSQL (por exemplo, `.pgpass`/pgpass.conf), sem incluí-la na linha de comando ou no repositório. Exemplo PowerShell:

```powershell
$backupDir = Join-Path (Get-Location) "migration-backup-$(Get-Date -Format yyyyMMdd-HHmmss)"
New-Item -ItemType Directory -Path $backupDir | Out-Null

$env:PGHOST = "localhost"
$env:PGUSER = "root"
$env:PGPORT = "5432"
$env:PGDATABASE = "trampolim_db"
pg_dump --format=custom --file (Join-Path $backupDir "oportunidades.dump")
if ($LASTEXITCODE -ne 0) { throw "Falha no backup de Oportunidades." }
pg_restore --list (Join-Path $backupDir "oportunidades.dump") | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Backup de Oportunidades inválido." }

$env:PGUSER = "trampolim_empresas"
$env:PGPORT = "5433"
$env:PGDATABASE = "trampolim_empresas_db"
pg_dump --format=custom --file (Join-Path $backupDir "empresas.dump")
if ($LASTEXITCODE -ne 0) { throw "Falha no backup de Empresas." }
pg_restore --list (Join-Path $backupDir "empresas.dump") | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Backup de Empresas inválido." }

$env:PGUSER = "trampolim_capacita"
$env:PGPORT = "5434"
$env:PGDATABASE = "trampolim_capacita_db"
pg_dump --format=custom --file (Join-Path $backupDir "capacita.dump")
if ($LASTEXITCODE -ne 0) { throw "Falha no backup de Capacita." }
pg_restore --list (Join-Path $backupDir "capacita.dump") | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Backup de Capacita inválido." }

Get-ChildItem -Path $backupDir -Filter "*.dump" | Get-FileHash -Algorithm SHA256
```

Além de listar o catálogo com `pg_restore --list`, restaure cada dump em um banco isolado de teste e execute consultas de contagem antes de aprovar o backup para uso. Mantenha os arquivos fora do Git e com acesso restrito.

## Inventário, prévia e cópia

1. Execute `legacy-company-audit.sql` e `legacy-education-audit.sql` no banco Oportunidades e guarde as contagens junto aos backups.
2. Inicie o backend com os três bancos e segredos locais exclusivos configurados.
3. Consulte, em ordem, `GET /api/admin/empresas/legacy-migration/preview`, `GET /api/admin/empresas/legacy-organizations/preview` e `GET /api/admin/capacita/legacy-migration/preview`.
4. Interrompa se houver conflito ou contagens inesperadas. CNPJ inválido, usuário sem correspondência, duplicata e associação sem empresa migrada exigem reconciliação; não force a importação.
5. Execute `POST /api/admin/empresas/legacy-migration/execute`, depois `/api/admin/empresas/legacy-organizations/execute` e `/api/admin/capacita/legacy-migration/execute` apenas após aprovar as prévias.
6. Rode as prévias de novo. As IDs legadas tornam a cópia idempotente. Confirme que a origem permanece inalterada e que os destinos contêm somente as linhas elegíveis.
7. Valide autenticação independente, estado pendente da organização, conteúdo público, permissões, logs sem segredos e os fluxos da interface antes de liberar tráfego.
8. Mantenha o backup, os resultados do inventário, os previews e os resultados de teste como evidência de auditoria. Remova o superadministrador temporário e encerre tokens/sessões usados na migração.

## Reversão

O importador é aditivo e não grava nem apaga na origem. Se a migração falhar antes de aceitar novos dados no destino, suspenda o tráfego daquele destino e restaure seu dump pré-migração **em uma instância/banco novo e isolado**. Valide as contagens e a inicialização da aplicação apontando para essa instância antes de trocar a configuração. Não restaure sobre um banco que já recebeu novas contas, vagas, pagamentos ou conteúdo.

Em produção, uma restauração integral pode perder gravações posteriores ao backup. Qualquer reversão exige janela de manutenção, snapshot/PITR aprovado pelo DBA, inventário das gravações posteriores, reconciliação e plano de retorno validado. Como a origem permanece intacta, manter a aplicação em modo de leitura/retorno ao banco antigo é preferível quando compatível com a mudança; não remova as estruturas novas nem reverta o Flyway manualmente.

## Evidência da execução local em 28/10/2026

- Dumps custom-format dos três bancos foram listados com `pg_restore --list` e tiveram SHA256 calculado antes da cópia.
- Oportunidades preservou 2 empresas, 3 etapas e 18 categorias.
- Empresas recebeu 1 empresa elegível com usuário e verificação pendentes. A segunda empresa, com CNPJ inválido, foi mantida na origem para reconciliação.
- Capacita recebeu 3 etapas e 18 categorias; nenhum conteúdo ou progresso foi inventado ou transferido.
- A prévia de organizações/associações encontrou zero registros.
- Nenhuma estrutura/tabela legada foi removida. A migração ainda precisa ser validada em ambiente de homologação e produção.

## Teste automatizado da migração de associações

Execute no diretório backend: `./mvnw.cmd -Dtest=LegacyOrganizationPostgresTest test` (Windows) ou `./mvnw -Dtest=LegacyOrganizationPostgresTest test` (Linux). O teste usa OPORTUNIDADES_DB_URL/USERNAME/PASSWORD (com fallback DATABASE_URL/USERNAME/PASSWORD) e EMPRESAS_DB_URL/USERNAME/PASSWORD. Sem configuração, utiliza os valores locais de desenvolvimento do Compose. Use bancos locais ou de teste com permissão CREATE SCHEMA; nunca aponte este teste para produção.

O teste cria schemas aleatórios exclusivos nos dois bancos e remove somente esses schemas em finally. Verifica a migração de associação, membro e vínculo, a preservação de vínculo cuja empresa ainda não foi migrada e a reexecução sem duplicatas. O destino usa a migração V8 real; as tabelas de origem são fixtures mínimas. O CI disponibiliza três serviços PostgreSQL independentes antes de executar a suíte.
