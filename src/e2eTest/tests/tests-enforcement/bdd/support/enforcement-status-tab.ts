// pcs-api renders both enforcement tabs as GOV.UK markup in one markdown label, so they are read as headings
// with summary-list rows. A tab only exists once a warrant or writ is generated; earlier statuses come from the case data.
import { Page, expect } from '@playwright/test';
import Axios from 'axios';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { ensureSignedIn } from './session';

export const ENFORCEMENT_STATUS_TAB = 'Eviction status';

export type TabBlock = {
  level: number;
  heading: string;
  rows: Record<string, string>;
  lists: Record<string, string>[];
  text: string;
};

export type EnforcementSection = {
  heading: string;
  summary: Record<string, string>;
  sections: Record<string, TabBlock>;
};

export async function openCaseTab(page: Page, caseReference: string, tabName: string): Promise<void> {
  await ensureSignedIn(page);
  const url = `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${caseReference}#${encodeURIComponent(tabName)}`;
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('tab', { name: tabName, exact: true })).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  // Selecting the tab by hash alone is unreliable once XUI has already rendered another tab.
  await page.getByRole('tab', { name: tabName, exact: true }).click();
}

export const activeTab = (page: Page) => page.locator('.mat-tab-body-active').first();

/** Re-fetches the case and selects the tab again: set-up goes through the APIs, so already-rendered markup would never gain what is asserted. */
export async function reopenCurrentTab(page: Page): Promise<void> {
  const selected = page.locator('[role="tab"][aria-selected="true"]').first();
  const tabName = (await selected.count()) ? (await selected.innerText()).trim() : undefined;
  await page.reload({ waitUntil: 'domcontentloaded' }).catch(() => undefined);
  if (!tabName) {
    return;
  }
  const tab = page.getByRole('tab', { name: tabName, exact: true });
  await expect(tab).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  await tab.click();
}

/** Waits for the markup first: XUI fills the tab asynchronously after it is selected. */
export async function tabBlocks(page: Page): Promise<TabBlock[]> {
  const panel = activeTab(page);
  await expect(panel.locator('h2').first()).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  return panel.evaluate((root: Element) => {
    const blocks: {
      level: number;
      heading: string;
      rows: Record<string, string>;
      lists: Record<string, string>[];
      text: string;
    }[] = [];
    const clean = (value: string | null | undefined) => (value ?? '').replace(/\s+/g, ' ').trim();
    root.querySelectorAll('h2, h3, dl, p').forEach(element => {
      const tag = element.tagName.toLowerCase();
      if (tag === 'h2' || tag === 'h3') {
        blocks.push({ level: Number(tag.slice(1)), heading: clean(element.textContent), rows: {}, lists: [], text: '' });
        return;
      }
      const current = blocks[blocks.length - 1];
      if (!current) {
        return;
      }
      if (tag === 'dl') {
        const list: Record<string, string> = {};
        current.lists.push(list);
        element.querySelectorAll('.govuk-summary-list__row').forEach(row => {
          const key = clean(row.querySelector('dt')?.textContent);
          const value = (row.querySelector('dd') as HTMLElement | null)?.innerText.trim() ?? '';
          current.rows[key] = value;
          list[key] = value;
        });
        return;
      }
      current.text = clean(`${current.text} ${element.textContent}`);
    });
    return blocks;
  });
}

export async function tabSections(page: Page): Promise<Record<string, TabBlock>> {
  return Object.fromEntries((await tabBlocks(page)).map(block => [block.heading, block]));
}

export async function enforcementSections(page: Page): Promise<EnforcementSection[]> {
  const sections: EnforcementSection[] = [];
  for (const block of await tabBlocks(page)) {
    if (block.level === 2) {
      sections.push({ heading: block.heading, summary: block.rows, sections: {} });
    } else if (sections.length) {
      sections[sections.length - 1].sections[block.heading] = block;
    }
  }
  return sections;
}

export async function soleEnforcementSection(page: Page, caseReference: string): Promise<EnforcementSection> {
  await openCaseTab(page, caseReference, ENFORCEMENT_STATUS_TAB);
  const sections = await enforcementSections(page);
  expect(sections, 'exactly one warrant or writ on the "Eviction status" tab').toHaveLength(1);
  return sections[0];
}

