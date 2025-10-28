import {actionData, actionRecord, IAction} from '../../interfaces/action.interface';
import {Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
import {claimantDetailsWales} from '@data/page-data/claimantDetailsWales.page.data';
import {caseNumber, CreateCaseAction} from "@utils/actions/custom-actions/createCase.action";
import {occupationContractOrLicenceDetailsWales} from '@data/page-data/occupationContractOrLicenceDetailsWales.page.data';

export class CreateCaseWalesAction extends CreateCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectOccupationContractOrLicenceDetails', () => this.selectOccupationContractOrLicenceDetails(fieldName as actionRecord)],
      ['selectClaimantDetails', () => this.selectClaimantDetails(fieldName as actionRecord)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectOccupationContractOrLicenceDetails(occupationContractData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performAction('clickRadioButton', {question: occupationContractOrLicenceDetailsWales.occupationContractOrLicenceType, option: occupationContractData.occupationContractType});
    if (occupationContractData.occupationContractType === occupationContractOrLicenceDetailsWales.other) {
      await performAction('inputText', occupationContractOrLicenceDetailsWales.giveDetailsOfTypeOfOccupationContractAgreement, occupationContractOrLicenceDetailsWales.detailsOfLicence);
    }
    if (occupationContractData.day && occupationContractData.month && occupationContractData.year) {
      await performActions(
        'Enter Date',
        ['inputText', occupationContractOrLicenceDetailsWales.dayLabel, occupationContractOrLicenceDetailsWales.day],
        ['inputText', occupationContractOrLicenceDetailsWales.monthLabel, occupationContractOrLicenceDetailsWales.month],
        ['inputText', occupationContractOrLicenceDetailsWales.yearLabel, occupationContractOrLicenceDetailsWales.year]);
    }
    if (occupationContractData.files) {
      await performAction('uploadFile', occupationContractData.files);
    }
    await performAction('clickButton', occupationContractOrLicenceDetailsWales.continue);
  }

  private async selectClaimantDetails(claimant: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performAction('clickRadioButton', {question: claimant.question1, option: claimant.option1});
    if (claimant.option1 == claimantDetailsWales.yes) {
      await performAction('inputText', claimantDetailsWales.whatsYourRegistrationNumber, claimantDetailsWales.sampleTestRegistrationNumberInput);
    }
    await performAction('clickRadioButton', {question: claimant.question2, option: claimant.option2});
    if (claimant.option2 == claimantDetailsWales.yes) {
      await performAction('inputText', claimantDetailsWales.whatsYourLicenseNumber, claimantDetailsWales.sampleTestLicenseNumberInput);
    }
    await performAction('clickRadioButton', {question: claimant.question3, option: claimant.option3});
    if (claimant.option3 == claimantDetailsWales.yes) {
      await performAction('inputText', claimantDetailsWales.agentsFirstnameLabel, claimantDetailsWales.agentsFirstnameInput);
      await performAction('inputText', claimantDetailsWales.agentsLastnameLabel, claimantDetailsWales.agentsLastnameInput);
      await performAction('inputText', claimantDetailsWales.agentsLicenseNumberLabel, claimantDetailsWales.agentsLicenseNumberInput);
      await performActions('Enter Date',
        ['inputText', claimantDetailsWales.dayLabel, claimantDetailsWales.dayInput],
        ['inputText', claimantDetailsWales.monthLabel, claimantDetailsWales.monthInput],
        ['inputText', claimantDetailsWales.yearLabel, claimantDetailsWales.yearInput]);
    }
    await performAction('clickButton', claimantDetailsWales.continue);
  }
}
