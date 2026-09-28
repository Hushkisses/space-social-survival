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

    if exist "%IA_TARGET%\textures\font\hud" (
        echo [SpaceSurvival] Cropping mission HUD images to native panel bounds...
        powershell -NoProfile -ExecutionPolicy Bypass -Command ^
          "$ErrorActionPreference='Stop'; Add-Type -AssemblyName System.Drawing; try { Get-ChildItem '%IA_TARGET%\textures\font\hud' -Filter 'mission_*.png' | ForEach-Object { $path=$_.FullName; $tmp=$path + '.cropped.tmp.png'; $src=[Drawing.Bitmap]::new($path); try { $cropWidth=[Math]::Min(176,$src.Width); $cropHeight=[Math]::Min(84,$src.Height); $rect=[Drawing.Rectangle]::new(0,0,$cropWidth,$cropHeight); $crop=$src.Clone($rect,$src.PixelFormat); try { $crop.Save($tmp,[Drawing.Imaging.ImageFormat]::Png); } finally { $crop.Dispose() } } finally { $src.Dispose() }; Move-Item -LiteralPath $tmp -Destination $path -Force } } catch { Write-Error $_; exit 1 }"
        if errorlevel 1 (
            echo [SpaceSurvival] Mission HUD crop failed.
            exit /b 1
        )
    )

    echo [SpaceSurvival] ItemsAdder content sync complete.
)

echo [SpaceSurvival] Deploy complete.
echo [SpaceSurvival] After ItemsAdder asset changes, start the server and run /iazip.
endlocal
