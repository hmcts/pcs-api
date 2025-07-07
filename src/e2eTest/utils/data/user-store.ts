
export interface UserCredentials {
  email: string;
  password: string;
  temp?: boolean;
  roles: string[];
}

export const userStore: Record<string, UserCredentials> = {
  caseworker1: {
    email: 'sampleuser@pcs.com',
    password: process.env.PCS_FRONTEND_IDAM_USER_TEMP_PASSWORD || 'Pa$$w0rd',
    temp: false,
    roles: ['caseworker', 'basic-access'],
  },

};
