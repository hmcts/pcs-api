import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import {caseNumber} from '@utils/actions/custom-actions/createCase.action';
import {test} from '@utils/test-fixtures';
import {createCaseApiData, submitCaseApiData} from '@data/api-data';
import {caseSummary, user} from '@data/page-data';
import {staff} from '@data/user-data/staff.user.data';
import {judicialEmails} from '@data/user-data/judicial.user.data';
import {
  addCommentsForFlag,
  manageCaseFlags,
  reviewFlagDetails,
  selectFlagType,
  specialMeasureForFlag,
  updateFlagComments,
  viewCaseFlag,
  whereShouldThisFlagBeAdded
} from '@data/page-data-figma';
import {dismissCookieBanner} from '@config/cookie-banner';
import {BrowserContext, Page} from '@playwright/test';
import {
  logUserTestResultsAndAssert,
  recordUserTestFailure,
  UserTestResult
} from '@utils/common/userTestResults.utils';

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

test.use({storageState: undefined});

test.beforeEach(async ({page, context}) => {
  await context.clearCookies();
  initializeExecutor(page);
  await performAction('createCaseAPI', {data: createCaseApiData.createCasePayload});
  await performAction('submitCaseAPI', {data: submitCaseApiData.submitCasePayload});
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
});

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
});

test.describe('[Common Component Case Flags] @nightly @CC @caseFlags', async () => {

  test('Case Flags - Verify the create and manage case flag menu @smoke', async ({page}) => {
    await performAction('login', user.ctscAdministrator);
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToCaseSummary');
    await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', whereShouldThisFlagBeAdded.mainHeader);
    await performAction('clickButton', whereShouldThisFlagBeAdded.cancelButton);
    await performAction('select', manageCaseFlags.nextStepEventList, manageCaseFlags.manageCaseFlagsEvent);
    await performAction('clickButton', manageCaseFlags.goButton);
    await performAction('clickButton', manageCaseFlags.cancelButton);
  });
  test('Case Flags - Create case level Flag', async ({page}) => {
    await performAction('login', user.ctscAdministrator);
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToCaseSummary');
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
  test('Case Flags - Create Party Level Case Flag', async ({page}) => {
    await performAction('login', user.ctscAdministrator);
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToCaseSummary');
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

test.describe('[Common Component Case Flags - Access Management] @CC @caseFlags @nightly', async () => {
  test.describe.configure({ retries: 0 });
  test('Staff users can create, manage and view case-level and party-level flags', async ({page, context}) => {
    test.setTimeout(ACCESS_CONTROL_TEST_TIMEOUT);
    const results: UserTestResult[] = [];
    const password = process.env.IDAM_PCS_USER_PASSWORD as string;
    for (const email of staffUserEmails) {
      try {
        await test.step(`Staff user ${email}`, async () => {
          await performAction('login', {email, password});
          await dismissCookieBanner(page, 'analytics');
          await performAction('navigateToCaseSummary', 'yes');
          await performAction('canCreateCaseLevelFlag', 'yes');
          await performAction('canCreatePartyLevelFlag', 'yes');
          await performAction('canManageCaseLevelFlag', 'yes');
          await performAction('canManagePartyLevelFlag', 'yes');
          await performAction('canViewCaseAndPartyFlag', 'yes');
          await clearBrowserSession(page, context);
        });
        results.push({email, status: 'PASS'});
      } catch (error) {
        recordUserTestFailure(results, email, error);
        await clearBrowserSession(page, context).catch(() => {
        });
      }
    }
    logUserTestResultsAndAssert('STAFF USER RESULTS', results);
  });

  test('Judicial users can only view case-level and party-level flags and cannot create or manage flags', async ({page, context}) => {
    test.setTimeout(ACCESS_CONTROL_TEST_TIMEOUT);
    const results: UserTestResult[] = [];
    const password = process.env.IDAM_PCS_USER_PASSWORD as string;
    for (const email of judicialUserEmails) {
      try {
        await test.step(`Judicial user ${email}`, async () => {
          await performAction('login', {email, password});
          await dismissCookieBanner(page, 'analytics');
          await performAction('handleJudgeBookingPage');
          await performAction('navigateToCaseSummary', 'yes');
          await performValidation('elementNotToBeVisible', caseSummary.nextStepEventList);
          await performAction('canViewCaseAndPartyFlag', 'yes');
          await clearBrowserSession(page, context);
        });
        results.push({email, status: 'PASS'});
      } catch (error) {
        recordUserTestFailure(results, email, error);
        await clearBrowserSession(page, context).catch(() => {
        });
      }
    }
    logUserTestResultsAndAssert('JUDICIAL USER RESULTS', results);
  });

  test('Solicitor user cannot view, create or manage case-level and party-level flags', async ({page, context}) => {
    const {email, password} = user.claimantSolicitor;
    await performAction('login', {email, password});
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToCaseSummary', 'yes');
    await performAction('canCreateCaseLevelFlag', 'no');
    await performAction('canCreatePartyLevelFlag', 'no');
    await performAction('canManageCaseLevelFlag', 'no');
    await performAction('canManagePartyLevelFlag', 'no');
    await performAction('canViewCaseAndPartyFlag', 'no');
    await clearBrowserSession(page, context);
  });

  test('User with Caseworker IDAM role without AM roles cannot view, create or manage case-level and party-level flags', async ({page, context}) => {
    const {email, password} = user.caseworker;
    await performAction('login', {email, password});
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToCaseSummary', 'no');
    await clearBrowserSession(page, context);
  });
});
