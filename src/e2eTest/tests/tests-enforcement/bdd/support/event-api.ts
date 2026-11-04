// Set-up submits the real event through CCD's data store API as the user who would submit it in XUI, so the start
// callback and the submit handler run but mid-event callbacks do not, and the browser is left alone.
import Axios, { AxiosInstance } from 'axios';
import { IdamUtils } from '@hmcts/playwright-common';
import { isLocalStack, localIdamToken } from '@config/local-stack.config';
import { recordCaseChange } from './demo-continuity';

export type EventConfirmation = { header?: string; body?: string };

type CaseData = Record<string, unknown>;

const tokens = new Map<string, Promise<string>>();
const userIds = new Map<string, Promise<string>>();

export function userToken(email: string): Promise<string> {
  let token = tokens.get(email);
  if (!token) {
    token = isLocalStack()
      ? localIdamToken(email)
      : new IdamUtils().generateIdamToken({
          username: email,
          password: process.env.IDAM_PCS_USER_PASSWORD as string,
          grantType: 'password',
          clientId: 'pcs-api',
          clientSecret: process.env.PCS_API_IDAM_SECRET as string,
          scope: 'profile openid roles',
        });
    token.catch(() => tokens.delete(email));
    tokens.set(email, token);
  }
  return token;
}

export function userId(email: string): Promise<string> {
  let id = userIds.get(email);
  if (!id) {
    id = (async () => {
      const response = await Axios.get(`${process.env.IDAM_WEB_URL}/o/userinfo`, {
        headers: { Authorization: `Bearer ${await userToken(email)}` },
      });
      return String(response.data.uid ?? response.data.sub);
    })();
    id.catch(() => userIds.delete(email));
    userIds.set(email, id);
  }
  return id;
}

async function dataStoreAs(email: string): Promise<AxiosInstance> {
  return Axios.create({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${await userToken(email)}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      experimental: 'experimental',
      Accept: '*/*',
    },
    validateStatus: () => true,
  });
}

export async function submitEventAs(
  email: string,
  eventId: string,
  answers: CaseData | ((startData: CaseData) => CaseData),
  caseReference = String(process.env.CASE_NUMBER)
): Promise<EventConfirmation> {
  const api = await dataStoreAs(email);
  const trigger = await api.get(`/cases/${caseReference}/event-triggers/${eventId}`);
  if (trigger.status !== 200 || !trigger.data?.token) {
    throw new Error(`Starting ${eventId} on ${caseReference} as ${email} returned ${trigger.status}: ${JSON.stringify(trigger.data)}`);
  }
  const startData: CaseData = trigger.data.case_details?.case_data ?? {};
  const data = typeof answers === 'function' ? answers(startData) : answers;
  const submitted = await api.post(`/cases/${caseReference}/events`, {
    data,
    event: { id: eventId },
    event_token: trigger.data.token,
    ignore_warning: false,
  });
  if (submitted.status !== 201) {
    throw new Error(
      `Submitting ${eventId} on ${caseReference} as ${email} returned ${submitted.status}: ${JSON.stringify(submitted.data)}`
    );
  }
  recordCaseChange(caseReference);
  const confirmation = submitted.data?.after_submit_callback_response ?? {};
  return { header: confirmation.confirmation_header ?? undefined, body: confirmation.confirmation_body ?? undefined };
}

/** A CCD DateTime value, "yyyy-MM-ddTHH:mm:00.000", for a local date `daysFromToday` away at `time` ("HH:mm"). */
export function ccdDateTime(daysFromToday: number, time: string): string {
  const when = new Date();
  when.setDate(when.getDate() + daysFromToday);
  const pad = (value: number): string => String(value).padStart(2, '0');
  return `${when.getFullYear()}-${pad(when.getMonth() + 1)}-${pad(when.getDate())}T${time}:00.000`;
}

export function slashedDate(daysFromToday: number): string {
  const when = new Date();
  when.setDate(when.getDate() + daysFromToday);
  const pad = (value: number): string => String(value).padStart(2, '0');
  return `${pad(when.getDate())}/${pad(when.getMonth() + 1)}/${when.getFullYear()}`;
}

export function chooseFromList(startData: CaseData, field: string, code: string): { value: unknown; list_items: unknown[] } {
  const list = startData[field] as { list_items?: { code: string }[] } | undefined;
  const items = list?.list_items ?? [];
  const value = items.find(item => item.code === code);
  if (!value) {
    throw new Error(`"${field}" does not offer ${code}. Offered: ${items.map(item => item.code).join(', ')}`);
  }
  return { value, list_items: items };
}

export function confirmationValue(confirmation: EventConfirmation, label: string): string {
  const text = `${confirmation.header ?? ''}\n${(confirmation.body ?? '').replace(/<[^>]+>/g, ' ')}`;
  const match = text.match(new RegExp(`${label}\\s*(\\S+)`));
  if (!match) {
    throw new Error(`No "${label}" in the confirmation: ${text}`);
  }
  return match[1];
}
