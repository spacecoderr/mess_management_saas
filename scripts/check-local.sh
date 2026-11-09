#!/usr/bin/env bash
# Is the local stack healthy? Run from anywhere:  ./scripts/check-local.sh
# Checks Postgres, the API, the web dev server, the Vite proxy, and real logins (needs demo data from the dev profile).
API=${API:-http://localhost:8081}
WEB=${WEB:-http://localhost:5173}
ok=0; bad=0
pass() { printf "  \033[32m✔\033[0m %s\n" "$1"; ok=$((ok+1)); }
fail() { printf "  \033[31m✘\033[0m %s\n      → %s\n" "$1" "$2"; bad=$((bad+1)); }
login() { curl -s -m 10 -H 'Content-Type: application/json' -X POST "$1/api/v1/auth/login" -d "{\"email\":\"$2\",\"password\":\"$3\"}"; }

echo "Thali local stack check"
if command -v pg_isready >/dev/null && pg_isready -q -h localhost -p 5432; then pass "PostgreSQL accepting connections on :5432"
else fail "PostgreSQL on :5432" "start it (brew services start postgresql@14) or use Docker: docker compose up -d db, then export DB_URL=jdbc:postgresql://localhost:5433/thali"; fi

H=$(curl -s -m 5 "$API/api/v1/health")
if [[ "$H" == *'"database":"UP"'* ]]; then pass "API healthy and connected to the database ($API)"
else fail "API at $API" "start it: cd backend && ./mvnw spring-boot:run   (response: ${H:-none})"; fi

code=$(curl -s -o /dev/null -w '%{http_code}' -m 5 "$WEB")
[[ "$code" == 200 ]] && pass "Web dev server serving the app ($WEB)" || fail "Web at $WEB" "start it: cd frontend && npm install && npm run dev (HTTP $code)"

PH=$(curl -s -m 5 "$WEB/api/v1/health")
[[ "$PH" == *'"status":"UP"'* ]] && pass "Vite proxy forwards /api to the backend" || fail "Vite proxy $WEB/api" "backend not reachable through the proxy (response: ${PH:-none})"

for who in "admin@thali.local:Admin@12345:ADMIN" "staff1@thali.local:Demo@1234:STAFF" "z900002@iiit-bh.ac.in:Demo@1234:STUDENT"; do
  IFS=: read -r email pw role <<<"$who"
  r=$(login "$WEB" "$email" "$pw")
  [[ "$r" == *"\"role\":\"$role\""* ]] && pass "Login works for $role ($email)" || fail "Login for $role ($email)" "demo data missing? see README 'Demo data' (response: ${r:0:120})"
done
echo; echo "$ok passed, $bad failed"; [[ $bad -eq 0 ]]
