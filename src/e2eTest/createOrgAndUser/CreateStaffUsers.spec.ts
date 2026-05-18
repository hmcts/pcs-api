import {expect, test, type Page} from '@playwright/test';

import {completeIdamPasswordActivation, waitForLatestIdamNotificationLink,} from './idam-testing-support-notification';
import {
  type IdamOAuthTokenResponse, makeAClaimSmoke,
  pickRandomPba,
  registerOrganisationAsSolicitor,
  runCurlScript,
  runCurlScriptJson,
} from './utils';

/** Forename shown in IdAM / forms: `Solicitor First`, `Solicitor Second`, … aligned with email `…caseworker${sol}@…`. */
function caseworkerForenameForSol(sol: number): string {
  const users = [
    {email: 'pcs-ctsc-admin-01@hmcts.in', role: [], jobTitle: ['CTSC Administrator']},
    {email: 'pcs-ctsc-admin-ts-ca-01@hmcts.in', role:['Case allocator','Task Supervisor'], jobTitle: ['CTSC Administrator']},
    {email: 'pcs-ctsc-admin-team-leader-01@hmcts.in', role:[], jobTitle: ['CTSC Team Leader']}
  ];
  const word = ordinals[sol - 1];
  return word != null ? `Solicitor ${word}` : `Solicitor ${String(sol)}`;
}

/** Drop HMCTS/IdAM SSO cookies before any sign-in or protected app URL — avoids stale session / expiry pages. */
async function resetCookiesBeforeIdamAccess(page: Page): Promise<void> {
  await page.context().clearCookies();
}

test('test', async ({page, request}) => {
  const hmctsEnvRaw = process.env.HMCTS_ENV?.trim();
  if (!hmctsEnvRaw) {
    throw new Error('HMCTS_ENV is required (e.g. export HMCTS_ENV=aat)');
  }
  const hmctsEnv = hmctsEnvRaw;

  const userCreationNeeded = 'true';
  const newTempUser = 'false';
  const addTheUserViaStaffEUI = 'true';
  const triggerAMRefresh = 'true';

  const numberOfUsers = 1;
  const caseworkerEmailStartsWith = 1;

  const orgName = `Possession Claims Solicitor Org`;
  const postCode = 'SW1H 9AJ';
  const PBA1 = 'PBA0089864';
  const orgFirstName = 'Solicitor';
  const orgLastName = `Admin Org`;
  const orgEmailAddress = `pcs-caseworker-org-adm-auto@mailinator.com`;
  const orgManageOrgLoginPassword = 'Pa$$w0rd';

  const caseworkerLastName = `PCS`;
  const caseworkerPassword = 'Pa$$w0rd';

  const pcsOrganisationAdmin = 'pcs-organisation-admin@test.com';
  const pcsIdamDashboardAdminEmail = 'pcs-idam-adminstrator@test.com';
  const password = 'Pa$$w0rd';

  /** PCS caseworker IdAM roles (aligned with idamTempUserUsingIdamAPIPCSSolicitor.sh). */
  const pcsSolitorRoles = [
    'caseworker'
  ];

  const {access_token: accessToken} = runCurlScriptJson('idamToken.sh', {
    HMCTS_ENV: hmctsEnv,
  }) as IdamOAuthTokenResponse;
  expect(accessToken, 'idamToken.sh JSON must include access_token').toBeTruthy();

    const caseworkerEmailAddress = users[0];


  const namePart = caseworkerEmailAddress.split("@")[0];

  const firstName = namePart.split("-")[0];

  const surname = namePart
    .split("-")
    .slice(1)
    .join(" ")
    .replace(/-/g, " ");
    // @ts-ignore
    if (userCreationNeeded == 'true') {
      // @ts-ignore
      if (newTempUser == 'true') {
        runCurlScript('tempUserUsingTestingSupportAPIPCSCaseWorker.sh', {
          HMCTS_ENV: hmctsEnv,
          IDAM_ACCESS_TOKEN: accessToken,
          SOLICITOR_EMAIL_ADDRESS: caseworkerEmailAddress,
          SOLICITOR_FORENAME: firstName,
          SOLICITOR_SURNAME: surname,
        });
      } else {
        //Register user
        await resetCookiesBeforeIdamAccess(page);
        await page.goto(`https://manage-case.${hmctsEnv}.platform.hmcts.net`);
        await page.getByRole('link', {name: 'create an account'}).click();
        await page.getByRole('textbox', {name: 'First name'}).fill(firstName);
        await page.getByRole('textbox', {name: 'Last name'}).fill(surname);
        await page.getByRole('textbox', {name: 'Email address'}).fill(caseworkerEmailAddress);
        await page.getByRole('button', {name: 'Continue'}).click();
        // activate the user
        const activationUrl = await waitForLatestIdamNotificationLink(request, {
          hmctsEnv,
          email: caseworkerEmailAddress,
          bearerToken: accessToken,
        });
        await resetCookiesBeforeIdamAccess(page);
        await completeIdamPasswordActivation(page, activationUrl, caseworkerPassword);
        uid = await getUidFor(caseworkerEmailAddress);
       add this to rray and use in
      }
    }

    if(triggerAMRefresh == 'true')
    {

    }
    await resetCookiesBeforeIdamAccess(page);
    await makeAClaimSmoke(page, hmctsEnv, {
      email: caseworkerEmailAddress,
      password: caseworkerPassword,
    });

});
