#!/usr/bin/env bash
set -euo pipefail

PORT="${1:-5000}"
export NITTEI_HTTP_PORT="$PORT"
export NITTEI__HTTP_PORT="$PORT"

mvn -q -f java/pom.xml -pl nittei-app -am -DskipTests compile
mvn -f java/nittei-app/pom.xml org.springframework.boot:spring-boot-maven-plugin:3.5.0:run
