import axios from 'axios';
import { referenceDataApiData } from '@data/api-data';
import { getRdS2SToken } from './rdServiceAuth';
import { getOrganisationAssignmentUserBearer } from './orgAssignmentAuth';

const defaultMaxAttempts = Number(process.env.E2E_RD_ORG_POLL_ATTEMPTS ?? '30');
const defaultDelayMs = Number(process.env.E2E_RD_ORG_POLL_DELAY_MS ?? '2000');

/**
 * Polls RD internal orgDetails until the new user is visible (same contract as pcs-api Feign client).
 */
export async function waitForOrganisationDetailsByUserId(userId: string): Promise<void> {
  const base =
    process.env.RD_PROFESSIONAL_API_URL?.trim().replace(/\/$/, '') ||
    referenceDataApiData.defaultInternalBaseUrlAat.replace(/\/$/, '');

  const fixedS2s = process.env.E2E_RD_SERVICE_AUTHORIZATION?.trim();
  const serviceAuth = fixedS2s
    ? fixedS2s.replace(/^Bearer\s+/i, '')
    : await getRdS2SToken();

  const userAuth = await getOrganisationAssignmentUserBearer();

  const url = `${base}/refdata/internal/v1/organisations/orgDetails/${userId}`;

  for (let attempt = 1; attempt <= defaultMaxAttempts; attempt++) {
    const res = await axios.get(url, {
      headers: {
        ServiceAuthorization: `Bearer ${serviceAuth}`,
        Authorization: `Bearer ${userAuth}`,
      },
      validateStatus: () => true,
    });

    if (res.status === 200 && res.data?.organisationIdentifier) {
      return;
    }

    await new Promise(r => setTimeout(r, defaultDelayMs));
  }

  throw new Error(
    `Timed out waiting for RD orgDetails for user ${userId} (${defaultMaxAttempts} attempts, ${defaultDelayMs}ms delay).`
  );
}
