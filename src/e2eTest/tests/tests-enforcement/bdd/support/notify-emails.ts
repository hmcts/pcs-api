/**
 * The GOV.UK Notify emails pcs-api sent for a case, read from wiremock's request journal.
 *
 * Locally Notify is wiremock's `gov-notify/send-email.json` stub, which accepts any template id. The dev profile
 * (src/main/resources/application-dev.yaml) gives each enforcement email a made-up template id so that pcs-api
 * sends it there; no such template exists in Notify, so the body text of an email cannot be checked, only the
 * template it was sent with, who it went to and the personalisation the template would be filled from.
 *
 * pcs-api sends each email on a db-scheduler task after the event commits, so an email arrives some seconds
 * after the event; every check here polls.
 */
import fs from 'fs';
import path from 'path';
import Axios from 'axios';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { localUsers } from '@config/local-stack.config';
import { expect, test } from '../fixtures/enforcement.fixtures';

export interface NotifyEmail {
  templateId: string;
  emailAddress: string;
  personalisation: Record<string, unknown>;
}

interface JournalEntry {
  body?: string;
}

/**
 * The template key under `enforcement-notify.templates` for each email, by the name the feature files (and the
 * HDPI-7538 notification table) give it: pcs-api's `EnforcementEmail`.
 */
const TEMPLATE_KEYS: Record<string, string> = {
  'Request for a warrant received': 'warrant-application-received',
  'Request for a writ received': 'writ-application-received',
  'Warrant of restitution application submitted': 'warrant-of-restitution-application-received',
  'Writ of restitution application submitted': 'writ-of-restitution-application-received',
  'Caseworker generates the warrant of possession': 'warrant-approved',
  'Application for a writ successful': 'writ-approved',
  'Caseworker generates the warrant of restitution': 'warrant-of-restitution-approved',
  'Caseworker rejects the warrant of possession application': 'warrant-rejected',
  'Caseworker rejects the writ of possession application': 'writ-rejected',
  'Caseworker rejects the warrant of restitution application': 'warrant-of-restitution-rejected',
  'Bailiff schedules the eviction date': 'eviction-scheduled',
  'Bailiff reschedules the eviction date': 'eviction-rescheduled',
  'Claimant confirms attendance at the eviction date': 'claimant-confirmed-attendance',
  'Claimant declines attendance at the eviction date': 'claimant-declined-attendance',
  // One template covers every return name that notifies the claimant; the body is passed as personalisation.
  'Claimant is notified of the return': 'warrant-return',
};

export const WARRANT_RETURN_EMAIL = 'Claimant is notified of the return';

/**
 * The claimant's email address on every test-case-generation fixture
 * (`Create-Case-Make-A-Claim-Basic-Case.json`, `claimantContactEmail`), which is the claimant solicitor's.
 */
export const FIXTURE_CLAIMANT_EMAIL = localUsers.claimantSolicitor;

const DEV_PROFILE = path.resolve(__dirname, '../../../../../main/resources/application-dev.yaml');

