import {staffUidsForOrgMapping} from '@data/user-data/staff.user.data';

/** AM testing-support `UserRequest` body: `{ "userIds": ["<uuid>", ...] }`. */
export type CreateOrgMappingPayload = {
  userIds: string[];
};

export const amOrgRoleMappingServiceApiData = {
  amOrgRoleMappingServiceApiInstance: () => ({
    baseURL: process.env.AM_ORG_ROLE_MAPPING,
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN_AM}`,
      ServiceAuthorization: `${process.env.SERVICE_AUTH_TOKEN_AM}`,
      'Content-Type': 'application/json',
      Accept: '*/*'
    }
  }),
  createOrgMappingApiEndpoint: (userType: string) =>
    `/am/testing-support/createOrgMapping?userType=${encodeURIComponent(userType)}`,
  /** Postman body shape; userIds come from `staff.user.data.ts` uids. */
  createOrgMappingPayload: (): CreateOrgMappingPayload => ({
    userIds: staffUidsForOrgMapping()
  })
};
