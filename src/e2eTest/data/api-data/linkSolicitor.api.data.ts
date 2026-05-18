
export const linkSolicitorApiData = {
  makeAnApplicationEventName: 'makeAnApplication',
  linkSolicitorApiEndPoint: (): string => `${process.env.MANAGE_CASE_BASE_URL}/testing-support/link-defendant-solicitor-to-party/${process.env.CASE_NUMBER}/${process.env.Defendant_ID}`,
};

//  linkSolicitorApiInstance: (): AxiosRequestConfig => ({
//    baseURL: process.env.DATA_STORE_URL_BASE,
//     headers: {
//       Authorization: `Bearer ${process.env.SOLICITOR_ACCESS_TOKEN}`,
//       ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
//       'Content-Type': 'application/json',
//       experimental: 'experimental',
//       Accept: '*/*',
//     },
//   }),

