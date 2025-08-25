#!/bin/bash
cd /tmp/kavia/workspace/code-generation/crypto-tracker-644240-645409/crypto_tracker_mobile_app
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

