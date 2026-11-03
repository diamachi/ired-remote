#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT
stdlib=$(find tools/gradle/lib -maxdepth 1 -name 'kotlin-stdlib-*.jar' -print -quit)
classpath="app/build/tmp/kotlin-classes/debug:$stdlib"
javac -cp "$classpath" -d "$test_dir" tests/GtvWireRegression.java
java -cp "$test_dir:$classpath" GtvWireRegression
