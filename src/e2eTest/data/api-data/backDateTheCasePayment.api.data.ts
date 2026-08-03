export const backDateTheCasePaymentApiData = {
  backDateTheCasePaymentApiInstance: () => ({
    baseURL: `http://payment-api-aat.service.core-compute-aat.internal`,
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json'
    },
  }),
  backDateTheCasePaymentApiEndPoint: () => `/payments/ccd_case_reference/${process.env.CASE_NUMBER}/lag_time/10`,
}
