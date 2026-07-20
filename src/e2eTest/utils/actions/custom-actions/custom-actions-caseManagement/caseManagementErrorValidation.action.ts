import { Page } from '@playwright/test';
import { performAction } from '@utils/controller-caseManagement';
import { IAction, actionData, actionRecord } from '@utils/interfaces/action.interface';
import {addReviewDates, selectDocument} from '@data/page-data-figma/page-data-caseManagement-figma';

export class ErrorValidationAction implements IAction {
  async execute(page: Page, action: string, errorFlag: string | actionRecord, roles?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['errorValidationSelectDocumentPage', () => this.errorValidationSelectDocumentPage(errorFlag as string)],
      ['errorValidationAddReviewDatesPage', () => this.errorValidationAddReviewDatesPage(errorFlag as string)],
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
        label: addReviewDates.dateOfReviewLabel,
        button: addReviewDates.continueButton
      });
      await performAction('inputErrorValidation',{
        validationType: addReviewDates.errorValidationType.two,
        inputArray: addReviewDates.errorValidationField.errorRadioOption,
        label: addReviewDates.reasonLabel,
        button: addReviewDates.continueButton
      });
      await performAction('inputErrorValidation',{
        validationType: addReviewDates.errorValidationType.one,
        inputArray: addReviewDates.errorValidationField.errorTextField,
        day: addReviewDates.dayInputText,
        Month: addReviewDates.monthInputText,
        Year: addReviewDates.yearInputText,
        option: addReviewDates.generalOrderRadioOption,
        header: addReviewDates.eventCouldNotBeCreatedErrorMessageHeader,
        label: addReviewDates.descriptionTextLabel,
        button: addReviewDates.continueButton
      });
    }
  }
}
