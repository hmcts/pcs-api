import { user } from "@data/user-data";

export const fetchCurrentUserTokenApiData = {
  fetchCurrentUserTokenApiInstance: () => ({
    baseURL: process.env.CASE_API_URL,
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
      'Content-Type': 'application/json',
      'experimental': 'experimental',
      'Accept': '*/*',
    },
  }),
  fetchCurrentUserApiEndPoint: (): string => `${process.env.IDAM_TESTING_SUPPORT_URL}/test/idam/users?email=${user.claimantSolicitor.email}`,
};