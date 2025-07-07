export interface UserCredentials {
  email: string;
  password: string;
  temp?: boolean;
  roles: string[];
}
export const permanentUsers: Record<string, UserCredentials> = {
  caseworker: {
    email: 'caseworker-9129314@test.aat',
    password: process.env.PCS_IDAM_USER_USER_PASSWORD || 'Pa$$w0rd',
    temp: false,
    roles: ['caseworker', 'caseworker-pcs'],
  },
};
