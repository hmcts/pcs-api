/**
 * Batch print (feature 17, HDPI-8012).
 *
 * pcs-api offers batch print as an API, not a CCD event: CCD has no screen that acts on several cases, and
 * XUI's "My work" cannot act on several tasks at once. So the steps call it the way a selection screen
 * would, as the bailiff, with the bailiff's own IDAM token:
 *
 *   - `POST /enforcement/batch-print/check` says what would be printed and why any case is left out;
 *   - `POST /enforcement/batch-print` returns the PDF, naming the printed and left-out cases in headers.
 *
 * The warrant, risk assessment and notice of eviction printed are the ones pcs-api generates as the warrant is
 * approved and its eviction scheduled. A rescheduled notice of eviction cannot be driven yet, so one is filed
 * as a placeholder through `POST /testing-support/enforcement/{caseReference}/documents`. Generating and
 * filing both upload through CDAM, which needs dm-store, which cftlib only runs with `LOCAL_DOC_SERVICES=true`.
 */
import Axios from 'axios';
import { localIdamToken, localUsers } from '@config/local-stack.config';

export const PRINT_ENFORCEMENT_DOCUMENTS = 'Print enforcement documents';

export type DocumentKind = 'WARRANT' | 'RISK_ASSESSMENT' | 'NOTICE_OF_EVICTION';

/** The documents printed for each case, in print order. */
export const PRINTED_KINDS: DocumentKind[] = ['WARRANT', 'RISK_ASSESSMENT', 'NOTICE_OF_EVICTION'];

export interface PrintableDocument {
  kind: DocumentKind;
  fileName: string;
  submittedDate: string;
}

export interface PrintableCase {
  caseReference: string;
  warrantNumber: string;
  documents: PrintableDocument[];
  missingDocuments: DocumentKind[];
}

export interface ExcludedCase {
  caseReference: string;
  code: string;
  reason: string;
}

export interface BatchPrintPlan {
  included: PrintableCase[];
  excluded: ExcludedCase[];
}

export interface BatchPrintFile {
  status: number;
  contentType: string;
  /** The first bytes of the body, which for a PDF read "%PDF". */
  signature: string;
  includedCases: string[];
  excludedCases: string[];
}

function serviceHeaders(): Record<string, string> {
  return {
    ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
    'Content-Type': 'application/json',
  };
}

async function bailiffHeaders(bailiffEmail: string): Promise<Record<string, string>> {
  return { ...serviceHeaders(), Authorization: `Bearer ${await localIdamToken(bailiffEmail)}` };
}

/**
 * Files a one-page placeholder PDF reading `title` in the case's Enforcement folder, against its current
 * enforcement. Documents filed later count as later: batch print takes the latest of each kind.
 */
export async function filePlaceholderDocument(caseReference: string, kind: DocumentKind, title: string): Promise<string> {
  const response = await Axios.post(
    `${process.env.CASE_API_URL}/testing-support/enforcement/${caseReference}/documents`,
    { kind, title },
    { headers: serviceHeaders(), validateStatus: () => true }
  );
  if (response.status !== 201) {
    throw new Error(
      `Filing a placeholder ${kind} on ${caseReference} returned ${response.status}: ${JSON.stringify(response.data)}. `
        + 'CDAM needs dm-store behind it: boot cftlib with LOCAL_DOC_SERVICES=true.'
    );
  }
  return response.data.fileName;
}

export async function checkBatchPrint(
  caseReferences: string[],
  bailiffEmail: string = localUsers.bailiff
): Promise<BatchPrintPlan> {
  const response = await Axios.post(
    `${process.env.CASE_API_URL}/enforcement/batch-print/check`,
    { caseReferences },
    { headers: await bailiffHeaders(bailiffEmail), validateStatus: () => true }
  );
  if (response.status !== 200) {
    throw new Error(`Checking a batch print returned ${response.status}: ${JSON.stringify(response.data)}`);
  }
  return response.data as BatchPrintPlan;
}

export async function printBatch(
  caseReferences: string[],
  bailiffEmail: string = localUsers.bailiff
): Promise<BatchPrintFile> {
  const response = await Axios.post(
    `${process.env.CASE_API_URL}/enforcement/batch-print`,
    { caseReferences },
    { headers: await bailiffHeaders(bailiffEmail), responseType: 'arraybuffer', validateStatus: () => true }
  );
  const body = Buffer.from(response.data as ArrayBuffer);
  const list = (header: unknown): string[] =>
    typeof header === 'string' && header.length > 0 ? header.split(',') : [];
  return {
    status: response.status,
    contentType: String(response.headers['content-type'] ?? ''),
    signature: body.subarray(0, 4).toString('latin1'),
    includedCases: list(response.headers['batch-print-included-cases']),
    excludedCases: list(response.headers['batch-print-excluded-cases']),
  };
}
