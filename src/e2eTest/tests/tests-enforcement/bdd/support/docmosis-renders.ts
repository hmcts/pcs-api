// Locally Docmosis is a wiremock stub that answers every render with the same placeholder PDF, so what the
// document would show is checked in the render request rather than in the stored document.
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

export function noteCheckedOnRenderData(what: string): void {
  test.info().annotations.push({
    type: 'checked on the Docmosis render data',
    description: `OPEN-QUESTIONS 42: ${what} is checked in the data pcs-api sends Docmosis; the stub returns a placeholder PDF`,
  });
}
