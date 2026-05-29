import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { expect, test } from '@utils/test-fixtures';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { caseSummary, user } from '@data/page-data';
import { staff } from '@data/user-data/staff.user.data';
import { judicialEmails } from '@data/user-data/judicial.user.data';
import {
  specialMeasureForFlag,
  whereShouldThisFlagBeAdded,
  selectFlagType,
  addCommentsForFlag,
  reviewFlagDetails,
  viewCaseFlag,
  manageCaseFlags,
  updateFlagComments
} from '@data/page-data-figma';
import { dismissCookieBanner } from '@config/cookie-banner';
import { BrowserContext, Page } from '@playwright/test';
import {caseInfo} from "@utils/actions/custom-actions";

const caseSummaryUrl = () =>
  `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`;

const staffUserEmails = Object.values(staff);
const judicialUserEmails = Object.values(judicialEmails);
const ACCESS_CONTROL_TEST_TIMEOUT = 30 * 60 * 1000;

async function clearBrowserSession(page: Page, context: BrowserContext): Promise<void> {
  await context.clearCookies();
  await page.evaluate(() => {
    try {
      localStorage.clear();
      sessionStorage.clear();
    } catch {
      // Ignore if storage is not accessible
    }
  });
}

async function navigateToCaseSummary(page: Page): Promise<void> {
  await performAction('navigateToUrl', caseSummaryUrl());
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
  }).toPass({ timeout: VERY_LONG_TIMEOUT });
}

async function loginAsUser(page: Page, email: string, password: string): Promise<void> {
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  //await dismissCookieBanner(page, 'additional');
  await performAction('login', { email, password });
  //await dismissCookieBanner(page, 'analytics');
  await navigateToCaseSummary(page);
}

async function getNextStepEventOptions(page: Page): Promise<string[]> {
  const select = page.locator(
    `:has-text("${caseSummary.nextStepEventList}") + select, :has-text("${caseSummary.nextStepEventList}") ~ select`
  ).first();
  await select.waitFor({ state: 'visible' });
  const options = await select.locator('option').allTextContents();
  return options.map((option) => option.trim()).filter(Boolean);
}

async function seedCaseLevelFlag(): Promise<void> {
  await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
  await performAction('clickButton', caseSummary.go);
  await performAction('whereShouldThisFlagBeAdded', {
    flagLevelQuestion: whereShouldThisFlagBeAdded.whereShouldThisFlagBeAddedQuestion,
    flagLevelOption: whereShouldThisFlagBeAdded.caseLevelRadioOption,
    continueButton: whereShouldThisFlagBeAdded.continueButton
  });
  await performAction('selectFlagType', {
    selectFlagQuestion: selectFlagType.selectFlagTypeLabel,
    selectFlagOption: selectFlagType.urgentCaseRadioOption,
    continueButton: selectFlagType.continueButton
  });
  await performAction('addCommentsForFlag', {
    label: addCommentsForFlag.addCommentsOptionalLabel,
    input: addCommentsForFlag.addCommentTextInput,
    continueButton: addCommentsForFlag.continueButton
  });
  await performAction('reviewFlagDetails', {
    saveButton: reviewFlagDetails.saveAndContinueButton
  });
  await performValidation('bannerAlert', 'Case #.* has been updated with event: Create case flags');
}

