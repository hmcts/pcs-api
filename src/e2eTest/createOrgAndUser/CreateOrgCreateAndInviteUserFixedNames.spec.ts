import {expect, test, type Page} from '@playwright/test';

import {completeIdamPasswordActivation, waitForLatestIdamNotificationLink,} from './idam-testing-support-notification';
import {
  type IdamOAuthTokenResponse, makeAClaimSmoke,
  pickRandomPba,
  registerOrganisationAsSolicitor,
  runCurlScript,
  runCurlScriptJson,
} from './utils';

/** Forename shown in IdAM / forms: `Solicitor First`, `Solicitor Second`, … aligned with email `…solicitor${sol}@…`. */
function solicitorForenameForSol(sol: number): string {
  const ordinals = [
    'First',
    'Second',
    'Third',
    'Fourth',
    'Fifth',
    'Sixth',
    'Seventh',
    'Eighth',
    'Ninth',
    'Tenth',
    'Eleventh',
    'Twelfth',
  ] as const;
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

  const createOrg = 'true';
  const orgApprovalNeeded = 'true';
  const userCreationNeeded = 'true';
  const newTempUser = 'true';
  const inviteTheUserToOrg = 'true';
  const updateIDAMRoles = 'true';

  /** Namespace for org emails/names (e.g. `org1` → …Org1, …org1…). Change only here to retarget all derived ids. */
  const org = 'org1';
  /** How many solicitors to provision (emails `pcs-${org}-solicitor1@test.com`, …solicitor2…, etc.). */
  const numberOfSolicitorUsers = 1;
  const solicitorEmailStartsWith = 1;
  const orgDisplay = org.charAt(0).toUpperCase() + org.slice(1);

  const orgName = `Possession Claims Solicitor Org`;
  const postCode = 'SW1H 9AJ';
  const orgRegistrationRef = `PCS ${orgDisplay}`;
  const PBA1 = 'PBA0093901';
  const orgFirstName = 'Solicitor';
  const orgLastName = `Admin Org`;
  const orgEmailAddress = `pcs-solicitor-org-adm@mailinator.com`;
  const orgManageOrgLoginPassword = 'Pa$$w0rd';

  const solicitorLastName = `PCS`;
  const solicitorPassword = 'Pa$$w0rd';

  const pcsOrganisationAdmin = 'pcs-organisation-admin@test.com';
  const pcsIdamDashboardAdminEmail = 'pcs-idam-adminstrator@test.com';
  const password = 'Pa$$w0rd';

  /** PCS solicitor IdAM roles (aligned with idamTempUserUsingIdamAPIPCSSolicitor.sh). */
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
    await resetCookiesBeforeIdamAccess(page);
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
    //Approve — administer-orgs shares IdAM SSO with manage-org / manage-case; stale cookies often show session expiry instead of login.
    await resetCookiesBeforeIdamAccess(page);
    await page.goto(`https://administer-orgs.${hmctsEnv}.platform.hmcts.net/organisation/pending`);
    // IdAM often redirects after goto; evaluate before navigation settles → "Execution context was destroyed".
    const orgAdminEmailField = page.getByRole('textbox', {name: 'Email address'});
    await orgAdminEmailField.waitFor({state: 'visible', timeout: 30_000});
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
    await orgAdminEmailField.click();
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
    await resetCookiesBeforeIdamAccess(page);
    await completeIdamPasswordActivation(page, orgActivationUrl, orgManageOrgLoginPassword);
  }

  if (!Number.isInteger(numberOfSolicitorUsers) || numberOfSolicitorUsers < 1) {
    throw new Error('numberOfSolicitorUsers must be a positive integer (1, 2, 3, …)');
  }

  let sol = solicitorEmailStartsWith;

  for (let index = 1; index <= numberOfSolicitorUsers; index += 1) {
    const solicitorEmailAddress = `pcs-solicitor-automation@test.com`;
    const solicitorFirstName = solicitorForenameForSol(sol);
    // @ts-ignore
    if (userCreationNeeded == 'true') {
      // @ts-ignore
      if (newTempUser == 'true') {
        runCurlScript('tempUserUsingTestingSupportAPIPCSSolicitor.sh', {
          HMCTS_ENV: hmctsEnv,
          IDAM_ACCESS_TOKEN: accessToken,
          SOLICITOR_EMAIL_ADDRESS: solicitorEmailAddress,
          SOLICITOR_FORENAME: solicitorFirstName,
          SOLICITOR_SURNAME: solicitorLastName,
        });
      } else {
        //Register user
        await resetCookiesBeforeIdamAccess(page);
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
        await resetCookiesBeforeIdamAccess(page);
        await completeIdamPasswordActivation(page, activationUrl, solicitorPassword);
      }
    }

    // @ts-ignore
    if (inviteTheUserToOrg == 'true') {
      //Invite user to Org
      await resetCookiesBeforeIdamAccess(page);
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
    if (updateIDAMRoles == 'true') {
      // Create IdAM user-dashboard admin (testing-support) once — same IdAM user for all iterations.
      if (index === 1) {
        runCurlScript('idamTempUserIDAMDashboardAdmin.sh', {HMCTS_ENV: hmctsEnv});
      }

      // IdAM user-dashboard shares SSO with other HMCTS apps on this context. An existing
      // manage-org / manage-case (or a half-finished dashboard) session often yields an error
      // page instead of the email field — clear storage so this step always starts logged out.
      await resetCookiesBeforeIdamAccess(page);
      await page.goto(`https://idam-user-dashboard.${hmctsEnv}.platform.hmcts.net/`);
      const idamDashboardEmailField = page.getByRole('textbox', {name: 'Enter your email address'});
      await idamDashboardEmailField.waitFor({state: 'visible', timeout: 30_000});
      await page.evaluate(() => {
        localStorage.clear();
        sessionStorage.clear();
      });

      //Login to IDAM dashboard and update roles
      await idamDashboardEmailField.click();
      await idamDashboardEmailField.fill('pcs-idam-admin@test.com');
      await page.getByRole('button', {name: 'Continue'}).click();
      await page.getByRole('textbox', {name: 'Enter your password'}).fill('Pa$$w0rd');
      await page.getByRole('button', {name: 'Continue'}).click();

      await page.getByRole('radio', {name: /Manage an existing user/i}).check();
      await page.getByRole('button', {name: 'Continue'}).click();

      await page.getByRole('textbox', {name: 'Search for an existing user'}).fill(solicitorEmailAddress);
      await page.getByRole('button', {name: 'Search'}).click();
      await page.getByRole('button', {name: 'Edit user'}).click();
      await page.waitForLoadState('domcontentloaded');
      await expect(page.getByRole('button', {name: 'Save'})).toBeVisible({timeout: 30_000});

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
    await resetCookiesBeforeIdamAccess(page);
    await makeAClaimSmoke(page, hmctsEnv, {
      email: solicitorEmailAddress,
      password: solicitorPassword,
    });
    sol = sol + 1;
  }
});
