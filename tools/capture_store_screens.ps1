param(
    [Parameter(Mandatory=$true)][string]$Adb,
    [Parameter(Mandatory=$true)][string]$Serial,
    [string[]]$Locales = @("en-GB","en-AU","en-IN","en-US","de-DE","es-ES","es-MX","fr-FR","it-IT","ja-JP","nl-NL","pl-PL","pt-BR","pt-PT","sv-SE"),
    [ValidateSet("fire-hd10","phone-layout")][string]$Profile = "fire-hd10",
    [string]$JapaneseFont
)
$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)
$package = "com.skillhando.app.screenshots"
$logs = "deliverables/android/build-logs"
$adbArgs = @("-s", $Serial)
& $Adb @adbArgs shell input keyevent KEYCODE_WAKEUP
if ((& $Adb @adbArgs shell dumpsys activity activities) -match "mKeyguardShowing=true") {
    throw "Unlock the tablet before running instrumentation."
}
$originalSize = @(& $Adb @adbArgs shell wm size) -match '^Override size: \d+x\d+$' -replace '^Override size: ', ''
$originalDensity = @(& $Adb @adbArgs shell wm density) -match '^Override density: \d+$' -replace '^Override density: ', ''
$keepAwake = (& $Adb @adbArgs shell settings get global stay_on_while_plugged_in).Trim()
& $Adb @adbArgs shell settings put global stay_on_while_plugged_in 7
$failed = @()
try {
    if ($Profile -eq "phone-layout") {
        & $Adb @adbArgs shell wm size 1080x1920
        & $Adb @adbArgs shell wm density 440
    }
    foreach($locale in $Locales) {
        if ($locale -notmatch '^[a-z]{2}-[A-Z]{2}$') { throw "Invalid locale $locale" }
        $raw = "deliverables/store-assets/device-captures/$locale/$Profile"
        New-Item -ItemType Directory -Force $raw | Out-Null
        $result = & $Adb @adbArgs shell am instrument -w -r -e locale $locale -e class com.skillhando.app.StoreScreenshotTest "${package}.test/androidx.test.runner.AndroidJUnitRunner" 2>&1
        $result | Set-Content -Encoding utf8NoBOM "$logs/store-$Profile-$locale.txt"
        if (($result -join "`n") -notmatch 'OK \(1 test\)') {
            Write-Output "FAIL $Profile $locale"
            $failed += $locale
            continue
        }
        & $Adb @adbArgs pull "/sdcard/Android/data/$package/files/store-shots/$locale/." $raw
        if ($LASTEXITCODE -ne 0) { throw "Could not pull $locale screenshots." }
        $font = @()
        if ($locale -eq "ja-JP") {
            if (!$JapaneseFont) { throw "Specify -JapaneseFont for Japanese headlines." }
            $font = @("--font", $JapaneseFont)
        }
        python tools/compose_screenshots.py $raw --locale $locale --device $Profile @font
        if ($LASTEXITCODE -ne 0) { throw "Could not compose $locale screenshots." }
    }
    if ($failed.Count) { throw ("Screenshot testing failed for " + ($failed -join ", ")) }
} finally {
    if ($Profile -eq "phone-layout") {
        & $Adb @adbArgs shell wm size $(if ($originalSize) {$originalSize[0]} else {"reset"})
        & $Adb @adbArgs shell wm density $(if ($originalDensity) {$originalDensity[0]} else {"reset"})
    }
    if ($keepAwake -eq "null") { & $Adb @adbArgs shell settings delete global stay_on_while_plugged_in }
    else { & $Adb @adbArgs shell settings put global stay_on_while_plugged_in $keepAwake }
}