async function seedPartyLevelFlag(): Promise<void> {
  await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
  await performAction('clickButton', caseSummary.go);
  await performAction('whereShouldThisFlagBeAdded', {
    flagLevelQuestion: whereShouldThisFlagBeAdded.whereShouldThisFlagBeAddedQuestion,
    flagLevelOption: whereShouldThisFlagBeAdded.respondentRadioOption,
    continueButton: whereShouldThisFlagBeAdded.continueButton
  });
  await performAction('selectFlagType', {
    selectFlagQuestion: selectFlagType.selectFlagTypeLabel,
    selectFlagOption: selectFlagType.specialMeasureRadioOption,
    continueButton: selectFlagType.continueButton
  });
  await performAction('selectSpecialMeasureForFlag', {
    specialMeasurelabel: specialMeasureForFlag.specialMeasureLabel,
    specialMeasureOption: specialMeasureForFlag.evidenceByLiveLinkRadioOption,
    continueButton: specialMeasureForFlag.continueButton
  });
  await performAction('addCommentsForFlag', {
    label: addCommentsForFlag.addCommentsLabel,
    input: addCommentsForFlag.addCommentTextInput,
    continueButton: addCommentsForFlag.continueButton
  });
  await performAction('reviewFlagDetails', {
    saveButton: reviewFlagDetails.saveAndContinueButton
  });
  await performValidation('bannerAlert', 'Case #.* has been updated with event: Create case flags');
}

function viewCaseFlagsLink(page: Page) {
  return page.locator(
    `button:has-text("${viewCaseFlag.viewFlagLink}"), a >> text=${viewCaseFlag.viewFlagLink}`
  ).first();
}

async function assertCanViewCaseAndPartyLevelFlags(page: Page): Promise<void> {
  await expect(viewCaseFlagsLink(page)).toBeVisible();
  await performAction('viewCaseFlags', { viewFlagLink: viewCaseFlag.viewFlagLink });
  await performValidation('mainHeader', manageCaseFlags.mainHeader);
  await expect(page.getByText(/Case level|Urgent case/i)).toBeVisible();
  await expect(page.getByText(/Peter Parker/i)).toBeVisible();
  await navigateToCaseSummary(page);
}

async function assertCanCreateCaseAndPartyLevelFlags(page: Page): Promise<void> {
  const options = await getNextStepEventOptions(page);
  expect(options).toContain(caseSummary.createFlagsEvent);

  await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
  await performAction('clickButton', caseSummary.go);
  await performValidation('mainHeader', whereShouldThisFlagBeAdded.mainHeader);
  await expect(
    page.locator(`label >> text=${whereShouldThisFlagBeAdded.caseLevelRadioOption}`)
  ).toBeVisible();
  await expect(
    page.locator(`label >> text=${whereShouldThisFlagBeAdded.respondentRadioOption}`)
  ).toBeVisible();
  await performAction('clickButton', whereShouldThisFlagBeAdded.cancelButton);
}

async function assertCanManageCaseAndPartyLevelFlags(page: Page): Promise<void> {
  const options = await getNextStepEventOptions(page);
  expect(options).toContain(manageCaseFlags.manageCaseFlagsEvent);

  await performAction('select', manageCaseFlags.nextStepEventList, manageCaseFlags.manageCaseFlagsEvent);
  await performAction('clickButton', manageCaseFlags.goButton);
  await performValidation('mainHeader', manageCaseFlags.mainHeader);
  await expect(page.locator(`label >> text=${manageCaseFlags.caseLevelUrgentCaseRadioOption}`)).toBeVisible();
  await expect(page.locator(`label >> text=${manageCaseFlags.respondentRadioOption}`)).toBeVisible();
  await performAction('clickButton', manageCaseFlags.cancelButton);
}

async function assertCannotCreateOrManageFlags(page: Page): Promise<void> {
  const options = await getNextStepEventOptions(page);
  expect(options).not.toContain(caseSummary.createFlagsEvent);
  expect(options).not.toContain(manageCaseFlags.manageCaseFlagsEvent);
}

async function assertNoFlagAccess(page: Page): Promise<void> {
  await expect(viewCaseFlagsLink(page)).not.toBeVisible();
  await assertCannotCreateOrManageFlags(page);
}

test.use({ storageState: undefined });

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await page.evaluate(() => {
    try {
      localStorage.clear();
      sessionStorage.clear();
    } catch (e) {
      // Ignore if storage is not accessible
    }
  });

  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.ctscAdministrator);
  await dismissCookieBanner(page, 'analytics');
  await performAction('searchCaseFromFindCase', caseInfo.fid);
});

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
});

