import { v4 as uuidv4 } from 'uuid';

export interface TestConfig {
  idamUrl: string;
  idamTestingSupportUrl: string;
  loginEndpoint: string;
  grantType: string;
  scope: string;
  clientId: string;
}
export interface UserData {
  password: string | undefined;
  user: {
    id: string;
    email: string;
    forename: string;
    surname: string;
    roleNames: string[];
  };
}

export function buildUserDataWithRole(role: string, password: string): UserData {
  return {
    password,
    user: {
      id: uuidv4(),
      email: `pcs-${role}-${Math.random().toString(36).slice(2, 9).toLowerCase()}@gmail.com`,
      forename: `fn_${role}_${Math.random().toString(36).slice(2, 15)}`,
      surname: `sn_${role}_${Math.random().toString(36).slice(2, 15)}`,
      roleNames: [role],
    },
  };
}
