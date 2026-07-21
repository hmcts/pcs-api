import { Page } from '@playwright/test';
import { performAction } from '@utils/controller-caseManagement';
import { IAction, actionData, actionRecord } from '@utils/interfaces/action.interface';
import {addReviewDates, selectDocument} from '@data/page-data-figma/page-data-caseManagement-figma';
import { changeCaseState, selectDocument } from '@data/page-data-figma/page-data-caseManagement-figma';

export class ErrorValidationAction implements IAction {
  async execute(page: Page, action: string, errorFlag: string | actionRecord, roles?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['errorValidationSelectDocumentPage', () => this.errorValidationSelectDocumentPage(errorFlag as string)],
      ['errorValidationAddReviewDatesPage', () => this.errorValidationAddReviewDatesPage(errorFlag as string)],
      ['errorValidationChangeCaseStatePage', () => this.errorValidationChangeCaseStatePage(errorFlag as string)],

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
        option: selectDocument.docFolderHiddenOption,
        button: selectDocument.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: selectDocument.errorValidationType.two,
        inputArray: selectDocument.errorValidationField.errorRadioOption,
        question: selectDocument.documentToAmendHiddenQuestion,
        option: selectDocument.typeOfDocumentHiddenRadioOption,
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
        label: addReviewDates.reasonHiddenLabel,
        button: addReviewDates.continueButton
      });

      await performAction('inputErrorValidation', {
        validationType: addReviewDates.errorValidationType.six,
        inputArray: addReviewDates.errorValidationField.errorMaxInputField,
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

}
