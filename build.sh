#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="${ROOT}/build/libs/apex-1.0.0-SNAPSHOT.jar"
SEND_DISCORD="true"
RELEASE_NOTES=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-send|--no-webhook) SEND_DISCORD="false"; shift ;;
        --send|--webhook) SEND_DISCORD="true"; shift ;;
        --notes|--changelog) RELEASE_NOTES="$2"; shift 2 ;;
        *) echo "unknown argument: $1" >&2; exit 1 ;;
    esac
done

echo "==> building Apex Anti-Cheat"
cd "$ROOT"
./gradlew build --console=plain

if [[ ! -f "$JAR" ]]; then
    JAR="$(find "${ROOT}/build/libs" -name "apex-*.jar" ! -name "*-sources.jar" | head -n 1)"
fi

echo "==> build artifact: ${JAR}"
md5sum "$JAR"

if [[ "$SEND_DISCORD" == "true" && -f "${ROOT}/scripts/send_build.py" ]]; then
    echo "==> uploading build to Discord"
    python3 "${ROOT}/scripts/send_build.py" --jar "$JAR" ${RELEASE_NOTES:+--notes "$RELEASE_NOTES"} || true
fi

echo "==> done"
