$ErrorActionPreference = "Stop"

$repoPath = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$git = (Get-Command git -ErrorAction Stop).Source
$dateStamp = Get-Date -Format "yyyyMMdd"
$backupBranch = "backup/fivelines-$dateStamp-pre-change"
$backupRef = "refs/heads/$backupBranch"

if (-not (Test-Path -LiteralPath (Join-Path $repoPath ".git"))) {
    throw "FiveLines Git repository was not found at $repoPath"
}

$currentBranch = (& $git -C $repoPath branch --show-current).Trim()
if ($LASTEXITCODE -ne 0 -or $currentBranch -ne "fivelines") {
    throw "Daily backup must run from the fivelines branch; current branch is '$currentBranch'."
}

& $git -C $repoPath fetch origin --prune
if ($LASTEXITCODE -ne 0) {
    throw "Unable to fetch origin before the daily backup."
}

$existingRef = & $git -C $repoPath ls-remote --heads origin $backupRef
if ($LASTEXITCODE -ne 0) {
    throw "Unable to check whether today's remote backup already exists."
}
if (-not [string]::IsNullOrWhiteSpace(($existingRef | Out-String))) {
    Write-Host "Today's pre-change backup already exists: origin/$backupBranch"
    exit 0
}

$status = & $git -C $repoPath status --porcelain
if ($LASTEXITCODE -ne 0) {
    throw "Unable to read the FiveLines working tree status."
}
if (-not [string]::IsNullOrWhiteSpace(($status | Out-String))) {
    & $git -C $repoPath add --all
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to stage the current source snapshot."
    }
    & $git -C $repoPath commit -m "Daily source backup $dateStamp"
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to commit the current source snapshot."
    }
}

& $git -C $repoPath push origin "HEAD:$backupRef"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to push $backupBranch to origin."
}

Write-Host "Daily pre-change backup completed: origin/$backupBranch"
