import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { expect, test } from '@utils/test-fixtures';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { cancelPayment, confirmYourPayment, enterPaymentDetails, serviceRequest } from '@data/page-data';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadNoDefendants });
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Service%20Request`);
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Service%20Request`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
});

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
});

test.describe('[Common Component Fee And Pay]', async () => {
  test('Fee And Pay - Pay by account PBA @nightly', async () => {
    await performAction('clickButton', serviceRequest.payNowLink);
    await performAction('selectPaymentTypePBA', {
      amountLabel: serviceRequest.amountToPayLabel,
      expectedAmount: serviceRequest.amount404,
      payByOption: serviceRequest.payByAccountRadioOption,
      pbaLabel: serviceRequest.selectPBALabel,
      pbaIndex: serviceRequest.pbaIndex1,
      referenceLabel: serviceRequest.pbaReferenceLable,
      referenceText: serviceRequest.pbaReferenceInputText,
      confirmButton: serviceRequest.confirmPaymentButton,
    });
    await performValidation('mainHeader', serviceRequest.paymentSuccessMainHeader);
  });

  test('Fee And Pay - Pay by Card @nightly', async () => {
    await performAction('clickButton', serviceRequest.payNowLink);
    await performAction('selectPaymentByCard', {
      amountLabel: serviceRequest.amountToPayLabel,
      payByOption: serviceRequest.payByCardRadioOption,
      continueButton: serviceRequest.continueButton
    });
    await performValidation('mainHeader', enterPaymentDetails.mainHeader);
    await performAction('enterPaymentDetails', {
      cardInput: enterPaymentDetails.cardNoInputText,
      monthInput: enterPaymentDetails.monthTextInput,
      yearInput: enterPaymentDetails.yearTextInput,
      nameInput: enterPaymentDetails.nameOnCardTextInput,
      cardSecInput: enterPaymentDetails.cardSecurityCodeTextInput,
      address1Input: enterPaymentDetails.addressLine1TextInput,
      address2Input: enterPaymentDetails.addressLine2TextInput,
      townInput: enterPaymentDetails.TownOrCityTextInput,
      countryInput: enterPaymentDetails.countryTextInput,
      postCodeInput: enterPaymentDetails.postcodeTextInput,
      emailInput: enterPaymentDetails.emailTextInput
    });
    await performValidation('mainHeader', confirmYourPayment.mainHeader);
    await performAction('clickButton', confirmYourPayment.confirmButton);
    await performValidation('mainHeader', serviceRequest.paymentSuccessMainHeader);
  });

  test('Fee And Pay - Cancel Payment from You Card Details Page @nightly', async () => {
    await performAction('clickButton', serviceRequest.payNowLink);
    await performAction('selectPaymentByCard', {
      amountLabel: serviceRequest.amountToPayLabel,
      payByOption: serviceRequest.payByCardRadioOption,
      continueButton: serviceRequest.continueButton
    });
    await performValidation('mainHeader', enterPaymentDetails.mainHeader);
    await performAction('clickButton', enterPaymentDetails.cancelPaymentButton);
    await performValidation('mainHeader', cancelPayment.mainHeader);
  });

  test('Fee And Pay - Cancel Payment from Confirm Card Details Page @nightly', async () => {
    await performAction('clickButton', serviceRequest.payNowLink);
    await performAction('selectPaymentByCard', {
      amountLabel: serviceRequest.amountToPayLabel,
      payByOption: serviceRequest.payByCardRadioOption,
      continueButton: serviceRequest.continueButton
    });
    await performValidation('mainHeader', enterPaymentDetails.mainHeader);
    await performAction('enterPaymentDetails', {
      cardInput: enterPaymentDetails.cardNoInputText,
      monthInput: enterPaymentDetails.monthTextInput,
      yearInput: enterPaymentDetails.yearTextInput,
      nameInput: enterPaymentDetails.nameOnCardTextInput,
      cardSecInput: enterPaymentDetails.cardSecurityCodeTextInput,
      address1Input: enterPaymentDetails.addressLine1TextInput,
      address2Input: enterPaymentDetails.addressLine2TextInput,
      townInput: enterPaymentDetails.TownOrCityTextInput,
      countryInput: enterPaymentDetails.countryTextInput,
      postCodeInput: enterPaymentDetails.postcodeTextInput,
      emailInput: enterPaymentDetails.emailTextInput
    });
    await performValidation('mainHeader', confirmYourPayment.mainHeader);
    await performAction('clickButton', confirmYourPayment.cancelPaymentButton);
    await performValidation('mainHeader', cancelPayment.mainHeader);
  });
});