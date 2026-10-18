import { Locator, Page } from '@playwright/test';

/**
 * Human-speed actions for the demo videos. Playwright's videos show no mouse pointer and `fill()` puts a
 * whole value in at once, so a recording at `slowMo` alone shows pages changing with nothing visibly done to
 * them. While the demo pacing is on (utils/page-pacing.ts, `enforcement-bdd-demo` project only), every
 * `Locator` action on the paced page is wrapped, so that every helper, step and sign-in gets it without
 * calling anything:
 *
 * - a pointer is drawn and glides to the element (scrolling it into view first) before a click, check,
 *   select or upload, and ripples as it clicks;
 * - `fill()` into a text field types the value a character at a time;
 * - `selectOption()` outlines the drop down and shows the chosen option before carrying on.
 *
 * With pacing off the wrappers call straight through, and nothing is wrapped until the demo turns pacing
 * on. The visual parts never fail a test: their errors are swallowed and the real action still runs.
 */
type Point = { x: number; y: number };
type LocatorMethod = (this: Locator, ...args: unknown[]) => Promise<unknown>;

const MOVE_MS = 500;
const SETTLE_MS = 300;
const CLICK_MS = 150;
const SCROLL_MS = 450;
const SELECTED_MS = 900;
/** Typing a long value is capped at about this long, so a note does not take a minute to type. */
const TYPING_BUDGET_MS = 4000;
const TYPING_MAX_DELAY_MS = 80;
const TYPING_MIN_DELAY_MS = 15;
/** Space kept clear at the bottom of the viewport for the step caption bar. */
const CAPTION_CLEARANCE_PX = 190;

const POINTED_ACTIONS = ['click', 'dblclick', 'check', 'uncheck', 'setChecked', 'tap', 'setInputFiles'] as const;

const PATCHED = Symbol.for('pcs.demoHumanActions');

let activeFor: (() => Page | undefined) | undefined;
let pointer: Point = { x: 960, y: 540 };

/**
 * Wraps the `Locator` actions, once per worker. `pacedPage` says which page, if any, actions are currently
 * slowed down on; it returns undefined whenever pacing is off.
 */
export function installHumanActions(page: Page, pacedPage: () => Page | undefined): void {
  activeFor = pacedPage;
  const viewport = page.viewportSize();
  if (viewport) {
    pointer = { x: viewport.width / 2, y: viewport.height / 2 };
  }
  const prototype = Object.getPrototypeOf(page.locator('html')) as Record<string | symbol, unknown>;
  if (prototype[PATCHED]) {
    return;
  }
  prototype[PATCHED] = true;
  for (const name of POINTED_ACTIONS) {
    wrap(prototype, name, async (locator, original, args) => {
      await pointAt(locator);
      return original.apply(locator, args);
    });
  }
  wrap(prototype, 'fill', typeVisibly);
  wrap(prototype, 'selectOption', selectVisibly);
}

function wrap(
  prototype: Record<string | symbol, unknown>,
  name: string,
  demo: (locator: Locator, original: LocatorMethod, args: unknown[]) => Promise<unknown>,
): void {
  const original = prototype[name] as LocatorMethod | undefined;
  if (typeof original !== 'function') {
    return;
  }
  prototype[name] = async function (this: Locator, ...args: unknown[]) {
    const page = activeFor?.();
    if (!page || this.page() !== page || isTrial(args)) {
      return original.apply(this, args);
    }
    return demo(this, original, args);
  };
}

function isTrial(args: unknown[]): boolean {
  return args.some(arg => typeof arg === 'object' && arg !== null && (arg as { trial?: boolean }).trial === true);
}

async function typeVisibly(locator: Locator, original: LocatorMethod, args: unknown[]): Promise<unknown> {
  const [value, options] = args as [unknown, { timeout?: number } | undefined];
  if (typeof value !== 'string' || value.length === 0) {
    return original.apply(locator, args);
  }
  const typeable = await locator
    .evaluate(
      element =>
        element instanceof HTMLTextAreaElement ||
        (element instanceof HTMLInputElement &&
          ['', 'text', 'email', 'password', 'search', 'tel', 'url', 'number'].includes(element.type)),
      undefined,
      { timeout: 2_000 },
    )
    .catch(() => false);
  if (!typeable) {
    await pointAt(locator);
    return original.apply(locator, args);
  }
  await pointAt(locator);
  try {
    await original.call(locator, '', options);
    const delay = Math.max(TYPING_MIN_DELAY_MS, Math.min(TYPING_MAX_DELAY_MS, Math.floor(TYPING_BUDGET_MS / value.length)));
    await locator.pressSequentially(value, { delay, timeout: options?.timeout });
  } catch {
    // Fall through to the check below, which leaves the field as fill() would.
  }
  // A field that masks, formats or limits what is typed ends up different from the value: fill it for real.
  if ((await locator.inputValue({ timeout: 1_000 }).catch(() => undefined)) !== value) {
    return original.apply(locator, args);
  }
  return undefined;
}

