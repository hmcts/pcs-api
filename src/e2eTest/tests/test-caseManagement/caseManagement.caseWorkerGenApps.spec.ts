import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test from '@playwright/test';
import { caseInfo, defendantUserDetails } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary, home, user } from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { initializeCMExecutor, performAction, performValidation } from '@utils/controller-caseManagement';
import { allPartyDetails } from '@utils/actions/custom-actions/custom-actions-caseManagement';

import {
  enterGenappApplication,
  enterGenAppapplicationFee,
  enterGenAppConsentAndNotice,
  enterGenAppHearingDate, enterGenAppPreferApplicationToJudge, enterGenAppUploadGeneralApplication,
  enterGenAppUploadRelatedEvidence, genAppsCheckYourAnswers
} from '@data/page-data-figma/page-data-caseManagement-figma';
import { label } from 'allure-js-commons';
import { CaseManagementCommonUtils } from '@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action';

test.use({ storageState: undefined })

let submitPayload: Record<string, any>;

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeCMExecutor(page);
  allPartyDetails.length = 0;
  submitPayload = submitCaseApiData.submitCasePayloadCaseFileView();
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitPayload });
  await performAction('getAddressInfo', { data: createCaseApiData.createCasePayload });
  await performAction('updatePaymentAPI');
  await performAction('getCaseAPI', 'Link Solicitor');
  await performAction('getAllPartyDetails', {
    defendant1NameKnown: submitPayload.defendant1.nameKnown,
    additionalDefendants: submitPayload.addAnotherDefendant,
    payLoad: submitPayload
  });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.hearingCenterAdmin);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToSummaryPage');
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});

