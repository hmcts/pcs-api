#!/usr/bin/env bash

# Lints the schema that the Flyway migrations produce against the conventions in
# config/schemalint/.schemalintrc.js.
#
#   bin/db-schema-lint.sh              migrate a throwaway Postgres and lint it
#   bin/db-schema-lint.sh --baseline   rewrite config/schemalint/baseline.json from the findings
#
# Set PGHOST (and optionally PGPORT/PGUSER/PGPASSWORD/PGDATABASE) to lint a database that is
# already migrated; nothing is started or torn down in that case.

set -euo pipefail

# Matches pgsql_version in infrastructure/variables.tf.
POSTGRES_IMAGE=${POSTGRES_IMAGE:-postgres:16-alpine}
# Matches flywayVersion in build.gradle.
FLYWAY_IMAGE=${FLYWAY_IMAGE:-flyway/flyway:13.5.0-alpine}

PROJECT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
CONFIG_DIR="$PROJECT_ROOT/config/schemalint"
MIGRATIONS_DIR="$PROJECT_ROOT/src/main/resources/db/migration"

REGENERATE_BASELINE=false
if [ "${1:-}" = "--baseline" ]; then
  REGENERATE_BASELINE=true
fi

# Not 5432 or 6432, which the cftlib stack uses.
export PGPORT=${PGPORT:-55432}
export PGUSER=${PGUSER:-postgres}
export PGPASSWORD=${PGPASSWORD:-postgres}
export PGDATABASE=${PGDATABASE:-pcs}

if [ -n "${PGHOST:-}" ]; then
  echo "Linting the database already running at $PGHOST:$PGPORT"
else
  export PGHOST=localhost

  if docker ps --format '{{.Ports}}' | grep -q ":$PGPORT->"; then
    echo "Port $PGPORT is already published by another container. Stop it, or set PGPORT to a free port." >&2
    exit 1
  fi

  RUN_ID="pcs-schemalint-$$"
  NETWORK="$RUN_ID"
  CONTAINER="$RUN_ID-db"

  cleanup() {
    docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
    docker network rm "$NETWORK" >/dev/null 2>&1 || true
  }
  trap cleanup EXIT

  # Flyway reaches Postgres by container name here; --network host is unavailable on Docker Desktop.
  docker network create "$NETWORK" >/dev/null

  echo "Starting $POSTGRES_IMAGE on port $PGPORT"
  docker run -d --name "$CONTAINER" --network "$NETWORK" \
    -e POSTGRES_PASSWORD="$PGPASSWORD" \
    -e POSTGRES_DB="$PGDATABASE" \
    -p "127.0.0.1:$PGPORT:5432" \
    "$POSTGRES_IMAGE" >/dev/null

  # -connectRetries waits for Postgres to accept connections. Polling pg_isready instead reports
  # ready during initdb's temporary server, before the restart that follows it.
  echo "Applying migrations from src/main/resources/db/migration"
  docker run --rm --network "$NETWORK" \
    -v "$MIGRATIONS_DIR:/flyway/sql:ro" \
    "$FLYWAY_IMAGE" \
    -url="jdbc:postgresql://$CONTAINER:5432/$PGDATABASE" \
    -user="$PGUSER" \
    -password="$PGPASSWORD" \
    -baselineOnMigrate=true \
    -baselineVersion=000 \
    -connectRetries=60 \
    migrate >/dev/null
fi

cd "$CONFIG_DIR"
if [ ! -d node_modules ]; then
  yarn install --frozen-lockfile --silent
fi

if [ "$REGENERATE_BASELINE" = true ]; then
  echo "Regenerating baseline.json"
  # schemalint reports findings on stderr as "<identifier>: error <rule> : <message>".
  set +e
  SCHEMALINT_IGNORE_BASELINE=true yarn --silent lint 2>findings.txt >/dev/null
  set -e
  node -e '
    const fs = require("fs");
    const findings = fs.readFileSync("findings.txt", "utf8")
      .replace(/\x1b\[[0-9;]*m/g, "")
      .split("\n")
      .map((line) => /^(\S+): error (\S+) : /.exec(line))
      .filter(Boolean)
      .map(([, identifier, rule]) => ({ identifier, rule }));
    const unique = [...new Map(findings.map((f) => [`${f.rule}|${f.identifier}`, f])).values()]
      .sort((a, b) => a.rule.localeCompare(b.rule) || a.identifier.localeCompare(b.identifier));
    fs.writeFileSync("baseline.json", JSON.stringify(unique, null, 2) + "\n");
    console.info(`Baselined ${unique.length} pre-existing violations`);
  '
  rm -f findings.txt
  exit 0
fi

yarn --silent lint
