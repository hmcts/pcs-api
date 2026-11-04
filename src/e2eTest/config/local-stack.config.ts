import Axios from 'axios';
import { IdamUtils } from '@hmcts/playwright-common';
import { Page } from '@playwright/test';
import { getCaseTypeId } from '@utils/common/caseType.utils';

export const LOCAL_ENVIRONMENT = 'local';

export function isLocalStack(): boolean {
  return (process.env.ENVIRONMENT || '').toLowerCase() === LOCAL_ENVIRONMENT;
}

/** cftlib ports. IDAM_TESTING_SUPPORT_URL is never called, but IdamUtils' constructor rejects a blank value. */
export function applyLocalServiceUrls(): void {
  process.env.MANAGE_CASE_BASE_URL ||= 'http://localhost:3000';
  process.env.DATA_STORE_URL_BASE ||= 'http://localhost:4452';
  process.env.CASE_API_URL ||= 'http://localhost:3206';
  process.env.IDAM_WEB_URL ||= 'http://localhost:5062';
  process.env.IDAM_TESTING_SUPPORT_URL ||= 'http://localhost:5062';
  process.env.S2S_URL ||= 'http://localhost:8489/testing-support/lease';
  process.env.ROLE_ASSIGNMENT_URL ||= 'http://localhost:4096';
  // CDAM is part of the cftlib stack; dm-store behind it only runs with LOCAL_DOC_SERVICES=true.
  process.env.CDAM_URL ||= 'http://localhost:4455';
  process.env.DM_STORE ||= 'http://localhost:4506';
  process.env.WIREMOCK_URL ||= 'http://localhost:8083';
  // The simulator ignores the password; the suite only checks that one is set.
  process.env.IDAM_PCS_USER_PASSWORD ||= 'local';
  process.env.PCS_API_IDAM_SECRET ||= 'local';
}

/** cftlib only starts dm-store behind CDAM with `LOCAL_DOC_SERVICES=true`, and CDAM answers 500 on upload without it. */
export async function localDocumentStoreIsUp(): Promise<boolean> {
  try {
    const response = await Axios.get(`${process.env.DM_STORE}/health`, {
      timeout: 5_000,
      validateStatus: () => true,
    });
    return response.status === 200;
  } catch {
    return false;
  }
}

export const localUsers = {
  claimantSolicitor: 'pcs-solicitor1@test.com',
  // Must match user.defendantSolicitor.email: createCaseAPI.action.ts hardcodes that address.
  defendantSolicitor: 'pcs-org1-solicitor2@test.com',
  hearingCentreAdmin: 'caseworker@pcs.com',
  hearingCentreTeamLeader: 'pcs-hearing-centre-team-leader-01@localhost',
  bailiff: 'sam.jones@justice.gov.uk',
  bailiffAdmin: 'pcs-bailiff-admin@localhost',
  bailiffManager: 'pcs-bailiff-manager@localhost',
  judge: 'judge@pcs.com',
  citizenDefendant: 'citizen@pcs.com',
} as const;

export type LocalUserKey = keyof typeof localUsers;

export const localUserNames: Record<string, string> = {
  [localUsers.bailiff]: 'Sam Jones',
  [localUsers.bailiffAdmin]: 'Ada BailiffAdmin',
  [localUsers.bailiffManager]: 'Mark BailiffManager',
};

export const localUserName = (email: string): string => localUserNames[email] ?? email;

export const localUserRoles: Record<string, string> = {
  [localUsers.bailiff]: 'bailiff',
  [localUsers.bailiffAdmin]: 'bailiff-admin',
  [localUsers.bailiffManager]: 'bailiff-manager',
  [localUsers.hearingCentreAdmin]: 'hearing-centre-admin',
  [localUsers.hearingCentreTeamLeader]: 'hearing-centre-team-leader',
};

export const localPassword = (): string => process.env.IDAM_PCS_USER_PASSWORD ?? 'local';

export async function signInToIdamSimulator(page: Page, email: string): Promise<void> {
  await page.waitForSelector('#username', { timeout: 30_000 });
  await page.locator('#username').fill(email);
  await page.locator('#password').fill(localPassword());
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.waitForURL(url => !url.href.includes('localhost:5062'), { timeout: 60_000 });
}

export const testCaseFixtures = {
  basicClaim: 'Create Case Make A Claim Basic Case',
  enforcementWarrant: 'Create Enforcement Warrant Basic Case',
  enforcementWrit: 'Create Enforcement Writ Basic Case',
} as const;

export type TestCaseFixture = (typeof testCaseFixtures)[keyof typeof testCaseFixtures];

interface DynamicListElement {
  code: string;
  label: string;
}

function dataStoreApi(bearerToken = process.env.BEARER_TOKEN) {
  return Axios.create({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${bearerToken}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      experimental: 'experimental',
      Accept: '*/*',
    },
  });
}

