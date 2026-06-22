import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { expect, Page } from '@playwright/test';
import { performAction, performActions, performValidation } from '@utils/controller';
import { enterPaymentDetails } from '@data/page-data/enterPaymentDetails.page.data';
import {caseSummary, serviceRequest} from '@data/page-data';
import {backDateTheCasePaymentApiData} from "@data/api-data/backDateTheCasePayment.api.data";
import Axios from "axios";
import {refundAndRemission} from "@data/user-data/staff.user.data";

export class FeeAndPayAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectPaymentTypePBA', () => this.selectPaymentTypePBA(fieldName as actionRecord, page)],
      ['selectPaymentByCard', () => this.selectPaymentByCard(fieldName as actionRecord, page)],
      ['enterPaymentDetails', () => this.enterPaymentDetails(fieldName as actionRecord)],
      ['clickPayNowLink', () => this.clickPayNowLink(fieldName as actionRecord, page)],
      ['verifyStatusInHistoryAndSummaryTab', () => this.verifyStatusInHistoryAndSummaryTab(fieldName as actionRecord, page)],
      ['backDateTheCasePaymentAPI', () => this.backDateTheCasePaymentAPI()],
      ['requestRemission', () => this.requestRemission(page)],
      ['requestRefund', () => this.requestRefund(page)],
      ['approveRefund', () => this.approveRefund(page)],
      ['rejectRefund', () => this.rejectRefund(page)],
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
    await performAction('select', paymentOptions.pbaLabel, paymentOptions.pbaValue);
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

  private async clickPayNowLink( pay: actionData, page: Page) {
    const maxRetries = 10;
    const payNowText = String(pay);
    for (
      let retryCount = 0;
      retryCount < maxRetries;
      retryCount++
    ) {
      await performAction('clickTab', caseSummary.servieRequestTab);
      const payNowLocator = page.getByText(payNowText,{ exact: true });
      let isPayNowVisible = false;
      for (let i = 0; i < 10; i++) {
        isPayNowVisible = await payNowLocator.isVisible();
        if (isPayNowVisible) {
          break;
        }
        await page.waitForTimeout(500);
      }
      if (isPayNowVisible) {
        await payNowLocator.scrollIntoViewIfNeeded();
        await payNowLocator.click();
        return;
      }
      await performAction('clickTab', caseSummary.HistoryTab );
    }
    throw new Error(
      `${payNowText} link was not visible after maximum retries`
    );
  }

  private async verifyStatusInHistoryAndSummaryTab(statusDetails: actionRecord, page: Page) {
    //Verify status only in AAT env as its NOT working in preview
    const currentUrl = process.env.MANAGE_CASE_BASE_URL;
    console.log(process.env.MANAGE_CASE_BASE_URL);
    if (currentUrl && currentUrl.includes('api-pr')) {
      console.log('Verification steps skipped as this is NOT working in PREVIEW env. POFCC-229');
    } else {
      console.log('Verifying payment status');
      await performAction('clickButton', statusDetails.serviceReqLink);
      await performAction('clickTab', statusDetails.historyTab);
      //Implementing retry login because POFCC-238
      const maxRetries = 10;
      let isStatusUpdated = false;
      for (let retryCount = 0; retryCount < maxRetries; retryCount++) {
        await page.reload();
        const endStateElement = page.locator(`th:has-text("${String(statusDetails.endState)}") ~ td span.text-16`);
        const actualText = (await endStateElement.textContent())?.trim();

        if (actualText === String(statusDetails.historyStatus)) {
          isStatusUpdated = true;
          break;
        }
        await page.waitForTimeout(3000);
      }
      await performAction('clickTab', statusDetails.serviceReqTab);
      const summaryStatusElement = page.locator(`text=${String(statusDetails.status)}`);
      await expect(summaryStatusElement).toBeVisible();
    }
  }
  private async backDateTheCasePaymentAPI(): Promise<void> {
    const backDateApi = Axios.create(backDateTheCasePaymentApiData.backDateTheCasePaymentApiInstance());
    try {
      await backDateApi.patch(backDateTheCasePaymentApiData.backDateTheCasePaymentApiEndPoint());
      console.log(`Back date of the payment sucessful for the case ${process.env.CASE_NUMBER}`);
    } catch (error: any) {
      const status = error?.response?.status;
      const responseBody = error?.response?.data;
      console.error('Payment back date failed:', { status, responseBody, caseNumber: process.env.CASE_NUMBER });
      throw new Error(`Payment back date process failed - HTTP ${status ?? 'unknown'}.`);
    }
  }

  private async navigateToServiceRequestReview(page: Page): Promise<void> {
    await performAction('clickLink', serviceRequest.viewLink);
    await performAction('clickTab', caseSummary.servieRequestTab);
    await page.getByRole('link', { name: serviceRequest.reviewLink }).first().click();
  }

  private async requestRemission(page: Page): Promise<void> {
    await this.navigateToServiceRequestReview(page);
    await performAction('clickButton', serviceRequest.addRemissionButton);
    await performAction('inputText', serviceRequest.remissionCodeLabel, serviceRequest.remissionCodeValue);
    await performAction('clickButton', serviceRequest.continueButton);
    await page.getByRole('spinbutton', { name: 'amount' }).fill(serviceRequest.remissionAmountValue);
    await performAction('clickButton', serviceRequest.continueButton);
    await performAction('clickButton', serviceRequest.addRemissionButton);
    await performAction('clickButton', serviceRequest.continueButton);
    await performAction('inputText', serviceRequest.refundInformationLabel, refundAndRemission.requesterEmail);
    await performAction('clickButton', serviceRequest.continueButton);
    await performAction('clickButton', serviceRequest.submitRefundButton);
  }

  private async selectFeeForRefund(page: Page, feeName: string, refundAmount: string): Promise<void> {
    const feeRow = page.locator('tr[formarrayname="feesList"]').filter({ hasText: feeName });
    await feeRow.getByRole('checkbox').check();
    await feeRow.locator('input[formcontrolname="refund_amount"]').fill(refundAmount);
  }

  private async requestRefund(page: Page): Promise<void> {
    await this.navigateToServiceRequestReview(page);
    await performAction('clickButton', serviceRequest.issueRefundButton);
    await this.selectFeeForRefund(page, serviceRequest.recoveryOfLandCountyCourtCheckbox, serviceRequest.refundAmountValue);
    await performAction('clickButton', serviceRequest.continueButton);
    await page.locator('.govuk-radios__item').filter({ hasText: 'Amended claim' }).locator('input').check();
    await performAction('clickButton', serviceRequest.continueButton);
    await performAction('inputText', serviceRequest.refundInformationLabel, refundAndRemission.requesterEmail);
    await performAction('clickButton', serviceRequest.continueButton);
    await performAction('clickButton', serviceRequest.submitRefundButton);
  }

  private async navigateToRefundsReview(page: Page): Promise<void> {
    await performAction('clickLink', serviceRequest.viewLink);
    await performAction('clickTab', caseSummary.servieRequestTab);
    await page.locator('ccpay-refund-status').getByRole('link', { name: serviceRequest.reviewLink }).click();
  }

  private async approveRefund(page: Page): Promise<void> {
    await this.navigateToRefundsReview(page);
    await performAction('clickButton', serviceRequest.processRefundButton);
    await page.getByRole('radio', { name: serviceRequest.approveRefundOption, exact: true }).check();
    await performAction('clickButton', serviceRequest.submitButton);
    await performValidation('mainHeader', serviceRequest.refundApprovedHeader);
    await performAction('clickLink', serviceRequest.returnToCaseLink);
    await performAction('clickTab', caseSummary.servieRequestTab);
    await page.getByLabel(caseSummary.servieRequestTab).getByText(serviceRequest.refundsTab).click();
    await expect(page.getByRole('cell', { name: serviceRequest.approvedStatus, exact: true })).toBeVisible();
  }

  private async rejectRefund(page: Page): Promise<void> {
    await this.navigateToRefundsReview(page);
    await performAction('clickButton', serviceRequest.processRefundButton);
    await page.getByRole('radio', { name: serviceRequest.rejectRefundOption, exact: true }).check();
    await page.getByRole('radio', { name: serviceRequest.rejectReasonOption, exact: true }).check();
    await performAction('clickButton', serviceRequest.submitButton);
    await performValidation('mainHeader', serviceRequest.refundRejectedHeader);
    await performAction('clickLink', serviceRequest.returnToCaseLink);
    await performAction('clickTab', caseSummary.servieRequestTab);
    await page.getByLabel(caseSummary.servieRequestTab).getByText(serviceRequest.refundsTab).click();
    await expect(page.getByRole('cell', { name: serviceRequest.rejectedStatus, exact: true })).toBeVisible();
  }
}
