import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {expect, Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
export class FeeAndPayAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectPaymentTypePBA', () => this.selectPaymentTypePBA(fieldName as actionRecord, page)],
      ['selectPaymentByCard', () => this.selectPaymentByCard(fieldName as actionRecord, page)],
      ['enterPaymentDetails', () => this.enterPaymentDetails(fieldName as actionRecord, page)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectPaymentTypePBA(paymentOptions: actionRecord, page: Page) { 
    const radio = page.locator(`label >> text=${paymentOptions.payByOption}`);
    await radio.waitFor({ state: 'visible' });
    await radio.waitFor({ state: 'attached' });
    await performAction('clickRadioButton', { option: paymentOptions.payByOption });
    await performAction('select', paymentOptions.pbaLabel, paymentOptions.pbaIndex);
    const locator = page.locator('button', { hasText: 'Confirm payment' });
    await expect(locator).toBeDisabled();
    await performAction('inputText', paymentOptions.referenceLabel, paymentOptions.referenceText);
    await page.click('body');
    await expect(locator).toBeEnabled();
    await performAction('clickButton', paymentOptions.confirmButton);
}
  private async selectPaymentByCard(paymentOptions: actionRecord, page: Page) { 
    const radio = page.locator(`label >> text=${paymentOptions.payByOption}`);
    await radio.waitFor({ state: 'visible' });
    await radio.waitFor({ state: 'attached' });
    await performAction('clickRadioButton', { option: paymentOptions.payByOption });
    await performAction('clickButton', paymentOptions.continueButton)
}

 private async enterPaymentDetails(payDetails: actionRecord, page: Page) { 
   await performActions('Enter details', ['inputText', payDetails.cardLabel, payDetails.cardText]
                                               , ['inputText', payDetails.monthLabel, payDetails.monthText]
                                               , ['inputText', payDetails.yearLabel, payDetails.yearText]
                                               , ['inputText', payDetails.nameLabel, payDetails.nameText]
                                               , ['inputText', payDetails.cardSecCodeLabel, payDetails.cardSecText]
                                               , ['inputText', payDetails.addressLabel1 , payDetails.addressText1]
                                               , ['inputText', payDetails.addressLabel2 , payDetails.addressText2]
                                               , ['inputText', payDetails.townLabel , payDetails.townText]
                                               , ['inputText', payDetails.countryLabel , payDetails.countryText]
                                               , ['inputText', payDetails.postCodeLabel , payDetails.postCodeText]
                                               , ['inputText', payDetails.emailLabel , payDetails.emailText]);
                                      
    await performAction('clickButton', payDetails.button);
   }
 
}