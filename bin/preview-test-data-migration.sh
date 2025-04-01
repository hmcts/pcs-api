#!/bin/bash

# Get the directory where the script is located
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Set the target directory relative to the script's location
directory="$script_dir/../src/main/resources/db/migration/"

# Extract version numbers, sort numerically, and get the highest one
latest_version=$(ls "$directory" | grep -oE 'V[0-9]+' | sort -V | tail -n 1)

if [[ -z "$latest_version" ]]; then
    next_version="V001"
else
    # Extract the numeric part and increment it
    latest_number=$(echo "$latest_version" | grep -oE '[0-9]+')
    next_number=$(printf "%03d" $((latest_number + 1)))
    next_version="V$next_number"
fi

echo "Next version: $next_version"

# Rename preview_test_data.sql to include the new version prefix
if [[ -f "$directory/preview_test_data.sql" ]]; then
    mv "$directory/preview_test_data.sql" "$directory/${next_version}__preview_test_data.sql"
    echo "Renamed preview_test_data.sql to ${next_version}__preview_test_data.sql"
fi