async function selectVisibly(locator: Locator, original: LocatorMethod, args: unknown[]): Promise<unknown> {
  await pointAt(locator);
  await locator
    .evaluate(
      (element: HTMLElement) => {
        element.style.outline = '5px solid #ffdd00';
        element.style.outlineOffset = '3px';
      },
      undefined,
      { timeout: 1_000 },
    )
    .catch(() => undefined);
  const result = await original.apply(locator, args);
  try {
    const chosen = await locator.evaluate(
      element => (element instanceof HTMLSelectElement ? element.selectedOptions[0]?.textContent?.trim() ?? '' : ''),
      undefined,
      { timeout: 1_000 },
    );
    const page = locator.page();
    if (chosen) {
      await page.evaluate(drawPointer, { from: pointer, to: pointer, ms: 0, ripple: false, label: chosen });
    }
    await page.waitForTimeout(SELECTED_MS);
    await page.evaluate(drawPointer, { from: pointer, to: pointer, ms: 0, ripple: false, label: '' });
  } catch {
    // Only for the recording.
  }
  return result;
}

/** Scrolls the element into view if it is not clear of the edges and the caption, glides the pointer to it and clicks. */
async function pointAt(locator: Locator): Promise<void> {
  const page = locator.page();
  try {
    const scrolled = await locator.evaluate(
      (element, clearance) => {
        const box = element.getBoundingClientRect();
        if (box.top >= 70 && box.bottom <= window.innerHeight - clearance) {
          return false;
        }
        element.scrollIntoView({ block: 'center', behavior: 'smooth' });
        return true;
      },
      CAPTION_CLEARANCE_PX,
      { timeout: 2_000 },
    );
    if (scrolled) {
      await page.waitForTimeout(SCROLL_MS);
    }
    const box = await locator.boundingBox({ timeout: 1_000 });
    if (!box) {
      return;
    }
    const to = { x: box.x + Math.min(box.width / 2, 40), y: box.y + box.height / 2 };
    await page.evaluate(drawPointer, { from: pointer, to, ms: MOVE_MS, ripple: false, label: '' });
    pointer = to;
    await page.waitForTimeout(MOVE_MS + SETTLE_MS);
    await page.evaluate(drawPointer, { from: to, to, ms: 0, ripple: true, label: '' });
    await page.waitForTimeout(CLICK_MS);
  } catch {
    // The element may not be there yet, or the page may be navigating: the real action deals with that.
  }
}

/** Runs in the page. Self-contained: Playwright serialises it. */
function drawPointer({ from, to, ms, ripple, label }: { from: Point; to: Point; ms: number; ripple: boolean; label: string }): void {
  const key = '__pcsDemoPointer';
  const w = window as unknown as Record<string, { host: HTMLElement; root: ShadowRoot } | undefined>;
  let parts = w[key];
  if (!parts) {
    const host = document.createElement('pcs-demo-pointer');
    host.setAttribute('aria-hidden', 'true');
    host.style.cssText = 'all: initial; position: fixed; inset: 0; z-index: 2147483647; pointer-events: none;';
    const root = host.attachShadow({ mode: 'closed' });
    root.innerHTML = `
      <style>
        .pointer { position: fixed; left: 0; top: 0; width: 34px; height: 34px; will-change: transform;
                   filter: drop-shadow(0 2px 3px rgba(0, 0, 0, 0.5)); }
        .ripple { position: fixed; left: -26px; top: -26px; width: 52px; height: 52px; border-radius: 50%;
                  border: 5px solid #ffdd00; box-sizing: border-box; opacity: 0; }
        .ripple.go { animation: ripple 500ms ease-out; }
        @keyframes ripple { from { transform: var(--at) scale(0.3); opacity: 1; } to { transform: var(--at) scale(1.4); opacity: 0; } }
        .label { position: fixed; left: 0; top: 0; font: 700 24px/32px Arial, Helvetica, sans-serif; color: #0b0c0c;
                 background: #ffdd00; padding: 4px 12px; border: 3px solid #0b0c0c; white-space: nowrap; display: none; }
      </style>
      <div class="ripple"></div>
      <svg class="pointer" viewBox="0 0 24 24"><path d="M3 2 L3 20 L8 15.5 L11.5 22.5 L14.5 21 L11 14 L18 14 Z"
           fill="#ffffff" stroke="#0b0c0c" stroke-width="1.6" stroke-linejoin="round"/></svg>
      <div class="label"></div>`;
    parts = { host, root };
    w[key] = parts;
  }
  if (!parts.host.isConnected) {
    document.documentElement.appendChild(parts.host);
  }
  const pointerElement = parts.root.querySelector<SVGElement>('.pointer');
  const rippleElement = parts.root.querySelector<HTMLElement>('.ripple');
  const labelElement = parts.root.querySelector<HTMLElement>('.label');
  if (!pointerElement || !rippleElement || !labelElement) {
    return;
  }
  const at = (point: Point): string => `translate(${point.x}px, ${point.y}px)`;
  pointerElement.style.transition = 'none';
  if (ms > 0) {
    pointerElement.style.transform = at(from);
    pointerElement.getBoundingClientRect();
    pointerElement.style.transition = `transform ${ms}ms ease-in-out`;
  }
  pointerElement.style.transform = at(to);
  if (ripple) {
    rippleElement.style.setProperty('--at', at(to));
    rippleElement.classList.remove('go');
    rippleElement.getBoundingClientRect();
    rippleElement.classList.add('go');
  }
  labelElement.textContent = label;
  labelElement.style.display = label ? 'block' : 'none';
  labelElement.style.transform = at({ x: to.x + 30, y: to.y + 24 });
}
