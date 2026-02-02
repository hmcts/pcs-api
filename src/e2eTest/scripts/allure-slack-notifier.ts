import * as fs from 'fs';
import * as path from 'path';
import * as https from 'https';
import * as http from 'http';

// -----------------------------
// Allure summary parsing
// -----------------------------

const DEFAULT_SUMMARY_CANDIDATES = [
  'e2e-output/widgets/summary.json',
  'e2e-output/data/widgets/summary.json',
  'allure-report/widgets/summary.json',
  'allure-report/data/widgets/summary.json',
];

export function findAllureSummaryJson(
  baseDir: string = process.cwd(),
  candidates: string[] = DEFAULT_SUMMARY_CANDIDATES
): string {
  for (const p of candidates) {
    const fullPath = path.join(baseDir, p);
    if (fs.existsSync(fullPath)) {
      return fullPath;
    }
  }
  throw new Error(
    `Allure summary.json not found. Checked:\n- ${candidates.map((c) => path.join(baseDir, c)).join('\n- ')}`
  );
}

export interface AllureSummary {
  total: number;
  passed: number;
  failed: number;
  broken: number;
  skipped: number;
  unknown: number;
  duration_ms: number;
  duration_seconds: number;
  pass_rate: number;
}

export function parseAllureSummary(summaryPath: string): AllureSummary {
  if (!fs.existsSync(summaryPath)) {
    throw new Error(`Allure summary not found at: ${summaryPath}`);
  }
  const data = JSON.parse(fs.readFileSync(summaryPath, 'utf-8'));
  const stats = data.statistic ?? {};
  const timeInfo = data.time ?? {};
  const total = parseInt(String(stats.total ?? 0), 10) || 0;
  const passed = parseInt(String(stats.passed ?? 0), 10) || 0;
  const failed = parseInt(String(stats.failed ?? 0), 10) || 0;
  const broken = parseInt(String(stats.broken ?? 0), 10) || 0;
  const skipped = parseInt(String(stats.skipped ?? 0), 10) || 0;
  const unknown = parseInt(String(stats.unknown ?? 0), 10) || 0;
  const durationMs = parseInt(String(timeInfo.duration ?? 0), 10) || 0;
  const durationSeconds = Math.round((durationMs / 1000) * 100) / 100;
  const passRate = total > 0 ? Math.round((passed / total) * 10000) / 100 : 0;

  return {
    total,
    passed,
    failed,
    broken,
    skipped,
    unknown,
    duration_ms: durationMs,
    duration_seconds: durationSeconds,
    pass_rate: passRate,
  };
}

// -----------------------------
// Optional: Per-test parsing (top slowest + failures)
// -----------------------------

function safeGet<T>(d: Record<string, unknown>, key: string, defaultVal: T): T {
  const v = d[key];
  return (v === undefined || v === null ? defaultVal : v) as T;
}

function extractLabel(resultJson: Record<string, unknown>, labelName: string): string {
  const labels = (resultJson.labels ?? []) as Array<{ name?: string; value?: string }>;
  for (const lab of labels) {
    if (lab?.name === labelName) {
      return String(lab.value ?? '');
    }
  }
  return '';
}

export interface AllureTestRecord {
  name: string;
  status: string;
  duration_ms: number;
  duration_seconds: number;
  suite: string;
  feature: string;
  story: string;
  severity: string;
  message: string;
}

export function parseAllureResults(
  allureResultsDir: string = path.join(process.cwd(), 'allure-results')
): AllureTestRecord[] {
  if (!fs.existsSync(allureResultsDir)) {
    throw new Error(`Allure results directory not found: ${allureResultsDir}`);
  }
  const files = fs.readdirSync(allureResultsDir).filter((f) => f.endsWith('-result.json'));
  const tests: AllureTestRecord[] = [];

  for (const file of files.sort()) {
    const filePath = path.join(allureResultsDir, file);
    let data: Record<string, unknown>;
    try {
      data = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    } catch {
      continue;
    }
    const name = String(safeGet(data, 'name', file.replace('-result.json', '')));
    const status = String(safeGet(data, 'status', 'unknown'));
    const statusDetails = (data.statusDetails ?? {}) as Record<string, unknown>;
    const message = String(statusDetails.message ?? '');
    const start = (safeGet(data, 'start', 0) as number) ?? 0;
    const stop = (safeGet(data, 'stop', 0) as number) ?? 0;
    const durationMs =
      start && stop && stop >= start ? Math.round(stop - start) : 0;
    const suite =
      extractLabel(data, 'suite') || extractLabel(data, 'parentSuite');
    const feature = extractLabel(data, 'feature');
    const story = extractLabel(data, 'story');
    const severity = extractLabel(data, 'severity');

    tests.push({
      name,
      status,
      duration_ms: durationMs,
      duration_seconds: Math.round((durationMs / 1000) * 100) / 100,
      suite,
      feature,
      story,
      severity,
      message,
    });
  }
  return tests;
}

