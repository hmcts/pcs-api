import { createCaseApiData, enforceWarrantApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction, performValidation } from '@utils/controller-caseManagement';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement';
import {
  checkYourAnswersDecideEnforcement,
  confirmEnforcementDecision,
  decideEnforcement
} from "@data/page-data-figma/page-data-caseManagement-figma";
import {
  CaseManagementCommonUtils
} from "@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action";

test.use({ storageState: undefined })

let confirmDecision: ReturnType<typeof confirmEnforcementDecision>;

test.beforeEach(async ({ page, context }, testInfo) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  allPartyDetails.length = 0;
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView() });
  await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
  await performAction('updatePaymentAPI');
  await performAction('getAllPartyDetails', {
    defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseFileView().defendant1.nameKnown,
    additionalDefendants: submitCaseApiData.submitCasePayloadCaseFileView().addAnotherDefendant,
    payLoad: submitCaseApiData.submitCasePayloadCaseFileView()
  });

  await performAction('enforceCaseAPI', { data: enforceWarrantApiData.enforceCasePayloadYesJourney });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.hearingCenterAdmin);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToSummaryPage');
  confirmDecision = confirmEnforcementDecision(test.info().title);
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();
});

test.describe('Case management - Case Worker Manage Enforcement @nightly', async () => {

  test(`Case management - As a Case Worker I want to reject an enforcement application after reviewing it`,
    {
      tag: ['@CM', '@regression'],
      annotation: [
        { type: 'jira', description: `The corresponding jira ticket : https://hmcts.atlassian.net/browse/HDPI-8656` },
        { type: 'ac', description: 'AC1 , AC2, AC4 - AC9, AC11- AC16' },
      ],
    }, async () => {

      const rejectOption = decideEnforcement.rejectHiddenOptions[Math.floor(Math.random() * decideEnforcement.rejectHiddenOptions.length)];
      const rejectNote = CaseManagementCommonUtils.generateRandomString(decideEnforcement.rejectHiddenNotes);

      await test.step(`Given the user has selected the Decide enforcement event`, async () => {
        await performAction('selectAnEvent', { eventType: caseSummary.decideEnforcement });
        await performValidation('mainHeader', decideEnforcement.mainHeader);
      });

      await test.step(`When the user validates and rejects the enforcement application after reviewing it`, async () => {
        await performAction('errorValidationDecideEnforcementPage', decideEnforcement.errorValidation);
        await performAction(`decideEnforcementDecision`, {
          acceptOrRejectQn: decideEnforcement.acceptOrRejectQuestion,
          option: decideEnforcement.rejectRadioOption,
          reasonForRejectQn: decideEnforcement.selectReasonHiddenLabel,
          rejectOption: rejectOption,
          rejectTextInput: rejectNote,
          nextPage: checkYourAnswersDecideEnforcement.mainHeader
        });
      });

      await test.step(`Then the rejection is submitted successfully`, async () => {
        await performAction('clickButton', checkYourAnswersDecideEnforcement.submitButton);
        await performAction('confirmEnforcementDecision', { submitPayload: submitCaseApiData.submitCasePayloadCaseFileView(), decisionText: confirmDecision.decisionHiddenText, confirmData: confirmDecision });
        await performValidation('bannerAlert', 'Case #.* has been updated with event: Decide enforcement');
      });
    });

  test(`Case management - As a Case Worker I want to accept an enforcement application after reviewing it`,
    {
      tag: ['@CM', '@regression'],
      annotation: [
        { type: 'jira', description: `The corresponding jira ticket : https://hmcts.atlassian.net/browse/HDPI-8656` },
        { type: 'ac', description: 'AC10' },
      ],
    }, async () => {

      await test.step(`Given the user has selected the Decide enforcement event`, async () => {
        await performAction('selectAnEvent', { eventType: caseSummary.decideEnforcement });
        await performValidation('mainHeader', decideEnforcement.mainHeader);
      });

      await test.step(`When the user validates and accepts the enforcement application after reviewing it`, async () => {
        await performAction(`decideEnforcementDecision`, {
          acceptOrRejectQn: decideEnforcement.acceptOrRejectQuestion,
          option: decideEnforcement.acceptRadioOption,
          nextPage: checkYourAnswersDecideEnforcement.mainHeader
        });
      });

      await test.step(`Then the acceptance is submitted successfully`, async () => {
        await performAction('clickButton', checkYourAnswersDecideEnforcement.submitButton);
        await performAction('confirmEnforcementDecision', { submitPayload: submitCaseApiData.submitCasePayloadCaseFileView(), decisionText: confirmDecision.decisionHiddenText, confirmData: confirmDecision });
        await performValidation('bannerAlert', 'Case #.* has been updated with event: Decide enforcement');
      });
    });
})