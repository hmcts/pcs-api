export interface UserCredentials {
  email: string;
  password: string;
  temp?: boolean;
  roles: string[];
}
export const permanentUsersData: Record<string, UserCredentials> = {
  superUser: {
    email: 'testuser@test.aat',
    password: process.env.PCS_IDAM_TEST_USER_PASSWORD || '',
    temp: false,
    roles: ['caseworker', 'caseworker-pcs'],
  }//sample user provided for reference-need to be updated with valid permanent users
};
