// The caption bar lives in a closed shadow root attached to <html>, so selectors, `innerText`, the page content
// checks and the axe audit cannot see it, and it ignores the pointer, so it never takes a click.
import { BrowserContext, Page } from '@playwright/test';

export type DemoSetUpStep = { text: string; done: boolean };

export type DemoCaption = {
  scenario: string;
  step: string;
  setUp: boolean;
  status?: string;
  resolved?: string[];
  setUpCard?: DemoSetUpStep[];
};

const BINDING = '__pcsDemoCaption';
const RENDER = '__pcsDemoRenderCaption';

let current: DemoCaption | undefined;
const installed = new WeakSet<BrowserContext>();

export async function showDemoCaption(page: Page, caption: DemoCaption): Promise<void> {
  current = caption;
  await install(page.context());
  await render(page);
}

export async function showDemoCaptionStatus(page: Page, status: string | undefined): Promise<void> {
  if (!current) {
    return;
  }
  current = { ...current, status };
  await render(page);
}

export async function showDemoCaptionResolved(page: Page, resolved: string[]): Promise<void> {
  if (!current || resolved.length === 0 || sameValues(current.resolved, resolved)) {
    return;
  }
  current = { ...current, resolved };
  await render(page);
}

export function resolvedValuesIn(step: string, values: Record<string, string>): string[] {
  return Object.entries(values)
    .map(([quoted, actual]) => ({ quoted, actual, at: step.indexOf(`"${quoted}"`) }))
    .filter(({ quoted, actual, at }) => at >= 0 && actual && actual !== quoted)
    .sort((one, other) => one.at - other.at)
    .map(({ quoted, actual }) => `"${quoted}" is ${actual}`);
}

const sameValues = (one: string[] | undefined, other: string[]): boolean =>
  one?.length === other.length && other.every((value, index) => one?.[index] === value);

/** Retried once the document has loaded: a page mid-navigation rejects the evaluate, and a new document has no render function yet. */
async function render(page: Page): Promise<void> {
  for (let attempt = 1; attempt <= 2; attempt++) {
    if (page.isClosed()) {
      return;
    }
    const drawn = await page
      .evaluate(([name, caption]) => {
        const draw = (window as unknown as Record<string, ((c: unknown) => void) | undefined>)[name];
        draw?.(caption);
        return Boolean(draw);
      }, [RENDER, current] as const)
      .catch(() => false);
    if (drawn) {
      return;
    }
    await page.waitForLoadState('domcontentloaded', { timeout: 5_000 }).catch(() => undefined);
  }
}

async function install(context: BrowserContext): Promise<void> {
  if (installed.has(context)) {
    return;
  }
  installed.add(context);
  try {
    await context.exposeBinding(BINDING, () => current);
    await context.addInitScript(captionScript, { binding: BINDING, render: RENDER });
    // The init script covers documents created from now on; the page already open is given it directly.
    for (const page of context.pages()) {
      await page.evaluate(captionScript, { binding: BINDING, render: RENDER }).catch(() => undefined);
    }
  } catch {
    // Only for the recording.
  }
}

