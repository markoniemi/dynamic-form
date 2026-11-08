# Stop hook: compile-check only the modules this turn actually touched.
#
# Run by hand with -DryRun to see the decision without building anything.
#
# Touched paths come from the working tree AND the last commit, because the task
# protocol commits as it goes - a turn's own changes are often already in HEAD
# and `git status` is clean by the time this runs.

param([switch]$DryRun)

$ErrorActionPreference = 'Continue'
Set-Location (Resolve-Path (Join-Path $PSScriptRoot '..\..'))

function Get-TouchedPaths {
    $paths = @()

    $status = git status --porcelain 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }   # git unusable; caller checks everything
    foreach ($line in $status) {
        if ($line.Length -gt 3) { $paths += $line.Substring(3).Trim('"') }
    }

    git rev-parse --verify --quiet HEAD~1 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        $paths += (git diff --name-only HEAD~1 HEAD 2>$null)
    }

    return @($paths | Where-Object { $_ })
}

$touched = Get-TouchedPaths

if ($null -eq $touched) {
    # Could not read git: check everything rather than skip silently.
    $checkFrontend = $true
    $checkBackend = $true
    $reason = 'git unreadable, checking everything'
} else {
    $checkFrontend = [bool](@($touched | Where-Object { $_ -like 'frontend/*' }).Count)
    $checkBackend = [bool](@($touched | Where-Object { $_ -like 'backend/*' -or $_ -like 'api/*' }).Count)
    $reason = "$($touched.Count) touched path(s)"
}

if ($DryRun) {
    "stop-check dry run: $reason"
    "  frontend (lint + tsc): $checkFrontend"
    "  backend  (mvn compile): $checkBackend"
    if ($touched) { "  paths: " + (($touched | Select-Object -Unique) -join ', ') }
    exit 0
}

if (-not $checkFrontend -and -not $checkBackend) { exit 0 }

$failures = @()

if ($checkFrontend) {
    Push-Location frontend
    # lint, not lint:fix: a Stop hook must not rewrite files after the turn has
    # already been reported. The type-aware rules here catch real bugs
    # (no-floating-promises, no-base-to-string), so report them.
    $out = npm run lint 2>&1
    if ($LASTEXITCODE -ne 0) {
        $failures += "eslint failed:`n" + (($out | Select-Object -Last 30) -join "`n")
    }
    $out = npx tsc --noEmit --pretty false 2>&1
    if ($LASTEXITCODE -ne 0) {
        $failures += "tsc failed:`n" + (($out | Select-Object -Last 30) -join "`n")
    }
    Pop-Location
}

if ($checkBackend) {
    # -pl backend -am, never `cd backend`: see .claude/commands/test.md
    # "-Dskip.npm" must stay quoted: PowerShell otherwise splits it at the dot
    # and Maven reads ".npm" as a lifecycle phase.
    $out = mvn -pl backend -am compile -q "-Dskip.npm" 2>&1
    if ($LASTEXITCODE -ne 0) {
        $failures += "mvn compile failed:`n" + (($out | Select-Object -Last 30) -join "`n")
    }
}

if ($failures.Count -gt 0) {
    [Console]::Error.WriteLine($failures -join "`n`n")
    exit 2
}

exit 0
