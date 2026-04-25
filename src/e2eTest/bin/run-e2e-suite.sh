#!/usr/bin/env bash
# Gradle entry: E2E_SUITE → Playwright --project. Nightly sets E2E_TEST_SCOPE / E2E_SPEC (playwright.config.ts).
# When those are unset, default title grep for pr / regression / enforcement matches CNP yarn scripts.
set -euo pipefail

SUITE="${1:?E2E_SUITE required}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}/.."
E2E_HOME="$(pwd)"

PROJECT=""

case "${SUITE}" in
  pr)
    PROJECT="chrome"
    ;;
  regression)
    PROJECT="chrome"
    ;;
  enforcement)
    PROJECT="chrome"
    ;;
  chrome|firefox|webkit|edge|mobile-android|mobile-ios|mobile-ipad)
    PROJECT="${SUITE}"
    ;;
  *)
    echo "Unknown E2E_SUITE: ${SUITE}" >&2
    exit 1
    ;;
esac

if [ -z "${E2E_TEST_SCOPE+x}" ]; then
  case "${SUITE}" in
    pr) export E2E_TEST_SCOPE="@PR" ;;
    regression) export E2E_TEST_SCOPE="@regression" ;;
    enforcement) export E2E_TEST_SCOPE="@enforcement" ;;
  esac
fi

yarn playwright install
if [[ "${PROJECT}" == "edge" ]]; then
  yarn playwright install msedge
fi

yarn playwright test --project "${PROJECT}"
EXIT_CODE=$?

allure generate -o "${E2E_HOME}/../../e2e-output" --clean
yarn exec tsx ./config/clean-attachments.config.ts

exit "${EXIT_CODE}"
