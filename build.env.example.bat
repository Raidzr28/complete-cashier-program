@echo off
REM ---------------------------------------------------------------------------
REM  Template for machine-local toolchain paths.
REM
REM  Copy this file to build.env.bat and point the three variables at your own
REM  install locations. build.env.bat is gitignored, so your paths stay local.
REM
REM      copy build.env.example.bat build.env.bat
REM
REM  You can skip this file entirely if JAVA_HOME and ANDROID_HOME are already
REM  set system-wide and `gradle` is on your PATH.
REM ---------------------------------------------------------------------------

set "JAVA_HOME=C:\path\to\jdk-17"
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "GRADLE_BIN=C:\path\to\gradle-8.11.1\bin\gradle.bat"
