

export const amOrgRoleMappingServiceApiData = {
  amOrgRoleMappingServiceApiInstance: () => ({
    baseURL: process.env.AM_ORG_ROLE_MAPPING,
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN_AM}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN_AM}`,
      'Content-Type': 'application/json',
      Accept: '*/*'
    }
  }),
  createOrgMappingApiEndpoint: (userType: string) =>
    `/am/testing-support/createOrgMapping?userType=${encodeURIComponent(userType)}`
};
