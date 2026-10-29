#!/usr/bin/env bash
# Builds one jar per Minecraft version listed in versions/ into build/libs.
# Usage: ./build-all.sh          (all versions)
#        ./build-all.sh 26.1 26.3 (only these)
set -euo pipefail
cd "$(dirname "$0")"
rm -rf build/libs
VERSIONS=("$@")
[ ${#VERSIONS[@]} -gt 0 ] || mapfile -t VERSIONS < <(ls versions | sed 's/\.properties$//' | sort -V)
for v in "${VERSIONS[@]}"; do
  echo "== $v"
  ./gradlew build -Pmc="$v" -q
done
echo; ls -1 build/libs/*.jar | grep -v sources