export function topSlowestTests(
  tests: AllureTestRecord[],
  topN: number = 5
): AllureTestRecord[] {
  return [...tests]
    .sort((a, b) => (b.duration_ms ?? 0) - (a.duration_ms ?? 0))
    .slice(0, topN);
}

export function failedTests(tests: AllureTestRecord[]): AllureTestRecord[] {
  return tests.filter((t) =>
    ['failed', 'broken'].includes(t.status ?? '')
  );
}

/** Count tests with duration >= thresholdSeconds (default 10s). */
export function countSlowTests(
  tests: AllureTestRecord[],
  thresholdSeconds: number = 10
): number {
  return tests.filter((t) => (t.duration_seconds ?? 0) >= thresholdSeconds).length;
}

/** Format seconds as human-readable (e.g. 1021.64 → "17m 2s", 3661 → "1h 1m 1s"). */
export function formatDuration(seconds: number): string {
  const s = Math.round(seconds);
  if (s < 60) return `${s}s`;
  const m = Math.floor(s / 60);
  const sec = s % 60;
  if (m < 60) return sec > 0 ? `${m}m ${sec}s` : `${m}m`;
  const h = Math.floor(m / 60);
  const min = m % 60;
  const parts = [`${h}h`];
  if (min > 0) parts.push(`${min}m`);
  if (sec > 0) parts.push(`${sec}s`);
  return parts.join(' ');
}

// -----------------------------
// RAG threshold logic (edit to match your standards)
// -----------------------------

export function ragStatus(summary: AllureSummary): string {
  if (summary.failed > 0 || summary.pass_rate < 95) {
    return '🔴 RED';
  }
  if (summary.broken > 0 || summary.pass_rate < 98) {
    return '🟠 AMBER';
  }
  return '🟢 GREEN';
}

// -----------------------------
// Slack posting (Incoming Webhook or Bot API)
// -----------------------------

/** Returns true if the value looks like a Slack channel name (#channel). */
function isChannelName(value: string): boolean {
  const trimmed = (value ?? '').trim();
  return trimmed.length > 0 && trimmed.startsWith('#');
}

/** Returns true if the value looks like a Slack Incoming Webhook URL (not a channel name). */
function isValidWebhookUrl(value: string): boolean {
  const trimmed = (value ?? '').trim();
  if (!trimmed) return false;
  if (trimmed.startsWith('#')) return false; // channel name
  try {
    const url = new URL(trimmed);
    return url.protocol === 'https:' && url.hostname.includes('slack.com');
  } catch {
    return false;
  }
}

/** Normalise channel to include # if missing. */
function normaliseChannel(channel: string): string {
  const trimmed = (channel ?? '').trim();
  return trimmed.startsWith('#') ? trimmed : `#${trimmed}`;
}

/** Post via Slack API chat.postMessage (same concept as Jenkins slackSend). Use with SLACK_BOT_TOKEN + channel name. */
export async function postToSlackViaApi(
  botToken: string,
  channel: string,
  text: string,
  timeoutMs: number = 20000
): Promise<void> {
  const ch = normaliseChannel(channel);
  const payload = JSON.stringify({ channel: ch, text });
  const options: https.RequestOptions = {
    hostname: 'slack.com',
    port: 443,
    path: '/api/chat.postMessage',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      Authorization: `Bearer ${botToken.trim()}`,
      'Content-Length': Buffer.byteLength(payload, 'utf-8'),
    },
  };

  return new Promise((resolve, reject) => {
    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => { body += chunk; });
      res.on('end', () => {
        if (res.statusCode !== 200) {
          reject(new Error(`Slack API failed: HTTP ${res.statusCode} - ${body}`));
          return;
        }
        try {
          const json = JSON.parse(body);
          if (!json.ok) {
            reject(new Error(`Slack API error: ${json.error ?? body}`));
            return;
        }
        resolve();
        } catch {
          reject(new Error(`Slack API invalid response: ${body}`));
        }
      });
    });
    req.on('error', reject);
    req.setTimeout(timeoutMs, () => {
      req.destroy();
      reject(new Error('Slack API timeout'));
    });
    req.write(payload, 'utf-8');
    req.end();
  });
}

