import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import { expect, test } from '@utils/test-fixtures';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { user } from '@data/user-data';
import { caseSummary, home } from '@data/page-data';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {contactDetailsLR} from '@data/page-data-figma';
import {startNow } from "@data/page-data-figma/page-data-legalRepresentative/startNow.page.data";
import {deleteDraftResponse} from "@data/page-data-figma/page-data-legalRepresentative";

test.beforeEach(async ({ page, context }, testInfo) => {
  await context.clearCookies();
  initializeExecutor(page);
  const manageCaseBaseUrl = process.env.MANAGE_CASE_BASE_URL;
  if (!manageCaseBaseUrl) {
    throw new Error('MANAGE_CASE_BASE_URL is not set.');
  }
  await performAction('createCaseAPI', {data: createCaseApiData.createCasePayload});
  if (testInfo.title.includes('Delete Draft Response')) {
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadDefault() });
  } else {
    await performAction('submitCaseAPI', {data: submitCaseApiData.submitCasePayload()});
  }
  await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');

  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.defendantSolicitor);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToSummaryPage');
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();
});

test.describe('XUI - Respond to a claim - e2e Journey @nightly', () => {
  test('Trigger respond event @healthCheck', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.amendRepresentativeDetails);
    await performAction('clickButton', caseSummary.go);
    await performAction('selectRespondToClaimContactPreferences', {
      representativeReference: contactDetailsLR.defendantLegalRepresentativeReferenceTextInput,
      notifications: contactDetailsLR.yesRadioOption,
      correspondenceAddress: contactDetailsLR.noRadioOption,
      phoneNumber: contactDetailsLR.noRadioOption
    });
    await performAction('clickButton', 'Close and Return to case details');
    await performAction('select', caseSummary.nextStepEventList, 'Respond to claim');
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', startNow.mainHeader);
  });

  test('Update LR Details @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.amendRepresentativeDetails);
    await performAction('clickButton', caseSummary.go);
    await performAction('selectRespondToClaimContactPreferences', {
      representativeReference: contactDetailsLR.defendantLegalRepresentativeReferenceTextInput,
      notifications: contactDetailsLR.yesRadioOption,
      correspondenceAddress: contactDetailsLR.yesRadioOption,
      phoneNumber: contactDetailsLR.yesRadioOption
    });
    await performAction('clickButton', 'Close and Return to case details');
    await performValidation('mainHeader', home.caseParties);
    await performValidation('bannerAlert', `Case #.* has been updated with event: ${caseSummary.amendRepresentativeDetails}`);
  });

  async function deleteDraftResponsePrerequisite(): Promise<void> {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.amendRepresentativeDetails);
    await performAction('clickButton', caseSummary.go);
    await performAction('selectRespondToClaimContactPreferences', {
      representativeReference: contactDetailsLR.defendantLegalRepresentativeReferenceTextInput,
      notifications: contactDetailsLR.yesRadioOption,
      correspondenceAddress: contactDetailsLR.noRadioOption,
      phoneNumber: contactDetailsLR.noRadioOption
    });
    await performAction('clickButton', 'Close and Return to case details');
    await performAction('select', caseSummary.nextStepEventList, 'Respond to claim');
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', startNow.mainHeader);
    await performAction('clickButton', startNow.startNowButton);
    await performAction('clickButton', home.saveForLater);
  }

  test('Trigger Delete Draft Response event - Yes option selected', async () => {
    await deleteDraftResponsePrerequisite();
    await performAction('select', caseSummary.nextStepEventList, caseSummary.deleteDraftResponse);
    await performAction('clickButton', caseSummary.go);
    await performAction('deleteDraftResponseOption', {
      question: deleteDraftResponse.areYouSureToDeleteQuestion,
      option: deleteDraftResponse.yesRadioOption,
    });
    await performAction('confirmDeleteDraftResponse');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Delete draft response');
    await performValidation('dropdownOption', caseSummary.nextStepEventList, {
      text: caseSummary.deleteDraftResponse,
      shouldBeAvailable: false
    });
  });

  test('Trigger Delete Draft Response event - No option selected', async () => {
    await deleteDraftResponsePrerequisite();
    await performAction('select', caseSummary.nextStepEventList, caseSummary.deleteDraftResponse);
    await performAction('clickButton', caseSummary.go);
    await performAction('deleteDraftResponseOption', {
      question: deleteDraftResponse.areYouSureToDeleteQuestion,
      option: deleteDraftResponse.noRadioOption,
    });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Delete draft response');
    await performValidation('dropdownOption', caseSummary.nextStepEventList, {
      text: caseSummary.deleteDraftResponse,
      shouldBeAvailable: true
    });
  });
});
