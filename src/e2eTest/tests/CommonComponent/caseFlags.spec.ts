import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import {
  addressCheckYourAnswers,
  addressDetails,
  caseSummary,
  home
} from '@data/page-data';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { expect, test } from '@utils/test-fixtures';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import {serviceRequest} from "@data/page-data/serviceRequest.page.data";
import { label } from 'allure-js-commons';
import { text } from 'stream/consumers';
import { caseFlags } from '@data/page-data/caseFlags.page.data';
test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadNoDefendants });
  /*await performAction('getDefendantDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadNoDefendants.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadNoDefendants.addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayloadNoDefendants
    }); */
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Service%20Request`);
  // Login and cookie consent are handled globally via storageState in global-setup.config.ts
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Service%20Request`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
});

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
 // PageContentValidation.finaliseTest();
});
test.describe('[Create Case Flags]', async () => {
    test('create case flags @PR @regression', async () => {
        // Reusable custom action for create-case-flags workflow
        await performAction('createCaseFlags');
    });
});
