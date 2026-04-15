@echo off
REM RASP Bypass Complete Build Script
REM Compiles all server-side modules and packages them

setlocal EnableDelayedExpansion

set "SRC_DIR=%~dp0src\shells\plugins\java\assets"
set "OUTPUT_DIR=%~dp0src\shells\plugins\java\assets"
set "TEMP_DIR=%~dp0temp_compile"

echo ========================================
echo RASP Bypass Complete Build Script
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

REM Create temp directory
if not exist "%TEMP_DIR%" mkdir "%TEMP_DIR%"

echo [+] Compiling RaspBypassModule.java...
"%JAVAC%" -source 1.8 -target 1.8 -encoding UTF-8 -Xlint:none -d "%TEMP_DIR%" "%SRC_DIR%\RaspBypassModule.java" 2>&1
if %errorlevel% neq 0 (
    echo [-] RaspBypassModule compilation failed!
    goto :cleanup
)
echo [+] RaspBypassModule compiled successfully!

echo [+] Compiling RaspBypassUtils.java...
"%JAVAC%" -source 1.8 -target 1.8 -encoding UTF-8 -Xlint:none -d "%TEMP_DIR%" "%SRC_DIR%\RaspBypassUtils.java" 2>&1
if %errorlevel% neq 0 (
    echo [-] RaspBypassUtils compilation failed!
    goto :cleanup
)
echo [+] RaspBypassUtils compiled successfully!

echo [+] Compiling MemShellInjector.java...
"%JAVAC%" -source 1.8 -target 1.8 -encoding UTF-8 -Xlint:none -d "%TEMP_DIR%" "%SRC_DIR%\MemShellInjector.java" 2>&1
if %errorlevel% neq 0 (
    echo [-] MemShellInjector compilation failed!
    echo [!] MemShellInjector requires servlet-api.jar, skipping...
)

REM Package class files
echo.
echo [+] Packaging class files...

for /r "%TEMP_DIR%" %%f in (*.class) do (
    set "CLASS_FILE=%%f"
    set "CLASS_NAME=%%~nf"
    
    echo [+] Processing: !CLASS_NAME!.class
    
    copy /Y "!CLASS_FILE!" "%OUTPUT_DIR%\!CLASS_NAME!.classs" >nul
    echo [+] Created: %OUTPUT_DIR%\!CLASS_NAME!.classs
)

:cleanup
REM Clean up temp directory
if exist "%TEMP_DIR%" rd /s /q "%TEMP_DIR%" 2>nul

echo.
echo ========================================
echo Build completed!
echo ========================================
echo.
echo Generated files in %OUTPUT_DIR%:
for %%f in ("%OUTPUT_DIR%\*.classs") do (
    echo   - %%~nxf
)

endlocal
