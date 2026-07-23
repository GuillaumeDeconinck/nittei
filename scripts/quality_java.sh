#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAVEN_LOCAL_REPO="${MAVEN_LOCAL_REPO:-/tmp/nittei-m2}"
MAVEN_BIN="${MAVEN_BIN:-}"

if [[ -z "$MAVEN_BIN" ]]; then
  if [[ -x "$ROOT_DIR/mvnw" ]]; then
    MAVEN_BIN="$ROOT_DIR/mvnw"
  else
    MAVEN_BIN="mvn"
  fi
fi

if [[ "${1:-}" == "--fix" ]]; then
  shift
  "$MAVEN_BIN" -f "$ROOT_DIR/java/pom.xml" -Dmaven.repo.local="$MAVEN_LOCAL_REPO" spotless:apply "$@"
fi

"$MAVEN_BIN" -f "$ROOT_DIR/java/pom.xml" -Dmaven.repo.local="$MAVEN_LOCAL_REPO" verify "$@"
