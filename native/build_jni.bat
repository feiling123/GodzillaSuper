@echo off
REM RASP Bypass JNI Library Build Script for Windows
REM Usage: build_jni.bat [target]
REM Targets: win-x64, win-x86, all

setlocal EnableDelayedExpansion

set "SRC_DIR=%~dp0"
REM Resolve to absolute path (MinGW ld often fails on ...\native\..\src\...)
for %%I in ("%SRC_DIR%..\src\shells\plugins\java\assets") do set "OUTPUT_DIR=%%~fI"

if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

for %%I in ("%SRC_DIR%rasp_bypass_jni.c") do set "JNI_C_SRC=%%~fI"

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

REM Check for MinGW
where gcc >nul 2>&1
if %errorlevel% neq 0 (
    echo [-] GCC not found. Please install MinGW-w64 and add to PATH.
    exit /b 1
)

if "%1"=="" goto :all
if "%1"=="win-x64" goto :win_x64
if "%1"=="win-x86" goto :win_x86
if "%1"=="all" goto :all
echo [-] Unknown target: %1
echo Usage: %0 [win-x64^|win-x86^|all]
exit /b 1

:win_x64
echo [+] Building for Windows x64...
gcc -shared -O2 -o "%OUTPUT_DIR%\rasp_bypass_win_x64.dll" "%JNI_C_SRC%" -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32"
if %errorlevel% neq 0 (
    echo [-] Windows x64 build failed
    exit /b 1
)
echo [+] Windows x64 build complete: %OUTPUT_DIR%\rasp_bypass_win_x64.dll
goto :end

:win_x86
echo [+] Building for Windows x86...
gcc -shared -m32 -O2 -o "%OUTPUT_DIR%\rasp_bypass_win_x86.dll" "%JNI_C_SRC%" -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32"
if %errorlevel% neq 0 (
    echo [-] Windows x86 build failed
    exit /b 1
)
echo [+] Windows x86 build complete: %OUTPUT_DIR%\rasp_bypass_win_x86.dll
goto :end

:all
call :win_x64
call :win_x86
goto :end

:end
echo [+] Build complete!
endlocal
