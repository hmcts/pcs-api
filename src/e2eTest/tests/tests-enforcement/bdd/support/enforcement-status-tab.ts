/**
 * Reading the enforcement tabs, and the enforcement status itself.
 *
 * pcs-api renders both enforcement tabs as GOV.UK markup in a single markdown label
 * (`EnforcementStatusTabMarkdown`, `RiskAssessmentTabMarkdown`): headings followed by summary lists
 * (`dt` / `dd` rows) or a placeholder paragraph such as "Not yet scheduled". On the "Enforcement status" tab
 * each warrant or writ is an `h2` headed with its number, and its sections ("Eviction notice", "Scheduled
 * eviction", ...) are `h3`s. On the "Risk assessment" tab the sections are `h2`s.
 *
 * The tab only appears once a warrant or writ has been generated, so the status of an application that is
 * still awaiting payment or review is read from the case data instead: the `enforcements` collection pcs-api
 * sets on the case view lists every application with its status.
 */
import { Page, expect } from '@playwright/test';
import Axios from 'axios';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { ensureSignedIn } from './session';

export const ENFORCEMENT_STATUS_TAB = 'Enforcement status';

/**
 * One heading on a tab, with what is under it: every summary list row (`rows`, merged), each summary list on
 * its own (`lists`, for repeated entries such as bailiff notes) and the paragraph text.
 */
export type TabBlock = {
  level: number;
  heading: string;
  rows: Record<string, string>;
  lists: Record<string, string>[];
  text: string;
};

/** One warrant or writ on the "Enforcement status" tab. */
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

/** The tab that is currently selected. */
export const activeTab = (page: Page) => page.locator('.mat-tab-body-active').first();

/**
 * Every heading on the selected tab, in order, with what follows it up to the next heading. Waits for the
 * markup to render first: XUI fills the tab asynchronously after it is selected.
 */
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

/** The headed sections of the selected tab, by heading. For the "Risk assessment" tab. */
export async function tabSections(page: Page): Promise<Record<string, TabBlock>> {
  return Object.fromEntries((await tabBlocks(page)).map(block => [block.heading, block]));
}

/** Every warrant and writ on the selected "Enforcement status" tab, in the order the tab lists them. */
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

/** The case's only warrant or writ on the "Enforcement status" tab, opening the tab first. */
export async function soleEnforcementSection(page: Page, caseReference: string): Promise<EnforcementSection> {
  await openCaseTab(page, caseReference, ENFORCEMENT_STATUS_TAB);
  const sections = await enforcementSections(page);
  expect(sections, 'exactly one warrant or writ on the "Enforcement status" tab').toHaveLength(1);
  return sections[0];
}

/**
 * One section of the case's only warrant, polling the tab so that what a just-submitted event wrote is
 * picked up once CCD has it.
 */
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

/** EnforcementStatus labels by the code the case data carries. */
const STATUS_LABELS: Record<string, string> = {
  AWAITING_PAYMENT: 'Awaiting payment',
  AWAITING_REVIEW: 'Awaiting review',
  REJECTED: 'Rejected',
  AWAITING_BAILIFF_ALLOCATION: 'Awaiting bailiff allocation',
  AWAITING_SCHEDULING: 'Awaiting scheduling',
  AWAITING_CLAIMANT_CONFIRMATION: 'Awaiting claimant confirmation',
  CLAIMANT_CONFIRMED: 'Claimant confirmed',
  CLAIMANT_UNABLE_TO_ATTEND: 'Claimant unable to attend',
  AWAITING_PROPERTY_VISIT: 'Awaiting property visit',
  BREATHING_SPACE: 'Breathing space',
  TRANSFERRED_TO_HIGH_COURT: 'Transferred to the High Court',
  CLOSED: 'Closed',
};

/** Feature-file spellings of a status that differ from the EnforcementStatus label. */
const STATUS_ALIASES: Record<string, string> = {
  'Claimant unable to attend on this date': 'Claimant unable to attend',
  'Transferred to High Court': 'Transferred to the High Court',
};

/** SelectEnforcementType labels by the code the case data carries. */
const TYPE_LABELS: Record<string, string> = {
  WARRANT: 'Warrant of possession',
  WRIT: 'Writ of possession',
  WARRANT_OF_RESTITUTION: 'Warrant of restitution',
  WRIT_OF_RESTITUTION: 'Writ of restitution',
};

export type EnforcementRow = { status?: string; number?: string; type?: string };

/** Every enforcement on the case, current first, as the case data lists them. */
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

/**
 * The enforcement the case is working on: its only one, or, when `earlierCount` enforcements came before it,
 * the first the case data lists (the live one, else the most recently changed). Fails if the case does not
 * have exactly `earlierCount + 1` enforcements.
 */
export async function currentEnforcement(caseReference: string, earlierCount = 0): Promise<EnforcementRow> {
  const rows = await enforcementRows(caseReference);
  expect(rows, `${earlierCount + 1} enforcement(s) on the case`).toHaveLength(earlierCount + 1);
  return rows[0];
}

/**
 * Asserts the status of the case's current enforcement (see {@link currentEnforcement}). Polls so that a
 * status set by a just-submitted event is picked up once CCD has it.
 */
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

/** The warrant or writ number of the case's only enforcement, or undefined when none has been generated. */
export async function enforcementNumber(_page: Page, caseReference: string): Promise<string | undefined> {
  const rows = await enforcementRows(caseReference);
  expect(rows, 'exactly one enforcement on the case').toHaveLength(1);
  return rows[0].number;
}

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

/** A date as the enforcement tabs show it: "15 March 2026". */
export function tabDate(day: number, month: number, year: number): string {
  return `${day} ${MONTHS[month - 1]} ${year}`;
}

/** Today's date as the enforcement tabs show it. */
export function todayTabDate(): string {
  const now = new Date();
  return tabDate(now.getDate(), now.getMonth() + 1, now.getFullYear());
}

/** A 24-hour "09:30" as the enforcement tabs show it: "09:30 AM". */
export function tabTime(time: string): string {
  const [hour, minute = '00'] = time.split(':').map(part => Number(part));
  const twelveHour = hour % 12 === 0 ? 12 : hour % 12;
  return `${String(twelveHour).padStart(2, '0')}:${String(minute).padStart(2, '0')} ${hour < 12 ? 'AM' : 'PM'}`;
}

/** "dd/MM/yyyy", "30 June 2027" or "Wednesday 30 June 2027" as the enforcement tabs show it. */
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
