import { IdamUtils } from '@hmcts/playwright-common';
import { accessTokenApiData } from '@data/api-data';

function ensureIdamProcessEnv(): void {
  process.env.IDAM_WEB_URL = process.env.IDAM_WEB_URL || accessTokenApiData.idamUrl;
  process.env.IDAM_TESTING_SUPPORT_URL =
    process.env.IDAM_TESTING_SUPPORT_URL || accessTokenApiData.idamTestingSupportUrl;
}

async function passwordGrantToken(username: string, password: string): Promise<string> {
  const clientId = process.env.E2E_RD_ASSIGNMENT_CLIENT_ID?.trim() || 'pcs-api';
  const clientSecret = process.env.PCS_API_IDAM_SECRET;
  if (!clientSecret) {
    throw new Error('PCS_API_IDAM_SECRET is required to obtain RD assignment user token.');
  }
  ensureIdamProcessEnv();
  const idam = new IdamUtils();
  try {
    return await idam.generateIdamToken({
      grantType: 'password',
      username,
      password,
      clientId,
      clientSecret,
      scope: 'profile openid roles',
    });
  } finally {
    await idam.dispose();
  }
}

/**
 * Bearer JWT for RD internal org user APIs (`prd-admin` or org admin, depending on your tenant).
 * Override with E2E_RD_USER_AUTHORIZATION (optional `Bearer ` prefix).
 */
export async function getOrganisationAssignmentUserBearer(): Promise<string> {
  const fixed = process.env.E2E_RD_USER_AUTHORIZATION?.trim();
  if (fixed) {
    return fixed.replace(/^Bearer\s+/i, '');
  }

  const preferOrgAdmin = process.env.E2E_RD_ASSIGNMENT_USE_ORG_USER_TOKEN !== 'false';
  if (preferOrgAdmin) {
    const email =
      process.env.PCS_ORG_ADMIN_EMAIL?.trim() ||
      process.env.E2E_ORG_ADMIN_EMAIL?.trim();
    const password =
      process.env.PCS_ORG_ADMIN_PASSWORD?.trim() ||
      process.env.E2E_ORG_ADMIN_PASSWORD?.trim();
    if (email && password) {
      return passwordGrantToken(email, password);
    }
  }

  const prdUser = process.env.PCS_PRD_ADMIN_USERNAME?.trim();
  const prdPass = process.env.PCS_PRD_ADMIN_PASSWORD?.trim();
  if (prdUser && prdPass) {
    return passwordGrantToken(prdUser, prdPass);
  }

  throw new Error(
    'RD org assignment auth: set E2E_RD_USER_AUTHORIZATION, or PCS_ORG_ADMIN_EMAIL/PASSWORD, or PCS_PRD_ADMIN_USERNAME/PASSWORD.'
  );
}
