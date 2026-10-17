/**
 * Local-stack (cftlib) mode for the e2e suite, selected with `ENVIRONMENT=local`.
 *
 * `./gradlew bootWithCCD` runs the whole CFT stack in-process. It differs from AAT in four ways that
 * the suite has to know about:
 *
 * 1. Service URLs are all on localhost (see {@link applyLocalServiceUrls}).
 * 2. IDAM is the RSE IDAM simulator, whose sign-in page is a single form (`#username`, `#password`,
 *    "Sign in") rather than AAT's two-step email-then-password journey, and which accepts any password.
 * 3. The permanent AAT test users do not exist. The cftlib users seeded by `CftlibConfig` stand in for
 *    them (see {@link localUsers}).
 * 4. dm-store is not started by cftlib, so CDAM cannot store anything and the suite's document uploads
 *    fail. Local runs therefore create cases from the `createTestCase` fixtures, which reference no
 *    documents, instead of the document-bearing `createPossessionClaim` / `submitPossessionClaim` payloads.
 *
 * Nothing here runs unless `ENVIRONMENT=local`, so AAT and the nightly environments are unaffected.
 */
import Axios from 'axios';
import { Page } from '@playwright/test';
import { getCaseTypeId } from '@utils/common/caseType.utils';

export const LOCAL_ENVIRONMENT = 'local';

export function isLocalStack(): boolean {
  return (process.env.ENVIRONMENT || '').toLowerCase() === LOCAL_ENVIRONMENT;
}

/**
 * cftlib ports, as exposed by `./gradlew bootWithCCD`. `IDAM_TESTING_SUPPORT_URL` is only read by
 * `IdamUtils`' constructor, which rejects a blank value; the suite never calls an IDAM testing-support
 * endpoint on a local run, so pointing it at the simulator is enough.
 */
export function applyLocalServiceUrls(): void {
  process.env.MANAGE_CASE_BASE_URL ||= 'http://localhost:3000';
  process.env.DATA_STORE_URL_BASE ||= 'http://localhost:4452';
  process.env.CASE_API_URL ||= 'http://localhost:3206';
  process.env.IDAM_WEB_URL ||= 'http://localhost:5062';
  process.env.IDAM_TESTING_SUPPORT_URL ||= 'http://localhost:5062';
  process.env.S2S_URL ||= 'http://localhost:8489/testing-support/lease';
  process.env.ROLE_ASSIGNMENT_URL ||= 'http://localhost:4096';
  // CDAM is part of the cftlib stack; dm-store behind it only runs with LOCAL_DOC_SERVICES=true
  // (docker-compose-documents.yml). Both must point at localhost, never the AAT defaults.
  process.env.CDAM_URL ||= 'http://localhost:4455';
  process.env.DM_STORE ||= 'http://localhost:4506';
  // The simulator ignores the password; the suite only checks that one is set.
  process.env.IDAM_PCS_USER_PASSWORD ||= 'local';
  process.env.PCS_API_IDAM_SECRET ||= 'local';
}

/**
 * Whether dm-store is behind CDAM on this stack. cftlib always starts CDAM, but it only starts dm-store
 * when `bootWithCCD` ran with `LOCAL_DOC_SERVICES=true`, and CDAM answers 500 ("Connection refused") on
 * upload without it. Probed rather than assumed so a local run works either way.
 */
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

/**
 * The cftlib users that stand in for the permanent AAT users, keyed by the role the feature files use.
 * Seeded by `src/cftlib/java/uk/gov/hmcts/reform/pcs/CftlibConfig.java`; the bailiff users' RAS
 * organisation roles come from `src/cftlib/resources/cftlib-am-role-assignments.json`.
 */
export const localUsers = {
  claimantSolicitor: 'pcs-solicitor1@test.com',
  defendantSolicitor: 'pcs-defendant-solicitor@test.com',
  hearingCentreAdmin: 'caseworker@pcs.com',
  hearingCentreTeamLeader: 'pcs-hearing-centre-team-leader-01@localhost',
  bailiff: 'pcs-bailiff@localhost',
  bailiffAdmin: 'pcs-bailiff-admin@localhost',
  bailiffManager: 'pcs-bailiff-manager@localhost',
  judge: 'judge@pcs.com',
} as const;

export type LocalUserKey = keyof typeof localUsers;

/**
 * Display names for the local users, as the IDAM simulator holds them. `BailiffAllocationService` stores
 * the bailiff's name alongside the allocation, so the allocation shortcut needs a real one.
 */
export const localUserNames: Record<string, string> = {
  [localUsers.bailiff]: 'Bob Bailiff',
  [localUsers.bailiffAdmin]: 'Ada BailiffAdmin',
  [localUsers.bailiffManager]: 'Mark BailiffManager',
};

