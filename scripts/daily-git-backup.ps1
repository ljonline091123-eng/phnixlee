$ErrorActionPreference = "Stop"

$logRoot = Join-Path $env:LOCALAPPDATA "CodexGitBackup"
New-Item -ItemType Directory -Path $logRoot -Force | Out-Null
$logFile = Join-Path $logRoot "daily-git-backup.log"
$git = (Get-Command git -ErrorAction Stop).Source
$script:HadFailure = $false

function Write-BackupLog {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    $line = "{0} {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    Add-Content -Path $logFile -Value $line -Encoding UTF8
}

function Invoke-Git {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoPath,

        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$GitArgs
    )

    & $git -C $RepoPath @GitArgs
    if ($LASTEXITCODE -ne 0) {
        throw "git $($GitArgs -join ' ') failed in $RepoPath with exit code $LASTEXITCODE"
    }
}

function Backup-Repo {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$Path,

        [string]$RemoteBranch
    )

    try {
        if (-not (Test-Path -LiteralPath (Join-Path $Path ".git"))) {
            Write-BackupLog "$Name skipped: $Path is not a git repository."
            return
        }

        Write-BackupLog "$Name started."

        $status = & $git -C $Path status --porcelain
        if ($LASTEXITCODE -ne 0) {
            throw "git status failed in $Path with exit code $LASTEXITCODE"
        }

        if (($status | Out-String).Trim().Length -gt 0) {
            Invoke-Git $Path add --all
            $stamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
            Invoke-Git $Path commit -m "Auto backup $Name $stamp"
            Write-BackupLog "$Name committed local changes."
        }
        else {
            Write-BackupLog "$Name has no local changes."
        }

        if ([string]::IsNullOrWhiteSpace($RemoteBranch)) {
            Invoke-Git $Path push
        }
        else {
            Invoke-Git $Path push origin "HEAD:refs/heads/$RemoteBranch"
        }

        Write-BackupLog "$Name pushed."
    }
    catch {
        $script:HadFailure = $true
        Write-BackupLog "$Name failed: $($_.Exception.Message)"
    }
}

$fiveLinesPath = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$workDirName = -join ([char]0x5DE5, [char]0x4F5C)
$devDirName = "AI" + (-join ([char]0x5F00, [char]0x53D1))
$geminiPath = [System.IO.Path]::Combine("C:\", $workDirName, $devDirName, "gemini-quant-agent")

Write-BackupLog "Daily backup run started."
Backup-Repo -Name "gemini-quant-agent" -Path $geminiPath
Backup-Repo -Name "FiveLines" -Path $fiveLinesPath -RemoteBranch "fivelines"
Write-BackupLog "Daily backup run finished."

if ($script:HadFailure) {
    exit 1
}
