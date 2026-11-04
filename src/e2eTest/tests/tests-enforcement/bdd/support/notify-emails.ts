// Locally Notify is a wiremock stub and the dev profile gives each enforcement email a made-up template id, so
// only the template, the recipient and the personalisation can be checked. Each is sent on a task, so polls.
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
  'Claimant is notified of the return': 'warrant-return',
};

export const WARRANT_RETURN_EMAIL = 'Claimant is notified of the return';

export const FIXTURE_CLAIMANT_EMAIL = localUsers.claimantSolicitor;

const DEV_PROFILE = path.resolve(__dirname, '../../../../../main/resources/application-dev.yaml');

function devTemplateIds(): Record<string, string> {
  const yaml = fs.readFileSync(DEV_PROFILE, 'utf8');
  const section = yaml.match(/^enforcement-notify:\n {2}templates:\n((?: {4}.*\n?)*)/m)?.[1] ?? '';
  return Object.fromEntries(
    [...section.matchAll(/^ {4}([\w-]+):[ \t]*(\S*)/gm)].map(([, key, id]) => [key, id.replace(/^['"]|['"]$/g, '')])
  );
}

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

/** An email is sent on a db-scheduler task polled every ten seconds, so this waits that long before concluding none was. */
export async function expectNoClaimantEmail(caseReference: string, name: string): Promise<void> {
  const templateId = devTemplateId(name);
  if (!templateId) {
    throw new Error(`The dev profile has no template id for the "${name}" email`);
  }
  await new Promise(resolve => setTimeout(resolve, NO_EMAIL_SETTLE_MS));
  expect(await caseEmails(caseReference, templateId), `no "${name}" email for case ${caseReference}`).toHaveLength(0);
}

export function emailEvictionDateTime(day: number, month: number, year: number, time: string): string {
  const [hour, minute = 0] = time.split(':').map(Number);
  const when = new Date(year, month - 1, day);
  const weekday = when.toLocaleDateString('en-GB', { weekday: 'long' });
  const monthName = when.toLocaleDateString('en-GB', { month: 'long' });
  const twelveHour = hour % 12 === 0 ? 12 : hour % 12;
  const pad = (value: number): string => String(value).padStart(2, '0');
  return `${weekday} ${day} ${monthName} ${year} at ${pad(twelveHour)}:${pad(minute)}${hour < 12 ? 'am' : 'pm'}`;
}

function noteCheckedOnNotifyRequest(name: string): void {
  test.info().annotations.push({
    type: 'checked on the Notify request',
    description:
      `HDPI-7538: the "${name}" email is checked in the request pcs-api sent the Notify stub (template id, ` +
      'recipient and personalisation); no template has been written in Notify, so there is no body text to read',
  });
}
