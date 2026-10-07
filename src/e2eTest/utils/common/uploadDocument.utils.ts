import fs from 'fs';
import path from 'path';

// eslint-disable-next-line import/no-named-as-default
import Axios from 'axios';

const INPUT_FILES_DIR = path.join(__dirname, '../../data/inputFiles');
const MANIFEST_PATH = path.join(__dirname, '../../.auth/test-documents.json');

const UPLOAD_ATTEMPTS = 3;
const UPLOAD_RETRY_DELAY_MS = 2_000;

const MIME_TYPES: Record<string, string> = {
  '.pdf': 'application/pdf',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  '.xlsx': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  '.ppt': 'application/vnd.ms-powerpoint',
};

/** A CCD `Document` value as it appears inside a case payload. */
export interface CcdDocument {
  document_url: string;
  document_binary_url: string;
  document_filename: string;
}

/**
 * Every document the case payloads reference, uploaded once per run by the setup
 * project. `slot` distinguishes two uploads of the same file where a payload needs
 * two genuinely different documents in one collection.
 */
export const TEST_DOCUMENT_SLOTS: readonly { file: string; slot?: string }[] = [
  { file: 'tenancy.pdf' },
  { file: 'NoticeDetails.pdf' },
  { file: 'rentArrears.pdf' },
  { file: 'rentArrears.pdf', slot: 'second' },
  { file: 'witnessStatement.pdf' },
  { file: 'rentStatement.pdf' },
  { file: 'NoticeDetails.pdf' },
  { file: 'energyPerformance.pdf' },
  { file: 'gasSafety.pdf' },
  { file: 'electricalReport.pdf' },
  { file: 'noticeForService.pdf' },
  { file: 'legalAidCertificate.pdf' },
  { file: 'otherDocument.pdf' },
  { file: 'inspectionOrReport.pdf' },
];

const slotKey = (file: string, slot?: string): string => (slot ? `${file}#${slot}` : file);

const mimeTypeFor = (fileName: string): string =>
  MIME_TYPES[path.extname(fileName).toLowerCase()] ?? 'application/octet-stream';

/**
 * Uploads one fixture file to CDAM.
 *
 * CDAM authenticates the user (IDAM bearer) *and* the calling microservice (S2S),
 * and `service_config.json` pins `pcs_api` to jurisdiction `PCS` -- a different
 * jurisdiction is a 403, not a warning. `caseTypeId` follows CASE_TYPE_ID so that
 * suffixed preview / staging case types work.
 */
export async function uploadTestDocument(fileName: string): Promise<CcdDocument> {
  const filePath = path.join(INPUT_FILES_DIR, fileName);
  if (!fs.existsSync(filePath)) {
    throw new Error(`Test document '${fileName}' not found in ${INPUT_FILES_DIR}`);
  }

  const formData = new FormData();
  formData.append('files', new Blob([fs.readFileSync(filePath)], { type: mimeTypeFor(fileName) }), fileName);
  formData.append('classification', 'PUBLIC');
  formData.append('caseTypeId', process.env.CASE_TYPE_ID ?? 'PCS');
  formData.append('jurisdictionId', 'PCS');

  let lastError: unknown;
  for (let attempt = 1; attempt <= UPLOAD_ATTEMPTS; attempt++) {
    try {
      const response = await Axios.post(`${process.env.CDAM_URL}/cases/documents`, formData, {
        timeout: 30_000,
        headers: {
          Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
          ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
        },
      });

      const document = response.data?.documents?.[0];
      const selfHref = document?._links?.self?.href;
      const binaryHref = document?._links?.binary?.href;
      if (!selfHref || !binaryHref) {
        throw new Error(`CDAM returned no document links for '${fileName}'`);
      }

      return {
        document_url: selfHref,
        document_binary_url: binaryHref,
        document_filename: document.originalDocumentName ?? fileName,
      };
    } catch (error: unknown) {
      lastError = error;
      if (attempt < UPLOAD_ATTEMPTS) {
        await new Promise(resolve => setTimeout(resolve, UPLOAD_RETRY_DELAY_MS));
      }
    }
  }

  const status = Axios.isAxiosError(lastError) ? lastError.response?.status : undefined;
  const body = Axios.isAxiosError(lastError) ? JSON.stringify(lastError.response?.data) : String(lastError);
  throw new Error(`CDAM upload failed for '${fileName}'${status ? ` with status ${status}` : ''}: ${body}`);
}

/**
 * Uploads every slot in {@link TEST_DOCUMENT_SLOTS} and writes the manifest the
 * worker processes read. Called by the setup project, once, after the IDAM and
 * S2S tokens are in place.
 */
export async function uploadTestDocuments(): Promise<void> {
  if (!process.env.CDAM_URL) {
    throw new Error('CDAM_URL is not set (applyPlaywrightServiceUrls should have defaulted it)');
  }

  const uploads = await Promise.all(
    TEST_DOCUMENT_SLOTS.map(async ({ file, slot }) => [slotKey(file, slot), await uploadTestDocument(file)] as const)
  );

  fs.mkdirSync(path.dirname(MANIFEST_PATH), { recursive: true });
  fs.writeFileSync(MANIFEST_PATH, JSON.stringify(Object.fromEntries(uploads), null, 2), 'utf8');
  console.log(`\n✅ UPLOADED ${uploads.length} TEST DOCUMENTS TO CDAM`);
}

let manifest: Record<string, CcdDocument> | undefined;

/**
 * The document uploaded for a slot this run. Case payloads call this instead of
 * hardcoding a dm-store UUID, so nothing depends on a document someone uploaded
 * by hand months ago.
 */
export function testDocument(file: string, slot?: string): CcdDocument {
  if (!manifest) {
    if (!fs.existsSync(MANIFEST_PATH)) {
      throw new Error(
        `No uploaded test documents found at ${MANIFEST_PATH}. Run the 'setup' project first (it uploads them).`
      );
    }
    manifest = JSON.parse(fs.readFileSync(MANIFEST_PATH, 'utf8')) as Record<string, CcdDocument>;
  }

  const key = slotKey(file, slot);
  const document = manifest[key];
  if (!document) {
    throw new Error(`No uploaded test document for '${key}'. Add it to TEST_DOCUMENT_SLOTS in uploadDocument.utils.ts`);
  }
  return document;
}

/** A {@link testDocument} wrapped as a CCD collection item. */
export const testDocumentListValue = (file: string, slot?: string): { value: CcdDocument } => ({
  value: testDocument(file, slot),
});
