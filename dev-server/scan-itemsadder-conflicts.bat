@echo off
setlocal
cd /d "%~dp0.."

set "IA_CONTENTS=dev-server\server\plugins\ItemsAdder\contents"

if not exist "%IA_CONTENTS%" (
    echo [SpaceSurvival] ItemsAdder contents folder was not found.
    exit /b 0
)

echo [SpaceSurvival] Scanning ItemsAdder material conflicts...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$patterns='^\s*material\s*:\s*(CLOCK|SPYGLASS|COMPASS|RECOVERY_COMPASS)\s*$'; $hits=Get-ChildItem '%IA_CONTENTS%' -Recurse -File -Include *.yml,*.yaml | Select-String -Pattern $patterns; if($hits){ Write-Host '[SpaceSurvival] Potential ItemsAdder 4.0.18 material conflicts:' -ForegroundColor Yellow; $hits | ForEach-Object { Write-Host ('  ' + $_.Path + ':' + $_.LineNumber + '  ' + $_.Line.Trim()) -ForegroundColor Yellow }; exit 2 } else { Write-Host '[SpaceSurvival] No CLOCK/SPYGLASS/COMPASS/RECOVERY_COMPASS declarations found in content YAML.' -ForegroundColor Green; exit 0 }"

set "RESULT=%ERRORLEVEL%"
if "%RESULT%"=="2" (
    echo [SpaceSurvival] These files are outside the spacesurvival namespace unless explicitly listed there.
    echo [SpaceSurvival] Do not delete them blindly; inspect the reported content pack first.
    exit /b 2
)

exit /b %RESULT%
