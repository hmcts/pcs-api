import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction, performValidation } from '@utils/controller-caseManagement';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement';
import { enterGenappApplication, enterGenAppapplicationFee, enterGenAppConsentAndNotice, enterGenAppHearingDate } from '@data/page-data-figma/page-data-caseManagement-figma';
import { label } from 'allure-js-commons';

test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  allPartyDetails.length = 0;
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');
  await performAction('getAllPartyDetails', {
    defendant1NameKnown: submitCaseApiData.submitCasePayloadCaseFileView.defendant1.nameKnown,
    additionalDefendants: submitCaseApiData.submitCasePayloadCaseFileView.addAnotherDefendant,
    payLoad: submitCaseApiData.submitCasePayloadCaseFileView
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

test.describe('Case management - Case Worker Enter a General application @nightly', async () => {
  test('Case management - Case Worker Enter a General application ADJOURN Journey @CM @regression', async () => {
    await performAction('selectAnEvent', { eventType: caseSummary.enterAGenApp });
    await performValidation('mainHeader', enterGenappApplication.mainHeader);
    await performAction('errorValidationEnterGeneralAppPage', enterGenappApplication.errorValidation);
    await performAction('enterApplicationDetails', {
      question1: enterGenappApplication.whichPartyMadeAppQuestion, option1: allPartyDetails[0],
      question2: enterGenappApplication.typeOfAppQuestion, option2: enterGenappApplication.adjournRadioOption,
      label: enterGenappApplication.whichCategoriesHiddenTextLabel,
      input: enterGenappApplication.whichCategoriesHiddenTextInput,
      label1: enterGenappApplication.dayTextLabel,
      label2: enterGenappApplication.monthTextLabel,
      label3: enterGenappApplication.yearTextLabel,
      dateType: enterGenappApplication.dateTypeHiddenUserInput,
      nextPage: enterGenAppHearingDate.mainHeader
    })
    await performAction('errorValidationHearingDatePage', enterGenAppHearingDate.errorValidation);
    await performAction('confirmIfCourtHearingInNext14Days', {
      question: enterGenAppHearingDate.hearingInNext14DaysQuestion,
      option: enterGenAppHearingDate.yesRadioOption,
      nextPage: enterGenAppapplicationFee.mainHeader
    });
    await performAction('errorValidationApplicationFeePage', enterGenappApplication.errorValidation);
    await performAction('enterApplicationFeeDetails', {
      question1: enterGenAppapplicationFee.appFeeReceivedQuestion, option1: enterGenAppapplicationFee.yesRadioOption,
      question2: enterGenAppapplicationFee.referenceNumberIncludedQuestion, option2: enterGenAppapplicationFee.yesRadioOption,
      label1: enterGenAppapplicationFee.enterTheAmountReceivedHiddenTextLabel,
      label2: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextLabel,
      input: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextInput,
      nextPage: enterGenAppConsentAndNotice.mainHeader
    })
  });

  test('Case management - Case Worker Enter a General application ADJOURN Journey - Application Fee Received - NO @CM', async () => {
    await performAction('selectAnEvent', { eventType: caseSummary.enterAGenApp });
    await performValidation('mainHeader', enterGenappApplication.mainHeader);
    await performAction('enterApplicationDetails', {
      question1: enterGenappApplication.whichPartyMadeAppQuestion, option1: allPartyDetails[0],
      question2: enterGenappApplication.typeOfAppQuestion, option2: enterGenappApplication.adjournRadioOption,
      label: enterGenappApplication.whichCategoriesHiddenTextLabel,
      input: enterGenappApplication.whichCategoriesHiddenTextInput,
      label1: enterGenappApplication.dayTextLabel,
      label2: enterGenappApplication.monthTextLabel,
      label3: enterGenappApplication.yearTextLabel,
      dateType: enterGenappApplication.dateTypeHiddenUserInput,
      nextPage: enterGenAppHearingDate.mainHeader
    });
    await performAction('confirmIfCourtHearingInNext14Days', {
      question: enterGenAppHearingDate.hearingInNext14DaysQuestion,
      option: enterGenAppHearingDate.yesRadioOption,
      nextPage: enterGenAppapplicationFee.mainHeader
    });
    await performAction('enterApplicationFeeDetails', {
      question1: enterGenAppapplicationFee.appFeeReceivedQuestion, option1: enterGenAppapplicationFee.noRadioOption,
      question2: enterGenAppapplicationFee.referenceNumberIncludedQuestion, option2: enterGenAppapplicationFee.yesRadioOption,
      label1: enterGenAppapplicationFee.enterTheAmountReceivedHiddenTextLabel,
      label2: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextLabel,
      input: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextInput,
      nextPage: enterGenAppConsentAndNotice.mainHeader
    })
  });

  test('Case management - Case Worker Enter a General application ADJOURN Journey - Fee Reference included - NO @CM', async () => {
    await performAction('selectAnEvent', { eventType: caseSummary.enterAGenApp });
    await performValidation('mainHeader', enterGenappApplication.mainHeader);
    await performAction('enterApplicationDetails', {
      question1: enterGenappApplication.whichPartyMadeAppQuestion, option1: allPartyDetails[0],
      question2: enterGenappApplication.typeOfAppQuestion, option2: enterGenappApplication.adjournRadioOption,
      label: enterGenappApplication.whichCategoriesHiddenTextLabel,
      input: enterGenappApplication.whichCategoriesHiddenTextInput,
      label1: enterGenappApplication.dayTextLabel,
      label2: enterGenappApplication.monthTextLabel,
      label3: enterGenappApplication.yearTextLabel,
      dateType: enterGenappApplication.dateTypeHiddenUserInput,
      nextPage: enterGenAppHearingDate.mainHeader
    });
    await performAction('confirmIfCourtHearingInNext14Days', {
      question: enterGenAppHearingDate.hearingInNext14DaysQuestion,
      option: enterGenAppHearingDate.yesRadioOption,
      nextPage: enterGenAppapplicationFee.mainHeader
    });
    await performAction('enterApplicationFeeDetails', {
      question1: enterGenAppapplicationFee.appFeeReceivedQuestion, option1: enterGenAppapplicationFee.yesRadioOption,
      question2: enterGenAppapplicationFee.referenceNumberIncludedQuestion, option2: enterGenAppapplicationFee.noRadioOption,
      label1: enterGenAppapplicationFee.enterTheAmountReceivedHiddenTextLabel,
      label2: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextLabel,
      input: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextInput,
      nextPage: enterGenAppConsentAndNotice.mainHeader
    })
  });

  test('Case management - Case Worker Enter a General application SET ASIDE Journey @CM', async () => {
    await performAction('selectAnEvent', { eventType: caseSummary.enterAGenApp });
    await performValidation('mainHeader', enterGenappApplication.mainHeader);
    await performAction('enterApplicationDetails', {
      question1: enterGenappApplication.whichPartyMadeAppQuestion, option1: allPartyDetails[1],
      question2: enterGenappApplication.typeOfAppQuestion, option2: enterGenappApplication.setAsideRadioOption,
      label: enterGenappApplication.whichCategoriesHiddenTextLabel,
      input: enterGenappApplication.whichCategoriesHiddenTextInput,
      label1: enterGenappApplication.dayTextLabel,
      label2: enterGenappApplication.monthTextLabel,
      label3: enterGenappApplication.yearTextLabel,
      dateType: enterGenappApplication.dateTypeHiddenUserInput,
      nextPage: enterGenAppapplicationFee.mainHeader
    })
    await performAction('enterApplicationFeeDetails', {
      question1: enterGenAppapplicationFee.appFeeReceivedQuestion, option1: enterGenAppapplicationFee.yesRadioOption,
      question2: enterGenAppapplicationFee.referenceNumberIncludedQuestion, option2: enterGenAppapplicationFee.yesRadioOption,
      label1: enterGenAppapplicationFee.enterTheAmountReceivedHiddenTextLabel,
      label2: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextLabel,
      input: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextInput,
      nextPage: enterGenAppConsentAndNotice.mainHeader
    })
  });

  test('Case management - Case Worker Enter a General application SOMETHING ELSE Journey @CM', async () => {
    await performAction('selectAnEvent', { eventType: caseSummary.enterAGenApp });
    await performValidation('mainHeader', enterGenappApplication.mainHeader);
    await performAction('enterApplicationDetails', {
      question1: enterGenappApplication.whichPartyMadeAppQuestion, option1: allPartyDetails[2],
      question2: enterGenappApplication.typeOfAppQuestion, option2: enterGenappApplication.somethingElseRadioOption,
      label: enterGenappApplication.whichCategoriesHiddenTextLabel,
      input: enterGenappApplication.whichCategoriesHiddenTextInput,
      label1: enterGenappApplication.dayTextLabel,
      label2: enterGenappApplication.monthTextLabel,
      label3: enterGenappApplication.yearTextLabel,
      dateType: enterGenappApplication.dateTypeHiddenUserInput,
      nextPage: enterGenAppapplicationFee.mainHeader
    })
    await performAction('enterApplicationFeeDetails', {
      question1: enterGenAppapplicationFee.appFeeReceivedQuestion, option1: enterGenAppapplicationFee.yesRadioOption,
      question2: enterGenAppapplicationFee.referenceNumberIncludedQuestion, option2: enterGenAppapplicationFee.yesRadioOption,
      label1: enterGenAppapplicationFee.enterTheAmountReceivedHiddenTextLabel,
      label2: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextLabel,
      input: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextInput,
      nextPage: enterGenAppConsentAndNotice.mainHeader
    })
  });
});
