import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import {
  addressCheckYourAnswers,
  addressDetails,
  home
} from '@data/page-data';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { expect, test } from '@utils/test-fixtures';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import {serviceRequest} from "@data/page-data/serviceRequest.page.data";
import { label } from 'allure-js-commons';
import { text } from 'stream/consumers';
import { enterPaymentDetails } from '@data/page-data/enterPaymentDetails.page.data';
import { confirmYourPayment } from '@data/page-data/confirmYourPayment.page.data';
import { cancelPayment } from '@data/page-data/cancelPayment.page.data';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadNoDefendants });
  /*await performAction('getDefendantDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadNoDefendants.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadNoDefendants.addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayloadNoDefendants
    }); */
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Service%20Request`);
  // Login and cookie consent are handled globally via storageState in global-setup.config.ts
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
 // PageContentValidation.finaliseTest();
});

test.describe('[Fee And Pay]', async () => {
  test('Fee And Pay - Pay by account PBA @PR @regression', async () => {
    await performAction('clickButton', serviceRequest.payNowLink);
    await performAction('selectPaymentTypePBA', {
      label: serviceRequest.amountToPayLabel,
      option: serviceRequest.payByAccountRadioOption,
      label1: serviceRequest.selectPBALabel,
      pbaIndex: serviceRequest.pbaIndex1,
      label2: serviceRequest.pbaReferenceLable,
      referenceText: serviceRequest.pbaReferenceInputText,
      confirmButton: serviceRequest.confirmPaymentButton,
    });
    await performValidation('mainHeader', serviceRequest.paymentSuccessMainHeader)
  });

  test('Fee And Pay - Pay by Card @PR @regression', async () => {
    await performAction('clickButton', serviceRequest.payNowLink);
    await performAction('selectPaymentByCard', {
      label: serviceRequest.amountToPayLabel,
      option: serviceRequest.payByCardRadioOption,
      continueButton: serviceRequest.continueButton
    });
    await performValidation('mainHeader', enterPaymentDetails.mainHeader)
    await performAction('enterCardDetails', {
      cardLabel: enterPaymentDetails.cardNoLabel,
      cardText: enterPaymentDetails.cardNoInputText,
      monthLabel: enterPaymentDetails.monthTextLabel,
      monthText: enterPaymentDetails.monthTextInput,
      yearLabel: enterPaymentDetails.yearTextLabel,
      yearText: enterPaymentDetails.yearTextInput,
      nameLabel: enterPaymentDetails.nameOnCardLabel,
      nameText: enterPaymentDetails.nameOnCardTextInput,
      cardSecCodeLabel: enterPaymentDetails.cardSecurityCodeLabel,
      cardSecText: enterPaymentDetails.cardSecurityCodeTextInput
    });
    await performAction('enterBillingAddress',{
      addressLabel1: enterPaymentDetails.addressLine1TextLabel,
      addressText1: enterPaymentDetails.addressLine1TextInput,
      addressLabel2: enterPaymentDetails.addressLine2TextLabel,
      addressText2: enterPaymentDetails.addressLine2TextInput,
      townLabel: enterPaymentDetails.townOrCityTextLabel,
      townText: enterPaymentDetails.TownOrCityTextInput,
      countryLabel: enterPaymentDetails.countryLabel,
      countryText: enterPaymentDetails.countryTextInput,
      postCodeLabel: enterPaymentDetails.postcodeTextLabel,
      postCodeText: enterPaymentDetails.postcodeTextInput
    });
    await performAction('enterContactDetails',{
      label: enterPaymentDetails.emailLabel,
      text: enterPaymentDetails.emailTextInput,
      continueButton: enterPaymentDetails.continueButton
    });
    await performValidation('mainHeader', confirmYourPayment.mainHeader)
    await performAction('confirmPayment',{
      confirmButton: confirmYourPayment.confirmButton
    })
    await performValidation('mainHeader', serviceRequest.paymentSuccessMainHeader)
    });

    test('Fee And Pay - Cancel Payment from You Card Details Page @regression', async () => {
    await performAction('clickButton', serviceRequest.payNowLink);
    await performAction('selectPaymentByCard', {
      label: serviceRequest.amountToPayLabel,
      option: serviceRequest.payByCardRadioOption,
      continueButton: serviceRequest.continueButton
    });
    await performValidation('mainHeader', enterPaymentDetails.mainHeader);
    await performAction('cancelPayment', { cancelButton: enterPaymentDetails.cancelPaymentButton});
    await performValidation('mainHeader', cancelPayment.mainHeader)
  });

    test('Fee And Pay - Cancel Payment from Confirm Card Details Page @regression', async () => {
       await performAction('clickButton', serviceRequest.payNowLink);
    await performAction('selectPaymentByCard', {
      label: serviceRequest.amountToPayLabel,
      option: serviceRequest.payByCardRadioOption,
      continueButton: serviceRequest.continueButton
    });
    await performValidation('mainHeader', enterPaymentDetails.mainHeader)
    await performAction('enterCardDetails', {
      cardLabel: enterPaymentDetails.cardNoLabel,
      cardText: enterPaymentDetails.cardNoInputText,
      monthLabel: enterPaymentDetails.monthTextLabel,
      monthText: enterPaymentDetails.monthTextInput,
      yearLabel: enterPaymentDetails.yearTextLabel,
      yearText: enterPaymentDetails.yearTextInput,
      nameLabel: enterPaymentDetails.nameOnCardLabel,
      nameText: enterPaymentDetails.nameOnCardTextInput,
      cardSecCodeLabel: enterPaymentDetails.cardSecurityCodeLabel,
      cardSecText: enterPaymentDetails.cardSecurityCodeTextInput
    });
    await performAction('enterBillingAddress',{
      addressLabel1: enterPaymentDetails.addressLine1TextLabel,
      addressText1: enterPaymentDetails.addressLine1TextInput,
      addressLabel2: enterPaymentDetails.addressLine2TextLabel,
      addressText2: enterPaymentDetails.addressLine2TextInput,
      townLabel: enterPaymentDetails.townOrCityTextLabel,
      townText: enterPaymentDetails.TownOrCityTextInput,
      countryLabel: enterPaymentDetails.countryLabel,
      countryText: enterPaymentDetails.countryTextInput,
      postCodeLabel: enterPaymentDetails.postcodeTextLabel,
      postCodeText: enterPaymentDetails.postcodeTextInput
    });
    await performAction('enterContactDetails',{
      label: enterPaymentDetails.emailLabel,
      text: enterPaymentDetails.emailTextInput,
      continueButton: enterPaymentDetails.continueButton
    });
    await performValidation('mainHeader', confirmYourPayment.mainHeader)
    await performAction('cancelPayment', { cancelButton: confirmYourPayment.cancelPaymentButton});
    await performValidation('mainHeader', cancelPayment.mainHeader)
  });
});