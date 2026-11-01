@echo off
rem Build better-glue (CMD version, ASCII only, safe for any code page).
rem Usage: build.cmd
rem        build.cmd C:\path\to\jdk21

setlocal
set "ROOT=%~dp0"
cd /d "%ROOT%"

set "JDK=%~1"
if "%JDK%"=="" if defined JAVA_HOME set "JDK=%JAVA_HOME%"
if "%JDK%"=="" if exist "D:\java21\bin\javac.exe" set "JDK=D:\java21"

if "%JDK%"=="" goto :nojdk
if not exist "%JDK%\bin\javac.exe" goto :nojdk

set "JAVA_HOME=%JDK%"
set "PATH=%JDK%\bin;%PATH%"
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%ROOT%.gradle-home"

echo JAVA_HOME        = %JAVA_HOME%
echo GRADLE_USER_HOME = %GRADLE_USER_HOME%
echo.

call "%ROOT%gradlew.bat" --no-daemon --console=plain build
if errorlevel 1 goto :failed

if not exist "%ROOT%build\libs\*.jar" goto :nojar

echo.
echo OK: %ROOT%build\libs
dir /b "%ROOT%build\libs\*.jar"
echo Drop the jar into .minecraft\mods (needs Fabric Loader 0.20.1+ and Fabric API for 1.21.10).
endlocal
exit /b 0

:nojdk
echo ERROR: no JDK 21 found. Pass one: build.cmd C:\path\to\jdk21
endlocal
exit /b 1

:failed
echo.
echo BUILD FAILED.
endlocal
exit /b 1

:nojar
echo.
echo Build finished but no jar was produced.
endlocal
exit /b 1