test.describe('Case management - As a Case Worker i want to test as all Four General Application Journey @nightly', async () => {
  test('Case management - As a Case Worker I want to Enter a General application Details to ADJOURN when Application Fee Received is set to YES @CM @regression', async () => {
    let fileName = enterGenAppUploadGeneralApplication.uploadDocHiddenOption[3];
    let date = CaseManagementCommonUtils.getRandomDate(enterGenappApplication.dateTypeHiddenUserInput);
    await performAction('selectAnEvent', { eventType: caseSummary.enterAGenApp });
    await performValidation('mainHeader', enterGenappApplication.mainHeader);
    await performAction('errorValidationEnterGeneralAppPage', enterGenappApplication.errorValidation);
    await performAction('enterApplicationDetails', {
      question1: enterGenappApplication.whichPartyMadeAppQuestion, option1: allPartyDetails[0],
      question2: enterGenappApplication.typeOfAppQuestion, option2: enterGenappApplication.adjournRadioOption,
      label: enterGenappApplication.whichCategoriesHiddenTextLabel,
      input: enterGenappApplication.whichCategoriesHiddenTextInput,
      label1: enterGenappApplication.whatDateAppReceivedQuestion,
      date: date,
      dateType: enterGenappApplication.dateTypeHiddenUserInput,
      submitPayload: submitPayload,
      nextPage: enterGenAppHearingDate.mainHeader
    })
    await performAction('errorValidationHearingDatePage', enterGenAppHearingDate.errorValidation);
    await performAction('confirmIfCourtHearingInNext14Days', {
      question: enterGenAppHearingDate.hearingInNext14DaysQuestion,
      option: enterGenAppHearingDate.yesRadioOption,
      submitPayload: submitPayload,
      nextPage: enterGenAppapplicationFee.mainHeader
    });
    await performAction('errorValidationApplicationFeePage', enterGenappApplication.errorValidation);
    await performAction('enterApplicationFeeDetails', {
      question1: enterGenAppapplicationFee.appFeeReceivedQuestion, option1: enterGenAppapplicationFee.yesRadioOption,
      question2: enterGenAppapplicationFee.referenceNumberIncludedQuestion, option2: enterGenAppapplicationFee.yesRadioOption,
      label1: enterGenAppapplicationFee.enterTheAmountReceivedHiddenTextLabel,
      label2: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextLabel,
      input: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextInput,
      submitPayload: submitPayload,
      nextPage: enterGenAppConsentAndNotice.mainHeader
    });
    await performAction('errorValidationApplicationConsentAndNotice', enterGenAppConsentAndNotice.errorValidation);
    await performAction('enterApplicationConsentAndNotice', {
      question1: enterGenAppConsentAndNotice.doAllPartiesAgreedQuestion, option1: enterGenAppConsentAndNotice.noRadioOption,
      question2: enterGenAppConsentAndNotice.hasApplicantMadeWithoutNoticeHiddenQuestion, option2: enterGenAppConsentAndNotice.yesHiddenRadioOption,
      submitPayload: submitPayload,
      nextPage: enterGenAppUploadGeneralApplication.mainHeader
    });
    await performAction('errorValidationUploadGenAppsFile', enterGenAppUploadGeneralApplication.errorValidation);
    await performAction('uploadADocument', {
      label: enterGenAppUploadGeneralApplication.uploadADocumentParagraph,
      file: fileName,
      submitPayload: submitPayload,
    });
    await performAction('reTryOnCallBackError', enterGenAppUploadGeneralApplication.continueButton, enterGenAppUploadRelatedEvidence.mainHeader);
    await performAction('uploadRelativeEvidence', {
      label: enterGenAppUploadRelatedEvidence.uploadADocumentHiddenTextLabel,
      files: enterGenAppUploadRelatedEvidence.uploadDocHiddenOption,
      submitPayload: submitPayload,
      nextPage: enterGenAppPreferApplicationToJudge.mainHeader
    });
    await performAction('verifyReferToJudge', {
      submitPayload: submitPayload,
      nextPage: genAppsCheckYourAnswers.mainHeader
    });
    await performAction('clickButton', genAppsCheckYourAnswers.submitButton);
    await performAction('verifyGenAppConfirm', { submitPayload: submitPayload });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Enter a general application');
  });


  test('Case management - As a Case Worker I want to Enter a General application Details to ADJOURN when Application Fee Received is set to NO @CM', async () => {
    let date = CaseManagementCommonUtils.getRandomDate(enterGenappApplication.dateTypeHiddenUserInput);
    await performAction('selectAnEvent', { eventType: caseSummary.enterAGenApp });
    await performValidation('mainHeader', enterGenappApplication.mainHeader);
    await performAction('enterApplicationDetails', {
      question1: enterGenappApplication.whichPartyMadeAppQuestion, option1: allPartyDetails[0],
      question2: enterGenappApplication.typeOfAppQuestion, option2: enterGenappApplication.adjournRadioOption,
      label: enterGenappApplication.whichCategoriesHiddenTextLabel,
      input: enterGenappApplication.whichCategoriesHiddenTextInput,
      label1: enterGenappApplication.whatDateAppReceivedQuestion,
      date: date,
      dateType: enterGenappApplication.dateTypeHiddenUserInput,
      submitPayload: submitPayload,
      nextPage: enterGenAppHearingDate.mainHeader
    });
    await performAction('confirmIfCourtHearingInNext14Days', {
      question: enterGenAppHearingDate.hearingInNext14DaysQuestion,
      option: enterGenAppHearingDate.yesRadioOption,
      submitPayload: submitPayload,
      nextPage: enterGenAppapplicationFee.mainHeader
    });
    await performAction('enterApplicationFeeDetails', {
      question1: enterGenAppapplicationFee.appFeeReceivedQuestion, option1: enterGenAppapplicationFee.noRadioOption,
      question2: enterGenAppapplicationFee.referenceNumberIncludedQuestion, option2: enterGenAppapplicationFee.yesRadioOption,
      label1: enterGenAppapplicationFee.enterTheAmountReceivedHiddenTextLabel,
      label2: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextLabel,
      input: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextInput,
      submitPayload: submitPayload,
      nextPage: enterGenAppConsentAndNotice.mainHeader
    });
  });

  test('Case management - As a Case Worker I want to Enter a General application Details to ADJOURN Journey with Fee Reference included - NO @CM', async () => {
    let fileName = enterGenAppUploadGeneralApplication.uploadDocHiddenOption[3];
    let date = CaseManagementCommonUtils.getRandomDate(enterGenappApplication.dateTypeHiddenUserInput);
    await performAction('selectAnEvent', { eventType: caseSummary.enterAGenApp });
    await performValidation('mainHeader', enterGenappApplication.mainHeader);
    await performAction('enterApplicationDetails', {
      question1: enterGenappApplication.whichPartyMadeAppQuestion, option1: allPartyDetails[0],
      question2: enterGenappApplication.typeOfAppQuestion, option2: enterGenappApplication.adjournRadioOption,
      label: enterGenappApplication.whichCategoriesHiddenTextLabel,
      input: enterGenappApplication.whichCategoriesHiddenTextInput,
      label1: enterGenappApplication.whatDateAppReceivedQuestion,
      date: date,
      dateType: enterGenappApplication.dateTypeHiddenUserInput,
      submitPayload: submitPayload,
      nextPage: enterGenAppHearingDate.mainHeader
    });
    await performAction('confirmIfCourtHearingInNext14Days', {
      question: enterGenAppHearingDate.hearingInNext14DaysQuestion,
      option: enterGenAppHearingDate.yesRadioOption,
      submitPayload: submitPayload,
      nextPage: enterGenAppapplicationFee.mainHeader
    });
    await performAction('enterApplicationFeeDetails', {
      question1: enterGenAppapplicationFee.appFeeReceivedQuestion, option1: enterGenAppapplicationFee.yesRadioOption,
      question2: enterGenAppapplicationFee.referenceNumberIncludedQuestion, option2: enterGenAppapplicationFee.noRadioOption,
      label1: enterGenAppapplicationFee.enterTheAmountReceivedHiddenTextLabel,
      label2: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextLabel,
      input: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextInput,
      submitPayload: submitPayload,
      nextPage: enterGenAppConsentAndNotice.mainHeader
    });
    await performAction('enterApplicationConsentAndNotice', {
      question1: enterGenAppConsentAndNotice.doAllPartiesAgreedQuestion, option1: enterGenAppConsentAndNotice.yesRadioOption,
      submitPayload: submitPayload,
      nextPage: enterGenAppUploadGeneralApplication.mainHeader
    });
    await performAction('uploadADocument', {
      submitPayload: submitPayload,
      label: enterGenAppUploadGeneralApplication.uploadADocumentParagraph,
      file: fileName
    });
    await performAction('reTryOnCallBackError', enterGenAppUploadGeneralApplication.continueButton, enterGenAppUploadRelatedEvidence.mainHeader);
    await performAction('uploadRelativeEvidence', {
      label: enterGenAppUploadRelatedEvidence.uploadADocumentHiddenTextLabel,
      submitPayload: submitPayload,
      nextPage: genAppsCheckYourAnswers.mainHeader
    });
    await performAction('clickButton', genAppsCheckYourAnswers.submitButton);
    await performAction('verifyGenAppConfirm', { submitPayload: submitPayload });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Enter a general application');
  });

  test('Case management - As a Case Worker I want to Enter a General application Details to SET ASIDE @CM', async () => {
    let fileName = enterGenAppUploadGeneralApplication.uploadDocHiddenOption[1];
    let date = CaseManagementCommonUtils.getRandomDate(enterGenappApplication.dateTypeHiddenUserInput);
    await performAction('selectAnEvent', { eventType: caseSummary.enterAGenApp });
    await performValidation('mainHeader', enterGenappApplication.mainHeader);
    await performAction('enterApplicationDetails', {
      question1: enterGenappApplication.whichPartyMadeAppQuestion, option1: allPartyDetails[1],
      question2: enterGenappApplication.typeOfAppQuestion, option2: enterGenappApplication.setAsideRadioOption,
      label: enterGenappApplication.whichCategoriesHiddenTextLabel,
      input: enterGenappApplication.whichCategoriesHiddenTextInput,
      label1: enterGenappApplication.whatDateAppReceivedQuestion,
      date: date,
      dateType: enterGenappApplication.dateTypeHiddenUserInput,
      submitPayload: submitPayload,
      nextPage: enterGenAppapplicationFee.mainHeader
    })
    await performAction('enterApplicationFeeDetails', {
      question1: enterGenAppapplicationFee.appFeeReceivedQuestion, option1: enterGenAppapplicationFee.yesRadioOption,
      question2: enterGenAppapplicationFee.referenceNumberIncludedQuestion, option2: enterGenAppapplicationFee.yesRadioOption,
      label1: enterGenAppapplicationFee.enterTheAmountReceivedHiddenTextLabel,
      label2: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextLabel,
      input: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextInput,
      submitPayload: submitPayload,
      nextPage: enterGenAppConsentAndNotice.mainHeader
    });
    await performAction('enterApplicationConsentAndNotice', {
      question1: enterGenAppConsentAndNotice.doAllPartiesAgreedQuestion, option1: enterGenAppConsentAndNotice.noRadioOption,
      question2: enterGenAppConsentAndNotice.hasApplicantMadeWithoutNoticeHiddenQuestion, option2: enterGenAppConsentAndNotice.yesHiddenRadioOption,
      submitPayload: submitPayload,
      nextPage: enterGenAppUploadGeneralApplication.mainHeader
    });
    await performAction('uploadADocument', {
      submitPayload: submitPayload,
      label: enterGenAppUploadGeneralApplication.uploadADocumentParagraph,
      file: fileName
    });
    await performAction('reTryOnCallBackError', enterGenAppUploadGeneralApplication.continueButton, enterGenAppUploadRelatedEvidence.mainHeader);
    await performAction('uploadRelativeEvidence', {
      label: enterGenAppUploadRelatedEvidence.uploadADocumentHiddenTextLabel,
      submitPayload: submitPayload,
      nextPage: enterGenAppPreferApplicationToJudge.mainHeader
    });
    await performAction('verifyReferToJudge', {
      submitPayload: submitPayload,
      nextPage: genAppsCheckYourAnswers.mainHeader
    });
    await performAction('clickButton', genAppsCheckYourAnswers.submitButton);
    await performAction('verifyGenAppConfirm', { submitPayload: submitPayload });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Enter a general application');
  });

  test('Case management - As a Case Worker I want to Enter a General application Details to SOMETHING ELSE @CM', async () => {
    let fileName = enterGenAppUploadGeneralApplication.uploadDocHiddenOption[2];
    let date = CaseManagementCommonUtils.getRandomDate(enterGenappApplication.dateTypeHiddenUserInput);
    await performAction('selectAnEvent', { eventType: caseSummary.enterAGenApp });
    await performValidation('mainHeader', enterGenappApplication.mainHeader);
    await performAction('enterApplicationDetails', {
      question1: enterGenappApplication.whichPartyMadeAppQuestion, option1: allPartyDetails[2],
      question2: enterGenappApplication.typeOfAppQuestion, option2: enterGenappApplication.somethingElseRadioOption,
      label: enterGenappApplication.whichCategoriesHiddenTextLabel,
      input: enterGenappApplication.whichCategoriesHiddenTextInput,
      label1: enterGenappApplication.whatDateAppReceivedQuestion,
      date: date,
      dateType: enterGenappApplication.dateTypeHiddenUserInput,
      submitPayload: submitPayload,
      nextPage: enterGenAppapplicationFee.mainHeader
    })
    await performAction('enterApplicationFeeDetails', {
      question1: enterGenAppapplicationFee.appFeeReceivedQuestion, option1: enterGenAppapplicationFee.yesRadioOption,
      question2: enterGenAppapplicationFee.referenceNumberIncludedQuestion, option2: enterGenAppapplicationFee.yesRadioOption,
      label1: enterGenAppapplicationFee.enterTheAmountReceivedHiddenTextLabel,
      label2: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextLabel,
      input: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextInput,
      submitPayload: submitPayload,
      nextPage: enterGenAppConsentAndNotice.mainHeader
    });
    await performAction('enterApplicationConsentAndNotice', {
      question1: enterGenAppConsentAndNotice.doAllPartiesAgreedQuestion, option1: enterGenAppConsentAndNotice.noRadioOption,
      question2: enterGenAppConsentAndNotice.hasApplicantMadeWithoutNoticeHiddenQuestion, option2: enterGenAppConsentAndNotice.yesHiddenRadioOption,
      submitPayload: submitPayload,
      nextPage: enterGenAppUploadGeneralApplication.mainHeader
    });
    await performAction('uploadADocument', {
      submitPayload: submitPayload,
      label: enterGenAppUploadGeneralApplication.uploadADocumentParagraph,
      file: fileName
    });
    await performAction('reTryOnCallBackError', enterGenAppUploadGeneralApplication.continueButton, enterGenAppUploadRelatedEvidence.mainHeader);
    await performAction('uploadRelativeEvidence', {
      label: enterGenAppUploadRelatedEvidence.uploadADocumentHiddenTextLabel,
      submitPayload: submitPayload,
      nextPage: enterGenAppPreferApplicationToJudge.mainHeader
    });
    await performAction('verifyReferToJudge', {
      submitPayload: submitPayload,
      nextPage: genAppsCheckYourAnswers.mainHeader
    });
    await performAction('clickButton', genAppsCheckYourAnswers.submitButton);
    await performAction('verifyGenAppConfirm', { submitPayload: submitPayload });
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Enter a general application');
  });

  test('Case management - As a Case Worker I want to Enter a General application Details to SUSPEND',
    {
      tag: ['@CM'],
      annotation: [
        { type: 'jira', description: `The corresponding jira ticket : https://hmcts.atlassian.net/browse/HDPI-8872` },
        { type: 'ac', description: 'AC1 to AC3' },
      ],
    }, async () => {
      let uploadFileName = enterGenAppUploadGeneralApplication.uploadDocHiddenOption[2];
      let fileName = (enterGenAppUploadGeneralApplication.uploadDocHiddenOption)[4].replace(/\s+-\s+(?:Claimant|Defendant)\s+\d+(?=\.[^.]+$|$)/i, '')
      let date = CaseManagementCommonUtils.getRandomDate(enterGenappApplication.dateTypeHiddenUserInput);
      let appType = CaseManagementCommonUtils.getGenApplicationType(defendantUserDetails.length)[0];
      let party = allPartyDetails[0];
      const partyType = party.split(' - ')[1].split(' ')[0];
      const partyIndex = Number(party.split(' - ')[1].split(' ')[1]);

      await test.step(`Given the user has selected the Enter a general application event`, async () => {
        await performAction('selectAnEvent', { eventType: caseSummary.enterAGenApp });
        await performValidation('mainHeader', enterGenappApplication.mainHeader);
      });

      await test.step(`When the user enters the application details related to Suspend application`, async () => {
        await performAction('enterApplicationDetails', {
          question1: enterGenappApplication.whichPartyMadeAppQuestion, option1: allPartyDetails[0],
          question2: enterGenappApplication.typeOfAppQuestion, option2: enterGenappApplication.suspendRadioOption,
          label: enterGenappApplication.whichCategoriesHiddenTextLabel,
          input: enterGenappApplication.whichCategoriesHiddenTextInput,
          label1: enterGenappApplication.whatDateAppReceivedQuestion,
          date: date,
          dateType: enterGenappApplication.dateTypeHiddenUserInput,
          submitPayload: submitPayload,
          nextPage: enterGenAppapplicationFee.mainHeader
        });
      });

      await test.step(`And the user provides information about the application fee `, async () => {
        await performAction('enterApplicationFeeDetails', {
          question1: enterGenAppapplicationFee.appFeeReceivedQuestion, option1: enterGenAppapplicationFee.yesRadioOption,
          question2: enterGenAppapplicationFee.referenceNumberIncludedQuestion, option2: enterGenAppapplicationFee.yesRadioOption,
          label1: enterGenAppapplicationFee.enterTheAmountReceivedHiddenTextLabel,
          label2: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextLabel,
          input: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextInput,
          submitPayload: submitPayload,
          nextPage: enterGenAppUploadGeneralApplication.mainHeader
        });
      });

      await test.step(`And the user uploads a document via Upload general application page`, async () => {
        await performAction('uploadADocument', {
          submitPayload: submitPayload,
          label: enterGenAppUploadGeneralApplication.uploadADocumentParagraph,
          file: uploadFileName
        });
        await performAction('reTryOnCallBackError', enterGenAppUploadGeneralApplication.continueButton, enterGenAppUploadRelatedEvidence.mainHeader);
      });

      await test.step(`And the user chooses not to upload additional supporting evidence and submits`, async () => {
        await performAction('uploadRelativeEvidence', {
          label: enterGenAppUploadRelatedEvidence.uploadADocumentHiddenTextLabel,
          submitPayload: submitPayload,
          nextPage: genAppsCheckYourAnswers.mainHeader
        });
        await performAction('clickButton', genAppsCheckYourAnswers.submitButton);
      });

      await test.step(`Then the confirmation page displays the application details`, async () => {
        await performAction('verifyGenAppConfirm', { submitPayload: submitPayload });
        await performValidation('bannerAlert', 'Case #.* has been updated with event: Enter a general application');
      });

      await test.step(`And the uploaded document is stored in Applications folder in Case File View `, async () => {
        await performAction('clickTab', home.caseFileView);
        await performAction('validateCaseFileViewFolders', home.caseFileFolders);
        await performAction('validateCaseFileViewIndividualFolder', {
          folder: 'Applications',
          caseWorkerUpload: CaseManagementCommonUtils.renameDocument(fileName, '', appType,`${partyType} ${partyIndex}`)
        });
      });
  });
});
