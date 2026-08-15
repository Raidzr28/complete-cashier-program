@echo off
REM ---------------------------------------------------------------------------
REM  Kasir Pro - command line build (no Android Studio required)
REM
REM  Usage:
REM    build.bat              -> debug APK
REM    build.bat release      -> release APK (unsigned)
REM    build.bat install      -> build + install on the connected device
REM    build.bat clean        -> delete build output
REM    build.bat <anything>   -> passed straight through to Gradle
REM ---------------------------------------------------------------------------

setlocal

REM  Toolchain locations come from build.env.bat, which is machine-local and
REM  gitignored. Copy build.env.example.bat to build.env.bat to set yours.
REM  If the variables are already exported system-wide, that file is optional.
if exist "%~dp0build.env.bat" call "%~dp0build.env.bat"

if not defined ANDROID_HOME if defined ANDROID_SDK_ROOT set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
if not defined ANDROID_HOME set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"

REM  Fall back to whatever `gradle` is on PATH.
if not defined GRADLE_BIN for %%G in (gradle.bat) do set "GRADLE_BIN=%%~$PATH:G"

if not defined JAVA_HOME (
  echo [ERROR] JAVA_HOME is not set. Copy build.env.example.bat to build.env.bat
  echo         and point JAVA_HOME at a JDK 17 install.
  exit /b 1
)
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [ERROR] JDK 17 not found at %JAVA_HOME%
  exit /b 1
)
if not defined GRADLE_BIN (
  echo [ERROR] Gradle not found. Set GRADLE_BIN in build.env.bat, or put
  echo         gradle.bat on your PATH.
  exit /b 1
)
if not exist "%GRADLE_BIN%" (
  echo [ERROR] Gradle not found at %GRADLE_BIN%
  exit /b 1
)
if not exist "%ANDROID_HOME%\platforms" (
  echo [ERROR] Android SDK not found at %ANDROID_HOME%
  echo         Set ANDROID_HOME in build.env.bat.
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"

set "TASK=%~1"
if "%TASK%"=="" set "TASK=assembleDebug"
if /i "%TASK%"=="release" set "TASK=assembleRelease"
if /i "%TASK%"=="debug"   set "TASK=assembleDebug"
if /i "%TASK%"=="install" set "TASK=installDebug"

echo.
echo   JDK     : %JAVA_HOME%
echo   SDK     : %ANDROID_HOME%
echo   Task    : %TASK%
echo.

REM -p pins the project directory, so this works no matter where it is called from.
call "%GRADLE_BIN%" -p "%~dp0." %TASK% --console=plain
set "RESULT=%ERRORLEVEL%"

if "%RESULT%"=="0" (
  if /i "%TASK%"=="assembleDebug" (
    echo.
    echo   APK: %~dp0app\build\outputs\apk\debug\app-debug.apk
    echo   Install with:  adb install -r "%~dp0app\build\outputs\apk\debug\app-debug.apk"
    echo.
  )
)

endlocal & exit /b %RESULT%
