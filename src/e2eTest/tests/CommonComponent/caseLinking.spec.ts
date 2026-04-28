import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { expect, test } from '@utils/test-fixtures';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { caseSummary } from '@data/page-data';
import { beforeYouStart } from '@data/page-data/beforeYouStart.page.data';
import { selectCasesToLink } from '@data/page-data/selectCaseToLink.page.data';
import { selectCasesToUnLink } from '@data/page-data/selectCasesToUnLink.page.data';
import { checkYourAnswersCaseLinking } from '@data/page-data/checkYourAnswersCaseLinking.page.data';

let caseNumbers: string[] = [];

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  //await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  for (let i = 0; i < 5; i++) {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadNoDefendants });
    const caseNumber = process.env.CASE_NUMBER;
    if (!caseNumber) {
      throw new Error('CASE_NUMBER not set');
    }
    caseNumbers.push(caseNumber);
    // 🔹 log each case number immediately
    console.log(`Created Case ${i + 1}: ${caseNumber}`);
  }
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#`);
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

test.describe('[Common Component Case Linking]', async () => {
  test('Case Linking @PR @regression @nightly', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.linkCaseEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', beforeYouStart.mainHeader);
    await performAction('clickButton', beforeYouStart.submitButton);
    await performValidation('mainHeader', selectCasesToLink.mainHeader);
    await performAction('selectCasesToLink', {
      caseRefInput: caseNumbers,
      question: selectCasesToLink.whyToLinkQuestion,
      option: [
        selectCasesToLink.caseConsolidateCheckbox,
        selectCasesToLink.progressedCheckbox,
        selectCasesToLink.relatedAppealCheckbx,
        selectCasesToLink.samePartyCheckbox,
      ],
      proposeButton: selectCasesToLink.proposeLinkButton
    });
    await performValidation('mainHeader', checkYourAnswersCaseLinking.mainHeader);
    await performAction('clickButton', checkYourAnswersCaseLinking.submitButton);
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Link cases');
    await performAction('select', caseSummary.nextStepEventList, caseSummary.manageCaseEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', beforeYouStart.mainHeader);
    await performAction('clickButton', beforeYouStart.submitButton);
    await performValidation('mainHeader', selectCasesToUnLink.mainHeader);
    await performAction('selectCasesToUnLink', { caseRefInput: caseNumbers });
    await performValidation('mainHeader', checkYourAnswersCaseLinking.mainHeader);
    await performAction('clickButton', checkYourAnswersCaseLinking.submitButton);
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage case links');
    await performAction('verifyLinkedCases', { caseRefInput: caseNumbers });
  });
});