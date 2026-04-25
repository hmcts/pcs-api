#!/usr/bin/env bash
# Gradle/Jenkins entry: E2E_SUITE maps to Playwright project; tag/spec filtering is in playwright.config.ts
# via E2E_TEST_SCOPE and E2E_SPEC (aligned with pcs-frontend HDPI-6105 / HDPI-6106).
# Legacy: if only PLAYWRIGHT_GREP is set, it is copied into E2E_TEST_SCOPE before defaults run.
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
  commoncomp)
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

# Prefer E2E_TEST_SCOPE from Jenkins/local; map legacy PLAYWRIGHT_GREP when E2E_TEST_SCOPE is unset.
if [ -z "${E2E_TEST_SCOPE+x}" ] && [ "${PLAYWRIGHT_GREP+x}" = x ]; then
  export E2E_TEST_SCOPE="${PLAYWRIGHT_GREP}"
fi

# CNP-style suites: default tag scope when E2E_TEST_SCOPE is still unset.
if [ -z "${E2E_TEST_SCOPE+x}" ]; then
  case "${SUITE}" in
    pr) export E2E_TEST_SCOPE="@PR" ;;
    regression) export E2E_TEST_SCOPE="@regression" ;;
    enforcement) export E2E_TEST_SCOPE="@enforcement" ;;
    commoncomp) export E2E_TEST_SCOPE="@commoncomp" ;;
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