export const localUserName = (email: string): string => localUserNames[email] ?? email;

/** The RAS organisation role each local bailiff user carries, for the assertions that name a role. */
export const localUserRoles: Record<string, string> = {
  [localUsers.bailiff]: 'bailiff',
  [localUsers.bailiffAdmin]: 'bailiff-admin',
  [localUsers.bailiffManager]: 'bailiff-manager',
  [localUsers.hearingCentreAdmin]: 'hearing-centre-admin',
  [localUsers.hearingCentreTeamLeader]: 'hearing-centre-team-leader',
};

export const localPassword = (): string => process.env.IDAM_PCS_USER_PASSWORD ?? 'local';

/**
 * Signs in through the IDAM simulator's single-page form. XUI redirects to it on any unauthenticated
 * page load, so callers navigate to a manage-case URL first.
 */
export async function signInToIdamSimulator(page: Page, email: string): Promise<void> {
  await page.waitForSelector('#username', { timeout: 30_000 });
  await page.locator('#username').fill(email);
  await page.locator('#password').fill(localPassword());
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.waitForURL(url => !url.href.includes('localhost:5062'), { timeout: 60_000 });
}

/** The `createTestCase` fixtures `TestCaseSupportHelper` offers, by the label it derives from the filename. */
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

/**
 * Creates a case through the `createTestCase` event ("Test Support Case Creation") from one of the
 * test-case-generation fixtures. `TestCaseGeneration` only configures the event when `ENVIRONMENT=preview`
 * or `ENABLE_TESTING_SUPPORT=true`, so the local stack must be booted with testing support on.
 *
 * The enforcement fixtures create the claim and submit the enforcement application in one submit, leaving
 * the case in `CASE_ISSUED` with an enforcement in `Awaiting payment`.
 */
export async function createCaseFromFixture(fixture: TestCaseFixture): Promise<{ id: string; state: string }> {
  const api = dataStoreApi();
  const caseType = getCaseTypeId();

  const trigger = await api.get(`/case-types/${caseType}/event-triggers/createTestCase`);
  const fileList = trigger.data?.case_details?.case_data?.testCaseSupportFileList;
  const listItems: DynamicListElement[] = fileList?.list_items ?? [];
  const selected = listItems.find(item => item.label === fixture);

  if (!selected) {
    throw new Error(
      `The local stack does not offer the "${fixture}" test-case fixture. Available: ${listItems.map(i => i.label).join(', ') || '(none)'}. ` +
        'Boot cftlib with ENABLE_TESTING_SUPPORT=true.'
    );
  }

  const created = await api.post(`/case-types/${caseType}/cases`, {
    data: { testCaseSupportFileList: { value: selected, list_items: listItems } },
    event: { id: 'createTestCase' },
    event_token: trigger.data.token,
  });

  return { id: created.data.id, state: created.data.state };
}

/**
 * Records the enforcement fee as paid, moving the case's application from "Awaiting payment" to
 * "Awaiting review". pcs-api cannot take an enforcement fee yet (HDPI-2877).
 */
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

/**
 * The defendant every fixture puts on the claim, in the shape the enforcement actions expect of a
 * `submitPossessionClaim` payload.
 *
 * `EnforcementTestCaseGenerator` always builds the claim from `Create-Case-Make-A-Claim-Basic-Case` and
 * then applies only the enforcement fields from the Create-Enforcement-* fixture, so the parties on the
 * case come from the base claim — one defendant, John Doe — whatever the enforcement fixture's own
 * `parties` / `allDefendants` say.
 */
export const basicClaimFixtureClaim = {
  defendant1: { nameKnown: 'YES', addressKnown: 'NO', firstName: 'John', lastName: 'Doe' },
  addAnotherDefendant: 'NO',
  additionalDefendants: [],
} as const;

/** The IDAM id of a local user, read from the simulator's userinfo endpoint rather than derived. */
export async function localIdamUserId(email: string): Promise<string> {
  const token = await localIdamToken(email);
  const response = await Axios.get(`${process.env.IDAM_WEB_URL}/o/userinfo`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data.uid ?? response.data.sub;
}

/**
 * Allocates the case's current warrant to a bailiff through pcs-api testing support, moving it from
 * "Awaiting bailiff allocation" to "Awaiting scheduling".
 *
 * The real `allocateBailiff` event cannot be driven outside AAT: its StaffUser type-ahead needs staff
 * reference data that neither cftlib nor a preview environment runs. This is the same shortcut as
 * `fee-paid` — it goes through `BailiffAllocationService`, so the outcome matches the event.
 *
 * Needs a user bearer token as well as the S2S one, because the service records who made the allocation.
 */
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

/** An IDAM token for one of the local users, for API calls made as somebody other than the suite's default user. */
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
