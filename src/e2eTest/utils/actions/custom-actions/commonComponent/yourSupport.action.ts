import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { Page } from '@playwright/test';
import { performAction, performValidation } from '@utils/controller';
import { createCaseApiData } from "@data/api-data";
import { formatTheCaseNumber } from "@utils/common/string.utils";

export class YourSupportAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectRadioButtonInYourSupport', () => this.selectRadioButtonInYourSupport(fieldName as actionRecord, page)],
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async validateCaseContext(): Promise<void> {
    if (process.env.CASE_NUMBER) {
      await performValidation('text', {
        elementType: 'paragraph',
        text: `Case number: ${formatTheCaseNumber(process.env.CASE_NUMBER)}`
      });
      await performValidation('text', {
        elementType: 'paragraph',
        text: `Property address: ${createCaseApiData.createCasePayload.propertyAddress.AddressLine1}, ${createCaseApiData.createCasePayload.propertyAddress.PostTown}, ${createCaseApiData.createCasePayload.propertyAddress.PostCode}`
      });
    }
  }

  private async selectRadioButtonInYourSupport(selectOptions: actionRecord, page: Page) {
    await this.validateCaseContext();
    const radio = page.locator(`label >> text=${selectOptions.optionToSelect}`);
    await radio.waitFor({ state: 'visible' });

    await performAction('clickRadioButton', { option: selectOptions.optionToSelect });
    await performAction('clickButtonAndWaitForElement', selectOptions.continueButton, selectOptions.headerToCheck);
  }
}
