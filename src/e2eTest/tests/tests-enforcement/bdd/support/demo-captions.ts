/**
 * Step captions for the demo videos: a bar along the bottom of the screen showing the Gherkin step being run,
 * with the scenario title above it, so the recording can be followed against the feature file. Set-up steps
 * (Given and the Background) get their own "Setting up" style. Installed by the step hooks in
 * fixtures/demo-hold.ts in the `enforcement-bdd-demo` project only.
 *
 * The bar lives in a closed shadow root attached to <html>, so selectors, `innerText`, the page content
 * checks and the axe audit cannot see it, and it ignores the pointer, so it never takes a click. Every new
 * document in the context (a sign-in redirect, a full page load) asks for the current caption through an
 * exposed binding, so the bar survives navigation. Nothing here may fail a test: errors are swallowed.
 */
import { BrowserContext, Page } from '@playwright/test';

export type DemoCaption = {
  scenario: string;
  /** The step with its keyword, e.g. `When I start the "Enforce the order" event`. */
  step: string;
  setUp: boolean;
  /** A note under the step, e.g. who is signing in. */
  status?: string;
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

/** Adds or clears the note under the current step. Does nothing unless a caption is showing (the demo project). */
export async function showDemoCaptionStatus(page: Page, status: string | undefined): Promise<void> {
  if (!current) {
    return;
  }
  current = { ...current, status };
  await render(page);
}

async function render(page: Page): Promise<void> {
  if (page.isClosed()) {
    return;
  }
  // A page mid-navigation rejects the evaluate; its next document picks the caption up through the binding.
  await page
    .evaluate(([name, caption]) => (window as unknown as Record<string, (c: unknown) => void>)[name]?.(caption), [RENDER, current] as const)
    .catch(() => undefined);
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

  const draw = (caption: { scenario: string; step: string; setUp: boolean; status?: string } | undefined): void => {
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
             background: rgba(11, 12, 12, 0.93); box-sizing: border-box; width: 100%; }
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
    if (caption.status) {
      const status = document.createElement('div');
      status.className = 'status';
      status.textContent = caption.status;
      bar.append(status);
    }
    shadow.replaceChildren(style, bar);
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