/** Runs in the page. Self-contained: Playwright serialises it. */
function captionScript({ binding, render }: { binding: string; render: string }): void {
  const w = window as unknown as Record<string, unknown>;
  if (window !== window.top || w[render]) {
    return;
  }
  let host: HTMLElement | undefined;
  let root: ShadowRoot | undefined;

  const ensureHost = (): ShadowRoot | undefined => {
    if (!document.documentElement) {
      return undefined;
    }
    if (!host || !root) {
      host = document.createElement('pcs-demo-caption');
      host.setAttribute('aria-hidden', 'true');
      host.style.cssText = 'all: initial; position: fixed; left: 0; right: 0; bottom: 0; z-index: 2147483647; pointer-events: none;';
      root = host.attachShadow({ mode: 'closed' });
    }
    if (!host.isConnected) {
      document.documentElement.appendChild(host);
    }
    return root;
  };

  const draw = (
    caption:
      | {
          scenario: string;
          step: string;
          setUp: boolean;
          status?: string;
          resolved?: string[];
          setUpCard?: { text: string; done: boolean }[];
        }
      | undefined,
  ): void => {
    const shadow = ensureHost();
    if (!shadow) {
      return;
    }
    if (!caption) {
      shadow.replaceChildren();
      return;
    }
    const style = document.createElement('style');
    style.textContent = `
      .bar { font-family: Arial, Helvetica, sans-serif; color: #ffffff; padding: 14px 40px 18px; border-top: 6px solid #ffdd00;
             background: rgba(11, 12, 12, 0.93); box-sizing: border-box; width: 100%; position: relative; z-index: 1; }
      .bar.setup { border-top-color: #b1b4b6; background: rgba(29, 112, 184, 0.93); }
      .scenario { font-size: 20px; line-height: 26px; color: #d6d6d6; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      .step { font-size: 34px; line-height: 42px; font-weight: 700; }
      .setup .scenario { color: #ffffff; }
      .setup .step { font-size: 28px; line-height: 36px; font-weight: 400; }
      .label { display: inline-block; font-size: 20px; line-height: 28px; font-weight: 700; padding: 2px 10px; margin-right: 14px;
               vertical-align: 5px; color: #0b0c0c; background: #ffdd00; }
      .setup .label { background: #ffffff; color: #1d70b8; }
      .keyword { color: #ffdd00; }
      .status { font-size: 22px; line-height: 30px; margin-top: 6px; color: #ffdd00; }
      .setup .status { color: #ffffff; font-style: italic; }
      .card { position: fixed; left: 0; top: 0; right: 0; bottom: 0; background: #1d2733; color: #ffffff;
              font-family: Arial, Helvetica, sans-serif; box-sizing: border-box; padding: 7% 9% 220px; overflow: hidden; }
      .card .heading { font-size: 46px; line-height: 58px; font-weight: 700; }
      .card .note { font-size: 24px; line-height: 34px; color: #c8d1db; margin: 10px 0 40px; }
      .card .task { font-size: 28px; line-height: 44px; color: #8e9bab; }
      .card .task.done { color: #ffffff; }
      .card .task.now { color: #ffdd00; font-weight: 700; }
      .card .marker { display: inline-block; width: 46px; }
    `;
    const bar = document.createElement('div');
    bar.className = caption.setUp ? 'bar setup' : 'bar';

    const scenario = document.createElement('div');
    scenario.className = 'scenario';
    scenario.textContent = caption.scenario;

    const step = document.createElement('div');
    step.className = 'step';
    const [keyword, ...rest] = caption.step.split(' ');
    if (caption.setUp) {
      const label = document.createElement('span');
      label.className = 'label';
      label.textContent = 'SETTING UP';
      step.append(label, caption.step);
    } else {
      const keywordSpan = document.createElement('span');
      keywordSpan.className = 'keyword';
      keywordSpan.textContent = keyword;
      step.append(keywordSpan, ` ${rest.join(' ')}`);
    }
    bar.append(scenario, step);
    for (const note of [caption.status, ...(caption.resolved ?? [])]) {
      if (note) {
        const status = document.createElement('div');
        status.className = 'status';
        status.textContent = note;
        bar.append(status);
      }
    }
    const tasks = window.location.protocol === 'about:' ? caption.setUpCard ?? [] : [];
    if (tasks.length === 0) {
      shadow.replaceChildren(style, bar);
      return;
    }
    const card = document.createElement('div');
    card.className = 'card';
    const heading = document.createElement('div');
    heading.className = 'heading';
    heading.textContent = caption.scenario;
    const note = document.createElement('div');
    note.className = 'note';
    note.textContent = 'Setting the case up through the APIs. Nothing is open in the browser yet.';
    card.append(heading, note);
    let running = true;
    for (const task of tasks) {
      const line = document.createElement('div');
      const now = !task.done && running;
      running = running && task.done;
      line.className = `task${task.done ? ' done' : ''}${now ? ' now' : ''}`;
      const marker = document.createElement('span');
      marker.className = 'marker';
      marker.textContent = task.done ? '✓' : now ? '→' : '·';
      line.append(marker, task.text);
      card.append(line);
    }
    shadow.replaceChildren(style, card, bar);
  };

  w[render] = draw;
  const start = (): void => {
    const ask = w[binding] as (() => Promise<unknown>) | undefined;
    if (typeof ask === 'function') {
      ask()
        .then(caption => draw(caption as Parameters<typeof draw>[0]))
        .catch(() => undefined);
    }
  };
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start, { once: true });
  } else {
    start();
  }
}