export async function expectEnforcementSection(
  page: Page,
  caseReference: string,
  section: string,
  check: (block: TabBlock | undefined) => void
): Promise<void> {
  await expect(async () => {
    check((await soleEnforcementSection(page, caseReference)).sections[section]);
  }).toPass({ timeout: VERY_LONG_TIMEOUT });
}

const STATUS_LABELS: Record<string, string> = {
  AWAITING_PAYMENT: 'Awaiting payment',
  AWAITING_REVIEW: 'Awaiting review',
  REJECTED: 'Rejected',
  AWAITING_BAILIFF_ALLOCATION: 'Awaiting bailiff allocation',
  AWAITING_SCHEDULING: 'Awaiting scheduling',
  AWAITING_CLAIMANT_CONFIRMATION: 'Awaiting claimant confirmation',
  CLAIMANT_CONFIRMED: 'Claimant confirmed',
  CLAIMANT_UNABLE_TO_ATTEND: 'Claimant unable to attend on this date',
  AWAITING_PROPERTY_VISIT: 'Awaiting property visit',
  BREATHING_SPACE: 'Breathing space',
  TRANSFERRED_TO_HIGH_COURT: 'Transferred to the High Court',
  CLOSED: 'Closed',
};

const STATUS_ALIASES: Record<string, string> = {
  'Transferred to High Court': 'Transferred to the High Court',
};

const TYPE_LABELS: Record<string, string> = {
  WARRANT: 'Warrant of possession',
  WRIT: 'Writ of possession',
  WARRANT_OF_RESTITUTION: 'Warrant of restitution',
  WRIT_OF_RESTITUTION: 'Writ of restitution',
};

export type EnforcementRow = { status?: string; number?: string; type?: string };

export async function enforcementRows(caseReference: string): Promise<EnforcementRow[]> {
  const response = await Axios.get(`${process.env.DATA_STORE_URL_BASE}/cases/${caseReference}`, {
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      experimental: 'experimental',
      Accept: '*/*',
    },
  });
  const enforcements: { value?: Record<string, string> }[] = response.data?.data?.enforcements ?? [];
  return enforcements.map(item => ({
    status: STATUS_LABELS[item.value?.status ?? ''] ?? item.value?.status,
    number: item.value?.enforcementNumber,
    type: TYPE_LABELS[item.value?.enforcementType ?? ''] ?? item.value?.enforcementType,
  }));
}

export async function currentEnforcement(caseReference: string, earlierCount = 0): Promise<EnforcementRow> {
  const rows = await enforcementRows(caseReference);
  expect(rows, `${earlierCount + 1} enforcement(s) on the case`).toHaveLength(earlierCount + 1);
  return rows[0];
}

export async function expectEnforcementStatus(
  _page: Page | undefined,
  caseReference: string,
  status: string,
  earlierCount = 0
): Promise<void> {
  const expected = STATUS_ALIASES[status] ?? status;
  await expect
    .poll(async () => (await currentEnforcement(caseReference, earlierCount)).status, { timeout: VERY_LONG_TIMEOUT })
    .toBe(expected);
}

export async function enforcementNumber(_page: Page, caseReference: string): Promise<string | undefined> {
  const rows = await enforcementRows(caseReference);
  expect(rows, 'exactly one enforcement on the case').toHaveLength(1);
  return rows[0].number;
}

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

export function tabDate(day: number, month: number, year: number): string {
  return `${day} ${MONTHS[month - 1]} ${year}`;
}

export function todayTabDate(): string {
  const now = new Date();
  return tabDate(now.getDate(), now.getMonth() + 1, now.getFullYear());
}

export function tabTime(time: string): string {
  const [hour, minute = '00'] = time.split(':').map(part => Number(part));
  const twelveHour = hour % 12 === 0 ? 12 : hour % 12;
  return `${String(twelveHour).padStart(2, '0')}:${String(minute).padStart(2, '0')} ${hour < 12 ? 'AM' : 'PM'}`;
}

export function tabDateOf(date: string): string {
  const slashed = date.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (slashed) {
    return tabDate(Number(slashed[1]), Number(slashed[2]), Number(slashed[3]));
  }
  const spelled = date.match(/(\d{1,2})\s+([A-Za-z]+)\s+(\d{4})/);
  if (spelled) {
    return `${Number(spelled[1])} ${spelled[2]} ${spelled[3]}`;
  }
  throw new Error(`Unrecognised date "${date}". Use dd/MM/yyyy or "30 June 2027"`);
}
