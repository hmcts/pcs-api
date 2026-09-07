import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import {caseNumber} from '@utils/actions/custom-actions/createCase.action';
import {test} from '@utils/test-fixtures';
import {createCaseApiData, submitCaseApiData} from '@data/api-data';
import {caseSummary, home, user} from '@data/page-data';
import {reviewSupport} from '../../data/page-data-figma/page-data-common-component/reviewSupport.page.data';
import {dismissCookieBanner} from '@config/cookie-banner';
import {BrowserContext, Page} from '@playwright/test';
import { staff } from '@data/user-data/staff.user.data';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';

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
});

test.describe('[Review support request] - Solicitor user - @nightly @CC @supportEvents', async () => {

  test('Approve support request @smoke', async ({page}) => {
    await performAction('login', {email: user.claimantSolicitorForGATest.email, password: user.claimantSolicitorForGATest.password});
    await dismissCookieBanner(page, 'analytics');

    await performAction('clickTab', home.noticeOfChangeTab);
    await performAction('noticeOfChange', { caseRefNo: caseInfo.id } );
    await performAction('clientDetails', { firstName: 'Peter' , lastName: 'Parker' });
    await performAction('checkAndSubmit', { caseRefNo: caseInfo.id, firstName: 'Peter' , lastName: 'Parker' } );
    await new Promise(resolve => setTimeout(resolve, 5000));
    await performAction('noticeOfChangeSuccessful', { caseRefNo: caseInfo.fid } );
    await new Promise(resolve => setTimeout(resolve, 5000));
    await performAction('clickLink', reviewSupport.viewThisCase);
    await performValidation('mainHeader', reviewSupport.casePartiesHeader);

    await performAction('navigateToCaseSummary');
    await performAction('validateTabAccess', { user: user, tabs: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'Service Request', 'Support'] });
    await performAction('selectAnEvent', { eventType: caseSummary.requestSupport });
    await performValidation('mainHeader', reviewSupport.mainHeader);
    await performAction('selectRadioButtonInYourSupport', {
      optionToSelect: reviewSupport.whoIsTheSupportForOption,
      continueButton: reviewSupport.continueButton,
      headerToCheck: reviewSupport.supporTypeHeader
    });
    await performValidation('mainHeader', reviewSupport.mainHeader);
    await performAction('selectRadioButtonInYourSupport', {
      optionToSelect: reviewSupport.specialMeasureOption,
      continueButton: reviewSupport.continueButton,
      headerToCheck: reviewSupport.specialMeasureHeader
    });
    await performValidation('mainHeader', reviewSupport.mainHeader);
    await performAction('selectRadioButtonInYourSupport', {
      optionToSelect: reviewSupport.evidenceByLiveLinkOption,
      continueButton: reviewSupport.continueButton,
      headerToCheck: reviewSupport.addCommentLabel
    });
    await performValidation('mainHeader', reviewSupport.mainHeader);
    await performAction('inputText', reviewSupport.addCommentLabel, reviewSupport.addCommentText);
    await performAction('clickButton', reviewSupport.continueButton);
    await performAction('clickButton', 'Submit');
    await performValidation('bannerAlert', `Case #.* has been updated with event: Request support`);
    await performAction('signOut');

    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
    await performAction('login', {email: user.staffAdmin.email, password: user.staffAdmin.password});
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToCaseSummary');
    await performAction('select', caseSummary.nextStepEventList, caseSummary.reviewSupport);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', reviewSupport.reviewSupportHeader);
    await performAction('clickRadioButton', { option: 'Peter Parker (Defendant) - Special measure, Evidence by live link (Claimant Test Create Support)' });
    await performAction('clickButton', reviewSupport.continueButton);
    await performValidation('mainHeader', reviewSupport.reviewSupportHeader);
    await performAction('inputText', reviewSupport.reviewCommentLabel, reviewSupport.reviewCommentText);
    await performAction('clickRadioButton', { question: reviewSupport.reviewStatusLabel, option: reviewSupport.activeOption });
    await performAction('inputText', reviewSupport.reasonForStatusChangeLabel, reviewSupport.reasonForStatusChangeText);
    await performAction('clickButton', reviewSupport.continueButton);
    await performValidation('mainHeader', reviewSupport.reviewSupportHeader);
    await performAction('clickButton', reviewSupport.submitButton);
    await performValidation('bannerAlert', `Case #.* has been updated with event: Review support request`);
  });

});