import path from 'node:path';
import {execFileSync} from 'node:child_process';

import type {Page} from '@playwright/test';
import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import {addressCheckYourAnswers, addressDetails, borderPostcode, home} from '@data/page-data';
import {LONG_TIMEOUT} from "../playwright.config";

const scriptsDir = path.join(__dirname, 'scripts');

/** Data for manage-org “register organisation” solicitor journey (to Confirm and submit). */
export type RegisterOrgSolicitorParams = {
  hmctsEnv: string;
  orgName: string;
  postCode: string;
  orgRegistrationRef: string;
  pbaNumber: string;
  orgFirstName: string;
  orgLastName: string;
  orgEmailAddress: string;
  /** Shown when “Service not listed” is selected; default `PCS`. */
  hmctsAreaCode?: string;
};

/** Complete https://manage-org.{env}/register-org-new/register as Solicitor through Confirm and submit. */
export async function registerOrganisationAsSolicitor(page: Page, p: RegisterOrgSolicitorParams): Promise<void> {

  initializeExecutor(page);

  const area = p.hmctsAreaCode ?? 'PCS';
  await page.goto(`https://manage-org.${p.hmctsEnv}.platform.hmcts.net/register-org-new/register`);
  await page.getByRole('checkbox', {name: 'I\'ve checked whether my'}).check();
  await page.getByRole('button', {name: 'Start'}).click();
  await page.getByRole('radio', {name: 'Solicitor'}).check();
  await page.getByRole('button', {name: 'Continue'}).click();
  await page.getByRole('textbox', {name: 'Enter the name of the'}).click();
  await page.getByRole('textbox', {name: 'Enter the name of the'}).fill(p.orgName);
  await page.getByRole('button', {name: 'Continue'}).click();
  await page.getByRole('textbox', {name: 'Enter a UK postcode'}).click();
  await page.getByRole('textbox', {name: 'Enter a UK postcode'}).fill(p.postCode);
  await page.getByRole('button', {name: 'Find address'}).click();
  await page.getByLabel('Select an address').selectOption('1: Object');
  await page.getByRole('button', {name: 'Continue'}).click();
  await page.getByRole('radio', {name: 'No'}).check();
  await page.getByRole('button', {name: 'Continue'}).click();
  await page.getByLabel('Select the type of regulatory').selectOption('Solicitor Regulation Authority (SRA)');
  await page.getByRole('textbox', {name: 'Enter your organisation\'s'}).click();
  await page.getByRole('textbox', {name: 'Enter your organisation\'s'}).fill(p.orgRegistrationRef);
  await page.getByRole('button', {name: 'Continue'}).click();
  await page.locator('[id="Service not listed"]').check();
  await page.getByRole('textbox', {name: 'Please enter the HMCTS'}).click();
  await page.getByRole('textbox', {name: 'Please enter the HMCTS'}).fill(area);
  await page.getByRole('button', {name: 'Continue'}).click();
  await page.getByRole('radio', {name: 'Yes'}).check();
  await page.getByRole('button', {name: 'Continue'}).click();
  await page.getByRole('textbox', {name: 'PBA number (Optional)'}).click();
  await page.getByRole('textbox', {name: 'PBA number (Optional)'}).fill(p.pbaNumber);
  await page.getByRole('button', {name: 'Continue'}).click();
  await page.getByRole('textbox', {name: 'First name'}).click();
  await page.getByRole('textbox', {name: 'First name'}).fill(p.orgFirstName);
  await page.getByRole('textbox', {name: 'First name'}).press('Tab');
  await page.getByRole('textbox', {name: 'Last name'}).fill(p.orgLastName);
  await page.getByRole('textbox', {name: 'Last name'}).press('Tab');
  await page.getByRole('textbox', {name: 'Enter your work email address'}).fill(p.orgEmailAddress);
  await page.getByRole('button', {name: 'Continue'}).click();
  await page.getByRole('radio', {name: 'No'}).check();
  await page.getByRole('button', {name: 'Continue'}).click();
  await page.getByRole('checkbox', {name: 'You have read, understood and'}).check();
  await page.getByRole('button', {name: 'Confirm and submit'}).click();
}

