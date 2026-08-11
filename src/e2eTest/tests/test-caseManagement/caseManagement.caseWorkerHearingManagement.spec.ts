import {createCaseApiData, submitCaseApiData} from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction, performValidation } from '@utils/controller-caseManagement';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement';
import { addHearing, checkYourAnswersManageHearing, manageHearing } from '@data/page-data-figma/page-data-caseManagement-figma';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';



test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  allPartyDetails.length = 0;
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
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

test.describe('Case management - Case Worker Manage Hearing @nightly', async () => {
  test('Case management - Case Worker Cancel a hearing @CM @regression', async () => {
    await performAction('selectAnEvent', {eventType: caseSummary.manageHearing});
  })

  test('Case management - Case Worker Add a hearing @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(addHearing.dateTypeHiddenUserInput,'dateTime');
    let typeOfHearing = addHearing.typeOfHearingOption[0]
    await performAction('selectAnEvent', {eventType: caseSummary.manageHearing});
    await performValidation('mainHeader', addHearing.mainHeader);
    await performAction('errorValidationEnterAddAHearingPage', addHearing.errorValidation);
    await performAction('addAHearing', {
      hearingQuestion: addHearing.typeOfHearingQuestion, option: typeOfHearing,
      wordingQuestion: addHearing.wordingForHearingNoticeTextLabel, option1: addHearing.wordingForHearingHiddenOption,
      whenIsHearingLabel: addHearing.whenIsTheHearingQuestion,
      date: date,
      hourLabel: addHearing.hourTextLabel,
      minsLabel: addHearing.minutesTextLabel,
      hearingNotesLabel: addHearing.hearingNotesTextLabel,
      hearingNotesInput: addHearing.hearingNotesTextInput,
      noticeQuestion: addHearing.hearingNoticeQuestion, option2: addHearing.hearingNoticeYesRadioOption,
      withoutNoticeQuestion: addHearing.hearingWithOutNoticeHiddenQuestion, option3: addHearing.hearingNoticeNoRadioOption,
      additionalInfoLabel: addHearing.enterAdditionalInfoTextLabel,
      additionalInfoInput: addHearing.enterAdditionalInfoTextInput,
      nextPage: checkYourAnswersManageHearing.mainHeader
    });
    await performAction('clickButton', checkYourAnswersManageHearing.submitButton);
    await performAction('confirmAddHearing', { submitPayload: submitCaseApiData.submitCasePayloadCaseFileView });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage hearing');
  })

  test('Case management - Case Worker Add a hearing without Notice @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(addHearing.dateTypeHiddenUserInput,'dateTime');
    let party = allPartyDetails[0]
    let typeOfHearing = addHearing.typeOfHearingOption[0]
    await performAction('selectAnEvent', {eventType: caseSummary.manageHearing});
    await performValidation('mainHeader', addHearing.mainHeader);
    await performAction('addAHearing', {
      hearingQuestion: addHearing.typeOfHearingQuestion, option: typeOfHearing,
      wordingQuestion: addHearing.wordingForHearingNoticeTextLabel, option1: addHearing.wordingForHearingHiddenOption,
      whenIsHearingLabel: addHearing.whenIsTheHearingQuestion,
      date: date,
      hourLabel: addHearing.hourTextLabel,
      minsLabel: addHearing.minutesTextLabel,
      hearingNotesLabel: addHearing.hearingNotesTextLabel,
      hearingNotesInput: addHearing.hearingNotesTextInput,
      noticeQuestion: addHearing.hearingNoticeQuestion, option2: addHearing.hearingNoticeYesRadioOption,
      withoutNoticeQuestion: addHearing.hearingWithOutNoticeHiddenQuestion, option3: addHearing.hearingNoticeYesRadioOption,
      whoShouldReceiveNoticeQuestion : addHearing.whoShouldReceiveHiddenQuestion, option4: party,
      additionalInfoLabel: addHearing.enterAdditionalInfoTextLabel,
      additionalInfoInput: addHearing.enterAdditionalInfoTextInput,
      nextPage: checkYourAnswersManageHearing.mainHeader
    });
    await performAction('clickButton', checkYourAnswersManageHearing.submitButton);
    await performAction('confirmAddHearing', { submitPayload: submitCaseApiData.submitCasePayloadCaseFileView });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage hearing');
  })
});