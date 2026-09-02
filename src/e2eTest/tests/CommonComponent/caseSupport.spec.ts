import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import {caseNumber} from '@utils/actions/custom-actions/createCase.action';
import {test} from '@utils/test-fixtures';
import {createCaseApiData, submitCaseApiData} from '@data/api-data';
import {caseSummary, user} from '@data/page-data';
import {reviewSupport} from '@data/page-data-figma/page-data-common-component/reviewSupport.page.data';
import {dismissCookieBanner} from '@config/cookie-banner';
import {BrowserContext, Page} from '@playwright/test';
import { staff } from '@data/user-data/staff.user.data';

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

test.describe('Create and Manage Support Events @nightly @CC @supportEvents', async () => {

  test('Create and manage support events', async ({page}) => {
    await performAction('login', {email: user.claimantSolicitor.email, password: user.claimantSolicitor.password});
    await dismissCookieBanner(page, 'analytics');

    await performAction('navigateToCaseSummary');
    await performAction('validateTabAccess', { user: user, tabs: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'Service Request', 'Support'] });

    await performAction('select', caseSummary.nextStepEventList, caseSummary.requestSupport);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', reviewSupport.mainHeader);
    await performAction('clickRadioButton', { option: reviewSupport.optionOne });
    await performAction('clickButton', reviewSupport.continueButton);
    await performValidation('mainHeader', reviewSupport.mainHeader);
    await performAction('clickRadioButton', { option: reviewSupport.optionTwo });
    await performAction('clickButton', reviewSupport.continueButton);
    await performValidation('mainHeader', reviewSupport.mainHeader);
    await performAction('clickRadioButton', { option: reviewSupport.optionThree });
    await performAction('clickButton', reviewSupport.continueButton);
    await performValidation('mainHeader', reviewSupport.mainHeader);
    await performAction('clickRadioButton', { option: reviewSupport.optionFour });
    await performAction('clickButton', reviewSupport.continueButton);
    await performValidation('mainHeader', reviewSupport.mainHeader);
    await performAction('inputText', reviewSupport.addCommentLabel, reviewSupport.addCommentText);
    await performAction('clickButton', reviewSupport.continueButton);
    await performAction('clickButton', 'Submit');
    await performValidation('bannerAlert', `Case #.* has been updated with event: Request support`);

    await performAction('select', caseSummary.nextStepEventList, caseSummary.manageSupport);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', reviewSupport.mainHeaderManage);
    await performAction('clickRadioButton', { option: 'Possession Claims Solicitor Org (Claimant) - Reasonable adjustment, Friend or family with me (Claimant Test Create Support)' });
    await performAction('clickButton', reviewSupport.continueButton);
    await performValidation('mainHeader', reviewSupport.mainHeaderManage);
    await performAction('inputText', reviewSupport.updateCommentLabel, reviewSupport.updateCommentText);
    await performAction('clickButton', reviewSupport.continueButton);
    await performValidation('mainHeader', reviewSupport.mainHeaderManage);
    await performAction('clickButton', 'Submit');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage support');
  });

  test('Staff users can not see Support tab', async ({page}) => {
    await performAction('login', { email: staff.pcs_ctsc_admin_email, password: process.env.IDAM_PCS_USER_PASSWORD });
    await dismissCookieBanner(page, 'analytics');

    await performAction('navigateToCaseSummary');
    await performAction('validateTabAccess', { user: user, tabs: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags'] });
  });

});