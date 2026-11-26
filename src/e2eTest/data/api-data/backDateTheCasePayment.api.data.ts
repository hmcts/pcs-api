export const backDateTheCasePaymentApiData = {
  backDateTheCasePaymentApiInstance: () => ({
    baseURL: process.env.E2E_PAYMENT_API_URL,
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json'
    },
  }),
  backDateTheCasePaymentApiEndPoint: () => `/payments/ccd_case_reference/${process.env.CASE_NUMBER}/lag_time/10`,
}
