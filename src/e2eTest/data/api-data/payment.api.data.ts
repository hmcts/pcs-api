import { AxiosRequestConfig } from 'axios';

export const paymentApiData = {
  paymentApiInstance: (): AxiosRequestConfig => ({
    baseURL: process.env.CASE_API_URL,
    headers: {
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      Accept: '*/*',
      experimental: 'experimental',
    },
  }),

  getFeePaymentInfoApiEndPoint: (): string => `/testing-support/fee-payment-info/${process.env.CASE_NUMBER}`,

  updatePaymentApiEndPoint: '/payment-update',

  paymentUpdatePayload: (requestReference: string, feeAmountInPounds: number) => {
    if (!Number.isFinite(feeAmountInPounds) || feeAmountInPounds <= 0) {
      throw new Error(`Invalid fee amount received for service request ${requestReference}: ${feeAmountInPounds}`);
    }
    const serviceRequestAmount = Math.round(feeAmountInPounds * 100);
    const accountNumber = process.env.PBA_ACCOUNT_NUMBER || 'PBA0084541';
    console.log('[PAYMENT API] fee (pounds) =', feeAmountInPounds, '-> pence =', serviceRequestAmount);

    return {
    service_request_reference: requestReference,
    ccd_case_number: process.env.CASE_NUMBER,
    service_request_amount: serviceRequestAmount,
    service_request_status: 'Paid',
    payments: {
      payment_amount: serviceRequestAmount,
      payment_reference: `TEST-${requestReference}`,
      payment_method: 'payment by account',
      case_reference: process.env.CASE_NUMBER,
      account_number: accountNumber,
    },
    };
  },

};
