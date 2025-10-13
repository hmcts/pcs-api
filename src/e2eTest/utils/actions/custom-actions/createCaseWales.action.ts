import {actionData, actionRecord, IAction} from '../../interfaces/action.interface';
import {Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
import {claimantDetailsWales} from '@data/page-data/claimantDetailsWales.page.data';
import {caseNumber, CreateCaseAction} from "@utils/actions/custom-actions/createCase.action";

export class CreateCaseWalesAction extends CreateCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectClaimantDetails', () => this.selectClaimantDetails(fieldName as actionRecord)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectClaimantDetails(claimant: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performAction('clickRadioButton', {question: claimant.question1, option: claimant.option1});
    if (claimant.option1 == claimantDetailsWales.yes) {
      await performAction('inputText', claimantDetailsWales.whatsYourRegistrationNumber, claimantDetailsWales.sampleTestRegistrationNumber);
    }
    await performAction('clickRadioButton', {question: claimant.question2, option: claimant.option2});
    if (claimant.option2 == claimantDetailsWales.yes) {
      await performAction('inputText', claimantDetailsWales.whatsYourLicenseNumber, claimantDetailsWales.sampleTestLicenseNumber);
    }
    await performAction('clickRadioButton', {question: claimant.question3, option: claimant.option3});
    if (claimant.option3 == claimantDetailsWales.yes) {
      await performAction('inputText', claimantDetailsWales.agentsFirstnameLabel, claimantDetailsWales.agentsFirstname);
      await performAction('inputText', claimantDetailsWales.agentsLastnameLabel, claimantDetailsWales.agentsLastname);
      await performAction('inputText', claimantDetailsWales.agentsLicenseNumberLabel, claimantDetailsWales.agentsLicenseNumber);
      await performActions('Enter Date',
        ['inputText', claimantDetailsWales.dayLabel, claimantDetailsWales.day],
        ['inputText', claimantDetailsWales.monthLabel, claimantDetailsWales.month],
        ['inputText', claimantDetailsWales.yearLabel, claimantDetailsWales.year]);
    }
    await performAction('clickButton', claimantDetailsWales.continue);
  }
}
