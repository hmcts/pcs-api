import { AxiosRequestConfig } from 'axios';

export const fetchPINsApiData = {
  fetchPINSApiInstance: (): AxiosRequestConfig => ({
    baseURL: process.env.CASE_API_URL,
    headers: { ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`, 'Content-Type': 'application/json', experimental: 'experimental', Accept: '*/*' },
  }),
  fetchPINsApiEndPoint: (): string => `/testing-support/pins/${process.env.CASE_NUMBER}`,
};
