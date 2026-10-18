#!/usr/bin/env bash
#
# Records the enforcement BDD features as demo videos: one MP4 per feature file, made of that feature's
# passing scenarios in feature-file order, each preceded by a title card.
#
# Usage: scripts/enforcement-demo-videos.sh <output dir> [feature prefix ...]
#   e.g. scripts/enforcement-demo-videos.sh ~/enforcement-videos
#        scripts/enforcement-demo-videos.sh ~/enforcement-videos 01 06
#
# Runs against a local cftlib stack (ENVIRONMENT=local), one feature at a time with one worker, in the
# `enforcement-bdd-demo` Playwright project, which records every scenario at human speed (see
# tests/tests-enforcement/bdd/support/demo-pacing.ts). Feature 19 (the status model) is technical design and
# is never filmed.
#
# Writes:
#   <output dir>/<feature>.mp4        the stitched video, when at least one scenario passed
#   <output dir>/raw/<feature>/       Playwright's per-scenario output (video.webm, trace, screenshots)
#   <output dir>/raw/<feature>.json   Playwright's JSON report for the feature
#   <output dir>/work/<feature>/      the normalised clips and title cards the MP4 is concatenated from
#
# Environment:
#   MANAGE_CASE_BASE_URL     XUI to health-check (default http://localhost:3000)
#   XUI_CONTAINER            docker container restarted when XUI is down (default cftlib-xui-manage-cases-1)
#   DEMO_SLOW_MO, DEMO_HOLD_MS   pacing, passed through to Playwright
#   VIDEO_SIZE               output resolution (default 1920x1080, the demo project's viewport)
#   TITLE_SECONDS            scenario title card length (default 2.5)
#   MIN_CLIP_SECONDS         page videos shorter than this are left out as unused pages (default 1.5)
#   OPENING_SECONDS          feature title card length (default 3.5)

set -uo pipefail

usage() {
  sed -n '3,9p' "$0" | sed 's/^# \{0,1\}//'
  exit 2
}

