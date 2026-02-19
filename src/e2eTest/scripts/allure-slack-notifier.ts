import * as fs from 'fs';
import * as path from 'path';
import * as https from 'https';
import * as http from 'http';

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
  const runCount = total - skipped;
  const passRate =
    runCount > 0 ? Math.round((passed / runCount) * 10000) / 100 : total > 0 ? 0 : 100;

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
  /** Used to deduplicate retries: same test across attempts shares this id */
  historyId?: string;
  fullName?: string;
  /** Start timestamp (ms) for ordering retries – keep latest attempt */
  start?: number;
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
    const historyId = (data.historyId as string) ?? '';
    const fullName = (data.fullName as string) ?? '';

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
      historyId: historyId || undefined,
      fullName: fullName || undefined,
      start,
    });
  }
  return tests;
}

/** Tests with duration >= minDurationSeconds, sorted slowest first, up to topN. */
export function topSlowestTests(
  tests: AllureTestRecord[],
  topN: number = 5,
  minDurationSeconds: number = 5 * 60
): AllureTestRecord[] {
  return [...tests]
    .filter((t) => (t.duration_seconds ?? 0) >= minDurationSeconds)
    .sort((a, b) => (b.duration_ms ?? 0) - (a.duration_ms ?? 0))
    .slice(0, topN);
}

export function failedTests(tests: AllureTestRecord[]): AllureTestRecord[] {
  return tests.filter((t) =>
    ['failed', 'broken'].includes(t.status ?? '')
  );
}

export function deduplicateFailedTestsByRetry(tests: AllureTestRecord[]): AllureTestRecord[] {
  return failedTests(latestResultByKey(tests));
}

export function countSlowTests(
  tests: AllureTestRecord[],
  thresholdSeconds: number = 5 * 60
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

export function ragStatus(summary: AllureSummary): string {
  if (summary.failed > 0 || summary.pass_rate < 95) {
    return '🔴 RED';
  }
  if (summary.broken > 0 || summary.pass_rate < 98) {
    return '🟠 AMBER';
  }
  return '🟢 GREEN';
}

/** One record per test (latest by start time), keyed by historyId/fullName/name. */
function latestResultByKey(tests: AllureTestRecord[]): AllureTestRecord[] {
  const byKey = new Map<string, AllureTestRecord>();
  for (const t of tests) {
    const key = (t.historyId ?? t.fullName ?? t.name).trim() || t.name;
    const existing = byKey.get(key);
    const start = t.start ?? 0;
    const existingStart = existing?.start ?? 0;
    if (!existing || start >= existingStart) byKey.set(key, t);
  }
  return Array.from(byKey.values());
}

function latestResultSummary(tests: AllureTestRecord[]): { failed: number; broken: number; pass_rate: number } {
  const latest = latestResultByKey(tests);
  const total = latest.length;
  const failed = latest.filter((t) => t.status === 'failed').length;
  const broken = latest.filter((t) => t.status === 'broken').length;
  const skipped = latest.filter((t) => t.status === 'skipped').length;
  const passed = total - failed - broken - skipped;
  const runCount = total - skipped;
  const pass_rate =
    runCount > 0 ? Math.round((passed / runCount) * 10000) / 100 : total > 0 ? 0 : 100;
  return { failed, broken, pass_rate };
}

function isValidWebhookUrl(value: string): boolean {
  const trimmed = (value ?? '').trim();
  if (!trimmed || trimmed.startsWith('#')) return false;
  try {
    const url = new URL(trimmed);
    return url.protocol === 'https:' && url.hostname.includes('slack.com');
  } catch {
    return false;
  }
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
  slowThresholdSeconds: number = 5 * 60,
  serviceName: string = 'pcs-api',
  pipelineType: string = 'nightly'
): string {
  const summaryForRag =
    tests && tests.length > 0
      ? { ...summary, ...latestResultSummary(tests) }
      : summary;
  const rag = ragStatus(summaryForRag);
  const reportUrl = buildUrl ? `${buildUrl}${reportPathSuffix}` : '';
  const lines: string[] = [];
  lines.push(`*E2E Test Results* — Build #${buildNumber}  ${rag}`);
  lines.push(`*Service:* ${serviceName}  |  *Pipeline:* ${pipelineType}`);
  lines.push('');
  if (reportUrl) {
    lines.push(`*Allure report:* ${reportUrl}`);
    lines.push('');
  }
  const slowCount =
    tests && tests.length > 0
      ? countSlowTests(tests, slowThresholdSeconds)
      : 0;
  const durationFormatted = formatDuration(summary.duration_seconds);
  lines.push('*Status*');
  lines.push(`Total: *${summary.total}*`);
  lines.push(`✅ Passed: *${summary.passed}*`);
  lines.push(`❌ Failed: *${summary.failed}*`);
  const slowLabel =
    slowThresholdSeconds >= 60
      ? `≥${Math.round(slowThresholdSeconds / 60)}m`
      : `≥${slowThresholdSeconds}s`;
  lines.push(`🐢 Slow (${slowLabel}): *${slowCount}*`);
  lines.push(`⏭️ Skipped: *${summary.skipped}*`);
  if (summary.broken > 0) {
    lines.push(`⚠️ Broken: *${summary.broken}*`);
  }
  lines.push(`Pass rate: *${summary.pass_rate}%*`);
  lines.push(`Duration: *${durationFormatted}*`);
  lines.push('');

  if (tests && tests.length > 0) {
    const fails = deduplicateFailedTestsByRetry(tests);
    const slow = topSlowestTests(tests, topNSlowest, slowThresholdSeconds);

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
      lines.push(`*Top ${slow.length} slowest tests* (${slowLabel})`);
      for (const t of slow) {
        lines.push(`• ${t.name} — ${t.duration_seconds}s`);
      }
      lines.push('');
    }
  }

  return lines.join('\n').trim();
}

