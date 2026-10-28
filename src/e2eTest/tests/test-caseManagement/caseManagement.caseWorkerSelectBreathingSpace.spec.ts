import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction, performValidation } from '@utils/controller-caseManagement';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement';
import { enterBreathingSpace } from '@data/page-data-figma/page-data-caseManagement-figma';


import { label } from 'allure-js-commons';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';

test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  allPartyDetails.length = 0;
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView() });
  await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');
  await performAction('getAllPartyDetails', {
    defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseFileView().defendant1.nameKnown,
    additionalDefendants: submitCaseApiData.submitCasePayloadCaseFileView().addAnotherDefendant,
    payLoad: submitCaseApiData.submitCasePayloadCaseFileView()
  });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.hearingCenterAdmin);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToSummaryPage');
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});


test.describe('Case management - Case worker selects Breathing Space', () => {
  test(
    'Caseworker selects event to "enter breathing space" and navigates to the breathing space page',
    {
      tag: ['@CM', '@regression'],
      annotation: [
        { type: 'jira', description: 'The corresponding jira ticket : https://hmcts.atlassian.net/browse/HDPI-6287?' },
        { type: 'ac', description: 'AC1 and AC2' },
      ],
    },
    async () => {
      await test.step('Given the caseworker selects the "Enter breathing space" event', async () => {
        await performAction('select', caseSummary.nextStepEventList, caseSummary.enterBreathingSpaceEvent);
        await performAction('clickButton', caseSummary.go);
      });

      await test.step('Then the user is directed to the "Enter breathing space" screen', async () => {
        await performValidation('mainHeader', enterBreathingSpace.mainHeader);
        await performAction(
          'reTryOnCallBackError',
          enterBreathingSpace.continueButton,
          enterBreathingSpace.mainHeader
        );
      });
    }
  );
});