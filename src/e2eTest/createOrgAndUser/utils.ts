import path from 'node:path';
import {execFileSync} from 'node:child_process';

import type {Page} from '@playwright/test';
import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import {addressCheckYourAnswers, addressDetails, borderPostcode, home} from '@data/page-data';
import {LONG_TIMEOUT} from "../playwright.config";

const scriptsDir = path.join(__dirname, 'scripts');

/** AAT-style PBA numbers for manage-org registration (pool for random pick per run). */
const AAT_PBA_NUMBERS = [
  'PBA0095264', 'PBA0089829', 'PBA0095040', 'PBA0089837', 'PBA0084464', 'PBA0084541', 'PBA0093139', 'PBA0084543',
  'PBA0089864', 'PBA0089874', 'PBA0095030', 'PBA0089894', 'PBA0089904', 'PBA0077554', 'PBA0089908', 'PBA0077555',
  'PBA0092956', 'PBA0089939', 'PBA0094998', 'PBA0092928', 'PBA0094997', 'PBA0092921', 'PBA0092904', 'PBA0092862',
  'PBA0092853', 'PBA0089979', 'PBA0095799', 'PBA0092815', 'PBA0089989', 'PBA0094983', 'PBA0094981', 'PBA0094974',
  'PBA0094962', 'PBA0092739', 'PBA0085302', 'PBA0090030', 'PBA0092719', 'PBA0077702', 'PBA0012281', 'PBA0085595',
  'PBA0090066', 'PBA0092643', 'PBA0094939', 'PBA0085866', 'PBA0092630', 'PBA0085870', 'PBA0094936', 'PBA0077736',
  'PBA0092568', 'PBA0085941', 'PBA0086068', 'PBA0094928', 'PBA0094924', 'PBA0094917', 'PBA0086304', 'PBA0086306',
  'PBA0086311', 'PBA0094905', 'PBA0094896', 'PBA0094893', 'PBA0094889', 'PBA0092487', 'PBA0092451', 'PBA0090930',
  'PBA0077814', 'PBA0075809', 'PBA0092379', 'PBA0090213', 'PBA0086596', 'PBA0092337', 'PBA0090233', 'PBA0090234',
  'PBA0086605', 'PBA0086701', 'PBA0086781', 'PBA0086856', 'PBA0094878', 'PBA0096107', 'PBA0094877', 'PBA0094874',
  'PBA0086869', 'PBA0094873', 'PBA0094871', 'PBA0094869', 'PBA0090308', 'PBA0092176', 'PBA0090313', 'PBA0094866',
  'PBA0090319', 'PBA0090322', 'PBA0092142', 'PBA0090325', 'PBA0090326', 'PBA0094862', 'PBA0086905', 'PBA0086913',
  'PBA0078095', 'PBA0090348', 'PBA0092114', 'PBA0094854', 'PBA0090354', 'PBA0090361', 'PBA0090369', 'PBA0090372',
  'PBA0090375', 'PBA0094837', 'PBA0092046', 'PBA0090398', 'PBA0094833', 'PBA0078106',
] as const;

/** Returns one random PBA from the AAT pool above. */
export function pickRandomPba(): string {
  const i = Math.floor(Math.random() * AAT_PBA_NUMBERS.length);
  return AAT_PBA_NUMBERS[i];
}

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
