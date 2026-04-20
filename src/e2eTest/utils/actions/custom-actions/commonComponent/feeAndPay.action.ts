import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { expect, Page } from '@playwright/test';
import { performAction, performActions, performValidation } from '@utils/controller';
import { enterPaymentDetails } from '@data/page-data/enterPaymentDetails.page.data';

export class FeeAndPayAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectPaymentTypePBA', () => this.selectPaymentTypePBA(fieldName as actionRecord, page)],
      ['selectPaymentByCard', () => this.selectPaymentByCard(fieldName as actionRecord, page)],
      ['enterPaymentDetails', () => this.enterPaymentDetails(fieldName as actionRecord)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectPaymentTypePBA(paymentOptions: actionRecord, page: Page) {
    const amountLabel = paymentOptions.amountLabel;
    if (typeof amountLabel === 'string' && amountLabel !== '') {
      await performValidation('elementToBeVisible', amountLabel);
    }
    const expectedAmount = paymentOptions.expectedAmount;
    if (typeof expectedAmount === 'string' && expectedAmount !== '') {
      await expect(page.getByText(expectedAmount, { exact: true })).toBeVisible();
    }
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
    const amountLabel = paymentOptions.amountLabel;
    if (typeof amountLabel === 'string' && amountLabel !== '') {
      await performValidation('elementToBeVisible', amountLabel);
    }
    const expectedAmount = paymentOptions.expectedAmount;
    if (typeof expectedAmount === 'string' && expectedAmount !== '') {
      await expect(page.getByText(expectedAmount, { exact: true })).toBeVisible();
    }
    await performAction('clickRadioButton', { option: paymentOptions.payByOption });
    await performAction('clickButton', paymentOptions.continueButton);
  }

  private async enterPaymentDetails(payDetails: actionRecord) {
    await performActions(
      'Enter details'
      , ['inputText', enterPaymentDetails.cardNoLabel, payDetails.cardInput]
      , ['inputText', enterPaymentDetails.monthTextLabel, payDetails.monthInput]
      , ['inputText', enterPaymentDetails.yearTextLabel, payDetails.yearInput]
      , ['inputText', enterPaymentDetails.nameOnCardLabel, payDetails.nameInput]
      , ['inputText', enterPaymentDetails.cardSecurityCodeLabel, payDetails.cardSecInput]
      , ['inputText', enterPaymentDetails.addressLine1TextLabel, payDetails.address1Input]
      , ['inputText', enterPaymentDetails.addressLine2TextLabel, payDetails.address2Input]
      , ['inputText', enterPaymentDetails.townOrCityTextLabel, payDetails.townInput]
      , ['inputText', enterPaymentDetails.countryLabel, payDetails.countryInput]
      , ['inputText', enterPaymentDetails.postcodeTextLabel, payDetails.postCodeInput]
      , ['inputText', enterPaymentDetails.emailLabel, payDetails.emailInput]
    );
    await performAction('clickButton', enterPaymentDetails.continueButton);
  }
}