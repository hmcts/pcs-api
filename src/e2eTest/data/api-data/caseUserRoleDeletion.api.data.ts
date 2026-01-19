export const caseUserRoleDeletionApiData = {
  deleteCaseUsersApiInstance: () => ({
    baseURL: process.env.DATA_STORE_URL_BASE || 'http://ccd-data-store-api-aat.service.core-compute-aat.internal',
    headers: {
      Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
      ServiceAuthorization: `${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      'Accept': '*/*',
    }
  }),
  deleteCaseUsersApiEndPoint: '/case-users',
  deleteCaseUsersPayload: (caseId: string, userId: string, caseRole: string) => ({
    case_users: [
      {
        case_id: caseId,
        user_id: userId,
        case_role: caseRole
      }
    ]
  })
};
