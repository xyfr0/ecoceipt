$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$assetDir = Join-Path $projectRoot 'app\src\main\assets\figma'
New-Item -ItemType Directory -Force -Path $assetDir | Out-Null

$assets = @{
    '8d105.png' = 'https://www.figma.com/api/mcp/asset/0c6179e4-3b88-4fb2-a3a5-d366e023c948/8d105.png'
    '53454.png' = 'https://www.figma.com/api/mcp/asset/0c6179e4-3b88-4fb2-a3a5-d366e023c948/53454.png'
    '7d5c6.png' = 'https://www.figma.com/api/mcp/asset/0c6179e4-3b88-4fb2-a3a5-d366e023c948/7d5c6.png'
    'c7474.svg' = 'https://www.figma.com/api/mcp/asset/f7bfc483-7161-4c1d-8fb1-57bb3d14b98f/c7474.svg'
    '60c30.svg' = 'https://www.figma.com/api/mcp/asset/a8886453-6aa5-49e9-972b-d7eae65b91fb/60c30.svg'
    'd2a24.svg' = 'https://www.figma.com/api/mcp/asset/a8886453-6aa5-49e9-972b-d7eae65b91fb/d2a24.svg'
    'eb40c.svg' = 'https://www.figma.com/api/mcp/asset/a8886453-6aa5-49e9-972b-d7eae65b91fb/eb40c.svg'
}
foreach ($name in $assets.Keys) {
    $target = Join-Path $assetDir $name
    if ((Test-Path -LiteralPath $target) -and (Get-Item -LiteralPath $target).Length -gt 0) { continue }
    $partial = "$target.part"
    & curl.exe -fL --connect-timeout 15 --max-time 60 --retry 1 $assets[$name] -o $partial
    if ($LASTEXITCODE -ne 0) {
        throw "Gagal mengunduh $name. Periksa koneksi. Jika URL Figma kedaluwarsa, minta ekspor baru dari node yang tercantum di docs/FIGMA.md."
    }
    if ((Get-Item -LiteralPath $partial).Length -eq 0) { throw "Aset $name kosong." }
    Move-Item -LiteralPath $partial -Destination $target -Force
    Write-Output "Tersimpan: $name"
}
Write-Output 'Aset Figma siap. File lokal ini dapat disimpan dalam repository.'
