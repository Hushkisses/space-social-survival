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

echo [SpaceSurvival] Deploy complete.
endlocal
