import { Page } from '@playwright/test';
import { performAction } from '@utils/controller-caseManagement';
import { IAction, actionData, actionRecord } from '@utils/interfaces/action.interface';
import { changeCaseState, enterGenappApplication, selectDocument } from '@data/page-data-figma/page-data-caseManagement-figma';
import { allPartyDetails } from './caseManagement.action';

export class ErrorValidationAction implements IAction {
  async execute(page: Page, action: string, errorFlag: string | actionRecord, roles?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['errorValidationSelectDocumentPage', () => this.errorValidationSelectDocumentPage(errorFlag as string)],
      ['errorValidationChangeCaseStatePage', () => this.errorValidationChangeCaseStatePage(errorFlag as string)],
      ['errorValidationEnterGeneralAppPage', () => this.errorValidationEnterGeneralAppPage(errorFlag as string)],

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
        option: enterGenappApplication.somethingElseRadioOption,
        button: enterGenappApplication.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: enterGenappApplication.errorValidationType.five,
        inputArray: enterGenappApplication.errorValidationField.errorTextField,
        header: enterGenappApplication.eventCouldNotBeCreatedErrorMessageHeader,
        label1: enterGenappApplication.dayTextLabel,
        label2: enterGenappApplication.monthTextLabel,
        label3: enterGenappApplication.yearTextLabel,
        button: enterGenappApplication.continueButton
      });

    }
  }

}
