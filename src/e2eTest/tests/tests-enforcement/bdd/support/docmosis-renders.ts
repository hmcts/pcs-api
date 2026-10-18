/**
 * What pcs-api sent Docmosis for each enforcement document, read from wiremock's request journal.
 *
 * Locally Docmosis is wiremock's `docmosis/render.json` stub, which answers every render with the same
 * placeholder PDF, so the document in case file view cannot show a seal, a date stamp or the claimant's
 * answers. The render request can: its `data` part is the `EnforcementDocumentPayload` the real template
 * will be filled from (the seal, the generation time and the enforcement order as submitted).
 */
import Axios from 'axios';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { expect, test } from '../fixtures/enforcement.fixtures';

export interface DocmosisRender {
  outputName: string;
  data: {
    documentTitle?: string;
    caseReference?: string;
    seal?: string | null;
    generatedAt?: string;
    enforcementOrder?: Record<string, unknown>;
  };
}

interface JournalEntry {
  body?: string;
}

/** One named part of a multipart/form-data body. */
function formPart(body: string, name: string): string | undefined {
  const match = body.match(new RegExp(`name="${name}"\\r?\\n(?:[^\\r\\n]+\\r?\\n)*\\r?\\n([\\s\\S]*?)\\r?\\n--`));
  return match?.[1];
}

async function renderRequests(): Promise<DocmosisRender[]> {
  const response = await Axios.post(`${process.env.WIREMOCK_URL}/__admin/requests/find`, {
    method: 'POST',
    urlPath: '/rs/render',
  });
  const requests: JournalEntry[] = response.data?.requests ?? [];
  return requests.flatMap(request => {
    const body = request.body ?? '';
    const outputName = formPart(body, 'outputName');
    const data = formPart(body, 'data');
    return outputName && data ? [{ outputName, data: JSON.parse(data) }] : [];
  });
}

const dashed = (caseReference: string): string => caseReference.replace(/(.{4})(?=.)/g, '$1-');

/**
 * Waits for the render of the case's document whose file name starts with `title` and returns it. pcs-api
 * names the output the same way as the stored document ("<title> - <warrant number or case number>").
 */
export async function docmosisRender(caseReference: string, title: string): Promise<DocmosisRender> {
  let found: DocmosisRender | undefined;
  await expect
    .poll(
      async () => {
        found = (await renderRequests()).find(
          render => render.data.caseReference === dashed(caseReference) && render.outputName.startsWith(title)
        );
        return found !== undefined;
      },
      { message: `the Docmosis render of "${title}…" for case ${caseReference}`, timeout: VERY_LONG_TIMEOUT }
    )
    .toBe(true);
  return found as DocmosisRender;
}

/** Records that the check was made on the data sent to Docmosis, not on the stand-in PDF it returned. */
export function noteCheckedOnRenderData(what: string): void {
  test.info().annotations.push({
    type: 'checked on the Docmosis render data',
    description: `OPEN-QUESTIONS 42: ${what} is checked in the data pcs-api sends Docmosis; the stub returns a placeholder PDF`,
  });
}
