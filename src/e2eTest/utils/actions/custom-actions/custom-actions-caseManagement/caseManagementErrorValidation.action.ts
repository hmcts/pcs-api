import { Page } from '@playwright/test';
import { performAction } from '@utils/controller-enforcement';
import { IAction, actionData, actionRecord } from '@utils/interfaces/action.interface';
import { enforcementApplication } from '@data/page-data-figma/page-data-enforcement-figma';

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
        validationType: enforcementApplication.errorValidationType.three,
        inputArray: enforcementApplication.errorValidationField.errorRadioOption,
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.warrantOfPossessionRadioOption,
        button: enforcementApplication.continueButton
      });
    }
  }
  
}
