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
    "username": "alice",
    "category": "security",
    "status": "OPEN"
  }' | jq .

echo "\nList logs (filtered by source=auth)"
curl -sS "$BASE_URL/api/logs?source=auth" | jq .

echo "\nExport CSV (first 100)"
curl -sS -L "$BASE_URL/api/logs/export?format=csv&limit=100"

echo "\nCreate a server"
curl -sS -X POST "$BASE_URL/api/servers" -H 'Content-Type: application/json' \
  -d '{"name":"server-a","hostname":"host1","description":"prod"}' | jq .

echo "\nUpload single logfile to server 1"
curl -sS -F "file=@/var/log/syslog" "$BASE_URL/api/servers/1/logs/upload" | jq .

echo "\nUpload multiple logfiles to server 1"
curl -sS -F "files=@/var/log/syslog" -F "files=@/var/log/dpkg.log" "$BASE_URL/api/servers/1/logs/upload" | jq .
