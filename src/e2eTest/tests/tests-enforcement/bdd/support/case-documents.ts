// pcs-api generates each document on a db-scheduler task after the event commits, so every check polls. Each
// is recognised by file name: until the real templates exist every document renders from a stand-in.
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

export function documentTitle(name: string, world: EnforcementWorld): string {
  if (name === 'enforcement application') {
    if (!world.applicationType) {
      throw new Error('The scenario has not said which enforcement it applied for');
    }
    return `${capitalise(world.applicationType)} application - `;
  }
  return `${capitalise(name)} - `;
}

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

export async function countEnforcementDocuments(caseReference: string, title: string): Promise<number> {
  return (await enforcementDocuments(caseReference, 'internal user')).filter(name => name.startsWith(title)).length;
}

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