export async function postToSlack(
  webhookUrl: string,
  text: string,
  timeoutMs: number = 20000
): Promise<void> {
  if (!isValidWebhookUrl(webhookUrl)) {
    throw new Error(
      'SLACK_WEBHOOK_URL must be a Slack Incoming Webhook URL (e.g. https://hooks.slack.com/...), not a channel name (e.g. #qa-pipeline-status)'
    );
  }
  const payload = JSON.stringify({ text });
  const url = new URL(webhookUrl);
  const isHttps = url.protocol === 'https:';
  const options: https.RequestOptions = {
    hostname: url.hostname,
    port: url.port || (isHttps ? 443 : 80),
    path: url.pathname + url.search,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(payload, 'utf-8'),
    },
  };

  return new Promise((resolve, reject) => {
    const req = (isHttps ? https : http).request(options, (res) => {
      if (res.statusCode && res.statusCode >= 200 && res.statusCode < 300) {
        resolve();
      } else {
        reject(
          new Error(`Slack webhook failed: HTTP ${res.statusCode ?? 'unknown'}`)
        );
      }
    });
    req.on('error', reject);
    req.setTimeout(timeoutMs, () => {
      req.destroy();
      reject(new Error('Slack webhook timeout'));
    });
    req.write(payload, 'utf-8');
    req.end();
  });
}

export function buildSlackMessage(
  summary: AllureSummary,
  buildNumber: string,
  buildUrl: string,
  reportPathSuffix: string = 'allure/',
  tests: AllureTestRecord[] | null = null,
  topNSlowest: number = 5,
  maxFailuresToList: number = 8,
  slowThresholdSeconds: number = 10
): string {
  const rag = ragStatus(summary);
  const reportUrl = buildUrl ? `${buildUrl}${reportPathSuffix}` : '';
  const lines: string[] = [];

  // Header + RAG
  lines.push(`*E2E Test Results* — Build #${buildNumber}  ${rag}`);
  lines.push('');

  // Link to Allure report from build (prominent)
  if (reportUrl) {
    lines.push(`*Allure report:* ${reportUrl}`);
    lines.push('');
  }

  // Status: one metric per line
  const slowCount =
    tests && tests.length > 0
      ? countSlowTests(tests, slowThresholdSeconds)
      : 0;
  const durationFormatted = formatDuration(summary.duration_seconds);
  lines.push('*Status*');
  lines.push(`Total: *${summary.total}*`);
  lines.push(`✅ Passed: *${summary.passed}*`);
  lines.push(`❌ Failed: *${summary.failed}*`);
  lines.push(`🐢 Slow (≥${slowThresholdSeconds}s): *${slowCount}*`);
  lines.push(`⏭️ Skipped: *${summary.skipped}*`);
  if (summary.broken > 0) {
    lines.push(`⚠️ Broken: *${summary.broken}*`);
  }
  lines.push(`Pass rate: *${summary.pass_rate}%*`);
  lines.push(`Duration: *${durationFormatted}*`);
  lines.push('');

  if (tests && tests.length > 0) {
    const fails = failedTests(tests);
    const slow = topSlowestTests(tests, topNSlowest);

    if (fails.length > 0) {
      lines.push(`*Failures / Broken* (${fails.length})`);
      for (const t of fails.slice(0, maxFailuresToList)) {
        const msg = (t.message ?? '').trim().replace(/\n/g, ' ');
        const truncated = msg.length > 120 ? msg.slice(0, 120) + '…' : msg;
        lines.push(
          `• \`${t.status}\` ${t.name} (${t.duration_seconds}s)` +
            (truncated ? ` — ${truncated}` : '')
        );
      }
      if (fails.length > maxFailuresToList) {
        lines.push(`  _…and ${fails.length - maxFailuresToList} more_`);
      }
      lines.push('');
    }

    if (slow.length > 0) {
      lines.push(`*Top ${slow.length} slowest tests*`);
      for (const t of slow) {
        lines.push(`• ${t.name} — ${t.duration_seconds}s`);
      }
      lines.push('');
    }
  }

  return lines.join('\n').trim();
}

// -----------------------------
// Main (copy/paste friendly)
// -----------------------------

