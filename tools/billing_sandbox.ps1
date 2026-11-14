param(
    [Parameter(Mandatory=$true)][string]$Adb,
    [Parameter(Mandatory=$true)][string]$Serial,
    [Parameter(Mandatory=$true)][scriptblock]$Check
)
$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)
$adbArgs = @("-s", $Serial)
$remote = "/sdcard/amazon.sdktester.json"
$originalMode = (& $Adb @adbArgs shell getprop debug.amazon.sandboxmode).Trim()
if (!(& $Adb @adbArgs shell pm path com.amazon.sdktestclient)) {
    throw "Install Amazon App Tester from the Appstore first."
}
$folder = Join-Path ([IO.Path]::GetTempPath()) ("skillhando-billing-" + [Guid]::NewGuid())
New-Item -ItemType Directory $folder | Out-Null
$backup = Join-Path $folder "amazon-original.json"
$merged = Join-Path $folder "amazon.sdktester.json"
& $Adb @adbArgs pull $remote $backup 2>$null | Out-Null
$existing = $LASTEXITCODE -eq 0
$catalogue = if ($existing) { Get-Content -Raw $backup | ConvertFrom-Json -AsHashtable } else { @{} }
$fixture = Get-Content -Raw tools/amazon.sdktester.skillhando.json | ConvertFrom-Json -AsHashtable
foreach($sku in $fixture.Keys) { $catalogue[$sku] = $fixture[$sku] }
$catalogue | ConvertTo-Json -Depth 32 | Set-Content -Encoding utf8NoBOM $merged
$forwarded = $false
$fixtureCopied = $false
try {
    $mapping = (& $Adb @adbArgs reverse --list) -match 'tcp:18744'
    if ($mapping) { throw "Local verifier port 18744 is already forwarded." }
    & $Adb @adbArgs reverse tcp:18744 tcp:18744
    if ($LASTEXITCODE) { throw "Could not forward the sandbox verifier." }
    $forwarded = $true
    & $Adb @adbArgs push $merged $remote
    if ($LASTEXITCODE) { throw "Could not copy the test SKU." }
    $fixtureCopied = $true
    & $Adb @adbArgs shell setprop debug.amazon.sandboxmode debug
    if ((& $Adb @adbArgs shell getprop debug.amazon.sandboxmode).Trim() -ne "debug") {
        throw "Amazon sandbox mode could not be enabled."
    }
    & $Adb @adbArgs shell am force-stop com.amazon.sdktestclient
    & $Adb @adbArgs shell am start -W -n com.amazon.sdktestclient/.MainActivity
    & $Check
} finally {
    & $Adb @adbArgs shell am force-stop com.skillhando.app.billingtest
    & $Adb @adbArgs shell am force-stop com.amazon.sdktestclient
    if ($fixtureCopied) {
        if ($existing) { & $Adb @adbArgs push $backup $remote }
        else { & $Adb @adbArgs shell rm -f $remote }
    }
    $mode = if ($originalMode) { $originalMode } else { "none" }
    & $Adb @adbArgs shell setprop debug.amazon.sandboxmode $mode
    if ($forwarded) { & $Adb @adbArgs reverse --remove tcp:18744 }
    Remove-Item -LiteralPath $backup,$merged -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $folder -ErrorAction SilentlyContinue
}
