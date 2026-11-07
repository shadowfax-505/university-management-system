#!/usr/bin/env bash
# Walks through login, CRUD, RBAC and logout against a running app.
# Usage: ./demo.sh            (app must be running: docker compose up --build)
#        BASE=http://host:8080 ./demo.sh
set -u
BASE=${BASE:-http://localhost:8080}
JAR=$(mktemp -d)
trap 'rm -rf "$JAR"' EXIT

step() { printf '\n\033[1;34m== %s\033[0m\n' "$1"; }
# call <cookie-jar-name> <METHOD> <path> [json-body]: prints status code and body
call() {
  local jar="$JAR/$1" method=$2 path=$3 body=${4:-}
  if [ -n "$body" ]; then
    curl -s -o "$JAR/out" -w '%{http_code}' -b "$jar" -c "$jar" -X "$method" "$BASE$path" \
         -H 'Content-Type: application/json' -d "$body"
  else
    curl -s -o "$JAR/out" -w '%{http_code}' -b "$jar" -c "$jar" -X "$method" "$BASE$path"
  fi
  printf '  %s %s\n' "$method" "$path"; cat "$JAR/out"; echo
}

step "1. Not logged in -> 401"
call anon GET /api/students

step "2. Admin logs in (seeded account) -> 200 + JSESSIONID cookie"
call admin POST /api/auth/login '{"username":"admin","password":"admin123"}'

step "3. Admin creates a teacher and a student -> 201 Created"
call admin POST /api/teachers '{"name":"Dr. Karim","email":"karim@uni.edu","username":"karim","password":"secret123","department":"CSE","designation":"Professor"}'
call admin POST /api/students '{"name":"Rahim","email":"rahim@uni.edu","username":"rahim","password":"secret123","studentId":"2007001","department":"CSE","batch":2020}'

step "4. Duplicate username -> 409, invalid body -> 400"
call admin POST /api/students '{"name":"X","email":"x@uni.edu","username":"rahim","password":"secret123","studentId":"2007999","department":"CSE","batch":2020}'
call admin POST /api/students '{"name":"","email":"bad","username":"a b"}'

step "5. Teacher logs in: teachers OK, students/admins -> 403"
call teacher POST /api/auth/login '{"username":"karim","password":"secret123"}'
call teacher GET /api/teachers
call teacher GET /api/students
call teacher GET /api/admins

step "6. Student logs in: students OK, teachers -> 403"
call student POST /api/auth/login '{"username":"rahim","password":"secret123"}'
call student GET /api/students
call student GET /api/teachers

step "7. Admin updates and deletes the student -> 200, 204, then 404"
ID=$(curl -s -b "$JAR/admin" "$BASE/api/students" | sed -n 's/.*"id":\([0-9]*\).*"username":"rahim".*/\1/p')
call admin PUT "/api/students/$ID" '{"name":"Rahim Uddin","email":"rahim@uni.edu","username":"rahim","studentId":"2007001","department":"CSE","batch":2021}'
call admin DELETE "/api/students/$ID"
call admin GET "/api/students/$ID"
TID=$(curl -s -b "$JAR/admin" "$BASE/api/teachers" | sed -n 's/.*"id":\([0-9]*\).*"username":"karim".*/\1/p')
call admin DELETE "/api/teachers/$TID"

step "8. Logout -> 200, then the same client is no longer logged in -> 401"
call admin POST /api/auth/logout
call admin GET /api/auth/me
