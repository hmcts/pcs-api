import { IdamUtils, ServiceAuthUtils } from '@hmcts/playwright-common';
import { accessTokenApiData, s2STokenApiData } from '@data/api-data';
import { user } from '@data/user-data';
import { authenticateAndSaveState } from '@utils/auth/auth-setup';

async function globalSetupConfig(): Promise<void> {
  // Get API tokens (for API-based test setup)
  await getAccessToken();
  await getS2SToken();
  
  // Perform browser-based login and save authentication state
  // This allows all tests to reuse the same authenticated session
  await authenticateAndSaveState();
}

export const getS2SToken = async (): Promise<void> => {
  process.env.S2S_URL = s2STokenApiData.s2sUrl;
  process.env.SERVICE_AUTH_TOKEN = await new ServiceAuthUtils().retrieveToken({ microservice: s2STokenApiData.microservice });
}

export const getAccessToken = async (): Promise<void> => {
  process.env.IDAM_WEB_URL = accessTokenApiData.idamUrl;
  process.env.IDAM_TESTING_SUPPORT_URL = accessTokenApiData.idamTestingSupportUrl;
  process.env.BEARER_TOKEN = await new IdamUtils().generateIdamToken({
    username: user.claimantSolicitor.email,
    password: user.claimantSolicitor.password,
    grantType: 'password',
    clientId: 'pcs-api',
    clientSecret: process.env.PCS_API_IDAM_SECRET as string,
    scope: 'profile openid roles'
  });
};

export default globalSetupConfig;
