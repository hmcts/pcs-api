
import { expect, Page } from '@playwright/test';
import { IAction, actionData, actionRecord } from '@utils/interfaces';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { performAction, performValidation } from '@utils/controller-caseManagement';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { caseSummary, home } from '@data/page-data';
import {
  addReviewDates,
  confirmReviewDatesAdded,
  cancelHearing,
  changeCaseState,
  confirmCaseStateChange,
  editHearing,
  enterGenappApplication,
  enterGenAppapplicationFee,
  enterGenAppConfirmation,
  enterGenAppConsentAndNotice,
  enterGenAppHearingDate,
  enterGenAppPreferApplicationToJudge,
  manageHearing,
  selectDocument,
  confirmEditHearing, confirmAmend, confirmUpload,
  uploadADocument,
  addHearing,
  confirmHearing,
  confirmCancelHearing
} from '@data/page-data-figma/page-data-caseManagement-figma';
import { caseInfo } from '../createCaseAPI.action';
import { CaseManagementCommonUtils } from './caseManagementUtils.action';
import path from "path";
import {performActions} from "@utils/controller";
import {generateRandomString} from "@utils/common/string.utils";

export let addressInfo: { buildingStreet: string; addressLine2: string; townCity: string; engOrWalPostcode: string; };
export let allPartyDetails: string[] = [];

