import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {expect, Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
export let caseNumber: string;
export let claimantsName: string;
export let addressInfo: { buildingStreet: string; townCity: string; engOrWalPostcode: string };

export class FeeAndPayAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectPaymentTypePBA', () => this.selectPaymentTypePBA(fieldName as actionRecord, page)],
      ['selectPaymentByCard', () => this.selectPaymentByCard(fieldName as actionRecord, page)],
      ['enterCardDetails', () => this.enterCardDetails(fieldName as actionRecord, page)],
      ['enterBillingAddress', () => this.enterBillingAddress(fieldName as actionRecord, page)],
      ['enterContactDetails', () => this.enterContactDetails(fieldName as actionRecord, page)],
      ['confirmPayment', () => this.confirmPayment(fieldName as actionRecord, page)],
      ['cancelPayment', () => this.cancelPayment(fieldName as actionRecord, page)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectPaymentTypePBA(paymentOptions: actionRecord, page: Page) { 
    await page.waitForTimeout(2000);
    await performAction('clickRadioButton', { option: paymentOptions.option });
    await performActions('Select a PBA',['select', paymentOptions.label1, paymentOptions.pbaIndex]);
    const locator = page.locator('button', { hasText: 'Confirm payment' });
   // const locator = page.locator('button:has-text("Confirm payment")');
    await expect(locator).toBeDisabled();
    await performAction('inputText', paymentOptions.label2, paymentOptions.referenceText);
    await page.click('body');

    //await page.keyboard.press('Tab');
    await expect(locator).toBeEnabled();

    await performAction('clickButton', paymentOptions.confirmButton);
}
  private async selectPaymentByCard(paymentOptions: actionRecord, page: Page) { 
    await page.waitForTimeout(2000);
    await performAction('clickRadioButton', { option: paymentOptions.option });
    await performAction('clickButton', paymentOptions.continueButton)
}

 private async enterCardDetails(cardDetails: actionRecord, page: Page) { 
    await performAction('inputText', cardDetails.cardLabel, cardDetails.cardText);
    await performAction('inputText', cardDetails.monthLabel, cardDetails.monthText);
    await performAction('inputText', cardDetails.yearLabel, cardDetails.yearText);
    await performAction('inputText', cardDetails.nameLabel, cardDetails.nameText);
    await performAction('inputText', cardDetails.cardSecCodeLabel, cardDetails.cardSecText);
}

 private async enterBillingAddress(billingDetails: actionRecord, page: Page){
    await performAction('inputText',billingDetails.addressLabel1 , billingDetails.addressText1);
    await performAction('inputText',billingDetails.addressLabel2 , billingDetails.addressText2);
    await performAction('inputText',billingDetails.townLabel , billingDetails.townText);
    await performAction('inputText',billingDetails.countryLabel , billingDetails.countryText);
    await performAction('inputText',billingDetails.postCodeLabel , billingDetails.postCodeText)
 }

 private async enterContactDetails(contactDetails: actionRecord, page: Page){
    await performAction('inputText',contactDetails.label , contactDetails.text);
    await performAction('clickButton', contactDetails.continueButton)
 }
 
 private async confirmPayment(confirmPay: actionRecord, page: Page){
    await performAction('clickButton', confirmPay.confirmButton)
 }

 private async cancelPayment(cancelPay: actionRecord, page: Page){
    await performAction('clickButton', cancelPay.cancelButton)

 }
 
}
