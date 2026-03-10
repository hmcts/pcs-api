import { getCaseTypeId } from '@utils/common/caseType.utils';

export const createCaseEventTokenApiData = {
  createCaseEventTokenApiInstance: () => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      'experimental': 'experimental',
      'Accept': '*/*',
    }
  }),
  createCaseEventTokenApiEndPoint: `/case-types/${getCaseTypeId()}/event-triggers/createPossessionClaim`,
};