/** IdAM `/o/token` password-grant JSON (Postman cURL → idamToken.sh). */
export type IdamOAuthTokenResponse = {
  access_token: string;
  token_type?: string;
  expires_in?: number;
};

export function runCurlScript(scriptName: string, extraEnv?: NodeJS.ProcessEnv): void {
  execFileSync('bash', [path.join(scriptsDir, scriptName)], {
    stdio: 'inherit',
    env: {...process.env, ...extraEnv},
  });
}

export function runCurlScriptJson(scriptName: string, extraEnv?: NodeJS.ProcessEnv): unknown {
  const scriptPath = path.join(scriptsDir, scriptName);
  const env = {...process.env, ...extraEnv};
  let out: string;
  try {
    out = execFileSync('bash', [scriptPath], {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      env,
    });
  } catch (e: unknown) {
    const err = e as NodeJS.ErrnoException & {status?: number; stderr?: Buffer; stdout?: Buffer};
    const stderr = err.stderr?.toString?.().trim() ?? '';
    const stdout = err.stdout?.toString?.().trim() ?? '';
    const idamHint =
      scriptName === 'idamToken.sh'
        ? '\nFor idamToken.sh export: IDAM_TOKEN_USERNAME, IDAM_TOKEN_PASSWORD, IDAM_TOKEN_CLIENT_SECRET (and optionally HMCTS_ENV).'
        : '';
    throw new Error(
      `${scriptName} failed (exit ${String(err.status ?? err.code ?? '?')}).\n` +
        `stderr: ${stderr || '(empty)'}\n` +
        `stdout: ${stdout.slice(0, 800) || '(empty)'}${idamHint}`
    );
  }
  try {
    return JSON.parse(out.trim());
  } catch {
    throw new Error(
      `${scriptName} did not return JSON. First 500 chars:\n${out.trim().slice(0, 500)}`
    );
  }
}

/** IdAM / XUI-style manage-case login (#username / #password). */
export type ManageCaseLoginCredentials = {
  email: string;
  password: string;
};

/**
 * Smoke path: manage-case login → create Housing Possession → England/Wales border postcode → submit address CYA → capture case id.
 * Wires `@utils/controller` the same way as other specs (`initializeExecutor` before `performAction`).
 */
export async function createHousingPossessionClaimEnglandWalesAddressSmoke(
  page: Page,
  hmctsEnv: string,
  login: ManageCaseLoginCredentials,
): Promise<void> {
  initializeExecutor(page);
  await performAction('navigateToUrl', `https://manage-case.${hmctsEnv}.platform.hmcts.net`);

  await page.waitForSelector('#username', {timeout: LONG_TIMEOUT});
  await page.locator('#username').fill(login.email);
  await page.locator('#password').fill(login.password);
  await page.locator('#login-submit-btn').click();

  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: LONG_TIMEOUT });



  await performAction('clickTab', home.createCaseTab);
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');

  await performAction('selectAddress', {
    postcode: borderPostcode.englandWalesPostcode,
    addressIndex: addressDetails.addressIndex,
  });
  await performValidation('mainHeader', borderPostcode.mainHeader);
  await performValidation('text', {
    text: borderPostcode.englandWalesParagraphContent,
    elementType: 'paragraph',
  });
  await performValidation('text', {
    text: borderPostcode.isProtpertyLocatedInEnglandOrWalesQuestion,
    elementType: 'inlineText',
  });
  await performAction('selectBorderPostcode', borderPostcode.countryOptions.england);
  await performValidation('mainHeader', addressCheckYourAnswers.mainHeader);
  await performAction('submitAddressCheckYourAnswers');
  await performValidation('bannerAlert', 'Case #.* has been created.');
  await performAction('extractCaseIdFromAlert');
}
