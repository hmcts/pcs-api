#!/usr/bin/env bash

set -euo pipefail

base_sha="${BASE_SHA:?missing BASE_SHA}"
head_sha="${HEAD_SHA:?missing HEAD_SHA}"

migration_dir="src/main/resources/db/migration"
findings_file="db-scan-findings.tsv"
summary_file="db-scan-summary.md"

: > "$findings_file"

scan_file() {
  local file="$1"

  git diff --unified=0 --no-color "${base_sha}...${head_sha}" -- "$file" |
    awk -v file="$file" -v findings_file="$findings_file" '
      function emit(line, severity, label, advice, content) {
        printf "%s\t%s\t%s\t%s\t%s\t%s\n", file, line, severity, label, advice, content >> findings_file
      }

      function upper(value) {
        return toupper(value)
      }

      /^@@ / {
        if (match($0, /\+([0-9]+)/, m)) {
          line = m[1]
        }
        next
      }

      /^\+/ && !/^\+\+\+/ {
        content = substr($0, 2)
        trimmed = content
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", trimmed)

        if (trimmed == "" || trimmed ~ /^--/ || trimmed ~ /^\/\*/ || trimmed ~ /^\*/) {
          line++
          next
        }

        upper_content = upper(content)

        if (upper_content ~ /DROP[[:space:]]+TABLE/) {
          emit(line, "high", "Drop table", "Dropping a table will break any app code that still reads from or writes to it.", trimmed)
        } else if (upper_content ~ /DROP[[:space:]]+COLUMN/) {
          emit(line, "high", "Drop column", "Dropping a column can break queries, ORM mappings, serializers, and reports that still expect it.", trimmed)
        } else if (upper_content ~ /RENAME[[:space:]]+COLUMN/) {
          emit(line, "high", "Rename column", "Renaming a column is breaking unless the application is deployed with a matching change first.", trimmed)
        } else if (upper_content ~ /RENAME[[:space:]]+TO/) {
          emit(line, "high", "Rename table", "Renaming a table is breaking unless every caller is updated in lockstep.", trimmed)
        } else if (upper_content ~ /DROP[[:space:]]+INDEX/) {
          emit(line, "medium", "Drop index", "Dropping an index can cause performance regressions or query-plan changes that affect production behaviour.", trimmed)
        } else if (upper_content ~ /DROP[[:space:]]+CONSTRAINT/) {
          emit(line, "medium", "Drop constraint", "Dropping a constraint can change data integrity guarantees or break code that depends on the constraint existing.", trimmed)
        } else if (upper_content ~ /TRUNCATE[[:space:]]+TABLE/) {
          emit(line, "high", "Truncate table", "Truncating a table removes all rows immediately and can break the app if it still expects data to exist.", trimmed)
        } else if (upper_content ~ /ALTER[[:space:]]+COLUMN.*TYPE/) {
          emit(line, "medium", "Alter column type", "Changing a column type can break reads and writes if the application still uses the old type contract.", trimmed)
        } else if (upper_content ~ /SET[[:space:]]+NOT[[:space:]]+NULL/) {
          emit(line, "medium", "Set not null", "Making a column non-null can break writes while the old app version still sends null values.", trimmed)
        }

        line++
        next
      }

      /^\-/ {
        next
      }
    '
}

changed_files="$(git diff --name-only --diff-filter=ACMRT "${base_sha}...${head_sha}" -- "$migration_dir" \
  | grep -E '\.sql$' || true)"

while IFS= read -r file; do
  [ -n "$file" ] || continue
  scan_file "$file"
done <<EOF
$changed_files
EOF

findings_count=$(wc -l < "$findings_file" | tr -d ' ')

{
  printf '# DB breaking change scan\n\n'
  if [ "$findings_count" -gt 0 ]; then
    printf 'Found %s potentially breaking database change%s in Flyway migrations that could cause an issue while the application is still on the previous version.\n\n' \
      "$findings_count" \
      "$( [ "$findings_count" -eq 1 ] && printf '' || printf 's' )"
    printf '## Findings\n\n'
    while IFS=$'\t' read -r file line severity label advice content; do
      printf -- '- %s: `%s`:%s - %s - `%s`\n' \
        "$(printf '%s' "$severity" | tr '[:lower:]' '[:upper:]')" \
        "$file" \
        "$line" \
        "$label" \
        "$content"
    done < "$findings_file"
    printf '\nThis is advisory only, but each item above could break the app if the database is deployed before the compatible application version.\n'
  else
    printf 'No potentially breaking database changes were detected in Flyway migrations.\n\n'
    printf 'This check only scans changed `.sql` files under `src/main/resources/db/migration` and flags high-risk DDL patterns. It does not replace a full migration review.\n'
  fi
} > "$summary_file"