export async function main(): Promise<void> {
  const printOnly = process.argv.includes('--print-only');
  const webhook = (process.env.SLACK_WEBHOOK_URL ?? '').trim();
  const buildNumber = (process.env.BUILD_NUMBER ?? 'local').trim();
  const buildUrl = (process.env.BUILD_URL ?? '').trim();
  const jobName = (process.env.JOB_NAME ?? 'e2e').trim();

  // Paths: when run from src/e2eTest (Jenkins), cwd is src/e2eTest; e2e-output is at repo root (../../e2e-output)
  const scriptDir = __dirname;
  const e2eTestDir = path.resolve(scriptDir, '..');
  const projectRootFromScript = path.resolve(e2eTestDir, '../..');
  const projectRootFromCwd = path.resolve(process.cwd(), '../..');
  const projectRoot = fs.existsSync(path.join(projectRootFromCwd, 'e2e-output'))
    ? projectRootFromCwd
    : projectRootFromScript;
  const baseDir = fs.existsSync(path.join(projectRoot, 'e2e-output'))
    ? projectRoot
    : e2eTestDir;

  let msg: string;
  try {
    const summaryPath = findAllureSummaryJson(baseDir);
    const summary = parseAllureSummary(summaryPath);
    let tests: AllureTestRecord[] | null = null;
    const allureResultsPath = path.join(e2eTestDir, 'allure-results');
    try {
      tests = parseAllureResults(allureResultsPath);
    } catch {
      tests = null;
    }
    const reportPathSuffix =
      (process.env.ALLURE_REPORT_PATH_SUFFIX ?? 'E2E_20Test_20Report/').trim() ||
      'E2E_20Test_20Report/';
    msg = buildSlackMessage(
      summary,
      buildNumber,
      buildUrl,
      reportPathSuffix,
      tests,
      5,
      8
    );
  } catch (err) {
    const reportUrl = buildUrl
      ? `${buildUrl}${process.env.ALLURE_REPORT_PATH_SUFFIX ?? 'E2E_20Test_20Report/'}`
      : '';
    msg = `E2E stage completed for ${jobName} build ${buildNumber}. Allure report not available – check build logs.${reportUrl ? `\n*Allure report:* ${reportUrl}` : buildUrl ? `\n*Build:* ${buildUrl}` : ''}`;
    if (!printOnly) {
      console.warn('[WARN] Could not read Allure summary; sending fallback message.', err);
    }
  }

  if (printOnly) {
    process.stdout.write(msg);
    return;
  }

  // 4) Print to console (useful for Jenkins logs)
  console.log(msg);

  // 5) Post to Slack
  const botToken = (process.env.SLACK_BOT_TOKEN ?? '').trim();
  const channel = (process.env.SLACK_CHANNEL ?? webhook).trim();

  // Same as Jenkins: channel name + Bot token → use Slack API (chat.postMessage)
  if (botToken && channel && (isChannelName(channel) || isChannelName(webhook))) {
    const targetChannel = isChannelName(channel) ? channel : webhook;
    try {
      await postToSlackViaApi(botToken, targetChannel, msg);
      console.log(`\n[INFO] Slack notification sent to ${normaliseChannel(targetChannel)}.`);
    } catch (err) {
      console.error('\n[ERROR] Failed to post to Slack:', err);
      process.exit(1);
    }
    return;
  }

  // Incoming Webhook URL
  if (webhook) {
    if (!isValidWebhookUrl(webhook)) {
      console.warn(
        '\n[WARN] SLACK_WEBHOOK_URL is not a valid webhook URL (got channel or invalid value). Skipping Slack post.'
      );
      console.warn('       For channel by name use: SLACK_BOT_TOKEN + SLACK_CHANNEL=#qa-pipeline-status');
    } else {
      try {
        await postToSlack(webhook, msg);
        console.log('\n[INFO] Slack notification sent successfully.');
      } catch (err) {
        console.error('\n[ERROR] Failed to post to Slack:', err);
        process.exit(1);
      }
    }
  } else {
    console.log('\n[INFO] No Slack config (set SLACK_CHANNEL + SLACK_BOT_TOKEN, or SLACK_WEBHOOK_URL); skipping Slack post.');
  }
}

// Run when executed directly (e.g. tsx scripts/allure-slack-notifier.ts)
const isMain =
  require.main === module ||
  process.argv[1]?.includes('allure-slack-notifier');

if (isMain) {
  main().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}
