param([switch]$TestRestore,[switch]$Performance,[ValidateRange(1,3600)][int]$SoakSeconds=20,[ValidatePattern('^[a-z0-9-]*$')][string]$EvidenceName='', [ValidatePattern('^[A-Za-z0-9_.*,]*$')][string]$TestPattern='')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
# This harness is intentionally local-only and never restores into an existing database.
$endpoint=(& docker context inspect --format '{{.Endpoints.docker.Host}}').Trim()
if ($LASTEXITCODE -ne 0 -or $endpoint -notmatch '^(npipe|unix)://' -or ($env:DOCKER_HOST -and $env:DOCKER_HOST -notmatch '^(npipe|unix)://')) { throw 'Use a local Docker socket for this test harness.' }
$id=[guid]::NewGuid().ToString('N')
$schema='audit_security_'+$id
$restoreDb='audit_restore_'+$id
if ($schema -notmatch '^audit_security_[0-9a-f]{32}$' -or $restoreDb -notmatch '^audit_restore_[0-9a-f]{32}$') {throw 'Invalid isolated test targets.'}
$temporary=Join-Path ([IO.Path]::GetTempPath()) ('trampolim-security-'+$id)
New-Item -ItemType Directory -Path $temporary | Out-Null
$targets=@(
 @{Container='trampolim-db'; Database='trampolim_db'; Username='root'; Password='root'; Prefix='spring.datasource'; Trail='oportunidades'; Port=5432},
 @{Container='trampolim-empresas-db'; Database='trampolim_empresas_db'; Username='trampolim_empresas'; Password='dev_empresas_only'; Prefix='trampolim.datasource.empresas'; Trail='empresas'; Port=5433},
 @{Container='trampolim-capacita-db'; Database='trampolim_capacita_db'; Username='trampolim_capacita'; Password='dev_capacita_only'; Prefix='trampolim.datasource.capacita'; Trail='capacita'; Port=5434}
)
function Query($target,$database,$sql){
 $value=& docker exec $target.Container psql -X -U $target.Username -d $database -v ON_ERROR_STOP=1 -t -A -c $sql
 if ($LASTEXITCODE -ne 0) {throw ('Isolated SQL check failed for '+$target.Trail)}
 return (($value -join [Environment]::NewLine).Trim())
}
$created=@();$backups=@();$testExit=1
$environmentNames=@('DATABASE_URL','DATABASE_USERNAME','DATABASE_PASSWORD','OPORTUNIDADES_DB_URL','OPORTUNIDADES_DB_USERNAME','OPORTUNIDADES_DB_PASSWORD','EMPRESAS_DB_URL','EMPRESAS_DB_USERNAME','EMPRESAS_DB_PASSWORD','CAPACITA_DB_URL','CAPACITA_DB_USERNAME','CAPACITA_DB_PASSWORD','SPRING_PROFILES_ACTIVE')
$previous=@{};foreach($name in $environmentNames){$previous[$name]=[Environment]::GetEnvironmentVariable($name,'Process')}
try {
 $env:SPRING_PROFILES_ACTIVE='test'
 $evidence=Join-Path $projectRoot $(if($EvidenceName){'ops/'+$EvidenceName}elseif($Performance){'ops/backend-performance-2026-11-06'}else{'ops/backend-security-2026-11-06'})
 New-Item -ItemType Directory -Path $evidence -Force | Out-Null
 $argsList=@('--batch-mode','-Dspring.jpa.hibernate.ddl-auto=validate','-Dapp.mail.enabled=false','-Dcommunications.worker-enabled=false','-Doperations.catalog-jobs-enabled=false','-Dfuture.poll-enabled=false','-Dpassport.delivery-enabled=false',('-Dcapacita.media.root='+$temporary+'/media'),('-Dhub.logo-directory='+$temporary+'/logos'))
 if($TestPattern){$argsList+=('-Dtest='+$TestPattern)}
 if($Performance){$argsList+='-Daudit.performance.enabled=true';$argsList+=('-Daudit.performance.soak-seconds='+$SoakSeconds);$argsList+=('-Daudit.performance.output='+($evidence -replace '\\','/'))}
 foreach($target in $targets){
  Query $target $target.Database ('CREATE SCHEMA '+$schema) | Out-Null
  $created+=$target
  $url='jdbc:postgresql://localhost:'+$target.Port+'/'+$target.Database
  $prefix=$target.Trail.ToUpperInvariant()
  [Environment]::SetEnvironmentVariable($prefix+'_DB_URL',$url,'Process');[Environment]::SetEnvironmentVariable($prefix+'_DB_USERNAME',$target.Username,'Process');[Environment]::SetEnvironmentVariable($prefix+'_DB_PASSWORD',$target.Password,'Process')
  if($target.Trail -eq 'oportunidades'){$env:DATABASE_URL=$url;$env:DATABASE_USERNAME=$target.Username;$env:DATABASE_PASSWORD=$target.Password}
  $argsList+=('-D'+$target.Prefix+'.url='+$url+'?currentSchema='+$schema)
  $argsList+=('-D'+$target.Prefix+'.username='+$target.Username)
  $argsList+=('-D'+$target.Prefix+'.password='+$target.Password)
 }
 Push-Location (Join-Path $projectRoot 'backend')
 try { & .\mvnw.cmd @argsList verify; $testExit=$LASTEXITCODE } finally {Pop-Location}
 if ($testExit -ne 0) {throw 'Backend verification failed. Review surefire reports.'}
 if ($TestRestore) {foreach($target in $targets){
  $dump='/tmp/trampolim-security-'+$id+'-'+$target.Trail+'.dump'
  $restored=$false
  try {
   & docker exec $target.Container pg_dump -U $target.Username -d $target.Database --format=custom --no-owner --no-acl ('--schema='+$schema) ('--file='+$dump)
   if($LASTEXITCODE -ne 0){throw 'Synthetic backup failed.'}
   & docker exec $target.Container createdb -U $target.Username $restoreDb
   if($LASTEXITCODE -ne 0){throw 'Isolated restore target creation failed.'};$restored=$true
   & docker exec $target.Container pg_restore -U $target.Username -d $restoreDb --no-owner --no-acl --exit-on-error $dump
   if($LASTEXITCODE -ne 0){throw 'Synthetic restore failed.'}
   $tablesSql="SELECT count(*) FROM information_schema.tables WHERE table_schema='$schema'"
   $historySql="SELECT count(*) FROM $schema.flyway_schema_history WHERE success"
   $tables=Query $target $target.Database $tablesSql;$history=Query $target $target.Database $historySql
   if((Query $target $restoreDb $tablesSql) -ne $tables -or (Query $target $restoreDb $historySql) -ne $history){throw 'Restored schema validation failed.'}
   $backups+=@{trail=$target.Trail;tables=[int]$tables;successfulMigrations=[int]$history;restoredIntoNewDatabase=$true}
  } finally {
   if($restored){& docker exec $target.Container dropdb -U $target.Username $restoreDb;if($LASTEXITCODE -ne 0){throw 'Temporary restore target cleanup failed.'}}
   if($dump -match '^/tmp/trampolim-security-[0-9a-f]{32}-(oportunidades|empresas|capacita)\.dump$'){& docker exec $target.Container rm -f -- $dump}
  }
 }}
 New-Item -ItemType Directory -Path $evidence -Force | Out-Null
 @{testSchema=$schema;realDataUsed=$false;temporaryMedia=$true;verifyExitCode=$testExit;backupRestore=$backups} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $evidence 'isolated-tests.json') -Encoding utf8
} finally {
 foreach($target in $created){Query $target $target.Database ('DROP SCHEMA '+$schema+' CASCADE') | Out-Null}
 foreach($name in $environmentNames){[Environment]::SetEnvironmentVariable($name,$previous[$name],'Process')}
 $expected=[IO.Path]::GetFullPath((Join-Path ([IO.Path]::GetTempPath()) ('trampolim-security-'+$id)))
 if([IO.Path]::GetFullPath($temporary) -eq $expected -and (Test-Path -LiteralPath $temporary) -and -not (Get-Item -LiteralPath $temporary).LinkType){Remove-Item -LiteralPath $temporary -Recurse -Force}
}
