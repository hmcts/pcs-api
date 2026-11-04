import { ElementHandle, Locator, Page } from '@playwright/test';

/**
 * Human-speed actions for the demo videos: while the demo pacing is on (utils/page-pacing.ts) every `Locator`
 * action on the paced page is wrapped. The visual parts never fail a test: their errors are swallowed.
 */
type Point = { x: number; y: number };
type LocatorMethod = (this: Locator, ...args: unknown[]) => Promise<unknown>;

const MOVE_MS = 400;
const SETTLE_MS = 250;
const CLICK_MS = 150;
const SCROLL_MS = 450;
const SELECTED_MS = 750;
/** Typing a long value is capped at about this long, so a note does not take a minute to type. */
const TYPING_BUDGET_MS = 4000;
const TYPING_MAX_DELAY_MS = 65;
const TYPING_MIN_DELAY_MS = 15;
/** Space kept clear at the bottom of the viewport for the step caption bar. */
const CAPTION_CLEARANCE_PX = 190;

const POINTED_ACTIONS = ['click', 'dblclick', 'check', 'uncheck', 'setChecked', 'tap', 'setInputFiles'] as const;

const PATCHED = Symbol.for('pcs.demoHumanActions');

let activeFor: (() => Page | undefined) | undefined;
let pointer: Point = { x: 960, y: 540 };

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
  const typeable = await locator.evaluate(isFreeText, undefined, { timeout: 2_000 }).catch(() => false);
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
  }
  // A field that masks, formats or limits what is typed ends up different from the value: fill it for real.
  if ((await locator.inputValue({ timeout: 1_000 }).catch(() => undefined)) !== value) {
    return original.apply(locator, args);
  }
  return undefined;
}

/** Runs in the page. Not one part of a date or date-time field, which XUI validates as a whole on every keystroke, so typing into it shows an error until the last part is in. */
function isFreeText(element: SVGElement | HTMLElement): boolean {
  if (element instanceof HTMLTextAreaElement) {
    return true;
  }
  if (!(element instanceof HTMLInputElement)) {
    return false;
  }
  return (
    ['', 'text', 'email', 'password', 'search', 'tel', 'url', 'number'].includes(element.type) &&
    !/-(day|month|year|hour|minute|second)$/i.test(element.id)
  );
}

/** XUI revalidates the whole field on every keystroke, so in the demo every part is set in a single page call and the half-filled state is never seen. */
export async function fillDateParts(parts: { input: Locator; value: string }[]): Promise<void> {
  const fillInTurn = async (): Promise<void> => {
    for (const { input, value } of parts) {
      await input.fill(value);
    }
  };
  const page = parts[0]?.input.page();
  if (!page || activeFor?.() !== page) {
    return fillInTurn();
  }
  await pointAt(parts[0].input);
  const handles = await Promise.all(
    parts.map(({ input, value }) =>
      input
        .elementHandle({ timeout: 2_000 })
        .then(element => (element ? { element: element as ElementHandle<HTMLInputElement>, value } : undefined))
        .catch(() => undefined),
    ),
  );
  const found = handles.filter((handle): handle is { element: ElementHandle<HTMLInputElement>; value: string } => !!handle);
  if (found.length === parts.length) {
    await page.evaluate(setPartsTogether, found).catch(() => undefined);
  }
  const filled = await Promise.all(
    parts.map(({ input, value }) => input.inputValue({ timeout: 1_000 }).then(actual => actual === value, () => false)),
  );
  if (!filled.every(Boolean)) {
    await fillInTurn();
  }
}

/** Runs in the page. One task, so the browser paints the field once, with every part in it. */
function setPartsTogether(parts: { element: HTMLInputElement; value: string }[]): void {
  const setValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set;
  for (const { element, value } of parts) {
    setValue?.call(element, value);
    element.dispatchEvent(new Event('input', { bubbles: true }));
    element.dispatchEvent(new Event('change', { bubbles: true }));
  }
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
    // Styled through the CSSOM rather than a <style> element: XUI's Content Security Policy blocks inline style sheets.
    const ripple = document.createElement('div');
    ripple.className = 'ripple';
    ripple.style.cssText = 'position: fixed; left: -26px; top: -26px; width: 52px; height: 52px; border-radius: 50%; '
      + 'border: 5px solid #ffdd00; box-sizing: border-box; opacity: 0;';
    const svgNs = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(svgNs, 'svg');
    svg.setAttribute('class', 'pointer');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('width', '34');
    svg.setAttribute('height', '34');
    svg.style.cssText = 'position: fixed; left: 0; top: 0; width: 34px; height: 34px; will-change: transform; '
      + 'filter: drop-shadow(0 2px 3px rgba(0, 0, 0, 0.5));';
    const path = document.createElementNS(svgNs, 'path');
    path.setAttribute('d', 'M3 2 L3 20 L8 15.5 L11.5 22.5 L14.5 21 L11 14 L18 14 Z');
    path.setAttribute('fill', '#ffffff');
    path.setAttribute('stroke', '#0b0c0c');
    path.setAttribute('stroke-width', '1.6');
    path.setAttribute('stroke-linejoin', 'round');
    svg.appendChild(path);
    const labelBox = document.createElement('div');
    labelBox.className = 'label';
    labelBox.style.cssText = 'position: fixed; left: 0; top: 0; font: 700 24px/32px Arial, Helvetica, sans-serif; '
      + 'color: #0b0c0c; background: #ffdd00; padding: 4px 12px; border: 3px solid #0b0c0c; white-space: nowrap; '
      + 'display: none;';
    root.append(ripple, svg, labelBox);
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
    rippleElement.animate(
      [
        { transform: `${at(to)} scale(0.3)`, opacity: 1 },
        { transform: `${at(to)} scale(1.4)`, opacity: 0 },
      ],
      { duration: 500, easing: 'ease-out' }
    );
  }
  labelElement.textContent = label;
  labelElement.style.display = label ? 'block' : 'none';
  labelElement.style.transform = at({ x: to.x + 30, y: to.y + 24 });
}
