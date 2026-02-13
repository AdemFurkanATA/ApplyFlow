#!/bin/sh
# Maven Wrapper script for Unix-like systems
# Downloads and caches Maven, then delegates to it

set -e

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    DIST_URL=$(grep "distributionUrl" "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)
    WRAPPER_URL=$(grep "wrapperUrl" "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)
fi

DIST_URL="${DIST_URL:-https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip}"
MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-3.9.9"

if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven..."
    mkdir -p "$MAVEN_HOME"
    TMP_FILE=$(mktemp)
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL "$DIST_URL" -o "$TMP_FILE"
    elif command -v wget > /dev/null 2>&1; then
        wget -q "$DIST_URL" -O "$TMP_FILE"
    else
        echo "Error: curl or wget required" >&2
        exit 1
    fi
    unzip -q "$TMP_FILE" -d "$MAVEN_HOME"
    rm "$TMP_FILE"
fi

MVN_CMD=$(find "$MAVEN_HOME" -name "mvn" -path "*/bin/mvn" | head -1)
exec "$MVN_CMD" "$@"