const DEFAULT_REPORT_PATH = 'E2E_20Test_20Report/';

function resolveDirs(): { baseDir: string; e2eTestDir: string } {
  const e2eTestDir = path.resolve(__dirname, '..');
  const projectRootFromScript = path.resolve(e2eTestDir, '../..');
  const projectRootFromCwd = path.resolve(process.cwd(), '../..');
  const projectRoot = fs.existsSync(path.join(projectRootFromCwd, 'e2e-output'))
    ? projectRootFromCwd
    : projectRootFromScript;
  const baseDir = fs.existsSync(path.join(projectRoot, 'e2e-output')) ? projectRoot : e2eTestDir;
  return { baseDir, e2eTestDir };
}

export function getSlackMessage(): string {
  const { baseDir, e2eTestDir } = resolveDirs();
  const buildNumber = (process.env.BUILD_NUMBER ?? 'local').trim();
  const buildUrl = (process.env.BUILD_URL ?? '').trim();
  const jobName = (process.env.JOB_NAME ?? 'e2e').trim();
  const reportSuffix = (process.env.ALLURE_REPORT_PATH_SUFFIX ?? DEFAULT_REPORT_PATH).trim() || DEFAULT_REPORT_PATH;
  const serviceName = (process.env.E2E_SERVICE_NAME ?? process.env.COMPONENT ?? 'pcs-api').trim() || 'pcs-api';
  const pipelineType = (process.env.E2E_PIPELINE_TYPE ?? (jobName.toLowerCase().includes('nightly') ? 'nightly' : 'master')).trim() || 'master';

  try {
    const summary = parseAllureSummary(findAllureSummaryJson(baseDir));
    let tests: AllureTestRecord[] | null = null;
    try {
      tests = parseAllureResults(path.join(e2eTestDir, 'allure-results'));
    } catch {
      /* optional */
    }
    return buildSlackMessage(summary, buildNumber, buildUrl, reportSuffix, tests, 5, 8, 5 * 60, serviceName, pipelineType);
  } catch (err) {
    if (!process.argv.includes('--print-only')) {
      console.warn('[WARN] Could not read Allure summary; sending fallback.', err);
    }
    const reportUrl = buildUrl ? `${buildUrl}${reportSuffix}` : '';
    return `E2E Test Results — Build #${buildNumber}\n*Service:* ${serviceName}  |  *Pipeline:* ${pipelineType}\n\nAllure report not available – check build logs.${reportUrl ? `\n*Allure report:* ${reportUrl}` : buildUrl ? `\n*Build:* ${buildUrl}` : ''}`;
  }
}

export async function main(): Promise<void> {
  const printOnly = process.argv.includes('--print-only');
  const msg = getSlackMessage();

  if (printOnly) {
    process.stdout.write(msg);
    return;
  }

  console.log(msg);

  const webhook = (process.env.SLACK_WEBHOOK_URL ?? '').trim();
  if (!webhook) {
    console.log('\n[INFO] No SLACK_WEBHOOK_URL; skipping post.');
    return;
  }
  if (!isValidWebhookUrl(webhook)) {
    console.warn('\n[WARN] SLACK_WEBHOOK_URL is not a valid webhook URL; skipping post.');
    return;
  }
  await postToSlack(webhook, msg);
  console.log('\n[INFO] Slack notification sent.');
}

const isMain =
  require.main === module ||
  process.argv[1]?.includes('allure-slack-notifier');

if (isMain) {
  main().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}
