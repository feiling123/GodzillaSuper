@echo off
setlocal
set ROOT=%~dp0
set SRC=%ROOT%src\shells\payloads\java\payload.java
set OUT=%ROOT%build\tmp_payload_assets
set ASSETS=%ROOT%src\shells\payloads\java\assets
if not exist "%OUT%" mkdir "%OUT%"
javac -encoding UTF-8 -source 8 -target 8 -d "%OUT%" "%SRC%"
if errorlevel 1 exit /b 1
if not exist "%ASSETS%" mkdir "%ASSETS%"
copy /Y "%OUT%\shells\payloads\java\payload.class" "%ASSETS%\payload.classs"
copy /Y "%OUT%\shells\payloads\java\payload.class" "%ASSETS%\payload.class"
echo OK: %ASSETS%\payload.classs
