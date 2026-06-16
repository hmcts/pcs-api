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
import { cancelPayment, caseSummary, confirmYourPayment, enterPaymentDetails, serviceRequest } from '@data/page-data';
import { history } from '@data/page-data/history.page.data';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadNoDefendants });
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
});

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
});

test.describe('[Common Component Fee And Pay] @nightly @CC @caseFlags' , async () => {
  test('Fee And Pay - Pay by account PBA', async () => {
    await performAction('clickPayNowLink', serviceRequest.payNowLink);
    await performAction('selectPaymentTypePBA', {
      amountLabel: serviceRequest.amountToPayLabel,
      expectedAmount: serviceRequest.amount404,
      payByOption: serviceRequest.payByAccountRadioOption,
      pbaLabel: serviceRequest.selectPBALabel,
      pbaValue: serviceRequest.pbaIndex1,
      referenceLabel: serviceRequest.pbaReferenceLable,
      referenceText: serviceRequest.pbaReferenceInputText,
      confirmButton: serviceRequest.confirmPaymentButton,
    });
    await performValidation('mainHeader', serviceRequest.paymentSuccessMainHeader);
    await performAction('verifyStatusInHistoryAndSummaryTab', {
      serviceReqLink: serviceRequest.viewServiceRequestsLink,
      serviceReqTab: caseSummary.servieRequestTab,
      historyTab: caseSummary.HistoryTab,
      status: serviceRequest.paidStatus,
      endState: history.endStateTableHeader,
      historyStatus: history.caseIssuedTableHeader
    });
  });

  test('Fee And Pay - Pay by Card @nightly @feeAndPay', async () => {
    await performAction('clickPayNowLink', serviceRequest.payNowLink);
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
    await performAction('verifyStatusInHistoryAndSummaryTab', {
      serviceReqLink: serviceRequest.returnToServiceRequestLink,
      serviceReqTab: caseSummary.servieRequestTab,
      historyTab: caseSummary.HistoryTab,
      status: serviceRequest.paidStatus,
      endState: history.endStateTableHeader,
      historyStatus: history.caseIssuedTableHeader
    });
  });

  test('Fee And Pay - Cancel Payment from You Card Details Page @nightly @feeAndPay', async () => {
    await performAction('clickPayNowLink', serviceRequest.payNowLink);
    await performAction('selectPaymentByCard', {
      amountLabel: serviceRequest.amountToPayLabel,
      payByOption: serviceRequest.payByCardRadioOption,
      continueButton: serviceRequest.continueButton
    });
    await performValidation('mainHeader', enterPaymentDetails.mainHeader);
    await performAction('clickButton', enterPaymentDetails.cancelPaymentButton);
    await performValidation('mainHeader', cancelPayment.mainHeader);
    await performAction('clickButton', cancelPayment.continueButton);
    await performAction('verifyStatusInHistoryAndSummaryTab', {
      serviceReqLink: cancelPayment.returnServiceReqLink,
      serviceReqTab: caseSummary.servieRequestTab,
      historyTab: caseSummary.HistoryTab,
      status: serviceRequest.notPaidStatus,
      endState: history.endStateTableHeader,
      historyStatus: history.pendingCaseIssuedTableHeader
    });
  });

  test('Fee And Pay - Cancel Payment from Confirm Card Details Page @nightly @feeAndPay', async () => {
    await performAction('clickPayNowLink', serviceRequest.payNowLink);
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
    await performAction('clickButton', cancelPayment.continueButton);
    await performAction('verifyStatusInHistoryAndSummaryTab', {
      serviceReqLink: cancelPayment.returnServiceReqLink,
      serviceReqTab: caseSummary.servieRequestTab,
      historyTab: caseSummary.HistoryTab,
      status: serviceRequest.notPaidStatus,
      endState: history.endStateTableHeader,
      historyStatus: history.pendingCaseIssuedTableHeader
    });
  });
});
