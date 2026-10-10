#!/bin/sh
# A fixed offline demo project; never choose a project from ADC or a real .firebaserc.
set -eu
if [ -f /data/export/firebase-export-metadata.json ]; then
    set -- --import=/data/export
else
    set --
fi
exec firebase emulators:start --only auth --project demo-gems --config /emulator/firebase.json \
    --export-on-exit=/data/export --log-verbosity=QUIET "$@"
