import * as fs from 'fs';
import * as path from 'path';

const SUMMARY_CANDIDATES = [
  'e2e-output/widgets/summary.json',
  'e2e-output/data/widgets/summary.json',
  'allure-report/widgets/summary.json',
  'allure-report/data/widgets/summary.json',
];

const DEFAULT_REPORT_PATH = 'E2E_20Test_20Report/';
const SLOW_THRESHOLD_SEC = 5 * 60;
const TOP_SLOW_N = 5;
const MAX_FAILURES_LIST = 8;

interface AllureSummary {
  total: number;
  passed: number;
  failed: number;
  broken: number;
  skipped: number;
  duration_seconds: number;
  pass_rate: number;
}

interface AllureTestRecord {
  name: string;
  status: string;
  duration_ms: number;
  duration_seconds: number;
  message: string;
  historyId?: string;
  fullName?: string;
  start?: number;
}

function findSummaryJson(baseDir: string): string {
  for (const p of SUMMARY_CANDIDATES) {
    const fullPath = path.join(baseDir, p);
    if (fs.existsSync(fullPath)) return fullPath;
  }
  throw new Error(
    `Allure summary.json not found. Checked: ${SUMMARY_CANDIDATES.map(c => path.join(baseDir, c)).join(', ')}`
  );
}

function parseSummary(summaryPath: string): AllureSummary {
  const data = JSON.parse(fs.readFileSync(summaryPath, 'utf-8'));
  const stats = data.statistic ?? {};
  const timeInfo = data.time ?? {};
  const total = parseInt(String(stats.total ?? 0), 10) || 0;
  const passed = parseInt(String(stats.passed ?? 0), 10) || 0;
  const failed = parseInt(String(stats.failed ?? 0), 10) || 0;
  const broken = parseInt(String(stats.broken ?? 0), 10) || 0;
  const skipped = parseInt(String(stats.skipped ?? 0), 10) || 0;
  const durationMs = parseInt(String(timeInfo.duration ?? 0), 10) || 0;
  const durationSeconds = Math.round((durationMs / 1000) * 100) / 100;
  const runCount = total - skipped;
  const pass_rate = runCount > 0 ? Math.round((passed / runCount) * 10000) / 100 : total > 0 ? 0 : 100;

  return {
    total,
    passed,
    failed,
    broken,
    skipped,
    duration_seconds: durationSeconds,
    pass_rate,
  };
}

function parseResults(resultsDir: string): AllureTestRecord[] {
  if (!fs.existsSync(resultsDir)) return [];
  const files = fs.readdirSync(resultsDir).filter(f => f.endsWith('-result.json'));
  const tests: AllureTestRecord[] = [];

  for (const file of files.sort()) {
    try {
      const data = JSON.parse(fs.readFileSync(path.join(resultsDir, file), 'utf-8')) as Record<string, unknown>;
      const name = String(data.name ?? file.replace('-result.json', ''));
      const status = String(data.status ?? 'unknown');
      const statusDetails = (data.statusDetails ?? {}) as Record<string, unknown>;
      const message = String(statusDetails.message ?? '');
      const start = (data.start as number) ?? 0;
      const stop = (data.stop as number) ?? 0;
      const durationMs = start && stop && stop >= start ? Math.round(stop - start) : 0;
      const historyId = (data.historyId as string) ?? '';
      const fullName = (data.fullName as string) ?? '';

      tests.push({
        name,
        status,
        duration_ms: durationMs,
        duration_seconds: Math.round((durationMs / 1000) * 100) / 100,
        message,
        historyId: historyId || undefined,
        fullName: fullName || undefined,
        start,
      });
    } catch {
      // skip malformed files
    }
  }
  return tests;
}

function latestResultByKey(tests: AllureTestRecord[]): AllureTestRecord[] {
  const byKey = new Map<string, AllureTestRecord>();
  for (const t of tests) {
    const key = (t.historyId ?? t.fullName ?? t.name).trim() || t.name;
    const existing = byKey.get(key);
    if (!existing || (t.start ?? 0) >= (existing.start ?? 0)) byKey.set(key, t);
  }
  return Array.from(byKey.values());
}

function failedTests(tests: AllureTestRecord[]): AllureTestRecord[] {
  return tests.filter(t => ['failed', 'broken'].includes(t.status ?? ''));
}

function deduplicateFailed(tests: AllureTestRecord[]): AllureTestRecord[] {
  return failedTests(latestResultByKey(tests));
}

function topSlowest(tests: AllureTestRecord[], n: number, minSec: number): AllureTestRecord[] {
  return [...tests]
    .filter(t => (t.duration_seconds ?? 0) >= minSec)
    .sort((a, b) => (b.duration_ms ?? 0) - (a.duration_ms ?? 0))
    .slice(0, n);
}

function countSlow(tests: AllureTestRecord[], thresholdSec: number): number {
  return tests.filter(t => (t.duration_seconds ?? 0) >= thresholdSec).length;
}

