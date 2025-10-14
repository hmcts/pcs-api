import {actionData, actionRecord, IAction} from '../../interfaces/action.interface';
import {Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
import {caseNumber, CreateCaseAction} from "@utils/actions/custom-actions/createCase.action";
import {occupationContractOrLicenceDetailsWales} from "@data/page-data/occupationContractOrLicenceDetailsWales.page.data";

export class CreateCaseWalesAction extends CreateCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectOccupationContractOrLicenceDetails', () => this.selectOccupationContractOrLicenceDetails(fieldName as actionRecord)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectOccupationContractOrLicenceDetails(occupationContractData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performAction('clickRadioButton', occupationContractOrLicenceDetailsWales.occupationContractOrLicenceType);
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
}
