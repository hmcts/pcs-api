export interface UserCredentials {
  email: string;
  password: string;
  temp?: boolean;
  roles: string[];
}
export const permanentUsersData: Record<string, UserCredentials> = {
  superUser: {
    email: process.env.PCS_CASEWORKER_USERNAME || '',
    password: process.env.PCS_CASEWORKER_USERNAMEPASSWORD || '',
    temp: false,
    roles: ['caseworker', 'caseworker-pcs'],
  }
};
//sample test user provided for reference from civil, need to be updated with valid PCS permanent users when available.
