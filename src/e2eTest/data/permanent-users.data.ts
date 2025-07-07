export interface UserCredentials {
  email: string;
  password: string;
  temp?: boolean;
  roles: string[];
}
export const permanentUsersData: Record<string, UserCredentials> = {
  superUser: {
    email: 'caseworker-9129314@test.aat',
    password: process.env.PCS_IDAM_TEST_USER_PASSWORD || 'Pa$$w0rd',
    temp: false,
    roles: ['caseworker', 'caseworker-pcs'],
  }
};
