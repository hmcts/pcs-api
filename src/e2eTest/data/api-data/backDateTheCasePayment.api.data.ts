const paymentApiBaseUrl = (): string => {
  const env = (process.env.ENVIRONMENT || 'aat').toLowerCase();
  return process.env.PAYMENT_API_URL
    ?? `http://payment-api-${env}.service.core-compute-${env}.internal`;
};

export const backDateTheCasePaymentApiData = {
  backDateTheCasePaymentApiInstance: () => ({
    baseURL: paymentApiBaseUrl(),
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      Accept: '*/*',
    },
  }),
  backDateTheCasePaymentApiEndPoint: `/payments/ccd_case_reference/${process.env.CASE_NUMBER}/lag_time/10`,
};
