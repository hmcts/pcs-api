/**
 * Reading the generated enforcement documents in Case File View, through the CCD data store.
 *
 * pcs-api generates each document on a db-scheduler task after the event commits, so a document appears some
 * seconds after the event; every check here polls. The case is read as a given user because pcs-api filters
 * `allDocuments` by who is asking: defendants see only a served warrant's notice of eviction, and the claimant
 * side does not see the warrants or the notice of eviction.
 *
 * Documents are recognised by file name. pcs-api names each one "<title> - <warrant number or case number>",
 * from `EnforcementOutput.title`, because until the real Docmosis templates exist every document is rendered
 * from a stand-in template (locally, as the wiremock stub's placeholder PDF), so the content cannot tell them
 * apart. What each was rendered from is in `docmosis-renders.ts`.
 */
import Axios from 'axios';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { idamToken, isLocalStack, localUsers } from '@config/local-stack.config';
import { user } from '@data/user-data';
import { expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';

export const ENFORCEMENT_FOLDER = 'Enforcement';
const ENFORCEMENT_CATEGORY_ID = 'enforcement';

/** How long generation can take: a db-scheduler poll, Docmosis, the CDAM upload, then the data store read. */
const GENERATION_TIMEOUT = 2 * VERY_LONG_TIMEOUT;

export type DocumentViewer = 'internal user' | 'claimant';

/** The internal user is a hearing centre admin; the claimant is the suite's claimant legal representative. */
function viewerEmail(viewer: DocumentViewer): string {
  if (viewer === 'internal user') {
    return isLocalStack() ? localUsers.hearingCentreAdmin : user.hearingCenterAdmin.email;
  }
  return isLocalStack() ? localUsers.claimantSolicitor : (user.localAuthorityOrg1Usr1.email as string);
}

interface CaseFileDocument {
  filename: string;
  categoryId?: string;
}

/** The file name prefix pcs-api gives the document the feature files call `name`. */
export function documentTitle(name: string, world: EnforcementWorld): string {
  if (name === 'enforcement application') {
    if (!world.applicationType) {
      throw new Error('The scenario has not said which enforcement it applied for');
    }
    return `${capitalise(world.applicationType)} application - `;
  }
  return `${capitalise(name)} - `;
}

/** The enforcement documents `viewer` can see on the case. */
export async function enforcementDocuments(caseReference: string, viewer: DocumentViewer): Promise<string[]> {
  const token = await idamToken(viewerEmail(viewer));
  const response = await Axios.get(`${process.env.DATA_STORE_URL_BASE}/cases/${caseReference}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      experimental: 'true',
      Accept: '*/*',
    },
  });
  const allDocuments: { value?: { document_filename?: string; category_id?: string } }[] =
    response.data?.data?.allDocuments ?? [];
  return allDocuments
    .map(item => ({ filename: item.value?.document_filename ?? '', categoryId: item.value?.category_id }) as CaseFileDocument)
    .filter(document => document.categoryId === ENFORCEMENT_CATEGORY_ID)
    .map(document => document.filename);
}

/** Waits until a document whose file name starts with `title` is in the Enforcement folder for `viewer`. */
export async function expectEnforcementDocument(
  caseReference: string,
  title: string,
  viewer: DocumentViewer = 'internal user'
): Promise<void> {
  await expect
    .poll(async () => (await enforcementDocuments(caseReference, viewer)).some(name => name.startsWith(title)), {
      message: `"${title}…" in the ${ENFORCEMENT_FOLDER} folder for the ${viewer}`,
      timeout: GENERATION_TIMEOUT,
    })
    .toBe(true);
}

/** Counts the documents whose file name starts with `title`, as an internal user sees the folder. */
export async function countEnforcementDocuments(caseReference: string, title: string): Promise<number> {
  return (await enforcementDocuments(caseReference, 'internal user')).filter(name => name.startsWith(title)).length;
}

/** Asserts `viewer` cannot see a document whose file name starts with `title`, once an internal user can. */
export async function expectHiddenFrom(caseReference: string, title: string, viewer: DocumentViewer): Promise<void> {
  await expectEnforcementDocument(caseReference, title, 'internal user');
  expect(
    (await enforcementDocuments(caseReference, viewer)).some(name => name.startsWith(title)),
    `"${title}…" is hidden from the ${viewer}`
  ).toBe(false);
}


function capitalise(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
