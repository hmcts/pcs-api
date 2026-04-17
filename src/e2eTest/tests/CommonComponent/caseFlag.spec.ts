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
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Case%20flags`);
  // Login and cookie consent are handled globally via storageState in global-setup.config.ts
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Case%20flags`);
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
      continueButton: whereShouldThisFlagBeAdded.continueButton});
    await performValidation('mainHeader', selectFlagType.mainHeader);
    await performAction('selectFlag', {
        selectFlagQuestion: selectFlagType.selectFlagTypeQuestion,
        selectFlagOption: selectFlagType.complexCaseRadioOption,
        continueButton: selectFlagType.continueButton
      });
    await performValidation('mainHeader', addFlagcomment.mainHeader);
    await performAction('addComment', {
        addQuestion: addFlagcomment.addCommentsQuestion,
        addInput: addFlagcomment.addCommentTextInput,
        continueButton: addFlagcomment.continueButton
      });
      await performValidation('mainHeader', reviewFlagDetails.mainHeader);
      await performAction('reviewFlag', {
        continueButton: reviewFlagDetails.saveAndContinueButton
      });
      await performValidation('bannerAlert','Case #.* has been updated with event: Create flags');
  });

  // test('Case Flags - Cancel Case Flag @regression', async () => {
  //   await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
  //   await performAction('clickButtonAndVerifyPageNavigation', caseSummary.go, whereShouldThisFlagBeAdded.mainHeader);
  //   await performAction('clickButton', whereShouldThisFlagBeAdded.cancelButton);
  // });

  //  test('Case Flags - Mark Flag Inactive @PR @regression @nightly', async () => {
  //   await performAction('createFlag', {
  //     nextStepEventList: caseSummary.nextStepEventList,
  //     createFlagsEvent: caseSummary.createFlagsEvent,
  //     goButton: caseSummary.go,
  //     flagLevelHeader: whereShouldThisFlagBeAdded.mainHeader,
  //     flagLevelQuestion: whereShouldThisFlagBeAdded.whereShouldThisFlagBeAddedQuestion,
  //     flagLevelOption: whereShouldThisFlagBeAdded.caseLevelRadioOption,
  //     flagTypeQuestion: selectFlagType.selectFlagTypeQuestion,
  //     flagTypeOption: selectFlagType.complexCaseRadioOption,
  //     commentLabel: addFlagcomment.addCommentsLabel,
  //     commentText: addFlagcomment.addCommentTextInput,
  //     continueButton: whereShouldThisFlagBeAdded.continueButton,
  //     saveAndContinueButton: reviewFlagDetails.saveAndContinueButton,
  //     bannerMessage: 'Case #.* has been updated with event: Create flags',
  //   });
  //   await performAction('inactivateFlag', {
  //     nextStepEventList: caseSummary.nextStepEventList,
  //     manageFlagsEvent: caseSummary.manageCaseFlagsEvent,
  //     goButton: caseSummary.go,
  //     flagLevelHeader: whereShouldThisFlagBeAdded.mainHeader,
  //     option: whereShouldThisFlagBeAdded.caseLevelComplexCaseRadioOption,
  //     continueButton: whereShouldThisFlagBeAdded.continueButton,
  //     makeInactiveButton: addFlagcomment.makeInactiveButton,
  //     confirmButton: addFlagcomment.continueButton,
  //     saveAndContinueButton: reviewFlagDetails.saveAndContinueButton,
  //     bannerMessage: 'Case #.* has been updated with event: Manage Flags',
  //   });
  // });
});

