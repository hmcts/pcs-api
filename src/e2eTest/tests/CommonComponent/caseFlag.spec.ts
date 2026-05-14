import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import {
  manageCaseFlags,
  addCommentsForFlag,
  caseSummary,
  reviewFlagDetails,
  selectFlagType,
  updateFlagComments,
  viewCaseFlag,
  whereShouldThisFlagBeAdded
} from '@data/page-data';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { expect, test } from '@utils/test-fixtures';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { specialMeasureForFlag } from '@data/page-data/specialMeasureForFlag.page.data';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
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
  test('Case Flags - Create case level Flag @PR @regression @caseFlags @nightly', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', whereShouldThisFlagBeAdded.mainHeader);
    await performAction('whereShouldThisFlagBeAdded', {
      flagLevelQuestion: whereShouldThisFlagBeAdded.whereShouldThisFlagBeAddedQuestion,
      flagLevelOption: whereShouldThisFlagBeAdded.caseLevelRadioOption,
      continueButton: whereShouldThisFlagBeAdded.continueButton
    });
    await performValidation('mainHeader', selectFlagType.mainHeader);
    await performAction('selectFlagType', {
      selectFlagQuestion: selectFlagType.selectFlagTypeLabel,
      selectFlagOption: selectFlagType.complexCaseRadioOption,
      continueButton: selectFlagType.continueButton
    });
    await performValidation('mainHeader', addCommentsForFlag.mainHeader);
    await performAction('addCommentsForFlag', {
      label: addCommentsForFlag.addCommentsLabel,
      input: addCommentsForFlag.addCommentTextInput,
      continueButton: addCommentsForFlag.continueButton
    });
    await performValidation('mainHeader', reviewFlagDetails.mainHeader);
    await performAction('clickChangeLinkForRow', {
      rowLabel: reviewFlagDetails.rowLabel,
      changeLinkText: reviewFlagDetails.changeLink
    });
    await performAction('selectFlagType', {
      selectFlagQuestion: selectFlagType.selectFlagTypeLabel,
      selectFlagOption: selectFlagType.urgentCaseRadioOption,
      continueButton: selectFlagType.continueButton
    });
    await performAction('clickButton', addCommentsForFlag.continueButton);
    await performAction('reviewFlagDetails', {
      saveButton: reviewFlagDetails.saveAndContinueButton
    });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Create case flags');
    await performAction('viewCaseFlags', {
      viewFlagLink: viewCaseFlag.viewFlagLink
    });
    await performAction('select', manageCaseFlags.nextStepEventList, manageCaseFlags.manageCaseFlagsEvent);
    await performAction('clickButton', manageCaseFlags.goButton);
    await performAction('manageCaseFlags', {
      flagOption: manageCaseFlags.caseLevelUrgentCaseRadioOption,
      continueButton: manageCaseFlags.continueButton
    });
    await performAction('makeFlagInactive', {
      inactiveButton: updateFlagComments.makeInactiveButton,
      continueButton: updateFlagComments.continueButton
    });
    await performAction('reviewFlagDetails', {
      saveButton: reviewFlagDetails.saveAndContinueButton
    });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage case flags');
  });

  test('Case Flags - Create Party Level Case Flag @PR @regression @caseFlags @nightly', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', whereShouldThisFlagBeAdded.mainHeader);
    await performAction('whereShouldThisFlagBeAdded', {
      flagLevelQuestion: whereShouldThisFlagBeAdded.whereShouldThisFlagBeAddedQuestion,
      flagLevelOption: whereShouldThisFlagBeAdded.respondentRadioOption,
      continueButton: whereShouldThisFlagBeAdded.continueButton
    });
    await performValidation('mainHeader', selectFlagType.mainHeader);
    await performAction('selectFlagType', {
      selectFlaglabel: selectFlagType.selectFlagTypeLabel,
      selectFlagOption: selectFlagType.specialMeasureRadioOption,
      continueButton: selectFlagType.continueButton
    });
    await performValidation('mainHeader', specialMeasureForFlag.mainHeader);
    await performAction('selectSpecialMeasureForFlag', {
      specialMeasurelabel: specialMeasureForFlag.specialMeasureLabel,
      specialMeasureOption: specialMeasureForFlag.screeningWitnessFromAccusedRadioOption,
      continueButton: specialMeasureForFlag.continueButton
    });
    await performValidation('mainHeader', addCommentsForFlag.mainHeader);
    await performAction('addCommentsForFlag', {
      label: addCommentsForFlag.addCommentsLabel,
      input: addCommentsForFlag.addCommentTextInput,
      continueButton: addCommentsForFlag.continueButton
    });
    await performValidation('mainHeader', reviewFlagDetails.mainHeader);
    await performAction('clickChangeLinkForRow', {
      rowLabel: reviewFlagDetails.rowLabel,
      changeLinkText: reviewFlagDetails.changeLink
    });
    await performAction('selectSpecialMeasureForFlag', {
      specialMeasurelabel: specialMeasureForFlag.specialMeasureLabel,
      specialMeasureOption: specialMeasureForFlag.evidenceByLiveLinkRadioOption,
      continueButton: specialMeasureForFlag.continueButton
    });
    await performAction('clickButton', addCommentsForFlag.continueButton);
    await performAction('reviewFlagDetails', {
      saveButton: reviewFlagDetails.saveAndContinueButton
    });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Create case flags');
    await performAction('viewCaseFlags', {
      viewFlagLink: viewCaseFlag.viewFlagLink
    });
    await performAction('select', manageCaseFlags.nextStepEventList, manageCaseFlags.manageCaseFlagsEvent);
    await performAction('clickButton', manageCaseFlags.goButton);
    await performAction('manageCaseFlags', {
      flagOption: manageCaseFlags.respondentRadioOption,
      continueButton: manageCaseFlags.continueButton
    });
    await performAction('makeFlagInactive', {
      inactiveButton: updateFlagComments.makeInactiveButton,
      continueButton: updateFlagComments.continueButton
    });
    await performAction('reviewFlagDetails', {
      saveButton: reviewFlagDetails.saveAndContinueButton
    });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage case flags');
  });
  test('Case Flags - Verify the create and manage case flag menu @PR @regression @caseFlags @nightly @smoke', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', whereShouldThisFlagBeAdded.mainHeader);
    await performAction('clickButton', whereShouldThisFlagBeAdded.cancelButton);
    await performAction('select', manageCaseFlags.nextStepEventList, manageCaseFlags.manageCaseFlagsEvent);
    await performAction('clickButton', manageCaseFlags.goButton);
    await performAction('clickButton', manageCaseFlags.cancelButton);
  });
});