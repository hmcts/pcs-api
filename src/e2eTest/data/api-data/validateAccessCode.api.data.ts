import { AxiosRequestConfig } from 'axios';

export const validateAccessCodeApiData = {
  validateAccessCodeApiInstance: (): AxiosRequestConfig => ({
    baseURL: process.env.CASE_API_URL,
    headers: { Authorization: `Bearer ${process.env.CITIZEN_ACCESS_TOKEN}`, ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`, 'Content-Type': 'application/json', Accept: '*/*', experimental: 'experimental' },
  }),
  validateAccessCodeApiEndPoint: (): string => `/cases/${process.env.CASE_NUMBER}/validate-access-code`,
};
