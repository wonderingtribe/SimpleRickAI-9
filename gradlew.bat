@REM This script is the Windows wrapper for Gradle, used by CI/CD systems that run on Windows.
@echo off
set GRADLE_WRAPPER_HOME=%~dp0
set WRAPPER_JAR=%GRADLE_WRAPPER_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%WRAPPER_JAR%" (
echo Error: The Gradle wrapper JAR file is missing.
echo Please ensure 'gradle\wrapper\gradle-wrapper.jar' exists.
exit /b 1
)
if not defined JAVA_HOME (
set JAVA_CMD=java
) else (
set JAVA_CMD="%JAVA_HOME%\bin\java"
)
"%JAVA_CMD%" -Dorg.gradle.appname="%~nx0" -Dgradle.user.home="%GRADLE_WRAPPER_HOME%.gradle" -cp "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
if errorlevel 1 goto error
goto end
:error
echo Gradle build failed with exit code %errorlevel%
:end
