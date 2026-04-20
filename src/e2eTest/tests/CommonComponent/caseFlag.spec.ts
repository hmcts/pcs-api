import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import {
  addFlagcomment,
  caseSummary,
  reviewFlagDetails,
  selectFlagType,
  whereShouldThisFlagBeAdded
} from '@data/page-data';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { expect, test } from '@utils/test-fixtures';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadNoDefendants });
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  // Login and cookie consent are handled globally via storageState in global-setup.config.ts
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
});

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
});

test.describe('[Common Component Case Flags]', async () => {
  test('Case Flags - Create New Case Flag @PR @regression @nightly', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', whereShouldThisFlagBeAdded.mainHeader);
    await performAction('createFlag', {
      flagLevelQuestion: whereShouldThisFlagBeAdded.whereShouldThisFlagBeAddedQuestion,
      flagLevelOption: whereShouldThisFlagBeAdded.caseLevelRadioOption,
      continueButton: whereShouldThisFlagBeAdded.continueButton
    });
    await performValidation('mainHeader', selectFlagType.mainHeader);
    await performAction('selectFlag', {
      selectFlagQuestion: selectFlagType.selectFlagTypeQuestion,
      selectFlagOption: selectFlagType.complexCaseRadioOption,
      continueButton: selectFlagType.continueButton
    });
    await performValidation('mainHeader', addFlagcomment.mainHeader);
    await performAction('addComment', {
      label: addFlagcomment.addCommentsQuestion,
      input: addFlagcomment.addCommentTextInput,
      continueButton: addFlagcomment.continueButton
    });
    await performValidation('mainHeader', reviewFlagDetails.mainHeader);
    await performAction('reviewFlag', {
      saveButton: reviewFlagDetails.saveAndContinueButton
    });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Create flags');
    await performAction('viewFlag', {
      viewFlagLink: caseSummary.viewCaseFlagsLink
    });
    await performAction('select', caseSummary.nextStepEventList, caseSummary.manageCaseFlagsEvent);
    await performAction('clickButton', caseSummary.go);
    await performAction('selectFlagFromManageCaseFlags', {
      flagOptions: whereShouldThisFlagBeAdded.caseLevelComplexCaseRadioOption,
      continueButton: whereShouldThisFlagBeAdded.continueButton
    }); 
    await performAction('updateComment', {
      updateLabel: addFlagcomment.updateFlagQuestion,
      updateInput: addFlagcomment.updateFlagTextInput,
      inactiveButton: addFlagcomment.makeInactiveButton,
      continueButton: addFlagcomment.continueButton
    });
    await performAction('reviewFlag', {
      saveButton: reviewFlagDetails.saveAndContinueButton
    });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage Flags');});
});
