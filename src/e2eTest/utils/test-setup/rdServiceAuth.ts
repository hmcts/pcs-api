import { ServiceAuthUtils } from '@hmcts/playwright-common';
import { s2STokenApiData } from '@data/api-data';

/**
 * S2S token accepted by RD Professional. Requires the secret registered for that microservice in Service Auth.
 */
export async function getRdS2SToken(): Promise<string> {
  process.env.S2S_URL = process.env.S2S_URL || s2STokenApiData.s2sUrl;
  const microservice = process.env.E2E_RD_S2S_MICROSERVICE?.trim() || 'rd_professional_api';
  const secret =
    process.env.E2E_RD_S2S_SECRET?.trim() ||
    process.env.S2S_SECRET?.trim() ||
    process.env.PCS_API_S2S_SECRET?.trim();
  if (!secret) {
    throw new Error(
      'RD S2S: set E2E_RD_S2S_SECRET (preferred), or S2S_SECRET / PCS_API_S2S_SECRET for rd_professional_api lease.'
    );
  }
  return new ServiceAuthUtils({ secret }).retrieveToken({ microservice, secret });
}
