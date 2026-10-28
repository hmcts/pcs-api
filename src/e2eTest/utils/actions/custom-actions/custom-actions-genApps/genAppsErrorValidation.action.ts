import { Page } from '@playwright/test';
import { IAction, actionData, actionRecord } from '@utils/interfaces/action.interface';
import {chooseAnApplication} from "@data/page-data-figma/page-data-genApps-figma";
import {performAction} from "@utils/controller-genApps";

export class ErrorValidationAction implements IAction {
  async execute(page: Page, action: string, errorFlag: string | actionRecord, roles?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['errorValidationChooseAnApplicationPage', () => this.errorValidationChooseAnApplicationPage(errorFlag as string)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async errorValidationChooseAnApplicationPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: chooseAnApplication.errorValidationType.three,
        inputArray: chooseAnApplication.errorValidationField.errorRadioOption,
        question: chooseAnApplication.whatDoYouWantToApplyForQuestion,
        option: chooseAnApplication.suspendTheEvictionRadioOption,
        button: chooseAnApplication.continueButton
      });
    }
  }
}
