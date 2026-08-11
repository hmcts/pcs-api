import {createCaseApiData, manageHearingApiData, submitCaseApiData} from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction, performValidation } from '@utils/controller-caseManagement';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement';
import {
  checkYourAnswersEditHearing,
  editHearing,
  manageHearing
} from "@data/page-data-figma/page-data-caseManagement-figma";
import {
  CaseManagementCommonUtils
} from "@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action";


test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  allPartyDetails.length = 0;
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
  console.log(`Case created with case number: ${process.env.CASE_NUMBER}`);
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');
  await performAction('manageHearingAPI', {
    data: manageHearingApiData.AddHearingPayload,
    email: user.hearingCenterAdmin.email,
    password: user.hearingCenterAdmin.password
  });
  console.log(`Case has been associated with hearing`);
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
  test('Case management - Case Worker Edit a hearing @CM @regression', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(editHearing.dateTypeUserInput);
    await performAction('selectAnEvent', {eventType: caseSummary.manageHearing});
    await performValidation('mainHeader', manageHearing.mainHeader);
    await performAction('errorValidationManageHearing', manageHearing.errorValidation);
    await performAction('selectManageHearing', {
      question: manageHearing.doYouWantToAddQuestion,
      option: manageHearing.editAHearingRadioOption,
      nextPage: editHearing.mainHeader
    });
    await performAction('editHearing', {
      question: editHearing.whichTypeOfQuestion,
      option: editHearing.possessionFirstHearingRadioOption,
      label1: editHearing.wordingHearingNoticeLabel,
      dropDownInput: editHearing.ADJDropDownInput,
      whenHearingQuestion: editHearing.whenHearingQuestion,
      date: date,
      days: editHearing.daysLabel,
      hourLabel: editHearing.hoursLabel,
      minutesLabel: editHearing.minutesLabel,
      label3: editHearing.hearingNotesLabel,
      question2: editHearing.hearingNotesNeededQuestion,
      option2: editHearing.yesRadioOption,
      question3: editHearing.hearingWithoutNoticeHiddenQuestion,
      option3: editHearing.noHiddenRadioOption,
      label4: editHearing.enterAnyAdditionalInformationTextLabel,
      input: editHearing.additionalInformationTextInput,
      nextPage: checkYourAnswersEditHearing.mainHeader
    });
    await performAction('clickButton', checkYourAnswersEditHearing.submitButton);
    await performAction('confirmHearingEdited');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage hearing');
  });
});
