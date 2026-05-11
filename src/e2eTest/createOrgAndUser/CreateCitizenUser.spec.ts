import {expect, test} from '@playwright/test';

import {type IdamOAuthTokenResponse, runCurlScript, runCurlScriptJson,} from './utils';

function citizenForenameForIndex(index: number): string {
  return `Citizen${String(index)}`;
}

test('create citizen user and login to pcs', async ({page}) => {
  const hmctsEnvRaw = process.env.HMCTS_ENV?.trim();
  if (!hmctsEnvRaw) {
    throw new Error('HMCTS_ENV is required (e.g. export HMCTS_ENV=aat)');
  }
  const hmctsEnv = hmctsEnvRaw;

  const numberOfCitizenUsers = 1;
  const citizenEmailStartsWith = 4;
  const citizenPassword = 'Pa$$w0rd';

  const {access_token: accessToken} = runCurlScriptJson('idamToken.sh', {
    HMCTS_ENV: hmctsEnv,
  }) as IdamOAuthTokenResponse;
  expect(accessToken, 'idamToken.sh JSON must include access_token').toBeTruthy();

  if (!Number.isInteger(numberOfCitizenUsers) || numberOfCitizenUsers < 1) {
    throw new Error('numberOfCitizenUsers must be a positive integer (1, 2, 3, …)');
  }

  let citizenIndex = citizenEmailStartsWith;
  for (let i = 1; i <= numberOfCitizenUsers; i += 1) {
    const citizenEmailAddress = `pcs-citizen${citizenIndex}@test.com`;
    const citizenForename = citizenForenameForIndex(citizenIndex);

    runCurlScript('tempUserUsingTestingSupportAPICitizen.sh', {
      HMCTS_ENV: hmctsEnv,
      IDAM_ACCESS_TOKEN: accessToken,
      CITIZEN_EMAIL_ADDRESS: citizenEmailAddress,
      CITIZEN_FORENAME: citizenForename,
      CITIZEN_SURNAME: 'User',
      CITIZEN_ACC_PASSWORD: citizenPassword,
    });


    citizenIndex += 1;
  }
});
