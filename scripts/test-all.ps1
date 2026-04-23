$ErrorActionPreference = 'Stop'

function Invoke-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Label,
        [Parameter(Mandatory = $true)]
        [scriptblock]$Action
    )

    Write-Host ""
    Write-Host "==> $Label" -ForegroundColor Cyan
    & $Action

    if ($LASTEXITCODE -ne 0) {
        throw "Falha em: $Label"
    }
}

$root = Split-Path -Parent $PSScriptRoot

Push-Location (Join-Path $root 'cleanifyai-api')
try {
    Invoke-Step -Label 'Backend - mvnw clean test' -Action { .\mvnw.cmd clean test }
}
finally {
    Pop-Location
}

Push-Location (Join-Path $root 'cleanifyai-web')
try {
    Invoke-Step -Label 'Frontend - npm run build' -Action { npm run build }
    Invoke-Step -Label 'Frontend - npm run test:ci' -Action { npm run test:ci }
}
finally {
    Pop-Location
}

Write-Host ""
Write-Host "Suite completa executada com sucesso." -ForegroundColor Green
