#!/usr/bin/env bash
This script is the UNIX/Linux/macOS wrapper. It detects the required Gradle version
specified in the properties file and downloads/uses it automatically.
APP_BASE_NAME=$(basename "$0")
Determine the directory of the script
GRADLE_WRAPPER_HOME=(dirname "(realpath "$0")")
Set the path to the wrapper JAR
WRAPPER_JAR="$GRADLE_WRAPPER_HOME/gradle/wrapper/gradle-wrapper.jar"
Check for existence of the wrapper JAR
if [ ! -f "$WRAPPER_JAR" ]; then
echo "Error: The Gradle wrapper JAR file is missing."
echo "Please ensure 'gradle/wrapper/gradle-wrapper.jar' exists."
exit 1
fi
Define the Java command
if [ -z "$JAVA_HOME" ]; then
JAVA_CMD="java"
else
JAVA_CMD="$JAVA_HOME/bin/java"
fi
Execute the Gradle wrapper
exec "$JAVA_CMD" 
-Dorg.gradle.appname="$APP_BASE_NAME" 
-Dgradle.user.home="$GRADLE_WRAPPER_HOME/.gradle" 
-cp "WRAPPER_JAR" \
org.gradle.wrapper.GradleWrapperMain "@"
