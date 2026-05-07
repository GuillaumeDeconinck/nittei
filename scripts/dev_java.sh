#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PORT="${1:-5000}"
MAVEN_BIN="${MAVEN_BIN:-}"

if [[ -z "$MAVEN_BIN" ]]; then
  if [[ -x "$ROOT_DIR/mvnw" ]]; then
    MAVEN_BIN="$ROOT_DIR/mvnw"
  else
    MAVEN_BIN="mvn"
  fi
fi

export NITTEI_HTTP_PORT="$PORT"
export NITTEI__HTTP_PORT="$PORT"

"$MAVEN_BIN" -q -f "$ROOT_DIR/java/pom.xml" -pl nittei-app -am -DskipTests compile
"$MAVEN_BIN" -f "$ROOT_DIR/java/pom.xml" -pl nittei-app -am org.springframework.boot:spring-boot-maven-plugin:3.5.0:run
