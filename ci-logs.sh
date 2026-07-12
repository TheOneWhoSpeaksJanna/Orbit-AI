#!/usr/bin/env bash
set -euo pipefail

# ── ci-logs.sh ──────────────────────────────────────────────
# Fetch logs from the latest CI run — failed steps by default.
# Usage:
#   ./ci-logs.sh            # failed-step logs from latest run
#   ./ci-logs.sh --all      # full logs from latest run
#   ./ci-logs.sh --watch    # poll until latest run finishes, then show logs
#   ./ci-logs.sh --help     # this message

SCRIPT_NAME="${0##*/}"
WATCH=false
SHOW_ALL=false

for arg in "$@"; do
  case "$arg" in
    --all)  SHOW_ALL=true  ;;
    --watch) WATCH=true    ;;
    --help)
      sed -n '3,/^$/p' "$0" | sed 's/^# //;s/^#$//'
      exit 0
      ;;
  esac
done

get_latest_run_id() {
  gh run list --limit 1 --json databaseId --jq '.[0].databaseId'
}

if $WATCH; then
  echo "Waiting for the latest run to finish..."
  gh run watch "$(get_latest_run_id)"
  echo ""
fi

RUN_ID="$(get_latest_run_id)"
if [ -z "$RUN_ID" ]; then
  echo "No CI runs found." >&2
  exit 1
fi

echo "── Run #$RUN_ID ──────────────────────────────────"
if $SHOW_ALL; then
  gh run view "$RUN_ID" --log
else
  gh run view "$RUN_ID" --log-failed 2>/dev/null || {
    echo "No failed-step logs found — showing full run summary."
    gh run view "$RUN_ID"
  }
fi
