import { createCaseApiData, submitCaseApiData } from '@data/api-data';


import { initializeExecutor } from '@utils/controller';
import test, { expect } from '@playwright/test';
import { FieldsStore } from '@utils/actions/custom-actions/custom-actions-genApps/recordAnsweredFields.action';
import { initializeGenAppsExecutor, performAction, performValidation } from '@utils/controller-genApps';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { caseSummary } from '@data/page-data/caseSummary.page.data';
import { user } from '@data/user-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {
  askTheCourtToSuspendAnOrder, checkYourAnswersGenApps, chooseAnApplication,
  doYouWantToUploadDocumentsToSupportDefendantsApplication,
  helpPayingTheFee, selectParty,
  statementOfTruth, uploadDocumentsToSupportDefendantsApplication, whatOrderDoYouWantTheCourtToMakeAndWhy,
  whichLanguageDidYouUseToCompleteThisService
} from "@data/page-data-figma/page-data-genApps-figma";
import { defendantDetails } from '@utils/actions/custom-actions/custom-actions-genApps';
import { home } from '@data/page-data';


test.use({ storageState: undefined });

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeGenAppsExecutor(page);
  defendantDetails.length = 0;
  FieldsStore.clear();
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload() });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');
  await performAction('getDefendantDetails', {
    defendant1NameKnown: submitCaseApiData.submitCasePayload().defendant1.nameKnown,
    additionalDefendants: submitCaseApiData.submitCasePayload().addAnotherDefendant,
    payLoad: submitCaseApiData.submitCasePayload()
  });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);

  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.defendantSolicitor);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
  await page.waitForLoadState();
  await page.locator('.spinner-container').waitFor({ state: 'detached' });
  await performValidation('mainHeader', home.caseSummary);
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});

test.describe('Make an Application - e2e Journey @nightly', async () => {
  test(`As a Legal Representative I am applying online to suspend hearing on behalf of my client so it can be submitted to courts and viewed by case workers and judges`,
    {
      tag: ['@smoke', '@regression'],
      annotation: [
        { type: 'jira', description: `The corresponding jira ticket : https://hmcts.atlassian.net/browse/HDPI-9114` },
        { type: 'ac', description: 'AC1, AC3-AC9' },
      ],
    },
    async () => {
      await test.step(`Given the LR has selected the Make an Application event`, async () => {
        await performAction('select', caseSummary.nextStepEventList, caseSummary.makeAnApplication);
        await performAction('clickButton', caseSummary.go);
      });

      await test.step(`When the user selects the Suspend the Eviction option`, async () => {
        await performValidation('mainHeader', chooseAnApplication.mainHeader);
        //await performAction('errorValidationChooseAnApplicationPage', chooseAnApplication.errorValidation);
        await performAction('chooseAnApplication', {
          question: chooseAnApplication.whatDoYouWantToApplyForQuestion,
          option: chooseAnApplication.suspendTheEvictionRadioOption,
        });
      });

      await test.step(`And proceeds to the party screen`, async () => {
        await performValidation('mainHeader', askTheCourtToSuspendAnOrder.mainHeader);
        await performAction('clickButton', askTheCourtToSuspendAnOrder.continueButton);
      });

      await test.step(`Then selects the applicant they are representing`, async () => {
        await performValidation('mainHeader', selectParty.mainHeader);
        await performAction('selectApplicant', {
          question: selectParty.partyMakingApplicationQuestion,
          option: defendantDetails[1],
        });
      });

      await test.step(`And indicates that they do not need help paying the fee`, async () => {
        await performValidation('mainHeader', helpPayingTheFee('LR').mainHeader);
        await performAction('doYouNeedHelpPayingFee', {
          question: helpPayingTheFee('LR').doYouNeedHelpPayingTheFeeQuestion,
          option: helpPayingTheFee('LR').noRadioOption,
          claimType: 'LR',
        });
      });

      await test.step(`And explains what the defendant wants the court to do`, async () => {
        await performValidation('mainHeader', whatOrderDoYouWantTheCourtToMakeAndWhy.mainHeader);
        await performAction('confirmOrderDoYouWant', {
          label: whatOrderDoYouWantTheCourtToMakeAndWhy.explainWhatYouWantTextLabel,
          input: whatOrderDoYouWantTheCourtToMakeAndWhy.whatYouWantTheCourtToDoTextInput,
        });
      });

      await test.step(`And chooses to upload a document`, async () => {
        await performAction('confirmDocumentToUpload', {
          question: doYouWantToUploadDocumentsToSupportDefendantsApplication.doYouWantToUploadDocumentQuestion,
          option: doYouWantToUploadDocumentsToSupportDefendantsApplication.yesRadioOption,
        });
      });

      await test.step(`Then uploads the document`, async () => {
        await performValidation('mainHeader', uploadDocumentsToSupportDefendantsApplication.mainHeader);
        await performAction('uploadFilesGenApps', {
          documents: [
            {
              type: uploadDocumentsToSupportDefendantsApplication.inspectionOrReportDropDownInput,
              fileName: 'genApps.ppt'
            },
          ]
        });
      });

      await test.step(`And selects the language used to complete the service`, async () => {
        await performAction('selectLanguageUsedToComplete', {
          question: whichLanguageDidYouUseToCompleteThisService.whichLanguageDidYouUseQuestion,
          option: whichLanguageDidYouUseToCompleteThisService.englishAndWelshRadioOption,
        });
      });

      await test.step(`And completes the Statement of Truth step`, async () => {
        await performValidation('mainHeader', statementOfTruth.mainHeader);
        await performAction('selectStatementOfTruth', {
          option: statementOfTruth.theDefendantBelievesCheckBox,
          label1: statementOfTruth.fullNameTextLabel,
          input1: statementOfTruth.fullNameTextInput,
          label2: statementOfTruth.nameOfFirmTextLabel,
          input2: statementOfTruth.nameOfFirmTextInput,
          label3: statementOfTruth.positionOrOfficeHeldTextLabel,
          input3: statementOfTruth.positionOrOfficeHeldTextInput,
        });
      });

      await test.step(`And reviews the CYA page and submits the application`, async () => {
        await performValidation('mainHeader', checkYourAnswersGenApps.mainHeader);
        await performAction('retrieveCYATableData', {name: 'check your answers table'});
        await performAction('validateCYA');
        await performAction('clickButton', checkYourAnswersGenApps.submitButton);
      });

      await test.step(`Then verifies the fee amount displayed on the confirmation page`, async () => {
        await performAction('payClaimFeeGenApps', {clickLink: false, claimType: 'LR'});
      });
    });
});
