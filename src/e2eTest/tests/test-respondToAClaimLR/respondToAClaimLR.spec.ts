import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { expect, test } from '@utils/test-fixtures';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { user } from '@data/user-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from '../../playwright.config';
import { caseSummary, home } from '@data/page-data';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {contactDetailsLR} from '@data/page-data-figma';
import {startNow} from "@data/page-data-figma/page-data-legalRepresentative/startNow.page.data";
test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  const manageCaseBaseUrl = process.env.MANAGE_CASE_BASE_URL;
  if (!manageCaseBaseUrl) {
    throw new Error('MANAGE_CASE_BASE_URL is not set.');
  }
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');
  // Wait for CCD to propagate the solicitor link before the UI attempts to load the respond event
  // trigger. On AAT nightlies this race caused 404s from the data store immediately after linking.
  await performAction('pollRespondEventTriggerAPI');

  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.defendantSolicitor);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
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

test.describe('XUI - Respond to a claim - e2e Journey @nightly', () => {
  test('Trigger respond event @regression @healthCheck', async () => {
    // Skip on preview builds that do not have a paired frontend PR: without pcs-frontend-pr-<n>
    // Jenkins sets PCS_FRONTEND_URL to the AAT frontend, which cannot see a case that exists only
    // in the pull request's preview CCD, producing "You do not have access to this page".
    // Add the pcs-frontend-pr:<n> label to the pull request to enable this test on preview.
    test.skip(
      process.env.ENVIRONMENT === 'preview' &&
        !(process.env.PCS_FRONTEND_URL ?? '').includes('pcs-frontend-pr-'),
      'Legal representative journey needs a paired frontend: add the pcs-frontend-pr:<n> label to the pull request'
    );
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
});
