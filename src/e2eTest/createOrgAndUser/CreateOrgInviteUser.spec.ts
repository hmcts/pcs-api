import {expect, test} from '@playwright/test';

import {completeIdamPasswordActivation, waitForLatestIdamNotificationLink,} from './idam-testing-support-notification';
import {
  createHousingPossessionClaimEnglandWalesAddressSmoke,
  type IdamOAuthTokenResponse,
  pickRandomPba,
  registerOrganisationAsSolicitor,
  runCurlScript,
  runCurlScriptJson,
} from './utils';

test('test', async ({page, request}) => {
  const hmctsEnv = (process.env.HMCTS_ENV ?? 'aat').trim();

  const createOrg = 'false';
  const orgApprovalNeeded = 'false';
  const userCreationNeeded = 'false';
  const newTempUser = 'false';
  const inviteTheUserToOrg = 'false';
  const updateIDAMRoles = 'false';


  const orgName = 'Possession Claim Service Org1';
  const postCode = 'SW1H 9AJ';
  const orgRegistrationRef = 'PCS001';
  const PBA1 = pickRandomPba();
  const orgFirstName = 'Possession Claim';
  const orgLastName = 'Service Org1';
  const orgEmailAddress = 'pcs-solicitor-org1-admin@mailinator.com';
  const orgManageOrgLoginPassword = 'Pa$$w0rd';


  const solicitorEmailAddress = 'pcs-solicitor1-org1@test.com';
  const solicitorFirstName = 'Possession Claim';
  const solicitorLastName = 'Possession Claim';
  const solicitorPassword = 'Pa$$w0rd';

  /** Must match idamTempUserIDAMDashboardAdmin.sh testing-support user. */
  const pcsIdamDashboardAdminEmail = 'pcs-idam-adminstrator@test.com';
  const pcsIdamDashboardAdminPassword = 'Pa$$w0rd';

  /** PCS solicitor IdAM roles (aligned with idamTempUserPCSSolicitor.sh). */
  const pcsSolitorRoles = [
    'caseworker',
    'caseworker-pcs',
    'caseworker-pcs-solicitor',
    'pui-case-manager',
    'payments',
    'payments-refund',
  ];

  const {access_token: accessToken} = runCurlScriptJson('idamToken.sh', {
    HMCTS_ENV: hmctsEnv,
  }) as IdamOAuthTokenResponse;
  expect(accessToken, 'idamToken.sh JSON must include access_token').toBeTruthy();

  // @ts-ignore
  if (createOrg == 'true') {

    await registerOrganisationAsSolicitor(page, {
      hmctsEnv,
      orgName,
      postCode,
      orgRegistrationRef,
      pbaNumber: PBA1,
      orgFirstName,
      orgLastName,
      orgEmailAddress,
    });
  }

  // @ts-ignore
  if (orgApprovalNeeded == 'true') {
    //Create admin to approve
    runCurlScript('idamTempUserOrgAdmin.sh', {HMCTS_ENV: hmctsEnv});
    //Approve
    await page.goto(`https://administer-orgs.${hmctsEnv}.platform.hmcts.net/organisation/pending`);
    await page.getByRole('textbox', {name: 'Email address'}).click();
    await page.getByRole('textbox', {name: 'Email address'}).fill('pcs-organisation-admin@test.com');
    await page.getByRole('textbox', {name: 'Email address'}).press('Tab');
    await page.getByRole('textbox', {name: 'Password'}).fill('Pa$$w0rd');
    await page.getByRole('button', {name: 'Sign in'}).click();
    await page.getByRole('textbox', {name: 'Search'}).click();
    await page.getByRole('textbox', {name: 'Search'}).fill(orgName);
    await page.getByRole('button', {name: 'Search'}).click();
    // Pending list: one "View" per row — do not use .nth(1) (that is the *second* link). Target /organisation-details/… like <a class="govuk-link" href="/organisation-details/…">View</a>
    const viewPendingOrganisation = page
      .locator('a[href^="/organisation-details/"]')
      .filter({hasText: 'View'});
    await expect(viewPendingOrganisation.first()).toBeVisible({timeout: 20_000});
    await viewPendingOrganisation.first().click();
    await page.getByRole('radio', {name: 'Approve it'}).check();
    await page.getByRole('button', {name: 'Submit'}).click();
    await page.getByRole('button', {name: 'Confirm'}).click();
  }

  // @ts-ignore
  if (createOrg == 'true') {
    const orgActivationUrl = await waitForLatestIdamNotificationLink(request, {
      hmctsEnv,
      email: orgEmailAddress,
      bearerToken: accessToken,
    });
    await completeIdamPasswordActivation(page, orgActivationUrl, orgManageOrgLoginPassword);
  }
  // @ts-ignore
  if (userCreationNeeded == 'true') {
    // @ts-ignore
    if (newTempUser == 'true') {
      runCurlScript('idamTempUserPCSSolicitor.sh', {
        HMCTS_ENV: hmctsEnv,
        SOLICITOR_EMAIL_ADDRESS: solicitorEmailAddress,
        SOLICITOR_FORENAME: solicitorFirstName,
        SOLICITOR_SURNAME: solicitorLastName,
      });
    } else {

      //Register user
      await page.goto(`https://manage-case.${hmctsEnv}.platform.hmcts.net`);
      await page.getByRole('link', {name: 'create an account'}).click();
      await page.getByRole('textbox', {name: 'First name'}).fill(solicitorFirstName);
      await page.getByRole('textbox', {name: 'Last name'}).fill(solicitorLastName);
      await page.getByRole('textbox', {name: 'Email address'}).fill(solicitorEmailAddress);
      await page.getByRole('button', {name: 'Continue'}).click();
      // activate the user
      const activationUrl = await waitForLatestIdamNotificationLink(request, {
        hmctsEnv,
        email: solicitorEmailAddress,
        bearerToken: accessToken,
      });
      await completeIdamPasswordActivation(page, activationUrl, solicitorPassword);
    }
  }

  // @ts-ignore
  if(inviteTheUserToOrg == 'true') {
    //Invite user to Org
    await page.goto(`https://manage-org.${hmctsEnv}.platform.hmcts.net/users/invite-user`);
    await page.getByRole('textbox', {name: 'Email address'}).fill(orgEmailAddress);
    await page.getByRole('textbox', {name: 'Password'}).fill(orgManageOrgLoginPassword);
    await page.getByRole('button', {name: 'Sign in'}).click();
    await page.getByRole('link', {name: 'Users'}).click();
    await page.getByRole('button', {name: 'Invite user'}).click();
    await page.getByRole('textbox', {name: 'First name'}).fill(solicitorFirstName);
    await page.getByRole('textbox', {name: 'Last name'}).fill(solicitorLastName);
    await page.getByRole('textbox', {name: 'Email address'}).fill(solicitorEmailAddress);
    await page.getByRole('checkbox', {name: 'Permit users to   Manage Cases'}).check();
    await page.getByRole('button', {name: 'Send invitation'}).click();
  }

  // @ts-ignore
  if(updateIDAMRoles == 'true') {


  // Create IdAM user-dashboard admin (testing-support)
  runCurlScript('idamTempUserIDAMDashboardAdmin.sh', {HMCTS_ENV: hmctsEnv});

  //Login to IDAM dashboard and update roles
  await page.goto(`https://idam-user-dashboard.${hmctsEnv}.platform.hmcts.net/`);
  await page.getByRole('textbox', {name: 'Enter your email address'}).click();
  await page.getByRole('textbox', {name: 'Enter your email address'}).fill('pcs-idam-admin@test.com');
  await page.getByRole('button', {name: 'Continue'}).click();
  await page.getByRole('textbox', {name: 'Enter your password'}).fill('Pa$$w0rd');
  await page.getByRole('button', {name: 'Continue'}).click();

  await page.getByRole('radio', {name: /Manage an existing user/i}).check();
  await page.getByRole('button', {name: 'Continue'}).click();

  await page.getByRole('textbox', {name: 'Search for an existing user'}).fill(solicitorEmailAddress);
  await page.getByRole('button', {name: 'Search'}).click();
  await page.getByRole('button', {name: 'Edit user'}).click();

  const roleCheckboxes = page.getByRole('checkbox');
  for (let i = 0; i < await roleCheckboxes.count(); i += 1) {
    const box = roleCheckboxes.nth(i);
    if ((await box.getAttribute('id')) === 'multiFactorAuthentication') {
      continue;
    }
    if (await box.isChecked()) {
      await box.uncheck();
    }
  }
  for (const role of pcsSolitorRoles) {
    const escaped = role.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    await page.getByRole('checkbox', {name: new RegExp(`^\\s*${escaped}\\s*$`, 'i')}).check();
  }
  await page.getByRole('button', {name: 'Save'}).click();
}
  // Check whether created user can start a Housing Possession claim (manage-case smoke).
  await createHousingPossessionClaimEnglandWalesAddressSmoke(page, hmctsEnv, {
    email: solicitorEmailAddress,
    password: solicitorPassword,
  });
});
