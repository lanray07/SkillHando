param(
    [Parameter(Mandatory=$true)][string]$Adb,
    [string]$Serial = "",
    [switch]$Airplane
)
$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)
$device = @()
if ($Serial) { $device = @("-s", $Serial) }
$logs = "deliverables/android/build-logs"
$captures = "deliverables/store-assets/device-captures/en-GB/fire-hd10"
New-Item -Force -ItemType Directory $logs,$captures | Out-Null
& $Adb @device install -r app/build/outputs/apk/debug/app-debug.apk
if ($LASTEXITCODE -ne 0) { throw "The debug APK could not be installed" }
& $Adb @device install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if ($LASTEXITCODE -ne 0) { throw "The instrumentation APK could not be installed" }
& $Adb @device shell input keyevent KEYCODE_WAKEUP
& $Adb @device shell wm dismiss-keyguard
if ((& $Adb @device shell dumpsys activity activities) -match "mKeyguardShowing=true") { throw "Unlock the Fire tablet before running UI instrumentation." }
$network = (& $Adb @device shell settings get global airplane_mode_on).Trim()
try {
    if ($Airplane -and $network -ne "1") {
        & $Adb @device shell cmd connectivity airplane-mode enable
        if ($LASTEXITCODE -ne 0) { throw "Airplane mode could not be enabled" }
    }
    $result = & $Adb @device shell am instrument -w -r -e class com.skillhando.app.PracticeJourneyTest com.skillhando.app.test/androidx.test.runner.AndroidJUnitRunner 2>&1
    $result | Set-Content -Encoding utf8 "$logs/fire-journey.txt"
    $result | Write-Output
    & $Adb @device pull /sdcard/Android/data/com.skillhando.app/files/store-captures/. $captures
    if (($result -join "`n") -notmatch 'OK \(1 test\)') { throw "Fire journey failed" }
} finally {
    if ($Airplane -and $network -ne "1") { & $Adb @device shell cmd connectivity airplane-mode disable }
}
