import { IdamUtils } from '@hmcts/playwright-common';
import { TestConfig } from './test.config';

async function globalSetupConfig(): Promise<void> {
  await getAccessToken();
}

export const getAccessToken = async (): Promise<void> => {
  process.env.IDAM_WEB_URL = TestConfig.iDam.idamUrl;
  process.env.IDAM_TESTING_SUPPORT_URL = TestConfig.iDam.idamTestingSupportUrl;
  process.env.CREATE_USER_BEARER_TOKEN = await new IdamUtils().generateIdamToken({
    grantType: 'client_credentials',
    clientId: 'pcs-api',
    clientSecret: process.env.PCS_API_IDAM_SECRET as string,
    scope: 'profile roles'
  });
};
export default globalSetupConfig;
