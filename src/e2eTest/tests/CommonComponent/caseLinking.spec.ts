import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { expect, test } from '@utils/test-fixtures';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { caseSummary, user } from '@data/page-data';
import { beforeYouStart } from '@data/page-data/beforeYouStart.page.data';
import { selectCasesToLink } from '@data/page-data/selectCaseToLink.page.data';
import { selectCasesToUnLink } from '@data/page-data/selectCasesToUnLink.page.data';
import { checkYourAnswersCaseLinking } from '@data/page-data/checkYourAnswersCaseLinking.page.data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { CaseLinking } from '@utils/actions/custom-actions/commonComponent/caseLinking.action';
import { staff } from '@data/user-data/staff.user.data';
import { judicial } from '@data/user-data/judicial.user.data';

let caseNumbers: string[] = [];

test.beforeEach(async ({ page, context }) => {
   await context.clearCookies();
  initializeExecutor(page);
  await performAction('createCaseAPI', {data: createCaseApiData.createCasePayload});
  await performAction('submitCaseAPI', {data: submitCaseApiData.submitCasePayload});
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');

});

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
});

//Case Linking is not working in preview env as explained in https://tools.hmcts.net/jira/browse/HDPI-6095
//So these tests won't be executed in preview
test.describe('[Common Component Case Linking] @nightly @caseLinking', async () => {
  test('Case Linking', async ({page}) => {
    await performAction('login', {email: staff.pcs_ctsc_admin_email, password: process.env.IDAM_PCS_USER_PASSWORD});
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToCaseSummary', 'yes');
    await performAction('select', caseSummary.nextStepEventList, caseSummary.linkCaseEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', beforeYouStart.mainHeader);
    await performAction('clickButton', beforeYouStart.submitButton);
    await performValidation('mainHeader', selectCasesToLink.mainHeader);
    const caseLinking = new CaseLinking();
    caseNumbers = await caseLinking.createCases(5);
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


test.describe('[Common Component Case Linking - Access Management] @nightly @caseLinking', async () => {
  test.describe.configure({ retries: 0 });
  test('CTSC Admin users can create, manage and view case linking  @CC @caseLinking', async ({page, context}) => {
     await performAction('login', {email: staff.pcs_ctsc_team_leader_email, password: process.env.IDAM_PCS_USER_PASSWORD});
     await dismissCookieBanner(page, 'analytics');
     await performAction('navigateToCaseSummary', 'yes');
     await performAction('canLinkCases', 'yes');
     await performAction('canManageCases', 'yes');
     await performAction('canViewLinkedCases', 'yes');
  });

  test('Judicial users can create, manage and view case linking  @CC @caseLinking', async ({page, context}) => {
     await performAction('login', {email: judicial.possessionFeePaid_Judge_email, password: process.env.IDAM_PCS_USER_PASSWORD});
     await dismissCookieBanner(page, 'analytics');
     await performAction('handleJudgeBookingPage');
     await performAction('navigateToCaseSummary', 'yes');
     await performAction('canLinkCases', 'yes');
     await performAction('canManageCases', 'yes');
     await performAction('canViewLinkedCases', 'yes');
  });

  test('Solicitor user cannot view, create or manage case linking @CC @nightly @caseLinking', async ({ page, context }) => {
    const { email, password } = user.claimantSolicitor;
    await performAction('login', { email, password });
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToCaseSummary', 'yes');
    await performAction('canLinkCases', 'no');
    await performAction('canManageCases', 'no');
    await performAction('canViewLinkedCases', 'no');
  });

  test('WLU user cannot view, create or manage case linking @CC @nightly @caseLinking', async ({ page, context }) => {
    await performAction('login', {email: staff.pcs_wlu_administrator_email, password: process.env.IDAM_PCS_USER_PASSWORD});
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToCaseSummary', 'yes');
    await performAction('canLinkCases', 'no');
    await performAction('canManageCases', 'no');
    await performAction('canViewLinkedCases', 'no');
  });

  test('WLUTeamLead user cannot view, create or manage case linking @CC @nightly @caseLinking', async ({ page, context }) => {
   await performAction('login', {email: staff.pcs_wlu_team_leader_ca_email, password: process.env.IDAM_PCS_USER_PASSWORD});
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToCaseSummary', 'yes');
    await performAction('canLinkCases', 'no');
    await performAction('canManageCases', 'no');
    await performAction('canViewLinkedCases', 'no');
  });

});

