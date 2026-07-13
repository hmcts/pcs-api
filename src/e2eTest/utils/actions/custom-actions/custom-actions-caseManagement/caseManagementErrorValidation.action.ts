import { Page } from '@playwright/test';
import { performAction } from '@utils/controller-caseManagement';
import { IAction, actionData, actionRecord } from '@utils/interfaces/action.interface';
import { selectDocument } from '@data/page-data-figma/page-data-caseManagement-figma';

export class ErrorValidationAction implements IAction {
  async execute(page: Page, action: string, errorFlag: string | actionRecord, roles?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['errorValidationSelectDocumentPage', () => this.errorValidationSelectDocumentPage(errorFlag as string)],

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

}
