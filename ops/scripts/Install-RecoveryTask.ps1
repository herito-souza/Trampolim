#Requires -Version 7.0
[CmdletBinding(SupportsShouldProcess=$true,ConfirmImpact='Medium')]
param(
 [Parameter(Mandatory)][string]$ConfigPath,
 [ValidateRange(15,1440)][int]$IntervalMinutes=30,
 [ValidatePattern('^Trampolim-Backup[-A-Za-z0-9]*$')][string]$TaskName='Trampolim-Backup'
)
$ErrorActionPreference='Stop'
$config=(Resolve-Path -LiteralPath $ConfigPath).Path
$node=(Get-Command node -ErrorAction Stop).Source
$script=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../recovery/recovery.mjs')).Path
foreach($item in @($config,$node,$script)){if($item.Contains('"')){throw 'Unsupported quote in path.'}}
if(Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue){throw 'An existing task will not be overwritten. Review it separately.'}
# Validate paths and required sources without reading/dumping production data.
# Module validation uses an ordinary file URL; no config value becomes executable text.
$moduleUri=([uri]$script).AbsoluteUri
$validation='const m=await import(process.argv[1]);await m.loadConfig(process.argv[2]);'
& $node --input-type=module -e $validation $moduleUri $config
if($LASTEXITCODE -ne 0){throw 'Backup configuration validation failed.'}
if($PSCmdlet.ShouldProcess($TaskName,'Create periodic backup task for the current Windows user')){
 $action=New-ScheduledTaskAction -Execute $node -Argument ('"'+$script+'" backup "'+$config+'"')
 $trigger=New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(5) -RepetitionInterval (New-TimeSpan -Minutes $IntervalMinutes)
 $settings=New-ScheduledTaskSettingsSet -MultipleInstances IgnoreNew -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Hours 2)
 $principal=New-ScheduledTaskPrincipal -UserId ([System.Security.Principal.WindowsIdentity]::GetCurrent().Name) -LogonType Interactive -RunLevel Limited
 Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger -Settings $settings -Principal $principal | Out-Null
 Write-Output 'Backup task registered. Verify the first completed backup and Task Scheduler result; this interactive task requires the user to be logged in.'
}