/** The stand-in template ids the dev profile gives the enforcement emails, by template key. */
function devTemplateIds(): Record<string, string> {
  const yaml = fs.readFileSync(DEV_PROFILE, 'utf8');
  const section = yaml.match(/^enforcement-notify:\n {2}templates:\n((?: {4}.*\n?)*)/m)?.[1] ?? '';
  return Object.fromEntries(
    [...section.matchAll(/^ {4}([\w-]+):[ \t]*(\S*)/gm)].map(([, key, id]) => [key, id.replace(/^['"]|['"]$/g, '')])
  );
}

/**
 * The dev template id of the named email, or undefined when the dev profile leaves it blank because the
 * service has not written the email yet.
 */
export function devTemplateId(name: string): string | undefined {
  const key = TEMPLATE_KEYS[name];
  if (!key) {
    throw new Error(`Unknown enforcement email "${name}". Known: ${Object.keys(TEMPLATE_KEYS).join(', ')}`);
  }
  return devTemplateIds()[key] || undefined;
}

async function sentEmails(): Promise<NotifyEmail[]> {
  const response = await Axios.post(`${process.env.WIREMOCK_URL}/__admin/requests/find`, {
    method: 'POST',
    urlPath: '/v2/notifications/email',
  });
  const requests: JournalEntry[] = response.data?.requests ?? [];
  return requests.flatMap(request => {
    try {
      const body = JSON.parse(request.body ?? '');
      return [
        {
          templateId: body.template_id,
          emailAddress: body.email_address,
          personalisation: body.personalisation ?? {},
        },
      ];
    } catch {
      return [];
    }
  });
}

/** Two db-scheduler polls and the send. */
const NO_EMAIL_SETTLE_MS = 25_000;

const dashed = (caseReference: string): string => caseReference.replace(/(.{4})(?=.)/g, '$1-');

async function caseEmails(caseReference: string, templateId: string): Promise<NotifyEmail[]> {
  return (await sentEmails()).filter(
    email => email.templateId === templateId && email.personalisation.caseNumber === dashed(caseReference)
  );
}

/**
 * Waits for the named email on the case and returns it, having checked it was sent once, to the claimant, with
 * the given personalisation (on top of the case number every PCS email carries).
 */
export async function expectClaimantEmail(
  caseReference: string,
  name: string,
  personalisation: Record<string, unknown> = {}
): Promise<NotifyEmail> {
  const templateId = devTemplateId(name);
  if (!templateId) {
    throw new Error(`The dev profile has no template id for the "${name}" email`);
  }
  let found: NotifyEmail[] = [];
  await expect
    .poll(
      async () => {
        found = await caseEmails(caseReference, templateId);
        return found.length;
      },
      { message: `the "${name}" email for case ${caseReference}`, timeout: VERY_LONG_TIMEOUT }
    )
    .toBeGreaterThan(0);
  expect(found, `one "${name}" email for case ${caseReference}`).toHaveLength(1);
  const [email] = found;
  expect(email.emailAddress, `the address the "${name}" email was sent to`).toBe(FIXTURE_CLAIMANT_EMAIL);
  for (const [key, value] of Object.entries(personalisation)) {
    expect(email.personalisation[key], `"${key}" in the "${name}" email`).toEqual(value);
  }
  noteCheckedOnNotifyRequest(name);
  return email;
}

/**
 * Asserts the case gets no email from the named template. An email is sent on a db-scheduler task polled every
 * ten seconds, so this waits long enough for one to have been sent before checking.
 */
export async function expectNoClaimantEmail(caseReference: string, name: string): Promise<void> {
  const templateId = devTemplateId(name);
  if (!templateId) {
    throw new Error(`The dev profile has no template id for the "${name}" email`);
  }
  await new Promise(resolve => setTimeout(resolve, NO_EMAIL_SETTLE_MS));
  expect(await caseEmails(caseReference, templateId), `no "${name}" email for case ${caseReference}`).toHaveLength(0);
}

/** "Thursday 25 June 2026 at 10:00am", as pcs-api puts an eviction date into an email. */
export function emailEvictionDateTime(day: number, month: number, year: number, time: string): string {
  const [hour, minute = 0] = time.split(':').map(Number);
  const when = new Date(year, month - 1, day);
  const weekday = when.toLocaleDateString('en-GB', { weekday: 'long' });
  const monthName = when.toLocaleDateString('en-GB', { month: 'long' });
  const twelveHour = hour % 12 === 0 ? 12 : hour % 12;
  const pad = (value: number): string => String(value).padStart(2, '0');
  return `${weekday} ${day} ${monthName} ${year} at ${pad(twelveHour)}:${pad(minute)}${hour < 12 ? 'am' : 'pm'}`;
}

/** Records that the email was checked in the request pcs-api sent Notify, not as the claimant would read it. */
function noteCheckedOnNotifyRequest(name: string): void {
  test.info().annotations.push({
    type: 'checked on the Notify request',
    description:
      `HDPI-7538: the "${name}" email is checked in the request pcs-api sent the Notify stub (template id, ` +
      'recipient and personalisation); no template has been written in Notify, so there is no body text to read',
  });
}
