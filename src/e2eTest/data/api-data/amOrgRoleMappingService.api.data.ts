export const caseUserRoleDeletionApiData = {
  amOrgRoleMappingServiceApiInstance: () => ({
    baseURL: process.env.AM_ORG_ROLE_MAPPING,
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN_AM}`,
      ServiceAuthorization: `${process.env.SERVICE_AUTH_TOKEN_AM}`,
      'Content-Type': 'application/json',
      'Accept': '*/*',
    }
  }),
  amOrgRoleMappingServiceApiEndPoint: '/am/testing-support/createOrgMapping?userType=CASEWORKER',
  amOrgRoleMappingServicePayload: (caseId: string, userId: string, caseRole: string) => ({
    "userIds": [
      "8ad92f1f-0a04-4f43-bcf3-b94f26642730"
    ]
  })
};
