import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import {caseNumber} from '@utils/actions/custom-actions/createCase.action';
import {test} from '@utils/test-fixtures';
import {createCaseApiData, submitCaseApiData} from '@data/api-data';
import {caseSummary, user} from '@data/page-data';
import { createAndManageSupport } from '@data/page-data-figma/page-data-common-component/createAndManageSupport.page.data';
import {dismissCookieBanner} from '@config/cookie-banner';
import {BrowserContext, Page} from '@playwright/test';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';

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

  console.log("caseNumber",process.env.CASE_NUMBER);
});

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
  PageContentValidation.finaliseTest();
});

test.describe('Create and Manage Support Events @nightly @CC @supportEvents', async () => {

  test('Create and manage support events', async ({page}) => {
    //Email needs changing to solicitor user to create support event
    await performAction('login', {email: user.claimantSolicitor.email, password: user.claimantSolicitor.password});
    await dismissCookieBanner(page, 'analytics');
    
    // Create RequestSupport Event
    await performAction('navigateToCaseSummary');
    await performAction('select', caseSummary.nextStepEventList, caseSummary.requestSupport);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', createAndManageSupport.mainHeader);
    // Select the Possession Claims Solicitor Org (Claimant) radio option
    await performAction('clickRadioButton', { option: 'Possession Claims Solicitor Org (Claimant)' });
    await performAction('clickButton', createAndManageSupport.continueButton);
    // Select 'Reasonable adjustment' and continue
    await performValidation('mainHeader', createAndManageSupport.mainHeader);
    await performAction('clickRadioButton', { option: 'Reasonable adjustment' });
    await performAction('clickButton', createAndManageSupport.continueButton);
    // Choose to bring support to a hearing and continue
    await performValidation('mainHeader', createAndManageSupport.mainHeader);
    await performAction('clickRadioButton', { option: 'I need to bring support with me to a hearing' });
    await performAction('clickButton', createAndManageSupport.continueButton);
    // Select type of support and continue
    await performValidation('mainHeader', createAndManageSupport.mainHeader);
    await performAction('clickRadioButton', { option: 'Friend or family with me' });
    await performAction('clickButton', createAndManageSupport.continueButton);
    // Enter support details and continue
    await performValidation('mainHeader', createAndManageSupport.mainHeader);
    await performAction('inputText', createAndManageSupport.addCommentLabel, createAndManageSupport.addCommentText);
    await performAction('clickButton', createAndManageSupport.continueButton);
    // Submit the request
    await performAction('clickButton', 'Submit');
    // Validate success header after submission
    await performValidation('bannerAlert', `Case #.* has been updated with event: Request support`);

    // Now create ManageSupport Event
    await performAction('select', caseSummary.nextStepEventList, caseSummary.manageSupport);
    await performAction('clickButton', caseSummary.go);
    // Select the Support request and continue
    await performValidation('mainHeader', createAndManageSupport.mainHeaderManage);
    await performAction('clickRadioButton', { option: 'Possession Claims Solicitor Org (Claimant) - Reasonable adjustment, Friend or family with me (Claimant Test Create Support)' });
    await performAction('clickButton', createAndManageSupport.continueButton);
    // Add comments and continue
    await performValidation('mainHeader', createAndManageSupport.mainHeaderManage);
    await performAction('inputText', createAndManageSupport.updateCommentLabel, createAndManageSupport.updateCommentText);
    await performAction('clickButton', createAndManageSupport.continueButton);
    // Submit and validate success message
    await performValidation('mainHeader', createAndManageSupport.mainHeaderManage);
    await performAction('clickButton', 'Submit');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage support');
  });

});
