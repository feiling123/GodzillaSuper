@echo off
setlocal EnableDelayedExpansion

set "SRC_DIR=%~dp0src\shells\plugins\java\assets"
set "OUTPUT_DIR=%~dp0src\shells\plugins\java\assets"

if not defined JAVA_HOME (
    for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit" /s 2^>nul ^| findstr "JavaHome"') do set "JAVA_HOME=%%b"
)
if not defined JAVA_HOME (
    echo [-] JAVA_HOME not set
    exit /b 1
)
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
if not exist "%JAVAC%" (
    echo [-] javac not found
    exit /b 1
)

echo [+] Compiling JndiBypassModule.java...
"%JAVAC%" -source 1.8 -target 1.8 -encoding UTF-8 -Xlint:none -d "%OUTPUT_DIR%" "%SRC_DIR%\JndiBypassModule.java"
if %errorlevel% neq 0 exit /b 1

if exist "%OUTPUT_DIR%\shells\plugins\java\assets\JndiBypassModule.class" (
    copy /Y "%OUTPUT_DIR%\shells\plugins\java\assets\JndiBypassModule.class" "%OUTPUT_DIR%\JndiBypassModule.classs" >nul
    echo [+] JndiBypassModule.classs
    rd /s /q "%OUTPUT_DIR%\shells" 2>nul
) else (
    echo [-] class not found
    exit /b 1
)
endlocal