export class CaseManagementAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['navigateToSummaryPage', () => this.navigateToSummaryPage(page)],
      ['selectAnEvent', () => this.selectAnEvent(fieldName as actionRecord)],
      ['selectDocumentToAmend', () => this.selectDocumentToAmend(fieldName as actionRecord)],
      ['addReviewDates', () => this.addReviewDates(fieldName as actionRecord)],
      ['confirmReviewDatesAdded', () => this.confirmReviewDatesAdded()],
      ['changeCaseState', () => this.changeCaseState(fieldName as actionRecord)],
      ['confirmCaseStateChange', () => this.confirmCaseStateChange()],
      ['getAllPartyDetails', () => this.getAllPartyDetails(fieldName as actionRecord)],
      ['enterApplicationDetails', () => this.enterApplicationDetails(fieldName as actionRecord)],
      ['confirmIfCourtHearingInNext14Days', () => this.confirmIfCourtHearingInNext14Days(fieldName as actionRecord)],
      ['enterApplicationFeeDetails', () => this.enterApplicationFeeDetails(fieldName as actionRecord)],
      ['selectDynamicAppAndPartyDocRelatedTo', () => this.selectDynamicAppAndPartyDocRelatedTo(fieldName as actionRecord)],
      ['confirmUpload', () => this.confirmUpload(fieldName as actionRecord)],
      ['confirmAmend', () => this.confirmAmend(fieldName as actionRecord)],
      ['enterApplicationConsentAndNotice', () => this.enterApplicationConsentAndNotice(fieldName as actionRecord)],
      ['uploadRelativeEvidence',() => this.uploadRelativeEvidence(fieldName as actionRecord)],
      ['uploadADocument',() => this.uploadADocument(page, fieldName as actionRecord)],
      ['verifyReferToJudge', () => this.verifyReferToJudge(fieldName as actionRecord)],
      ['editHearing', () => this.editHearing(fieldName as actionRecord)],
      ['confirmHearingEdited', () => this.confirmHearingEdited(fieldName as actionRecord)],
      ['getAddressInfo', () => this.getAddressInfo(fieldName as actionRecord)],
      ['verifyGenAppConfirm', () => this.verifyGenAppConfirm()],
      ['addAHearing', () => this.addAHearing(fieldName as actionRecord)],
      ['confirmAddHearing', () => this.confirmAddHearing(fieldName as actionRecord)],
      ['selectManageHearing', () => this.selectManageHearing(fieldName as actionRecord)],
      ['cancelHearing', () => this.cancelHearing(fieldName as actionRecord)],
      ['confirmHearingCancelled', () => this.confirmHearingCancelled(fieldName as actionRecord)],
      ['inputErrorValidation', () => this.inputErrorValidation(page, fieldName as actionRecord)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) {
      throw new Error(`No action found for '${action}'`);
    }
    await actionToPerform();
  }

  private async navigateToSummaryPage(page: Page) {
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
    await expect(async () => {
      await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`, {waitUntil: 'domcontentloaded'});
    }).toPass({
      timeout: VERY_LONG_TIMEOUT,
    });
    await page.waitForLoadState();
    await page.locator('.spinner-container').waitFor({state: 'detached'});
    await performValidation('mainHeader', home.caseSummary);
  }

  private async selectAnEvent(event: actionRecord) {
    await performAction('select', caseSummary.nextStepEventList, event.eventType);
    await performAction('clickButton', caseSummary.go);
  }

  private async selectDocumentToAmend(selectDoc: actionRecord) {
    await performAction('select', selectDoc.question, selectDoc.option);
    await performAction('clickRadioButton', {question: selectDoc.question1, option: selectDoc.option1});
    await performAction('reTryOnCallBackError', selectDocument.continueButton, selectDoc.nextPage as string);
  }

  private async changeCaseState(caseState: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('select', caseState.question, caseState.option);
    await performAction('reTryOnCallBackError', changeCaseState.continueButton, caseState.nextPage as string);
  }

  private async confirmCaseStateChange(): Promise<void> {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('text', {elementType: 'inlineText', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'inlineText',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('mainHeader', confirmCaseStateChange.mainHeader);
    await performAction('clickButton', confirmCaseStateChange.closeAndReturnToCaseOverviewButton);
  }

  private async addReviewDates(reviewDateData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    const userInput =
      typeof reviewDateData.input === 'number'
        ? generateRandomString(reviewDateData.input)
        : (reviewDateData.input as string);
    let date = CaseManagementCommonUtils.getRandomDate(reviewDateData.dateType as string);
    await performActions('Date of Review',
      ['inputText', reviewDateData.day, date.split('/')[0]],
      ['inputText', reviewDateData.month, date.split('/')[1]],
      ['inputText', reviewDateData.year, date.split('/')[2]]);
    await performAction('clickRadioButton', {question: reviewDateData.question, option: reviewDateData.option});
    await performAction('inputText', reviewDateData.label, reviewDateData.userInput);
    await performAction('reTryOnCallBackError', addReviewDates.continueButton, reviewDateData.nextPage as string);
  }

  private async confirmReviewDatesAdded(): Promise<void> {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('text', {elementType: 'inlineText', text: confirmReviewDatesAdded.reviewDatesAdded});
    await performValidation('text', {elementType: 'inlineText', text: 'Case number #' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'inlineText',
      text: `${addressInfo.buildingStreet}, ${addressInfo.addressLine2}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('mainHeader', confirmReviewDatesAdded.mainHeader);
    await performAction('clickButton', confirmReviewDatesAdded.closeAndReturnToCaseOverviewButton);
  }


  private async getAllPartyDetails(allPartiesDetails: actionRecord) {

    let originalDefendantDetails: string[] = [];
    const payLoad = allPartiesDetails.payLoad as Record<string, any>;
    if (allPartiesDetails.defendant1NameKnown === 'YES') {
      originalDefendantDetails.push(
        `${payLoad.defendant1.firstName} ${payLoad.defendant1.lastName} - Defendant 1`
      );
    } else {
      originalDefendantDetails.push(
        `null null`
      );
    }

    if (allPartiesDetails.additionalDefendants === 'YES') {

      for (const [index, defendant] of payLoad.additionalDefendants.entries()) {
        if (defendant.value.nameKnown === 'YES') {
          originalDefendantDetails.push(`${defendant.value.firstName} ${defendant.value.lastName} - Defendant ${index + 2}`);
        } else {
          originalDefendantDetails.push(
            `null null`
          );
        }
      }
    }

    allPartyDetails = [...new Set(originalDefendantDetails.filter(n => n.trim().toLowerCase() !== "null null")),
      ...originalDefendantDetails.filter(n => n.trim().toLowerCase() === "null null")
    ];
    allPartyDetails.push(`${payLoad.claimantName} - Claimant 1`);
  }

  private async enterApplicationDetails(appDetails: actionRecord) {
    let date = CaseManagementCommonUtils.getRandomDate(appDetails.dateType as string);
    await performAction('clickRadioButton', { question: appDetails.question1, option: appDetails.option1 });
    await performAction('inputDate', appDetails.label1 as string, appDetails.date);
    await performAction('clickRadioButton', { question: appDetails.question2, option: appDetails.option2 });
    if (appDetails.option2 === 'Something else') {
      performAction('inputText', appDetails.label, CaseManagementCommonUtils.generateRandomString(appDetails.input as number))
    }
    await performAction('reTryOnCallBackError', enterGenappApplication.continueButton, appDetails.nextPage as string);
  }

  private async confirmIfCourtHearingInNext14Days(courtHearing: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('clickRadioButton', {
      question: courtHearing.question,
      option: courtHearing.option,
    });
    await performAction('reTryOnCallBackError', enterGenAppHearingDate.continueButton, courtHearing.nextPage as string);
  }

  private async enterApplicationFeeDetails(fee: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`});
    await performAction('clickRadioButton', {
      question: fee.question1,
      option: fee.option1,
    });
    if (fee.option1 === 'Yes') {
      await performAction('inputText', fee.label1, CaseManagementCommonUtils.getRandomNumberAsString(1, 500));
    }
    await performAction('clickRadioButton', {
      question: fee.question2,
      option: fee.option2,
    });

    if (fee.option2 === 'Yes') {
      await performAction('inputText', fee.label2, CaseManagementCommonUtils.generateRandomString(fee.input as number));
    }
    if (fee.option1 === 'No') {
      await performValidation('text', {
        elementType: 'paragraph',
        text: enterGenAppapplicationFee.yourMustRequestPaymentHiddenParagraph
      });
      await performAction('clickButton', enterGenAppapplicationFee.continueButton);
      await performValidation('errorMessage', { header: enterGenAppapplicationFee.eventCouldNotBeCreatedErrorMessageHeader, message: enterGenAppapplicationFee.yourMustRequestPaymentHiddenParagraph });

    } else {
      await performAction('reTryOnCallBackError', enterGenAppHearingDate.continueButton, fee.nextPage as string);
    }
  }

  private async enterApplicationConsentAndNotice(confirmApplicationConsent: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('clickRadioButton', {
      question: confirmApplicationConsent.question1,
      option: confirmApplicationConsent.option1,
    });
    if (confirmApplicationConsent.option1 === 'No') {
      await performAction('clickRadioButton', {
        question: confirmApplicationConsent.question2,
        option: confirmApplicationConsent.option2,
      });
    }
    await performAction('reTryOnCallBackError', enterGenAppConsentAndNotice.continueButton, confirmApplicationConsent.nextPage as string);
  }

  private async uploadADocument(page: Page, upload: actionRecord): Promise<void> {
    const fileInput = page.locator('input[type="file"].form-control.bottom-30');
    const filePath = path.resolve(__dirname, '../../../../data/inputFiles', upload.file as string);
    await fileInput.last().setInputFiles(filePath);
    let timeout = 6000;
    await performValidation('waitUntilElementDisappears', 'Uploading...');
    await expect(async () => {
      const rateLimit = page.locator(`label:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload"),
                                           span:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload")`);
      let limit = await rateLimit.count();

      while (limit > 0) {
        timeout *= 2;
        await page.waitForTimeout(timeout);
        await fileInput.last().setInputFiles(filePath);
        await performValidation('waitUntilElementDisappears', 'Uploading...');
        limit = await rateLimit.count();
      }
    }).toPass({
      timeout: VERY_LONG_TIMEOUT,
    });
    await page.waitForTimeout(timeout);
  }

  private async uploadRelativeEvidence(uploadEvidence: actionRecord): Promise<void> {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    if (uploadEvidence.files) {
      await performAction('uploadFile', { files: uploadEvidence.files, label: uploadEvidence.label });
    }
    await performAction('reTryOnCallBackError', enterGenAppPreferApplicationToJudge.continueButton, uploadEvidence.nextPage as string);
  }

    private async verifyReferToJudge(referToJudgeData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('mainHeader', enterGenAppPreferApplicationToJudge.mainHeader);
    await performAction('reTryOnCallBackError', enterGenAppPreferApplicationToJudge.continueButton, referToJudgeData.nextPage as string);
  }

  private async verifyGenAppConfirm(): Promise<void> {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'inlineText', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'inlineText',
      text: `${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('mainHeader', enterGenAppConfirmation.mainHeader);
    await performValidation('text', { elementType: 'inlineText', text: enterGenAppConfirmation.applicationEnteredText });
    await performAction('clickButton', enterGenAppConfirmation.closeAndReturnToCaseOverviewButton);
  }

  private async selectManageHearing(manageHearingOption: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('clickRadioButton', {
      question: manageHearingOption.question,
      option: manageHearingOption.option,
    });
    await performAction('reTryOnCallBackError', manageHearing.continueButton, manageHearingOption.nextPage as string);
  }

  private async editHearing(editHearingData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('clickRadioButton', {
      question: editHearingData.typeOfHearingQuestion,
      option: editHearingData.option
    });

    await performAction('select', editHearingData.hearingNoticeLabel, editHearingData.dropDownInput);
    await performAction('inputDate', editHearingData.whenHearingQuestion as string, editHearingData.date);
    await performAction('inputText', editHearingData.daysLabel, CaseManagementCommonUtils.getRandomNumberAsString(1, 10));
    await performAction('inputText', {
      textLabel: editHearingData.hourLabel,
      index: 1
    }, CaseManagementCommonUtils.getRandomNumberAsString(0, 10));
    await performAction('inputText', {
      textLabel: editHearingData.minutesLabel,
      index: 1
    }, CaseManagementCommonUtils.getRandomNumberAsString(1, 59));
    await performAction('inputText', editHearingData.hearingNotesLabel, CaseManagementCommonUtils.generateRandomString(editHearingData.hearingNotesInput as number));
    await performAction('clickRadioButton', {
      question: editHearingData.hearingNotesQuestion,
      option: editHearingData.option2
    });
    if (editHearingData.option2 === 'Yes') {
      await performAction('clickRadioButton', {
        question: editHearingData.hearingWithoutNoticeQuestion,
        option: editHearingData.option3
      });
    }
    if (editHearingData.option2 === 'Yes' && editHearingData.option3 === 'Yes') {
      await performAction('clickRadioButton', {
        question: editHearingData.whoShouldReceiveNoticeQuestion,
        option: editHearingData.option4,
      });
    }
    await performAction('inputText', editHearingData.additionalInformationLabel, CaseManagementCommonUtils.generateRandomString(editHearingData.input as number));
    await performAction('reTryOnCallBackError', editHearing.continueButton, editHearingData.nextPage as string);

  }

  private async confirmHearingEdited(confirmEdit: actionRecord): Promise<void> {
    let submitPayLoad = confirmEdit.submitPayload as Record<string, any>;

    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('text', { elementType: 'inlineText', text: confirmEditHearing.hearingEditedText });
    await performValidation('text', { elementType: 'inlineText', text: 'Case number #' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'inlineText',
      text: `${addressInfo.buildingStreet}, ${addressInfo.addressLine2}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('mainHeader',confirmEditHearing.mainHeader);
    await performValidation('text',{  elementType: 'inlineText', text: confirmEditHearing.hearingEditedText });
    await performValidation('text', { elementType: 'inlineText', text: `${submitPayLoad.claimantName} vs ${await this.getDefendantClaimDetails(submitPayLoad)}` });
    await performAction('clickButton', confirmEditHearing.closeAndReturnToCaseOverviewButton);
  }

  private async cancelHearing(cancelHearingData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('inputText', cancelHearingData.label, CaseManagementCommonUtils.generateRandomString(cancelHearingData.input as number));
    await performAction('reTryOnCallBackError', cancelHearing.continueButton, cancelHearingData.nextPage as string);
  }

  private async confirmHearingCancelled(confirmCancel: actionRecord) {
    let submitPayLoad = confirmCancel.submitPayload as Record<string, any>;
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('text', { elementType: 'inlineText', text: confirmCancelHearing.hearingCancelledText });
    await performValidation('text', { elementType: 'inlineText', text: 'Case number #' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'inlineText',
      text: `${addressInfo.buildingStreet}, ${addressInfo.addressLine2}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('mainHeader', confirmCancelHearing.mainHeader);
    await performValidation('text', { elementType: 'inlineText', text: `${submitPayLoad.claimantName} vs ${await this.getDefendantClaimDetails(submitPayLoad)}` });
    await performAction('clickButton', confirmCancelHearing.closeAndReturnToCaseOverviewButton);
  }

  private async selectDynamicAppAndPartyDocRelatedTo(selectApp: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', {
      question: selectApp.question,
      option: selectApp.option,
    });
    if (selectApp.option === 'Not related to an application or counterclaim') {
      await performAction('select', selectApp.dropQn, selectApp.selectOption);
    }
    if (selectApp.date) {
      await performAction('inputDate', selectApp.label as string, selectApp.date as string);
    }

    await performAction('clickRadioButton', { question: selectApp.question1, option: selectApp.option1 });
    await performAction('reTryOnCallBackError', uploadADocument.continueButton, selectApp.nextPage as string);
  }

  private async confirmUpload(confirm: actionRecord): Promise<void> {
    let submitPayLoad = confirm.submitPayload as Record<string, any>;
    let formattedDate;
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    const baseName = String(confirm.fileName).replace(/\.pdf$/i, '');
    const gaNumber = String(confirm.app).match(/\bGA\d+\b/i)?.[0] ?? '';
    if (confirm.fileDate) {
      const [day, month, year] = String(confirm.fileDate).split('/');
      formattedDate = `${day.padStart(2, '0')}${month.padStart(2, '0')}${year}`;
    } else {
      formattedDate = '';
    }
    const role = String(confirm.party).split(' - ')[1] ?? '';

    const uploadedFileName = `${baseName} ${formattedDate} ${gaNumber} - ${role}`;
    await performValidation('text', { elementType: 'inlineText', text: 'Case number #' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'inlineText',
      text: `${addressInfo.buildingStreet}, ${addressInfo.addressLine2}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('text', { elementType: 'inlineText', text: `‘${uploadedFileName}’ uploaded` });
    await performValidation('text', { elementType: 'inlineText', text: `${submitPayLoad.claimantName} vs ${await this.getDefendantClaimDetails(submitPayLoad)}` });
    await performValidation('mainHeader', confirmUpload.mainHeader);
    await performAction('clickButton', confirmUpload.closeAndReturnToCaseOverviewButton);
  }

  private async addAHearing(addAHearing: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('clickRadioButton', {
      question: addAHearing.hearingQuestion,
      option: addAHearing.option,
    });
    await performAction('select', addAHearing.wordingQuestion, addAHearing.option1);
    await performAction('inputDate', addAHearing.whenIsHearingLabel as string, addAHearing.date);
    await performAction('inputText', { textLabel: addAHearing.daysLabel, index: 1 }, CaseManagementCommonUtils.getRandomNumberAsString(1, 10));
    await performAction('inputText', { textLabel: addAHearing.hoursLabel, index: 1 }, CaseManagementCommonUtils.getRandomNumberAsString(1, 5));
    await performAction('inputText', { textLabel: addAHearing.minsLabel, index: 1 }, CaseManagementCommonUtils.getRandomNumberAsString(1, 60));
    await performAction('inputText', addAHearing.hearingNotesLabel, CaseManagementCommonUtils.generateRandomString(addAHearing.hearingNotesInput as number));
    await performAction('clickRadioButton', {
      question: addAHearing.noticeQuestion,
      option: addAHearing.option2,
    });
    if (addAHearing.option2 === 'Yes') {
      await performAction('clickRadioButton', {
        question: addAHearing.withoutNoticeQuestion,
        option: addAHearing.option3,
      });
    }
    if (addAHearing.option2 === 'Yes' && addAHearing.option3 === 'Yes') {
      await performAction('clickRadioButton', {
        question: addAHearing.whoShouldReceiveNoticeQuestion,
        option: addAHearing.option4,
      });
    }
    await performAction('inputText', addAHearing.additionalInfoLabel, CaseManagementCommonUtils.generateRandomString(addAHearing.additionalInfoInput as number));
    await performAction('reTryOnCallBackError', addHearing.continueButton, addAHearing.nextPage as string);
  }

  private async confirmAddHearing(confirmAdd: actionRecord): Promise<void> {
    let submitPayLoad = confirmAdd.submitPayload as Record<string, any>;
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('text', { elementType: 'inlineText', text: confirmHearing.addHearingText });
    await performValidation('text', { elementType: 'inlineText', text: 'Case number #' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'inlineText',
      text: `${addressInfo.buildingStreet}, ${addressInfo.addressLine2}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('text', { elementType: 'inlineText', text: `${submitPayLoad.claimantName} vs ${await this.getDefendantClaimDetails(submitPayLoad)}` });
    await performValidation('mainHeader', confirmHearing.mainHeader);
    await performAction('clickButton', confirmHearing.closeAndReturnToCaseOverviewButton);
  }

  private async getDefendantClaimDetails(defendantsDetails: actionRecord): Promise<string> {

    let originalDefendantDetails: string[] = [];
    const payLoad = defendantsDetails as Record<string, any>;
    if (payLoad.defendant1.nameKnown === 'YES') {
      originalDefendantDetails.push(
        `${payLoad.defendant1.firstName} ${payLoad.defendant1.lastName}`
      );
    } else {
      originalDefendantDetails.push(
        `persons unknown`
      );
    }

    if (payLoad.addAnotherDefendant === 'YES') {

      for (const defendant of payLoad.additionalDefendants) {
        if (defendant.value.nameKnown === 'YES') {
          originalDefendantDetails.push(`${defendant.value.firstName} ${defendant.value.lastName}`);
        } else {
          originalDefendantDetails.push(
            `persons unknown`
          );
        }
      }
    }

    let defendantText: string;

    if (originalDefendantDetails.length > 2) {
      defendantText = `${originalDefendantDetails[0]}, ${originalDefendantDetails[1]} and Others`;
    } else {
      defendantText = originalDefendantDetails.join(', ');
    }
    return defendantText;

  }

  private async confirmAmend(confirm: actionRecord): Promise<void> {
    let submitPayLoad = confirm.submitPayload as Record<string, any>;
    let formattedDate;
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    const baseName = String(confirm.fileName);
    if (confirm.fileDate) {
      const [day, month, year] = String(confirm.fileDate).split('/');
      formattedDate = `${day.padStart(2, '0')}${month.padStart(2, '0')}${year}`;
    } else {
      formattedDate = '';
    }
    const role = String(confirm.party).split(' - ')[0] ?? '';

    const amendedFileName = `${baseName} ${formattedDate}`;
    await performValidation('text', { elementType: 'inlineText', text: 'Case number #' + caseInfo.fid });
    await performValidation('text', {
      elementType: 'inlineText',
      text: `${addressInfo.buildingStreet}, ${addressInfo.addressLine2}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performValidation('text', { elementType: 'inlineText', text: `Document ${amendedFileName} amended` });
    await performValidation('text', { elementType: 'inlineText', text: `${role}` });
    await performValidation('mainHeader', confirmAmend.mainHeader);
    await performAction('clickButton', confirmAmend.closeAndReturnToCaseOverviewButton);
  }

 private async inputErrorValidation(page: Page, validationArr: actionRecord) {
    if (Array.isArray(validationArr.inputArray)) {
      for (const item of validationArr.inputArray) {

        switch (validationArr.validationType) {

          case 'radioOptions':
            await performAction('clickButton', validationArr.button);
            await performValidation('inputError', !validationArr?.label ? validationArr.question : validationArr.label, item.errInlineMessage);
            await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
            await performAction('clickRadioButton', { question: validationArr.question, option: validationArr.option });
            break;

          case 'checkBox':
            await performAction('clickButton', validationArr.button);
            await performValidation('inputError', !validationArr?.label ? validationArr.question : validationArr.label, item.errMessage);
            await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
            await performAction('check', validationArr.checkBox);
            break;

          case 'checkBoxPageLevel':
            await performAction('clickButton', validationArr.button);
            await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
            await performAction('check', validationArr.checkBox);
            break;

          case 'dropDown':
            await performAction('clickButton', validationArr.button);
            await expect(async () => {
              await performAction('clickButton', validationArr.button);
              await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
            }).toPass({
              timeout: VERY_LONG_TIMEOUT,
            });
            await performAction('select', validationArr.dropQn, validationArr.option);
            break;

          case 'textField':
            if (item.type === 'valid') {
              await performAction('inputText', validationArr.label, CaseManagementCommonUtils.generateRandomString(item.input));
            } else {
              await performAction('inputText', validationArr.label, CaseManagementCommonUtils.generateRandomString(item.input));
              await expect(async () => {
                await performAction('clickButton', validationArr.button);
                if (item.type === 'moreThanMax') {
                  await performValidation('errorMessage', { header: validationArr.header, message: item.errMessage });
                } else {
                  await performValidation('inputError', validationArr.label, item.errMessage);
                  await performValidation('errorMessage', validationArr.label, item.errMessage);
                }
              }).toPass({
                timeout: VERY_LONG_TIMEOUT,
              });
            }
            break;

          case 'dateField':
            let date: string = CaseManagementCommonUtils.getRandomDate(item.type as string);
            const enterDate = () =>
              performActions(
                'Enter Date',
                ['inputText', validationArr.label1, date.split('/')[0]],
                ['inputText', validationArr.label2, date.split('/')[1]],
                ['inputText', validationArr.label3, date.split('/')[2]]
              );

            if (item.type === 'empty') {
              await performAction('clickButton', validationArr.button);
              await performValidation('inputError', !validationArr?.label ? validationArr.question : validationArr.label, item.errInlineMessage);
              await performValidation('errorMessage', validationArr.header1, item.errMessage);
            } else if (item.type === 'past' || item.type === 'validFuture') {
              await enterDate();
            } else if (item.type === 'invalid') {
              await enterDate();
              await performAction('clickButton', validationArr.button);
              await performValidation('errorMessage', validationArr.header1, item.errMessage);
            } else {
              await enterDate();
              await performAction('clickButton', validationArr.button);
              await performValidation('errorMessage', { header: validationArr.header, message: item.errMessage });
            }
            break;

          case 'dateRadioOption':
            let dateOfReview: string = CaseManagementCommonUtils.getRandomDate(item.type as string);
            const enterDateOfReview = () =>
              performActions(
                'Enter Date',
                ['inputText', validationArr.label1, dateOfReview.split('/')[0]],
                ['inputText', validationArr.label2, dateOfReview.split('/')[1]],
                ['inputText', validationArr.label3, dateOfReview.split('/')[2]]
              );
            await enterDateOfReview();
            await performAction('clickRadioButton', {
              question: validationArr.question,
              option: validationArr.option
            });
            await performAction('inputText', validationArr.label, generateRandomString(Number(item.input)));
            await performAction('clickButton', validationArr.button);
            await performValidation('errorMessage', { header: validationArr.header, message: item.errMessage });
            break;

          case 'moneyField':
            if (item.index && validationArr.labelMulti) {
              await performAction('inputText', { textLabel: validationArr.label, index: item.index }, item.input);
              await performAction('inputText', { textLabel: validationArr.label1, index: item.index }, item.input2);
              await performAction('inputText', { textLabel: validationArr.labelMulti, index: item.index }, item.input1);
            } else if (item.index) {
              await performAction('inputText', { textLabel: validationArr.label, index: item.index }, item.input);
            } else {
              await performAction('inputText', validationArr.label, item.input);
            }

            await expect(async () => {
              await performAction('clickButton', validationArr.button);
              //await performValidation('errorMessage', { header: !validationArr?.header ? validationArr.header = 'The event could not be created' : validationArr.header, message: item.errMessage });
              if (item.errMessage1) {
                await performValidation('inputError', validationArr.labelMulti, item.errMessage1);
              } else if (item.errMessage2) {
                await performValidation('inputError', validationArr.label1, item.errMessage2);
              }
              else {
                await performValidation('inputError', validationArr.label, item.errMessage);
              }
            }).toPass({
              timeout: VERY_LONG_TIMEOUT,
            });
            break;

          case 'uploadADocument':
            await expect(async () => {
              await performAction('clickButton', validationArr.button);
              await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
            }).toPass({
              timeout: VERY_LONG_TIMEOUT,
            });
            break;

          default:
            throw new Error(`Validation type :"${validationArr.validationType}" is not valid`);
        }
      }
    }
    if (validationArr.buttonRemove) {
      await performAction('removeFile');
      await page.waitForTimeout(6000);
    }
  }

  private async getAddressInfo(address: actionRecord) {
    let createCasePayLoad = address.data as Record<string, any>;
      addressInfo = {
      buildingStreet: createCasePayLoad.propertyAddress.AddressLine1,
      addressLine2: createCasePayLoad.propertyAddress.AddressLine2,
      townCity: createCasePayLoad.propertyAddress.PostTown,
      engOrWalPostcode: createCasePayLoad.propertyAddress.PostCode
    };

  }
}