test.describe('[Common Component Case Flags] @CC @caseFlags @nightly', async () => {

  test('Case Flags - Verify the create and manage case flag menu @smoke', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', whereShouldThisFlagBeAdded.mainHeader);
    await performAction('clickButton', whereShouldThisFlagBeAdded.cancelButton);
    await performAction('select', manageCaseFlags.nextStepEventList, manageCaseFlags.manageCaseFlagsEvent);
    await performAction('clickButton', manageCaseFlags.goButton);
    await performAction('clickButton', manageCaseFlags.cancelButton);
  });
  test('Case Flags - Create case level Flag', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', whereShouldThisFlagBeAdded.mainHeader);
    await performAction('whereShouldThisFlagBeAdded', {
      flagLevelQuestion: whereShouldThisFlagBeAdded.whereShouldThisFlagBeAddedQuestion,
      flagLevelOption: whereShouldThisFlagBeAdded.caseLevelRadioOption,
      continueButton: whereShouldThisFlagBeAdded.continueButton
    });
    await performValidation('mainHeader', selectFlagType.mainHeader);
    await performAction('selectFlagType', {
      selectFlagQuestion: selectFlagType.selectFlagTypeLabel,
      selectFlagOption: selectFlagType.complexCaseRadioOption,
      continueButton: selectFlagType.continueButton
    });
    await performValidation('mainHeader', addCommentsForFlag.mainHeader);
    await performAction('addCommentsForFlag', {
      label: addCommentsForFlag.addCommentsOptionalLabel,
      input: addCommentsForFlag.addCommentTextInput,
      continueButton: addCommentsForFlag.continueButton
    });
    await performValidation('mainHeader', reviewFlagDetails.mainHeader);
    await performAction('clickChangeLinkForRow', {
      rowLabel: reviewFlagDetails.rowLabel,
      changeLinkText: reviewFlagDetails.changeLink
    });
    await performAction('selectFlagType', {
      selectFlagQuestion: selectFlagType.selectFlagTypeLabel,
      selectFlagOption: selectFlagType.urgentCaseRadioOption,
      continueButton: selectFlagType.continueButton
    });
    await performAction('clickButton', addCommentsForFlag.continueButton);
    await performAction('reviewFlagDetails', {
      saveButton: reviewFlagDetails.saveAndContinueButton
    });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Create case flags');
    await performAction('viewCaseFlags', {
      viewFlagLink: viewCaseFlag.viewFlagLink
    });
    await performAction('select', manageCaseFlags.nextStepEventList, manageCaseFlags.manageCaseFlagsEvent);
    await performAction('clickButton', manageCaseFlags.goButton);
    await performAction('manageCaseFlags', {
      flagOption: manageCaseFlags.caseLevelUrgentCaseRadioOption,
      continueButton: manageCaseFlags.continueButton
    });
    await performAction('makeFlagInactive', {
      inactiveButton: updateFlagComments.makeInactiveButton,
      continueButton: updateFlagComments.continueButton
    });
    await performAction('reviewFlagDetails', {
      saveButton: reviewFlagDetails.saveAndContinueButton
    });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage case flags');
  });

  test('Case Flags - Create Party Level Case Flag', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', whereShouldThisFlagBeAdded.mainHeader);
    await performAction('whereShouldThisFlagBeAdded', {
      flagLevelQuestion: whereShouldThisFlagBeAdded.whereShouldThisFlagBeAddedQuestion,
      flagLevelOption: whereShouldThisFlagBeAdded.respondentRadioOption,
      continueButton: whereShouldThisFlagBeAdded.continueButton
    });
    await performValidation('mainHeader', selectFlagType.mainHeader);
    await performAction('selectFlagType', {
      selectFlaglabel: selectFlagType.selectFlagTypeLabel,
      selectFlagOption: selectFlagType.specialMeasureRadioOption,
      continueButton: selectFlagType.continueButton
    });
    await performValidation('mainHeader', specialMeasureForFlag.mainHeader);
    await performAction('selectSpecialMeasureForFlag', {
      specialMeasurelabel: specialMeasureForFlag.specialMeasureLabel,
      specialMeasureOption: specialMeasureForFlag.screeningWitnessFromAccusedRadioOption,
      continueButton: specialMeasureForFlag.continueButton
    });
    await performValidation('mainHeader', addCommentsForFlag.mainHeader);
    await performAction('addCommentsForFlag', {
      label: addCommentsForFlag.addCommentsLabel,
      input: addCommentsForFlag.addCommentTextInput,
      continueButton: addCommentsForFlag.continueButton
    });
    await performValidation('mainHeader', reviewFlagDetails.mainHeader);
    await performAction('clickChangeLinkForRow', {
      rowLabel: reviewFlagDetails.rowLabel,
      changeLinkText: reviewFlagDetails.changeLink
    });
    await performAction('selectSpecialMeasureForFlag', {
      specialMeasurelabel: specialMeasureForFlag.specialMeasureLabel,
      specialMeasureOption: specialMeasureForFlag.evidenceByLiveLinkRadioOption,
      continueButton: specialMeasureForFlag.continueButton
    });
    await performAction('clickButton', addCommentsForFlag.continueButton);
    await performAction('reviewFlagDetails', {
      saveButton: reviewFlagDetails.saveAndContinueButton
    });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Create case flags');
    await performAction('viewCaseFlags', {
      viewFlagLink: viewCaseFlag.viewFlagLink
    });
    await performAction('select', manageCaseFlags.nextStepEventList, manageCaseFlags.manageCaseFlagsEvent);
    await performAction('clickButton', manageCaseFlags.goButton);
    await performAction('manageCaseFlags', {
      flagOption: manageCaseFlags.respondentRadioOption,
      continueButton: manageCaseFlags.continueButton
    });
    await performAction('makeFlagInactive', {
      inactiveButton: updateFlagComments.makeInactiveButton,
      continueButton: updateFlagComments.continueButton
    });
    await performAction('reviewFlagDetails', {
      saveButton: reviewFlagDetails.saveAndContinueButton
    });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage case flags');
  });
});

