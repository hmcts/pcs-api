import { IdamUtils } from '@hmcts/playwright-common';
import { accessTokenApiData } from '@data/api-data';

export type CreateSolicitorParams = {
  email: string;
  password: string;
  forename: string;
  surname: string;
  roleNames: string[];
};

function ensureIdamProcessEnv(): void {
  process.env.IDAM_WEB_URL = process.env.IDAM_WEB_URL || accessTokenApiData.idamUrl;
  process.env.IDAM_TESTING_SUPPORT_URL =
    process.env.IDAM_TESTING_SUPPORT_URL || accessTokenApiData.idamTestingSupportUrl;
}

async function getIdamSystemUserBearerToken(): Promise<string> {
  const prebaked = process.env.CREATE_USER_BEARER_TOKEN?.trim();
  if (prebaked) {
    return prebaked;
  }

  const username = process.env.IDAM_SYSTEM_USERNAME?.trim();
  const password = process.env.IDAM_SYSTEM_USER_PASSWORD?.trim();
  if (!username || !password) {
    throw new Error(
      'Dynamic user creation: set CREATE_USER_BEARER_TOKEN, or IDAM_SYSTEM_USERNAME and IDAM_SYSTEM_USER_PASSWORD.'
    );
  }

  const clientId =
    process.env.IDAM_SYSTEM_CLIENT_ID?.trim() ||
    process.env.E2E_IDAM_CREATOR_CLIENT_ID?.trim() ||
    'pcs-api';
  const clientSecret = process.env.PCS_API_IDAM_SECRET;
  if (!clientSecret) {
    throw new Error('PCS_API_IDAM_SECRET is required for IDAM password-grant (system user).');
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
 * Creates a professional user via IDAM testing support, then activates (LIVE / ACTIVE) like EXUI flows.
 */
export async function createIdamSolicitorUser(params: CreateSolicitorParams): Promise<string> {
  ensureIdamProcessEnv();
  const bearerToken = await getIdamSystemUserBearerToken();
  const idam = new IdamUtils();
  try {
    const created = await idam.createUser({
      bearerToken,
      password: params.password,
      user: {
        email: params.email,
        forename: params.forename,
        surname: params.surname,
        roleNames: params.roleNames,
      },
    });

    await idam.updateUser({
      id: created.id,
      bearerToken,
      password: params.password,
      user: {
        email: params.email,
        forename: params.forename,
        surname: params.surname,
        roleNames: params.roleNames,
      },
    });

    return created.id;
  } finally {
    await idam.dispose();
  }
}
