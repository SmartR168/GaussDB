#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build
find src -name '*.java' -print | sort > build/sources.txt
javac --release 11 -encoding UTF-8 -d build @build/sources.txt
echo COMPILE_OK
