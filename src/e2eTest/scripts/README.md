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
| **Channel (same as Jenkins)** | |
| `SLACK_CHANNEL` | Channel name (e.g. `#qa-pipeline-status`) – use with `SLACK_BOT_TOKEN` |
| `SLACK_BOT_TOKEN` | Slack Bot User OAuth Token (e.g. `xoxb-...`) – same concept as Jenkins Slack plugin |
| **Or Incoming Webhook** | |
| `SLACK_WEBHOOK_URL` | Slack Incoming Webhook URL (e.g. `https://hooks.slack.com/...`) – for webhook-only posting |

### Local run – same channel as Jenkins (#qa-pipeline-status)

Use channel name + Bot token (same mechanism as Jenkins `slackSend`):

```bash
export SLACK_CHANNEL=#qa-pipeline-status
export SLACK_BOT_TOKEN=xoxb-your-bot-token   # from Slack app / Jenkins Slack config
yarn allure-slack-notify
```

You can also set `SLACK_WEBHOOK_URL=#qa-pipeline-status` and `SLACK_BOT_TOKEN`; the script treats a channel name plus token as “post to this channel via API”.

### When to Run

1. Run e2e tests (generates `allure-results` and `e2e-output`)
2. Pipeline runs `yarn allure-slack-notify --print-only` and passes output to `slackSend` (preview, nightly, master)
