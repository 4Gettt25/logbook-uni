#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "Create a log entry"
curl -sS -X POST "$BASE_URL/api/logs" \
  -H 'Content-Type: application/json' \
  -d '{
    "timestamp": "2024-01-01T00:00:00Z",
    "logLevel": "INFO",
    "source": "auth",
    "message": "User logged in",
    "category": "security"
  }' | jq .

echo "\nList logs (filtered by source=auth)"
curl -sS "$BASE_URL/api/logs?source=auth" | jq .

echo "\nExport CSV (first 100)"
curl -sS -L "$BASE_URL/api/logs/export?format=csv&limit=100"

echo "\nCreate a server"
SID=$(curl -sS -X POST "$BASE_URL/api/servers" -H 'Content-Type: application/json' \
  -d '{"name":"server-a","hostname":"host1","description":"prod"}' | tee /dev/stderr | jq -r .id)
echo "Server ID: ${SID}"

echo "\nUpload single logfile to server ${SID:-1}"
curl -sS -F "file=@/var/log/syslog" "$BASE_URL/api/servers/${SID:-1}/logs/upload" | jq .

echo "\nUpload multiple logfiles to server ${SID:-1}"
curl -sS -F "files=@/var/log/syslog" -F "files=@/var/log/dpkg.log" "$BASE_URL/api/servers/${SID:-1}/logs/upload" | jq .

echo "\nList logs for server ${SID:-1}"
curl -sS "$BASE_URL/api/servers/${SID:-1}/logs?page=0&size=10" | jq .

echo "\nReevaluate server levels (dry run)"
curl -sS -X POST "$BASE_URL/api/servers/${SID:-1}/logs/reevaluate?merge=true&dryRun=true" | jq .
