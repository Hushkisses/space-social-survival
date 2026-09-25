@echo off
setlocal
cd /d "%~dp0server"

if not exist "paper.jar" (
    echo [SpaceSurvival] dev-server\server\paper.jar was not found.
    exit /b 1
)

java -Xms2G -Xmx4G -jar paper.jar --nogui
endlocal
