import { IdamUtils } from '@hmcts/playwright-common';
import { accessTokenApiData } from '@data/api-data/accessToken.api.data';

async function globalSetupConfig(): Promise<void> {
  await getAccessToken();
}

export const getAccessToken = async (): Promise<void> => {
  process.env.IDAM_WEB_URL = accessTokenApiData.idamUrl;
  process.env.IDAM_TESTING_SUPPORT_URL = accessTokenApiData.idamTestingSupportUrl;
  process.env.CREATE_USER_BEARER_TOKEN = await new IdamUtils().generateIdamToken({
    grantType: 'client_credentials',
    clientId: 'pcs-api',
    clientSecret: process.env.PCS_API_IDAM_SECRET as string,
    scope: 'profile roles'
  });
};
export default globalSetupConfig;
