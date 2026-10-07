import { Page } from '@playwright/test';
import { performAction } from '@utils/controller-caseManagement';
import { IAction, actionData, actionRecord } from '@utils/interfaces/action.interface';
import {
  addHearing, cancelHearing,
  addReviewDates, changeCaseState, enterGenappApplication, enterGenAppapplicationFee,
  enterGenAppConsentAndNotice, enterGenAppHearingDate, manageHearing, selectDocument, uploadADocument,
  enterGenAppUploadGeneralApplication
} from '@data/page-data-figma/page-data-caseManagement-figma';
import { allPartyDetails } from './caseManagement.action';
import { CaseManagementCommonUtils } from './caseManagementUtils.action';
import { defendantUserDetails } from '../createCaseAPI.action';

export class ErrorValidationAction implements IAction {
  async execute(page: Page, action: string, errorFlag: string | actionRecord, roles?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['errorValidationSelectDocumentPage', () => this.errorValidationSelectDocumentPage(errorFlag as string)],
      ['errorValidationAddReviewDatesPage', () => this.errorValidationAddReviewDatesPage(errorFlag as string)],
      ['errorValidationChangeCaseStatePage', () => this.errorValidationChangeCaseStatePage(errorFlag as string)],
      ['errorValidationEnterGeneralAppPage', () => this.errorValidationEnterGeneralAppPage(errorFlag as string)],
      ['errorValidationHearingDatePage', () => this.errorValidationHearingDatePage(errorFlag as string)],
      ['errorValidationApplicationFeePage', () => this.errorValidationApplicationFeePage(errorFlag as string)],
      ['errorValidationUploadADocumentPage', () => this.errorValidationUploadADocumentPage(errorFlag as string)],
      ['errorValidationApplicationConsentAndNotice', () => this.errorValidationApplicationConsentAndNotice(errorFlag as string)],
      ['errorValidationManageHearing', () => this.errorValidationManageHearing(errorFlag as string)],
      ['errorValidationEnterAddAHearingPage', () => this.errorValidationEnterAddAHearingPage(errorFlag as string)],
      ['errorValidationManageHearing', () => this.errorValidationManageHearing(errorFlag as string)],
      ['errorValidationCancelHearing', () => this.errorValidationCancelHearing(errorFlag as string)],
      ['errorValidationUploadGenAppsFile', () => this.errorValidationUploadGenAppsFile(errorFlag as string)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async errorValidationSelectDocumentPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: selectDocument.errorValidationType.four,
        inputArray: selectDocument.errorValidationField.errorDropDown,
        dropQn: selectDocument.whichFolderQuestion,
        option: (selectDocument.docFolderHiddenOption)[0],
        button: selectDocument.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: selectDocument.errorValidationType.two,
        inputArray: selectDocument.errorValidationField.errorRadioOption,
        question: selectDocument.documentToAmendHiddenQuestion,
        option: (selectDocument.typeOfDocumentHiddenRadioOption)[0],
        button: selectDocument.continueButton
      });
    }
  }

  private async errorValidationAddReviewDatesPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation',{
        validationType: addReviewDates.errorValidationType.five,
        inputArray: addReviewDates.errorValidationField.errorDateField,
        question: addReviewDates.dateOfReviewHiddenLabel,
        header1: addReviewDates.thereIsProbErrorMessageHeader,
        label1: addReviewDates.dayHiddenTextLabel,
        label2: addReviewDates.monthHiddenTextLabel,
        label3: addReviewDates.yearHiddenTextLabel,
        button: addReviewDates.continueButton
      });

      await performAction('inputErrorValidation',{
        validationType: addReviewDates.errorValidationType.two,
        inputArray: addReviewDates.errorValidationField.errorRadioOption,
        question: addReviewDates.reasonHiddenLabel,
        option: addReviewDates.unlessOrderHiddenRadioOption,
        button: addReviewDates.continueButton
      });

      await performAction('inputErrorValidation', {
        validationType: addReviewDates.errorValidationType.one,
        inputArray: addReviewDates.errorValidationField.errorTextField,
        header: addReviewDates.eventCouldNotBeCreatedErrorMessageHeader,
        label: addReviewDates.descriptionHiddenTextLabel,
        button: addReviewDates.continueButton
      });

      await performAction('inputErrorValidation', {
        validationType: addReviewDates.errorValidationType.six,
        inputArray: addReviewDates.errorValidationField.errorDateRadioOption,
        header: addReviewDates.eventCouldNotBeCreatedErrorMessageHeader,
        label1: addReviewDates.dayHiddenTextLabel,
        label2: addReviewDates.monthHiddenTextLabel,
        label3: addReviewDates.yearHiddenTextLabel,
        label: addReviewDates.descriptionHiddenTextLabel,
        question: addReviewDates.reasonHiddenLabel,
        option: addReviewDates.OtherHiddenRadioOption,
        button: addReviewDates.continueButton
      });
    }
  }
  private async errorValidationChangeCaseStatePage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: changeCaseState.errorValidationType.four,
        inputArray: changeCaseState.errorValidationField.errorDropDown,
        dropQn: changeCaseState.whichStateYouMovingCaseToQuestion,
        option: changeCaseState.caseStateHiddenOption,
        button: changeCaseState.continueButton
      });
    }
  }

  private async errorValidationEnterGeneralAppPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: enterGenappApplication.errorValidationType.two,
        inputArray: enterGenappApplication.errorValidationField.errorRadioOption1,
        question: enterGenappApplication.whichPartyMadeAppQuestion,
        option: allPartyDetails[0],
        button: enterGenappApplication.continueButton
      });

      await performAction('inputErrorValidation', {
        validationType: enterGenappApplication.errorValidationType.two,
        inputArray: enterGenappApplication.errorValidationField.errorRadioOption2,
        question: enterGenappApplication.typeOfAppQuestion,
        option: enterGenappApplication.adjournRadioOption,
        button: enterGenappApplication.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: enterGenappApplication.errorValidationType.five,
        inputArray: enterGenappApplication.errorValidationField.errorDateField,
        header: enterGenappApplication.eventCouldNotBeCreatedErrorMessageHeader,
        header1: enterGenappApplication.thereIsProbErrorMessageHeader,
        question: enterGenappApplication.whatDateAppReceivedQuestion,
        label1: enterGenappApplication.dayTextLabel,
        label2: enterGenappApplication.monthTextLabel,
        label3: enterGenappApplication.yearTextLabel,
        button: enterGenappApplication.continueButton
      });
      await performAction('clickRadioButton', {
        question: enterGenappApplication.typeOfAppQuestion,
        option: enterGenappApplication.somethingElseRadioOption,
      });
      await performAction('inputErrorValidation', {
        validationType: enterGenappApplication.errorValidationType.one,
        inputArray: enterGenappApplication.errorValidationField.errorTextField,
        header: enterGenappApplication.eventCouldNotBeCreatedErrorMessageHeader,
        label: enterGenappApplication.whichCategoriesHiddenTextLabel,
        button: enterGenappApplication.continueButton
      });

    }
  }

  private async errorValidationHearingDatePage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: enterGenAppHearingDate.errorValidationType.two,
        inputArray: enterGenAppHearingDate.errorValidationField.errorRadioOption,
        question: enterGenAppHearingDate.hearingInNext14DaysQuestion,
        option: enterGenAppHearingDate.yesRadioOption,
        button: enterGenAppHearingDate.continueButton
      });
    }
  }

  private async errorValidationApplicationFeePage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: enterGenAppapplicationFee.errorValidationType.two,
        inputArray: enterGenAppapplicationFee.errorValidationField.errorRadioOption1,
        question: enterGenAppapplicationFee.appFeeReceivedQuestion,
        option: enterGenAppapplicationFee.yesRadioOption,
        button: enterGenappApplication.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: enterGenAppapplicationFee.errorValidationType.two,
        inputArray: enterGenAppapplicationFee.errorValidationField.errorRadioOption2,
        question: enterGenAppapplicationFee.referenceNumberIncludedQuestion,
        option: enterGenAppapplicationFee.yesRadioOption,
        button: enterGenAppapplicationFee.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: enterGenAppapplicationFee.errorValidationType.one,
        inputArray: enterGenAppapplicationFee.errorValidationField.errorTextField,
        header: enterGenAppapplicationFee.thereIsProbErrorMessageHeader,
        label: enterGenAppapplicationFee.enterTheFeeReferenceHiddenTextLabel,
        button: enterGenAppapplicationFee.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: enterGenAppapplicationFee.errorValidationType.seven,
        inputArray: enterGenAppapplicationFee.errorValidationField.errorMoneyField,
        label: enterGenAppapplicationFee.enterTheAmountReceivedHiddenTextLabel,
        button: enterGenAppapplicationFee.continueButton
      });

    }
  }

  private async errorValidationUploadADocumentPage(validationReq: string) {
    let appType = CaseManagementCommonUtils.getGenApplicationType(defendantUserDetails.length)[0];
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: uploadADocument.errorValidationType.eight,
        inputArray: uploadADocument.errorValidationField.errorUploadADocument,
        button: uploadADocument.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: uploadADocument.errorValidationType.two,
        inputArray: uploadADocument.errorValidationField.errorRadioOption1,
        question: uploadADocument.whichAppOrCounterClaimThisRelateToQuestion,
        option: uploadADocument.notRelatedToAppRadioOption,
        button: uploadADocument.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: uploadADocument.errorValidationType.four,
        inputArray: uploadADocument.errorValidationField.errorDropDown,
        dropQn: uploadADocument.whichTypeOfDocHiddenQuestion,
        option: uploadADocument.whichTypeHiddenOption,
        button: uploadADocument.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: uploadADocument.errorValidationType.five,
        inputArray: uploadADocument.errorValidationField.errorDateField,
        header: enterGenappApplication.eventCouldNotBeCreatedErrorMessageHeader,
        header1: uploadADocument.thereIsProbErrorMessageHeader,
        question: uploadADocument.addIssueDateTextLabel,
        label1: enterGenappApplication.dayTextLabel,
        label2: enterGenappApplication.monthTextLabel,
        label3: enterGenappApplication.yearTextLabel,
        button: enterGenappApplication.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: uploadADocument.errorValidationType.two,
        inputArray: uploadADocument.errorValidationField.errorRadioOption2,
        question: uploadADocument.partyDocRelatedToQuestion,
        option: appType,
        button: uploadADocument.continueButton
      });
    }
  }

  private async errorValidationApplicationConsentAndNotice(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: enterGenAppConsentAndNotice.errorValidationType.two,
        inputArray: enterGenAppConsentAndNotice.errorValidationField.errorRadioOption1,
        question: enterGenAppConsentAndNotice.doAllPartiesAgreedQuestion,
        option: enterGenAppConsentAndNotice.noRadioOption,
        button: enterGenAppConsentAndNotice.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: enterGenAppapplicationFee.errorValidationType.two,
        inputArray: enterGenAppConsentAndNotice.errorValidationField.errorRadioOption2,
        question: enterGenAppConsentAndNotice.hasApplicantMadeWithoutNoticeHiddenQuestion,
        option: enterGenAppConsentAndNotice.noHiddenRadioOption,
        button: enterGenAppConsentAndNotice.continueButton
      });
    }
  };

  private async errorValidationEnterAddAHearingPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: addHearing.errorValidationType.two,
        inputArray: addHearing.errorValidationField.errorRadioOption1,
        question: addHearing.typeOfHearingQuestion,
        option:  addHearing.typeOfHearingOption[0],
        button: addHearing.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: addHearing.errorValidationType.four,
        inputArray: addHearing.errorValidationField.errorDropDown,
        dropQn: addHearing.wordingForHearingNoticeTextLabel,
        option: addHearing.wordingForHearingHiddenOption,
        button: addHearing.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: addHearing.errorValidationType.five,
        inputArray: addHearing.errorValidationField.errorDateField,
        header: addHearing.eventCouldNotBeCreatedErrorMessageHeader,
        header1: addHearing.thereIsProbErrorMessageHeader,
        question: addHearing.whenIsTheHearingQuestion,
        label1: addHearing.dayTextLabel,
        label2: addHearing.monthTextLabel,
        label3: addHearing.yearTextLabel,
        button: addHearing.continueButton
      });


      await performAction('inputErrorValidation', {
        validationType: addHearing.errorValidationType.two,
        inputArray: addHearing.errorValidationField.errorRadioOption2,
        question: addHearing.hearingNoticeQuestion,
        option:  addHearing.hearingNoticeNoRadioOption,
        button: addHearing.continueButton
      });

      await performAction('inputErrorValidation', {
        validationType: addHearing.errorValidationType.seven,
        inputArray: addHearing.errorValidationField.errorMoneyField,
        // header: addHearing.eventCouldNotBeCreatedErrorMessageHeader,
        label1: addHearing.daysTextLabel,
        label: addHearing.hoursTextLabel,
        labelMulti :addHearing.minutesTextLabel,
        button: addHearing.continueButton
      });
    }
  }
  private async errorValidationManageHearing(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: manageHearing.errorValidationType.two,
        inputArray: manageHearing.errorValidationField.errorRadioOption,
        question: manageHearing.doYouWantToAddQuestion,
        option: manageHearing.addAHearingRadioOption,
        button: manageHearing.continueButton
      });
    }
  }

  private async errorValidationCancelHearing(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: cancelHearing.errorValidationType.one,
        inputArray: cancelHearing.errorValidationField.errorTextField,
        header: cancelHearing.eventCouldNotBeCreatedErrorMessageHeader,
        label: cancelHearing.enterReasonForCancellationLabel,
        button: cancelHearing.continueButton

      });
    }
  }

  private async errorValidationUploadGenAppsFile(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: enterGenAppUploadGeneralApplication.errorValidationType.eight,
        inputArray: enterGenAppUploadGeneralApplication.errorValidationField.errorUploadADocument,
        button: enterGenAppUploadGeneralApplication.continueButton
      });
    }
  }
}
