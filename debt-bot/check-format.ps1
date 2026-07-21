#!/usr/bin/env pwsh
# Check Python code style with ruff and black
# Usage: .\check-format.ps1 [-Fix]

param(
    [switch]$Fix
)

$debtBotDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "🔍 Checking Python code style with ruff and black..." -ForegroundColor Cyan

if ($Fix) {
    Write-Host "📝 Auto-formatting with black..." -ForegroundColor Yellow
    black $debtBotDir --line-length=100
    if ($LASTEXITCODE -ne 0) { exit 1 }

    Write-Host "🧹 Linting with ruff (auto-fix)..." -ForegroundColor Yellow
    ruff check $debtBotDir --fix
    if ($LASTEXITCODE -ne 0) { exit 1 }

    Write-Host "✅ Code formatted and linted!" -ForegroundColor Green
}
else {
    Write-Host "🧹 Linting with ruff..." -ForegroundColor Yellow
    ruff check $debtBotDir
    if ($LASTEXITCODE -ne 0) { exit 1 }

    Write-Host "🎨 Checking format with black..." -ForegroundColor Yellow
    black $debtBotDir --check --line-length=100
    if ($LASTEXITCODE -ne 0) { exit 1 }

    Write-Host "✅ All checks passed!" -ForegroundColor Green
}
