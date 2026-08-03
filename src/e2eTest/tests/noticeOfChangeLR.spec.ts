import { createCaseApiData, submitCaseApiData } from '@data/api-data';


import {initializeExecutor, performAction, performValidation,} from '@utils/controller';
import test from '@playwright/test';
import { FieldsStore } from '@utils/actions/custom-actions/custom-actions-genApps/recordAnsweredFields.action';
import { user } from '@data/user-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { home } from '@data/page-data';
import {noc} from "@data/page-data-figma/page-data-legalRepresentative/noc.page.data";
import {clientDetails} from "@data/page-data-figma/page-data-legalRepresentative/clientDetails.page.data";
import {checkAndSubmit} from "@data/page-data-figma/page-data-legalRepresentative/checkAndSubmit.page.data";


test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  FieldsStore.clear();
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
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
  test('Notice of change - Change link - Same Org LR submits another NOC - LR @nightly', async () => {
    await performAction('noticeOfChange', { caseRefNo: caseInfo.id } );
    await performAction('clientDetails', { firstName: 'Peter' , lastName: 'Parker' });
    await performAction('verifyChangeLink', { caseRefNo: caseInfo.id, firstName: 'Peter' , lastName: 'Parker' });
    await performAction('noticeOfChange', { caseRefNo: caseInfo.id } );
    await performAction('clientDetails', { firstName: 'Jen' , lastName: 'Parker' });
    await performAction('checkAndSubmit' );
    await performAction('clickTab', home.noticeOfChangeTab);
    await performAction('noticeOfChange', { caseRefNo: caseInfo.id } );
    await performAction('clientDetails', { firstName: 'Jen' , lastName: 'Parker' });
    await performAction('validateErrorPage' );
  });

  test('Notice of change - successful - LR - @regression @smoke', async () => {
    await performAction('noticeOfChange', { caseRefNo: caseInfo.id } );
    await performAction('clientDetails', { firstName: 'Peter' , lastName: 'Parker' });
    await performAction('checkAndSubmit', { caseRefNo: caseInfo.id, firstName: 'Peter' , lastName: 'Parker' } );
    await performAction('noticeOfChangeSuccessful', { caseRefNo: caseInfo.fid } );
  });

  test('Notice of change - Error message validations - LR @nightly', async () => {
    await performAction('clickButton', noc.continueButton);
    await performValidation('text', { elementType: 'link', text: noc.errMessage });
    await performAction('noticeOfChange', { caseRefNo: '1111-2222-3333-4444-5555' } );
    await performValidation('text', { elementType: 'link', text: noc.errMessage });
    await performAction('noticeOfChange', { caseRefNo: caseInfo.id } );

    await performAction('clientDetails', { firstName: '' , lastName: '' });
    await performValidation('text', { elementType: 'inlineText', text: clientDetails.clientDetailsErrorMessage });


    await performAction('clientDetails', { firstName: 'Test' , lastName: '' });
    await performValidation('text', { elementType: 'inlineText', text: clientDetails.clientDetailsErrorMessage });

    await performAction('clientDetails', { firstName: '' , lastName: 'Invalid' });
    await performValidation('text', { elementType: 'inlineText', text: clientDetails.clientDetailsErrorMessage });

    await performAction('clientDetails', { firstName: 'Test' , lastName: 'Invalid' });
    await performValidation('text', { elementType: 'inlineText', text: clientDetails.clientDetailsErrorMessage });

    await performAction('clientDetails', { firstName: 'Peter' , lastName: 'Parker' });

    await performAction('clickButton', checkAndSubmit.submitButton);
    await performValidation('text', { elementType: 'link', text: checkAndSubmit.tickTheBoxErrorMessage });
    await performValidation('text', { elementType: 'link', text: checkAndSubmit.tickTheBoxConfirmDetailsErrorMessage });

    await performAction('check', checkAndSubmit.iConfirmCheckbox);
    await performAction('clickButton', checkAndSubmit.submitButton);
    await performValidation('text', { elementType: 'link', text: checkAndSubmit.tickTheBoxConfirmDetailsErrorMessage });

    await performAction('uncheck', checkAndSubmit.iConfirmCheckbox);
    await performAction('check', checkAndSubmit.iHaveServedCheckbox);
    await performAction('clickButton', checkAndSubmit.submitButton);
    await performValidation('text', { elementType: 'link', text: checkAndSubmit.tickTheBoxErrorMessage });
  });
});
