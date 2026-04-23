$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$browserCandidates = @(
    'C:\Program Files\Google\Chrome\Application\chrome.exe',
    'C:\Program Files (x86)\Google\Chrome\Application\chrome.exe',
    'C:\Program Files\Microsoft\Edge\Application\msedge.exe',
    'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
    'C:\Program Files\BraveSoftware\Brave-Browser\Application\brave.exe',
    'C:\Program Files (x86)\BraveSoftware\Brave-Browser\Application\brave.exe',
    (Join-Path $env:LOCALAPPDATA 'Vivaldi\Application\vivaldi.exe')
)

$browserPath = $browserCandidates | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1

if (-not $browserPath) {
    throw 'Nenhum navegador Chromium encontrado para executar os testes do Angular.'
}

$env:CHROME_BIN = $browserPath
Write-Host "Usando navegador para testes: $browserPath" -ForegroundColor Cyan

Push-Location $projectRoot
try {
    & .\node_modules\.bin\ng.cmd test --watch=false --browsers=ChromeHeadless

    if ($LASTEXITCODE -ne 0) {
        throw 'Falha na suite de testes do frontend.'
    }
}
finally {
    Pop-Location
}