export async function createCaseFromFixture(fixture: TestCaseFixture): Promise<{ id: string; state: string }> {
  const api = dataStoreApi();
  const caseType = getCaseTypeId();

  const trigger = await api.get(`/case-types/${caseType}/event-triggers/createTestCase`);
  const fileList = trigger.data?.case_details?.case_data?.testCaseSupportFileList;
  const listItems: DynamicListElement[] = fileList?.list_items ?? [];
  const selected = listItems.find(item => item.label === fixture);

  if (!selected) {
    throw new Error(
      `This environment does not offer the "${fixture}" test-case fixture. Available: ${listItems.map(i => i.label).join(', ') || '(none)'}. ` +
        'The CCD definition must be generated with ENABLE_TESTING_SUPPORT=true (boot cftlib with it locally).'
    );
  }

  const created = await api.post(`/case-types/${caseType}/cases`, {
    data: { testCaseSupportFileList: { value: selected, list_items: listItems } },
    event: { id: 'createTestCase' },
    event_token: trigger.data.token,
  });

  return { id: created.data.id, state: created.data.state };
}

export async function recordFeePaid(caseReference: string): Promise<void> {
  const response = await Axios.post(
    `${process.env.CASE_API_URL}/testing-support/enforcement/${caseReference}/fee-paid`,
    undefined,
    { headers: { ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}` }, validateStatus: () => true }
  );
  if (response.status !== 204) {
    throw new Error(`Recording the enforcement fee as paid on ${caseReference} returned ${response.status}`);
  }
}

export const fixturePropertyAddress = {
  AddressLine1: '1 Rse Way',
  AddressLine2: '',
  PostTown: 'London',
  PostCode: 'SW11 1PD',
} as const;

export const basicClaimFixtureClaim = {
  defendant1: { nameKnown: 'YES', addressKnown: 'NO', firstName: 'John', lastName: 'Doe' },
  addAnotherDefendant: 'NO',
  additionalDefendants: [],
} as const;

/** The service request is created on a db-scheduler task, so it appears seconds after the case; `updatePaymentAPI` only retries for ten. */
export async function waitForServiceRequest(caseReference: string, timeoutMs = 90_000): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const response = await Axios.get(`${process.env.CASE_API_URL}/testing-support/fee-payment-info/${caseReference}`, {
      headers: { ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}` },
      validateStatus: () => true,
    });
    if (Array.isArray(response.data) && response.data.length > 0) {
      return;
    }
    await new Promise(resolve => setTimeout(resolve, 2_000));
  }
  throw new Error(
    `No service request was created for case ${caseReference} within ${timeoutMs}ms, so its fee cannot be paid.`
  );
}

export async function idamUserId(email: string): Promise<string> {
  const token = await idamToken(email);
  const response = await Axios.get(`${process.env.IDAM_WEB_URL}/o/userinfo`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data.uid ?? response.data.sub;
}

export async function allocateBailiffViaTestingSupport(
  caseReference: string,
  bailiff: { userId: string; name: string; email: string }
): Promise<void> {
  const response = await Axios.post(
    `${process.env.CASE_API_URL}/testing-support/enforcement/${caseReference}/allocate-bailiff`,
    bailiff,
    {
      headers: {
        Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
        ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
        'Content-Type': 'application/json',
      },
      validateStatus: () => true,
    }
  );
  if (response.status !== 204) {
    throw new Error(
      `Allocating a bailiff on ${caseReference} returned ${response.status}: ${JSON.stringify(response.data)}`
    );
  }
}

export async function localIdamToken(email: string): Promise<string> {
  const form = new URLSearchParams({
    grant_type: 'password',
    client_id: 'pcs-api',
    client_secret: process.env.PCS_API_IDAM_SECRET ?? 'local',
    scope: 'profile openid roles',
    username: email,
    password: localPassword(),
  });
  const response = await Axios.post(`${process.env.IDAM_WEB_URL}/o/token`, form.toString(), {
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
  });
  return response.data.access_token;
}

export async function idamToken(email: string): Promise<string> {
  if (isLocalStack()) {
    return localIdamToken(email);
  }
  return new IdamUtils().generateIdamToken({
    username: email,
    password: process.env.IDAM_PCS_USER_PASSWORD as string,
    grantType: 'password',
    clientId: 'pcs-api',
    clientSecret: process.env.PCS_API_IDAM_SECRET as string,
    scope: 'profile openid roles',
  });
}

export type DefendantCaseRole = '[DEFENDANT]' | '[DEFENDANTSOLICITOR]';

export async function assignDefendantCaseRole(
  caseReference: string,
  email: string,
  caseRole: DefendantCaseRole
): Promise<void> {
  const userId = await idamUserId(email);
  const response = await Axios.post(
    `${process.env.CASE_API_URL}/testing-support/enforcement/${caseReference}/defendant-case-role`,
    { userId, caseRole },
    {
      headers: {
        ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
        'Content-Type': 'application/json',
      },
      validateStatus: () => true,
    }
  );
  if (response.status !== 204) {
    throw new Error(
      `Giving ${email} the ${caseRole} case role on ${caseReference} returned ${response.status}: ${JSON.stringify(response.data)}`
    );
  }
}

export async function caseDataAs(caseReference: string, email: string): Promise<Record<string, unknown>> {
  const response = await dataStoreApi(await idamToken(email)).get(`/cases/${caseReference}`);
  return (response.data?.data ?? {}) as Record<string, unknown>;
}
