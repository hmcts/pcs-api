import { createCaseApiData, submitCaseApiData } from '@data/api-data';


import { initializeExecutor } from '@utils/controller';
import test from '@playwright/test';
import { FieldsStore } from '@utils/actions/custom-actions/custom-actions-genApps/recordAnsweredFields.action';
import { initializeGenAppsExecutor, performAction } from '@utils/controller-genApps';
import { user } from '@data/user-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { defendantDetails } from '@utils/actions/custom-actions/custom-actions-genApps/genApps.action';
import { home } from '@data/page-data';
import {noc} from '@data/page-data-figma/page-data-legalRepresentative/noc.page.data';


test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeGenAppsExecutor(page);
  defendantDetails.length = 0;
  FieldsStore.clear();
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  await performAction('getDefendantDetails', {
    defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
    additionalDefendants: submitCaseApiData.submitCasePayload.addAnotherDefendant,
    payLoad: submitCaseApiData.submitCasePayload
  });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.defendantSolicitor);
  await dismissCookieBanner(page, 'analytics');
  await performAction('clickTab', home.noticeOfChangeTab);
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});

test.describe('Make an Application - LR - e2e Journey @nightly', async () => {
  test('Notice of change - LR @regression @smoke', async () => {
    await performAction('inputText', noc.onlineCaseReferenceNumberTextLabel, caseInfo.id);
    await performAction('clickButton', noc.continueButton);
  });
});
