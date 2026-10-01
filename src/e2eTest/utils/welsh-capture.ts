import * as fs from 'fs';
import * as path from 'path';
import type { BrowserContext, Request, TestInfo } from '@playwright/test';

/**
 * Welsh translation capture.
 *
 * When WELSH_CAPTURE=1 every test runs with the ExUI language cookie set to Welsh, so the
 * rpx-xui-translation client asks the translation service for every phrase it renders
 * (in English mode it never makes the request). We intercept that call:
 *
 *   1. answer the browser straight away with the English phrases echoed back, so the UI and
 *      the suite's English text assertions are unaffected;
 *   2. in the background, replay the same request to AAT (with the browser's XSRF header and
 *      the context's cookies) to learn whether each phrase actually has a Welsh translation;
 *   3. append one JSON line per phrase to welsh-capture/worker-<n>.jsonl.
 *
 * scripts/welsh-capture-report.ts turns those files into report.csv / untranslated.csv.
 */

export const WELSH_CAPTURE_ENABLED = ['1', 'true'].includes((process.env.WELSH_CAPTURE ?? '').toLowerCase());

export const WELSH_CAPTURE_DIR = path.join(__dirname, '..', 'welsh-capture');
const LANGUAGE_COOKIE = 'exui-preferred-language';
const TRANSLATION_URL_GLOB = '**/api/translation/cy';
const HEADERS_TO_REPLAY = ['x-xsrf-token', 'accept', 'content-type'];
const LOOKUP_TIMEOUT_MS = 30_000;
const FLUSH_TIMEOUT_MS = 20_000;
const MAX_VISIBILITY_CHECK_LENGTH = 300;

export interface WelshCaptureRecord {
  ts: string;
  spec: string;
  title: string;
  url: string;
  phrase: string;
  /** true = AAT returned Welsh, false = AAT echoed English (no translation), null = lookup failed */
  translated: boolean | null;
  translation: string | null;
  /** For untranslated phrases only: true/false if an element with exactly this text was found, null otherwise */
  visible: boolean | null;
}

type TranslationsBody = { translations?: Record<string, { translation?: string } | string> };

const warned = new Set<string>();
function warnOnce(message: string): void {
  if (warned.has(message)) return;
  warned.add(message);
  console.warn(`[welsh-capture] ${message}`);
}

export async function enableWelshCapture(context: BrowserContext, testInfo: TestInfo): Promise<() => Promise<void>> {
  // Create the output directory before anything can fail, so the report can tell
  // "fixture never ran" apart from "fixture ran but captured nothing".
  fs.mkdirSync(WELSH_CAPTURE_DIR, { recursive: true });
  warnOnce(`enabled — recording to ${WELSH_CAPTURE_DIR}`);
  const baseUrl = process.env.MANAGE_CASE_BASE_URL;
  if (!baseUrl) {
    throw new Error('WELSH_CAPTURE=1 needs MANAGE_CASE_BASE_URL (global setup derives it from ENVIRONMENT).');
  }
  const outFile = path.join(WELSH_CAPTURE_DIR, `worker-${testInfo.workerIndex}.jsonl`);
  const spec = path.relative(testInfo.project.testDir, testInfo.file);
  const title = testInfo.titlePath.slice(1).join(' > ');

  await context.addCookies([{ name: LANGUAGE_COOKIE, value: 'cy', url: baseUrl }]);

  const pending = new Set<Promise<void>>();

  await context.route(TRANSLATION_URL_GLOB, async (route) => {
    const request = route.request();
    if (request.method() !== 'POST') {
      await route.continue();
      return;
    }
    const phrases: string[] = (request.postDataJSON() as { phrases?: string[] } | null)?.phrases ?? [];
    const pageUrl = request.frame().page().url();

    await route.fulfill({
      json: { translations: Object.fromEntries(phrases.map((p) => [p, { translation: p }])) },
    });

    const job = lookupAndRecord(context, request, phrases, pageUrl, { spec, title, outFile }).finally(() => pending.delete(job));
    pending.add(job);
  });

  return async function flush(): Promise<void> {
    if (pending.size === 0) return;
    await Promise.race([
      Promise.allSettled([...pending]),
      new Promise<void>((resolve) => setTimeout(resolve, FLUSH_TIMEOUT_MS)),
    ]);
  };
}

async function lookupAndRecord(
  context: BrowserContext,
  request: Request,
  phrases: string[],
  pageUrl: string,
  meta: { spec: string; title: string; outFile: string },
): Promise<void> {
  if (phrases.length === 0) return;

  const headers: Record<string, string> = {};
  const requestHeaders = await request.allHeaders();
  for (const name of HEADERS_TO_REPLAY) {
    if (requestHeaders[name]) headers[name] = requestHeaders[name];
  }

  let real: Record<string, { translation?: string } | string> | null = null;
  try {
    const response = await context.request.post(request.url(), { data: { phrases }, headers, timeout: LOOKUP_TIMEOUT_MS });
    if (response.ok()) {
      real = ((await response.json()) as TranslationsBody).translations ?? null;
    } else {
      warnOnce(`translation lookup returned HTTP ${response.status()} — phrases recorded with translated=null`);
    }
  } catch (error) {
    warnOnce(`translation lookup failed (${(error as Error).message}) — phrases recorded with translated=null`);
  }

  const records: WelshCaptureRecord[] = [];
  for (const phrase of phrases) {
    const entry = real?.[phrase];
    const translation = entry === undefined ? null : typeof entry === 'string' ? entry : (entry.translation ?? null);
    const translated = real === null ? null : translation !== null && translation !== phrase;
    const visible = translated === false ? await isPhraseVisible(request, phrase) : null;
    records.push({
      ts: new Date().toISOString(),
      spec: meta.spec,
      title: meta.title,
      url: pageUrl,
      phrase,
      translated,
      translation,
      visible,
    });
  }
  fs.appendFileSync(meta.outFile, records.map((r) => JSON.stringify(r)).join('\n') + '\n');
}

/** Best-effort: is an element whose text is exactly this phrase visible on the page that asked for it? */
async function isPhraseVisible(request: Request, phrase: string): Promise<boolean | null> {
  if (phrase.length > MAX_VISIBILITY_CHECK_LENGTH || /\r|\n/.test(phrase)) return null;
  try {
    const page = request.frame().page();
    if (page.isClosed()) return null;
    const matches = page.getByText(phrase, { exact: true });
    if ((await matches.count()) === 0) return null;
    return await matches.first().isVisible();
  } catch {
    return null;
  }
}
