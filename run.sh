#!/bin/bash

APP_NAME="payment-gateway-cip"
APP_PORT=8080

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
JAVA_BIN="$JAVA_HOME/bin/java"

JAVA_OPTS="
-Xms512m
-Xmx1024m
-Dfile.encoding=UTF-8
"

echo "======================================="
echo "Starting $APP_NAME"
echo "======================================="

echo ""
echo "Checking port $APP_PORT..."

PID=$(lsof -ti tcp:$APP_PORT)

if [ -n "$PID" ]; then
    echo "Port $APP_PORT is already used by PID: $PID"
    echo "Killing existing process..."
    kill -9 $PID
    sleep 2
    echo "Process killed."
else
    echo "Port $APP_PORT is available."
fi

echo ""
echo "Java Version:"
$JAVA_BIN -version

echo ""
echo "Maven Build Started..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo ""
    echo "======================================="
    echo "BUILD FAILED"
    echo "======================================="
    exit 1
fi

echo ""
echo "Searching executable jar..."

JAR_FILE=$(find target -name "*.jar" ! -name "*original*" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "Jar file not found!"
    exit 1
fi

echo "Jar Found: $JAR_FILE"

echo ""
echo "======================================="
echo "Starting Application"
echo "======================================="

$JAVA_BIN $JAVA_OPTS -jar "$JAR_FILE"