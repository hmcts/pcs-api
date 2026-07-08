import { AxiosRequestConfig } from 'axios';

export const paymentApiData = {
  paymentApiInstance: (): AxiosRequestConfig => ({
    baseURL: process.env.PCS_API_URL,
    headers: {
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      Accept: '*/*',
      experimental: 'experimental',
    },
  }),

  getFeePaymentInfoApiEndPoint: (): string => `/testing-support/fee-payment-info/${process.env.CASE_NUMBER}`,

  updatePaymentApiEndPoint: '/payment-update',

  paymentUpdatePayload: (requestReference: string) => ({
    service_request_reference: requestReference,
    ccd_case_number: process.env.CASE_NUMBER,
    service_request_amount: 41500,
    service_request_status: 'Paid',
    payments: {
      payment_amount: 2500,
      payment_reference: 'RC-1780-3235-2218-3722',
      payment_method: 'payment by account',
      case_reference: '098DC868',
      account_number: 'PBA0084541',
    },
  }),
};
