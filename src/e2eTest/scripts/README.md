# E2E Test Scripts

## Allure Slack Notifier

Posts **E2E-specific detailed test results** (Allure summary, pass rate, failures, slowest tests) to **#pcs-tech** using the **same mechanism as Jenkins build notifications**: the Jenkins Slack Notification Plugin (`slackSend` step).

### How It Works

- **Build notifications** (e.g. "Build #661 has FAILED") use `slackSend` from the Jenkins Slack plugin
- **Allure E2E details** use the same `slackSend` step with the same channel and credentials
- **No extra secrets** – uses the existing Jenkins Slack integration (Bot OAuth token configured globally)

### Usage

```bash
# From src/e2eTest directory
yarn allure-slack-notify              # Parse, print, and post (if SLACK_WEBHOOK_URL set)
yarn allure-slack-notify --print-only  # Output message only (for Jenkins slackSend)
```

### Environment Variables

| Variable | Description |
|----------|-------------|
| `BUILD_NUMBER` | Jenkins build number (default: `local`) |
| `BUILD_URL` | Jenkins build URL for Allure report link (default: empty) |
| `SLACK_WEBHOOK_URL` | Optional – for standalone runs; pipeline uses `slackSend` instead |

### When to Run

1. Run e2e tests (generates `allure-results` and `e2e-output`)
2. Pipeline runs `yarn allure-slack-notify --print-only` and passes output to `slackSend` (preview, nightly, master)
