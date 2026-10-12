#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out/tests
javac -encoding UTF-8 -d out/tests \
  app/src/main/java/ua/schaslyve/tryvoha/AlertMatcher.java \
  app/src/main/java/ua/schaslyve/tryvoha/TransitionPolicy.java \
  tests/MatcherTest.java tests/TransitionPolicyTest.java
java -cp out/tests MatcherTest
java -cp out/tests TransitionPolicyTest