[[ $# -ge 1 ]] || usage
mkdir -p "$1" || exit 1
OUT_DIR="$(cd "$1" && pwd)"
shift
FEATURE_FILTERS=("$@")

E2E_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FEATURES_DIR="$E2E_DIR/tests/tests-enforcement/bdd/features"
EXCLUDED_FEATURE="19-enforcement-status-model"

XUI_URL="${MANAGE_CASE_BASE_URL:-http://localhost:3000}"
XUI_CONTAINER="${XUI_CONTAINER:-cftlib-xui-manage-cases-1}"
VIDEO_SIZE="${VIDEO_SIZE:-1920x1080}"
VIDEO_WIDTH="${VIDEO_SIZE%x*}"
VIDEO_HEIGHT="${VIDEO_SIZE#*x}"
FPS=25
TITLE_SECONDS="${TITLE_SECONDS:-2.5}"
OPENING_SECONDS="${OPENING_SECONDS:-3.5}"
MIN_CLIP_SECONDS="${MIN_CLIP_SECONDS:-1.5}"
BACKGROUND_COLOUR="0x1d2733"

log() { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }

for tool in ffmpeg node npx curl; do
  command -v "$tool" >/dev/null || { echo "$tool is required but not on the PATH" >&2; exit 1; }
done

FONT_FILE="$(fc-match -f '%{file}' 'DejaVu Sans:style=Bold' 2>/dev/null || true)"
if [[ -z "$FONT_FILE" || ! -f "$FONT_FILE" || "$FONT_FILE" != *.ttf ]]; then
  FONT_FILE="$(fc-match -f '%{file}' 'Liberation Sans:style=Bold' 2>/dev/null || true)"
fi
[[ -f "$FONT_FILE" ]] || { echo "No usable font found (tried DejaVu Sans and Liberation Sans)" >&2; exit 1; }

# --- XUI --------------------------------------------------------------------------------------------------

xui_status() {
  curl -s -o /dev/null -m 10 -w '%{http_code}' "$XUI_URL/" || true
}

# Local XUI dies on concurrent sign-ins (node-lib's OAuth error handler), and every scenario after that fails
# with ERR_CONNECTION_REFUSED. Check before each feature and bring it back if it has gone.
ensure_xui() {
  [[ "$(xui_status)" == "200" ]] && return 0
  # Usually the container has exited; if it is still running but not answering, restart it.
  local action=start
  [[ "$(docker inspect -f '{{.State.Running}}' "$XUI_CONTAINER" 2>/dev/null)" == "true" ]] && action=restart
  log "XUI at $XUI_URL is not answering 200; docker $action $XUI_CONTAINER"
  docker "$action" "$XUI_CONTAINER" >/dev/null || { log "docker $action $XUI_CONTAINER failed"; return 1; }
  local waited=0
  until [[ "$(xui_status)" == "200" ]]; do
    if (( waited >= 300 )); then
      log "XUI did not come back within 300s"
      return 1
    fi
    sleep 5
    waited=$((waited + 5))
  done
  # XUI answers before it has finished connecting to its back ends.
  sleep 20
  log "XUI is back"
}

# --- ffmpeg -----------------------------------------------------------------------------------------------

ENCODE=(-c:v libx264 -preset veryfast -crf 22 -pix_fmt yuv420p -r "$FPS" -video_track_timescale 90000 -an)

# Wraps text to a width, so drawtext (which does not wrap) keeps it on screen.
wrap() {
  fold -s -w "$2" <<<"$1" | sed 's/[[:space:]]*$//'
}

# title_card <output.mp4> <seconds> <small text> <large text>
title_card() {
  local out="$1" seconds="$2" small="$3" large="$4"
  local small_file="${out%.mp4}.small.txt" large_file="${out%.mp4}.large.txt"
  wrap "$small" 60 >"$small_file"
  wrap "$large" 40 >"$large_file"
  local small_size=$((VIDEO_HEIGHT / 24)) large_size=$((VIDEO_HEIGHT / 14))
  local filter
  filter="drawtext=fontfile='$FONT_FILE':textfile='$small_file':expansion=none:fontcolor=0xc8d1db"
  filter+=":fontsize=$small_size:line_spacing=$((small_size / 3)):x=(w-text_w)/2:y=h*0.22"
  filter+=",drawtext=fontfile='$FONT_FILE':textfile='$large_file':expansion=none:fontcolor=white"
  filter+=":fontsize=$large_size:line_spacing=$((large_size / 3)):x=(w-text_w)/2:y=(h-text_h)/2"
  ffmpeg -nostdin -loglevel error -y \
    -f lavfi -i "color=c=$BACKGROUND_COLOUR:s=${VIDEO_WIDTH}x${VIDEO_HEIGHT}:d=$seconds:r=$FPS" \
    -vf "$filter" "${ENCODE[@]}" "$out"
}

# normalise <input.webm> <output.mp4>: same size, frame rate and codec as the title cards, so the concat
# demuxer can join them without re-encoding.
normalise() {
  ffmpeg -nostdin -loglevel error -y -i "$1" \
    -vf "scale=${VIDEO_WIDTH}:${VIDEO_HEIGHT}:force_original_aspect_ratio=decrease,pad=${VIDEO_WIDTH}:${VIDEO_HEIGHT}:(ow-iw)/2:(oh-ih)/2:color=black,fps=$FPS,format=yuv420p" \
    "${ENCODE[@]}" "$2"
}

# concat <list file> <output.mp4>
concat() {
  ffmpeg -nostdin -loglevel error -y -f concat -safe 0 -i "$1" -c copy -movflags +faststart "$2"
}

# --- Playwright report ------------------------------------------------------------------------------------

# Prints the feature title, then one tab-separated line per scenario in feature-file order:
#   <status> <TAB> <scenario title> <TAB> <video paths, separated by |>
# status is passed, failed or skipped (a fixme from a pending step). Outline examples without a title of
# their own ("Example #2") are named after their outline.
read_report() {
  node - "$1" <<'NODE'
const report = require(process.argv[2]);
const scenarios = [];
let featureTitle = '';
const walk = (suite, parents) => {
  for (const spec of suite.specs ?? []) {
    const test = spec.tests[0];
    const result = test?.results?.[test.results.length - 1];
    const status = !result ? 'skipped'
      : result.status === 'passed' ? 'passed'
      : result.status === 'skipped' ? 'skipped'
      : 'failed';
    // One video per browser page, in the order the pages were opened: a scenario that signs in as a second
    // user gets a second page.
    const video = (result?.attachments ?? []).filter(a => a.name === 'video' && a.path).map(a => a.path).join('|');
    const parent = parents[parents.length - 1] ?? '';
    const title = /^Example #\d+$/.test(spec.title) && parent !== featureTitle ? `${parent} (${spec.title})` : spec.title;
    scenarios.push({ line: spec.line, column: spec.column, status, title, video });
  }
  for (const child of suite.suites ?? []) {
    walk(child, [...parents, child.title]);
  }
};
for (const file of report.suites ?? []) {
  for (const feature of file.suites ?? []) {
    featureTitle ||= feature.title;
  }
  walk(file, []);
}
scenarios.sort((a, b) => a.line - b.line || a.column - b.column);
console.log(featureTitle);
for (const s of scenarios) {
  console.log([s.status, s.title.replace(/\s+/g, ' '), s.video].join('\t'));
}
NODE
}

# --- Main -------------------------------------------------------------------------------------------------

cd "$E2E_DIR" || exit 1

log "Generating the BDD specs"
npx bddgen || { echo "bddgen failed" >&2; exit 1; }

FEATURES=()
for path in "$FEATURES_DIR"/[0-9][0-9]-*.feature; do
  feature="$(basename "$path" .feature)"
  [[ "$feature" == "$EXCLUDED_FEATURE" ]] && continue
  if (( ${#FEATURE_FILTERS[@]} )); then
    match=false
    for filter in "${FEATURE_FILTERS[@]}"; do
      [[ "$feature" == "$filter"* ]] && match=true
    done
    $match || continue
  fi
  FEATURES+=("$feature")
done
(( ${#FEATURES[@]} )) || { echo "No feature files match: ${FEATURE_FILTERS[*]}" >&2; exit 1; }

VIDEOS=()
NO_VIDEO=()
FAILED=()
SKIPPED=()

for feature in "${FEATURES[@]}"; do
  log "=== $feature"
  raw_dir="$OUT_DIR/raw/$feature"
  report="$OUT_DIR/raw/$feature.json"
  work_dir="$OUT_DIR/work/$feature"
  rm -rf "$raw_dir" "$work_dir" "$report"
  mkdir -p "$raw_dir" "$work_dir"

  if ! ensure_xui; then
    NO_VIDEO+=("$feature: XUI was down and could not be restarted")
    continue
  fi

  ENVIRONMENT=local E2E_WORKERS=1 CI=1 PLAYWRIGHT_JSON_OUTPUT_FILE="$report" \
    npx playwright test --project enforcement-bdd-demo --grep-invert '@pending' \
    --reporter=list,json --output="$raw_dir" \
    ".features-gen/enforcement/$feature.feature.spec.js"
  log "Playwright exited with $? for $feature"

  if [[ ! -s "$report" ]]; then
    NO_VIDEO+=("$feature: no Playwright report (no runnable scenarios, or the run did not start)")
    continue
  fi

  mapfile -t lines < <(read_report "$report")
  feature_title="${lines[0]:-$feature}"
  scenario_lines=("${lines[@]:1}")
  if (( ${#scenario_lines[@]} == 0 )); then
    NO_VIDEO+=("$feature: no runnable scenarios")
    continue
  fi

  list_file="$work_dir/concat.txt"
  : >"$list_file"
  title_card "$work_dir/00-opening.mp4" "$OPENING_SECONDS" "Enforcement journey ${feature%%-*}" "$feature_title" \
    || { NO_VIDEO+=("$feature: could not render the title card"); continue; }
  echo "file '$work_dir/00-opening.mp4'" >>"$list_file"

  clips=0
  index=0
  for line in "${scenario_lines[@]}"; do
    IFS=$'\t' read -r status title video <<<"$line"
    index=$((index + 1))
    case "$status" in
      passed) ;;
      failed) FAILED+=("$feature: $title"); continue ;;
      *) SKIPPED+=("$feature: $title"); continue ;;
    esac
    number="$(printf '%02d' "$index")"
    # A page the scenario never used (the default one, when its steps open their own) records about a
    # second of blank screen, so it is left out.
    parts=()
    IFS='|' read -r -a videos <<<"$video"
    for part in "${videos[@]}"; do
      [[ -f "$part" ]] || continue
      seconds="$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$part" 2>/dev/null)"
      awk -v s="${seconds:-0}" -v min="$MIN_CLIP_SECONDS" 'BEGIN { exit !(s >= min) }' || continue
      clip="$work_dir/$number-scenario-${#parts[@]}.mp4"
      normalise "$part" "$clip" && parts+=("$clip")
    done
    if (( ${#parts[@]} == 0 )); then
      FAILED+=("$feature: $title (passed, but no video was recorded or encoded)")
      continue
    fi
    if title_card "$work_dir/$number-title.mp4" "$TITLE_SECONDS" "$feature_title" "$title"; then
      echo "file '$work_dir/$number-title.mp4'" >>"$list_file"
      for clip in "${parts[@]}"; do
        echo "file '$clip'" >>"$list_file"
      done
      clips=$((clips + 1))
    else
      FAILED+=("$feature: $title (passed, but its title card could not be rendered)")
    fi
  done

  if (( clips == 0 )); then
    NO_VIDEO+=("$feature: no scenario passed")
    continue
  fi
  if concat "$list_file" "$OUT_DIR/$feature.mp4"; then
    VIDEOS+=("$OUT_DIR/$feature.mp4 ($clips scenario$( ((clips == 1)) || echo s))")
  else
    NO_VIDEO+=("$feature: concatenating the clips failed")
  fi
done

print_list() {
  local heading="$1"
  shift
  echo
  echo "$heading ($#):"
  if (( $# == 0 )); then
    echo "  (none)"
  else
    printf '  %s\n' "$@"
  fi
}

echo
echo "================ Summary ================"
print_list "Videos" "${VIDEOS[@]}"
print_list "Features with no video" "${NO_VIDEO[@]}"
print_list "Failed scenarios (left out)" "${FAILED[@]}"
print_list "Scenarios stopped at a pending step (left out)" "${SKIPPED[@]}"

(( ${#FAILED[@]} == 0 ))
