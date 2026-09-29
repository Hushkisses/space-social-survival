@echo off
setlocal

cd /d "%~dp0.."

echo [SpaceSurvival] Running tests...
call gradlew.bat test
if errorlevel 1 (
    echo [SpaceSurvival] Tests failed. Deploy cancelled.
    exit /b 1
)

echo [SpaceSurvival] Building plugin...
call gradlew.bat :paper-plugin:build
if errorlevel 1 (
    echo [SpaceSurvival] Build failed. Deploy cancelled.
    exit /b 1
)

if not exist "dev-server\server\plugins" mkdir "dev-server\server\plugins"

for %%F in ("paper-plugin\build\libs\space-social-survival-*.jar") do (
    copy /Y "%%~fF" "dev-server\server\plugins\SpaceSurvival.jar" >nul
    if errorlevel 1 (
        echo [SpaceSurvival] Copy failed.
        exit /b 1
    )
)

if not exist "dev-server\server\plugins\SpaceSurvival.jar" (
    echo [SpaceSurvival] Built plugin jar was not found.
    exit /b 1
)

set "IA_SOURCE=dev-server\itemsadder-content\spacesurvival"
set "IA_TARGET=dev-server\server\plugins\ItemsAdder\contents\spacesurvival"

if exist "%IA_SOURCE%" (
    echo [SpaceSurvival] Syncing ItemsAdder content...
    if exist "%IA_TARGET%" rmdir /S /Q "%IA_TARGET%"
    mkdir "%IA_TARGET%" >nul 2>nul
    xcopy "%IA_SOURCE%\*" "%IA_TARGET%\" /E /I /Y >nul
    if errorlevel 1 (
        echo [SpaceSurvival] ItemsAdder content sync failed.
        exit /b 1
    )

    for /R "%IA_TARGET%\textures" %%F in (*.png.b64) do (
        powershell -NoProfile -ExecutionPolicy Bypass -Command ^
          "$src='%%~fF'; $dst=$src.Substring(0,$src.Length-4); [IO.File]::WriteAllBytes($dst,[Convert]::FromBase64String([IO.File]::ReadAllText($src)));"
        if errorlevel 1 (
            echo [SpaceSurvival] ItemsAdder image decode failed: %%~fF
            exit /b 1
        )
        del /Q "%%~fF"
    )

    echo [SpaceSurvival] ItemsAdder content sync complete.
    call dev-server\scan-itemsadder-conflicts.bat
    if errorlevel 3 (
        echo [SpaceSurvival] ItemsAdder conflict scan failed unexpectedly.
    )
)

echo [SpaceSurvival] Deploy complete.
echo [SpaceSurvival] After ItemsAdder asset changes, start the server and run /iazip.
endlocal