test.describe('[Common Component Case Flags - Access control] @CC @caseFlags @access', async () => {
  test.beforeEach(async ({ page, context }) => {
    await seedCaseLevelFlag();
    await seedPartyLevelFlag();
    await clearBrowserSession(page, context);
  });

  test('Staff users can view, create and manage case-level and party-level flags', async ({ page, context }) => {
    test.setTimeout(ACCESS_CONTROL_TEST_TIMEOUT);
    const password = process.env.IDAM_PCS_USER_PASSWORD as string;

    for (const email of staffUserEmails) {
      await test.step(`Staff user ${email}`, async () => {
        await loginAsUser(page, email, password);
        await assertCanViewCaseAndPartyLevelFlags(page);
        await assertCanCreateCaseAndPartyLevelFlags(page);
        await assertCanManageCaseAndPartyLevelFlags(page);
        await clearBrowserSession(page, context);
      });
    }
  });

  test('Judicial users can view only case-level and party-level flags and cannot create or manage flags', async ({ page, context }) => {
    test.setTimeout(ACCESS_CONTROL_TEST_TIMEOUT);
    const password = process.env.IDAM_PCS_USER_PASSWORD as string;

    for (const email of judicialUserEmails) {
      await test.step(`Judicial user ${email}`, async () => {
        await loginAsUser(page, email, password);
        await assertCanViewCaseAndPartyLevelFlags(page);
        await assertCannotCreateOrManageFlags(page);
        await clearBrowserSession(page, context);
      });
    }
  });

  test('Claimant solicitor cannot view, create or manage case-level and party-level flags', async ({ page }) => {
    const { email, password } = user.claimantSolicitor;
    await loginAsUser(page, email, password as string);
    await assertNoFlagAccess(page);
  });

  test('Caseworker cannot view, create or manage case-level and party-level flags', async ({ page }) => {
    const { email, password } = user.caseworker;
    await loginAsUser(page, email, password as string);
    await assertNoFlagAccess(page);
  });
});
