#!/usr/bin/env bash
# orbit-build.sh — robust Orbit-AI flavor build for the redroid verification host.
# Clears gradle caches BEFORE building so a 368MB+ APK never fails with
# "No space left on device" (the host disk is ~45G and fills up fast).
#
# Usage:  ./orbit-build.sh <flavor>        e.g.  ./orbit-build.sh hermes
set -u
FLAVOR="${1:-hermes}"
cd /home/ubuntu/Orbit-AI || { echo "cannot cd to repo"; exit 1; }

export ANDROID_HOME=/usr/lib/android-sdk
export ANDROID_SDK_ROOT=/usr/lib/android-sdk
export LD_LIBRARY_PATH=/usr/x86_64-linux-gnu/lib:/usr/x86_64-linux-gnu/lib64:$LD_LIBRARY_PATH
export GRADLE_OPTS="-Xmx4g"

echo "=== disk before ==="; df -h / | tail -1

# Free space proactively so large APKs build reliably.
echo "=== cleaning gradle caches ==="
rm -rf /home/ubuntu/.gradle/caches/build-cache-1 2>/dev/null
rm -rf /home/ubuntu/.gradle/caches/journal-1 2>/dev/null
rm -rf /home/ubuntu/.gradle/daemon 2>/dev/null
# drop stale APKs from other flavors
rm -f /home/ubuntu/Orbit-AI/app/build/outputs/apk/*/debug/app-*-debug.apk 2>/dev/null

echo "=== disk after cleanup ==="; df -h / | tail -1

CAP=$(echo "$FLAVOR" | sed 's/.*/\u&/')
echo "=== building :app:assemble${CAP}Debug ==="
./gradlew ":app:assemble${CAP}Debug" --no-daemon 2>&1 | tail -25
RC=${PIPESTATUS[0]}
echo "=== EXIT=$RC ==="
ls -la "app/build/outputs/apk/$FLAVOR/debug/"*.apk 2>/dev/null
exit $RC
