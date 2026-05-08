import type {APIRequestContext, Page} from '@playwright/test';
import {expect} from '@playwright/test';

/** Strip trailing punctuation accidentally captured from plain-text emails. */
function trimUrlSuffix(u: string): string {
  return u.replace(/[),.;]+$/g, '');
}

function extractHrefUrls(html: string): string[] {
  const out: string[] = [];
  const re = /href\s*=\s*["'](https?:\/\/[^"'>\s]+)/gi;
  let m: RegExpExecArray | null;
  while ((m = re.exec(html)) !== null) {
    out.push(trimUrlSuffix(m[1]));
  }
  return out;
}

function extractBareHttpsUrls(text: string): string[] {
  const re = /https?:\/\/[^\s<>"']+/gi;
  return [...text.matchAll(re)].map((x) => trimUrlSuffix(x[0]));
}

/** Prefer IdAM / activation style links when the body is a full email (HTML or text). */
function preferActivationUrl(urls: string[]): string | undefined {
  if (urls.length === 0) {
    return undefined;
  }
  const patterns = [/idam/i, /hmcts/i, /forgerock/i, /activate/i, /registration/i, /password/i, /oauth/i, /openid/i];
  for (const p of patterns) {
    const hit = urls.find((u) => p.test(u));
    if (hit) {
      return hit;
    }
  }
  return urls[0];
}

/**
 * `body` from IdAM testing-support is often the raw email: prose + HTML + activation link.
 */
export function extractLinkFromEmailBody(body: string): string | undefined {
  const hrefs = extractHrefUrls(body);
  if (hrefs.length > 0) {
    return preferActivationUrl(hrefs);
  }
  const bare = extractBareHttpsUrls(body);
  if (bare.length > 0) {
    return preferActivationUrl(bare);
  }
  return undefined;
}

function extractHttpUrl(text: string): string | undefined {
  const bare = extractBareHttpsUrls(text);
  return bare[0];
}

/** Parse IdAM testing-support latest-notification response and return the activation/set-password URL. */
export function extractLinkFromNotificationPayload(raw: string): string {
  const trimmed = raw.trim();
  try {
    const j = JSON.parse(trimmed) as Record<string, unknown>;

    // API often returns { "body": "<html>…<a href=\"https://…\">…" } or plain text + URL
    if (typeof j.body === 'string') {
      const fromBody = extractLinkFromEmailBody(j.body);
      if (fromBody) {
        return fromBody;
      }
    }

    const richEmailKeys = ['notificationBody', 'message', 'content', 'htmlBody', 'textBody'];
    for (const k of richEmailKeys) {
      const v = j[k];
      if (typeof v === 'string' && v.includes('http')) {
        const u = extractLinkFromEmailBody(v);
        if (u) {
          return u;
        }
      }
    }

    const directUrlKeys = ['link', 'url', 'href', 'activationLink'];
    for (const k of directUrlKeys) {
      const v = j[k];
      if (typeof v === 'string') {
        const t = v.trim();
        const u = t.startsWith('http') ? trimUrlSuffix(t) : extractHttpUrl(v);
        if (u) {
          return u;
        }
      }
    }

    for (const v of Object.values(j)) {
      if (typeof v === 'string' && v.includes('http')) {
        const u = extractLinkFromEmailBody(v) ?? extractHttpUrl(v);
        if (u) {
          return u;
        }
      }
    }
  } catch {
    /* plain text / HTML */
  }
  const u = extractLinkFromEmailBody(trimmed) ?? extractHttpUrl(trimmed);
  if (u) {
    return u;
  }
  throw new Error(`Could not find an http(s) URL in IdAM notification response: ${trimmed.slice(0, 400)}`);
}

export type WaitForNotificationLinkParams = {
  hmctsEnv: string;
  email: string;
  bearerToken: string;
  /** Total wait if notification is delayed (ms). */
  timeoutMs?: number;
  intervalMs?: number;
};

/** GET …/test/idam/notifications/latest/{email} until a parseable link is returned. */
export async function waitForLatestIdamNotificationLink(
  request: APIRequestContext,
  p: WaitForNotificationLinkParams,
): Promise<string> {
  const enc = encodeURIComponent(p.email);
  const url = `https://idam-testing-support-api.${p.hmctsEnv}.platform.hmcts.net/test/idam/notifications/latest/${enc}`;
  const timeoutMs = p.timeoutMs ?? 90_000;
  const intervalMs = p.intervalMs ?? 3_000;
  const deadline = Date.now() + timeoutMs;
  let lastError: unknown;

  while (Date.now() < deadline) {
    try {
      const res = await request.get(url, {
        headers: {accept: '*/*', Authorization: `Bearer ${p.bearerToken}`},
      });
      if (res.ok()) {
        const text = await res.text();
        return extractLinkFromNotificationPayload(text);
      }
      lastError = new Error(`GET ${url} → ${res.status()} ${await res.text()}`);
    } catch (e) {
      lastError = e;
    }
    await new Promise((r) => setTimeout(r, intervalMs));
  }
  throw lastError instanceof Error ? lastError : new Error(String(lastError));
}

/**
 * Open IdAM activation / set-password link and submit.
 * IdAM typically shows **two** password fields (new + confirm); we wait until both exist, then fill each.
 */
export async function completeIdamPasswordActivation(page: Page, activationUrl: string, newPassword: string): Promise<void> {
  await page.goto(activationUrl, {waitUntil: 'domcontentloaded'});

  const passwords = page.locator('input[type="password"]:visible');
  await expect(passwords.first()).toBeVisible({timeout: 30_000});

  await expect
    .poll(async () => passwords.count(), {timeout: 20_000})
    .toBeGreaterThanOrEqual(2);

  await passwords.nth(0).fill(newPassword);
  await passwords.nth(1).fill(newPassword);

  const submit = page.getByRole('button', {name: /continue|submit|save|set password|next|confirm/i}).first();
  await expect(submit).toBeVisible({timeout: 10_000});
  await submit.click();
}
