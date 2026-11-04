import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor, performAction } from '@utils/controller';
import { initializeCMExecutor, performAction as performCMAction } from '@utils/controller-caseManagement';
import test from '@playwright/test';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { dismissCookieBanner } from '@config/cookie-banner';
import { user } from '@data/user-data';
import { home } from '@data/page-data';
import { respondPossessionClaimCitizenApiData } from '@data/api-data/respondPossessionClaimCitizen.api.data';

test.use({ storageState: undefined });

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView() });
  await performAction('updatePaymentAPI');
  await performAction('createUser', 'citizen', ['citizen']);
  await performAction('generateCitizenAccessToken');
  const defendantIndex = 2;
  await performAction('fetchPINsAPI', { defendantIndex });
  await performAction('validateAccessCodeAPI', { defendantIndex });
  await performAction('midEventRespondPossessionClaimCitizenAPI', {
    data: respondPossessionClaimCitizenApiData.midEventPayload(),
    defendantIndex,
  });
  await performAction('submitPossessionClaimResponseCitizenAPI');
  await performAction('updatePaymentAPI', { amt: 3500, app: 'Counter Claim' });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.hearingCenterAdmin);
  await dismissCookieBanner(page, 'analytics');
  await performCMAction('navigateToSummaryPage');
});

test.afterEach(async () => {
  if (caseInfo.id) await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  PageContentValidation.finaliseTest();
});

test.describe('Citizen respond to a claim - counterclaim documents @regression @nightly', () => {
  test('Counterclaim PDF reference code sequencing @regression', async () => {
    const defendantRank = process.env.CITIZEN_DEFENDANT_RANK ?? 'Defendant 1';
    await performCMAction('clickTab', home.caseFileView);
    await performCMAction('validateCaseFileViewFolders', home.caseFileFolders);
    await performCMAction('validateCaseFileViewIndividualFolder', {
      folder: 'Statements of case',
      submitPayload: submitCaseApiData.submitCasePayloadCaseFileView(),
      ccPayload: respondPossessionClaimCitizenApiData.midEventPayload(),
      expectedFiles: [
        `Counterclaim CC1 - ${defendantRank}.pdf`,
        `possessionNoticeWithOutNotice CC1 - ${defendantRank}.pdf`,
      ],
    });
  });
});
