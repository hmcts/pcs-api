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
import {
  noticeOfChangeSuccessful
} from "@data/page-data-figma/page-data-legalRepresentative/noticeOfChangeSuccessful.page.data";

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

test.describe('Legal Representative NOC - e2e Journey @nightly', async () => {
  test('Notice of change - Change link - Same Org LR submits another NOC - LR @noticeOfChange', async ( { page }) => {
    await performAction('noticeOfChange', { caseRefNo: caseInfo.id } );
    await performAction('clientDetails', { firstName: 'Peter' , lastName: 'Parker' });
    await performAction('verifyChangeLink', { caseRefNo: caseInfo.id, firstName: 'Peter' , lastName: 'Parker' });
    await performAction('noticeOfChange', { caseRefNo: caseInfo.id } );
    await performAction('clientDetails', { firstName: 'Jen' , lastName: 'Parker' });
    await performAction('checkAndSubmit', { caseRefNo: caseInfo.id, firstName: 'Jen' , lastName: 'Parker' });
    await performAction('clickTab', home.noticeOfChangeTab);
    await performAction('noticeOfChange', { caseRefNo: caseInfo.id } );
    await page.waitForTimeout(10000);
    await performAction('clientDetails', { firstName: 'Jen' , lastName: 'Parker' });
    await page.waitForTimeout(10000);
    await performAction('validateErrorPage' );
  });

  test('Notice of change - successful - LR - @regression @smoke @noticeOfChange', async () => {
    await performAction('noticeOfChange', { caseRefNo: caseInfo.id } );
    await performAction('clientDetails', { firstName: 'Peter' , lastName: 'Parker' });
    await performAction('checkAndSubmit', { caseRefNo: caseInfo.id, firstName: 'Peter' , lastName: 'Parker' } );
    await performAction('noticeOfChangeSuccessful', { caseRefNo: caseInfo.fid } );
  });

  test('Notice of change - Error message validations - LR @noticeOfChange', async () => {
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

  test('Notice of change - Content Validation - LR @noticeOfChange' , async ({ page }) => {
    await performValidation('mainHeader', noc.mainHeader );
    await performValidation('text', { elementType: 'paragraph', text: noc.youCanUseThisNoticeParagraph });
    await performValidation('text', { elementType: 'listItem', text: noc.aClientActingInPersonListItem });
    await performValidation('text', { elementType: 'listItem', text: noc.aLegalRepresentativeListItem });
    await performValidation('text', { elementType: 'hintText', text: noc.thisIsHintText });
    await performAction('noticeOfChange', { caseRefNo: caseInfo.id } );
    await page.waitForTimeout(10000);

    await performValidation('mainHeader', clientDetails.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: clientDetails.youMustEnterParagraph });
    await performAction('clientDetails', { firstName: 'Peter' , lastName: 'Parker' });
    await page.waitForTimeout(10000);

    await performValidation('mainHeader', checkAndSubmit.mainHeader );
    await performValidation('text', { elementType: 'paragraph', text: checkAndSubmit.afterYouSubmitParagraph });
    await performValidation('text', { elementType: 'tableElement', text: checkAndSubmit.requestTableElement });
    await performValidation('text', { elementType: 'tableElement', text: checkAndSubmit.caseNumberTableElement });
    await performValidation('text', { elementType: 'tableElement', text: checkAndSubmit.clientFirstNameTableElement });
    await performValidation('text', { elementType: 'tableElement', text: checkAndSubmit.clientLastNameTableElement });
    await performValidation('text', { elementType: 'subHeading', text: checkAndSubmit.notificationsHeader });
    await performValidation('text', { elementType: 'paragraph', text: checkAndSubmit.ifTheClientParagraph });
    await performValidation('text', { elementType: 'tableElement', text: checkAndSubmit.afterYouSubmitParagraph });
    await performAction('checkAndSubmit', { caseRefNo: caseInfo.id, firstName: 'Peter' , lastName: 'Parker' } );

    await performAction('noticeOfChangeSuccessful', { caseRefNo: caseInfo.fid } );
    await performValidation('text', { elementType: 'subHeading', text: noticeOfChangeSuccessful.whatHappensNextHeading });
    await performValidation('text', { elementType: 'paragraph', text: noticeOfChangeSuccessful.ifTheClientHadParagraph });
    await performValidation('text', { elementType: 'paragraph', text: noticeOfChangeSuccessful.thisCaseWillNowParagraph });
    await performValidation('text', { elementType: 'warningText',text: noticeOfChangeSuccessful.youShouldNowAmendWarningText });
    await performValidation('text', { elementType: 'paragraph', text: noticeOfChangeSuccessful.youMustNowInformParagraph });
    await performValidation('text', { elementType: 'paragraph', text: noticeOfChangeSuccessful.thisIsNewOnlineProcessParagraph });
    await performValidation('text', { elementType: 'link', text: noticeOfChangeSuccessful.viewCaseListLink });
    await performValidation('text', { elementType: 'link', text: noticeOfChangeSuccessful.viewThisCaseLink });
  });
});
