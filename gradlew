#!/bin/bash

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

DEFAULT_JVM_OPTS=(-Xmx64m -Xms64m)

# Resolve APP_HOME
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Find java
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/bin/java" ]; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
        exit 1
    fi
else
    if [ -x /usr/bin/java ] ; then
        JAVACMD=/usr/bin/java
    else
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
        exit 1
    fi
fi

exec "$JAVACMD" "${DEFAULT_JVM_OPTS[@]}" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