function formatDuration(seconds: number): string {
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

function ragStatus(summary: AllureSummary): string {
  if (summary.failed > 0 || summary.pass_rate < 95) return '🔴 RED';
  if (summary.broken > 0 || summary.pass_rate < 98) return '🟠 AMBER';
  return '🟢 GREEN';
}

function latestResultSummary(tests: AllureTestRecord[]): { failed: number; broken: number; pass_rate: number } {
  const latest = latestResultByKey(tests);
  const total = latest.length;
  const failed = latest.filter(t => t.status === 'failed').length;
  const broken = latest.filter(t => t.status === 'broken').length;
  const skipped = latest.filter(t => t.status === 'skipped').length;
  const passed = total - failed - broken - skipped;
  const runCount = total - skipped;
  const pass_rate = runCount > 0 ? Math.round((passed / runCount) * 10000) / 100 : total > 0 ? 0 : 100;
  return { failed, broken, pass_rate };
}

function buildMessage(
  summary: AllureSummary,
  buildNumber: string,
  buildUrl: string,
  reportSuffix: string,
  tests: AllureTestRecord[] | null,
  serviceName: string,
  pipelineType: string
): string {
  const summaryForRag = tests && tests.length > 0 ? { ...summary, ...latestResultSummary(tests) } : summary;
  const rag = ragStatus(summaryForRag);
  const reportUrl = buildUrl ? `${buildUrl}${reportSuffix}` : '';
  const lines: string[] = [
    `*E2E Test Results* — Build #${buildNumber}  ${rag}`,
    `*Service:* ${serviceName}  |  *Pipeline:* ${pipelineType}`,
    '',
  ];
  if (reportUrl) {
    lines.push(`*Allure report:* ${reportUrl}`, '');
  }
  const slowCount = tests && tests.length > 0 ? countSlow(tests, SLOW_THRESHOLD_SEC) : 0;
  const slowLabel = SLOW_THRESHOLD_SEC >= 60 ? `≥${Math.round(SLOW_THRESHOLD_SEC / 60)}m` : `≥${SLOW_THRESHOLD_SEC}s`;
  lines.push(
    '*Status*',
    `Total: *${summary.total}*`,
    `✅ Passed: *${summary.passed}*`,
    `❌ Failed: *${summary.failed}*`,
    `🐢 Slow (${slowLabel}): *${slowCount}*`,
    `⏭️ Skipped: *${summary.skipped}*`,
    ...(summary.broken > 0 ? [`⚠️ Broken: *${summary.broken}*`] : []),
    `Pass rate: *${summary.pass_rate}%*`,
    `Duration: *${formatDuration(summary.duration_seconds)}*`,
    ''
  );

  if (tests && tests.length > 0) {
    const fails = deduplicateFailed(tests);
    const slow = topSlowest(tests, TOP_SLOW_N, SLOW_THRESHOLD_SEC);
    if (fails.length > 0) {
      lines.push(`*Failures / Broken* (${fails.length})`);
      for (const t of fails.slice(0, MAX_FAILURES_LIST)) {
        const msg = (t.message ?? '').trim().replace(/\n/g, ' ');
        const truncated = msg.length > 120 ? msg.slice(0, 120) + '…' : msg;
        lines.push(`• \`${t.status}\` ${t.name} (${t.duration_seconds}s)` + (truncated ? ` — ${truncated}` : ''));
      }
      if (fails.length > MAX_FAILURES_LIST) {
        lines.push(`  _…and ${fails.length - MAX_FAILURES_LIST} more_`);
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

/** pcs-api: script lives in src/e2eTest/scripts; allure may be at project root (e2e-output) or e2eTestDir. */
function resolveDirs(): { baseDir: string; resultsDir: string } {
  const e2eTestDir = path.resolve(__dirname, '..');
  const projectRootFromScript = path.resolve(e2eTestDir, '../..');
  const projectRootFromCwd = path.resolve(process.cwd(), '../..');
  const projectRoot = fs.existsSync(path.join(projectRootFromCwd, 'e2e-output'))
    ? projectRootFromCwd
    : projectRootFromScript;
  const baseDir = fs.existsSync(path.join(projectRoot, 'e2e-output')) ? projectRoot : e2eTestDir;
  const resultsDir = path.join(e2eTestDir, 'allure-results');
  return { baseDir, resultsDir };
}

function getSlackMessage(): string {
  const { baseDir, resultsDir } = resolveDirs();
  const buildNumber = (process.env.BUILD_NUMBER ?? 'local').trim();
  const buildUrl = (process.env.BUILD_URL ?? '').trim();
  const jobName = (process.env.JOB_NAME ?? 'e2e').trim();
  const reportSuffix = (process.env.ALLURE_REPORT_PATH_SUFFIX ?? DEFAULT_REPORT_PATH).trim() || DEFAULT_REPORT_PATH;
  const serviceName = (process.env.E2E_SERVICE_NAME ?? 'pcs-api').trim() || 'pcs-api';
  const pipelineType =
    (process.env.E2E_PIPELINE_TYPE ?? (jobName.toLowerCase().includes('nightly') ? 'nightly' : 'master')).trim() ||
    'master';

  try {
    const summary = parseSummary(findSummaryJson(baseDir));
    let tests: AllureTestRecord[] | null = null;
    if (fs.existsSync(resultsDir)) {
      tests = parseResults(resultsDir);
    }
    return buildMessage(summary, buildNumber, buildUrl, reportSuffix, tests, serviceName, pipelineType);
  } catch {
    const reportUrl = buildUrl ? `${buildUrl}${reportSuffix}` : '';
    return [
      `E2E Test Results — Build #${buildNumber}`,
      `*Service:* ${serviceName}  |  *Pipeline:* ${pipelineType}`,
      '',
      'Allure report not available – check build logs.',
      reportUrl ? `*Allure report:* ${reportUrl}` : buildUrl ? `*Build:* ${buildUrl}` : '',
    ]
      .filter(Boolean)
      .join('\n');
  }
}

function main(): void {
  const msg = getSlackMessage();
  if (process.argv.includes('--print-only')) {
    process.stdout.write(msg);
  } else {
    console.log(msg);
  }
}

if (require.main === module || process.argv[1]?.includes('allure-slack-notifier')) {
  try {
    main();
  } catch (err) {
    console.error(err);
    process.exit(1);
  }
}
