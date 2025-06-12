import { TokenEndpointResponse } from 'oauth4webapi';

import { UserData,buildUserDataWithRole } from './testConfig.helper';

import { request, retriedRequest } from './rest.helper';
import config from "@data/config.data";

const testConfig = config.iDam;
const username = process.env.IDAM_SYSTEM_USERNAME as string;
const password = process.env.IDAM_SYSTEM_USER_PASSWORD as string;
const clientSecret = process.env.PCS_API_IDAM_SECRET as string;


export  async  function createUser(role: string[]): Promise<{ userData: UserData; password: string }> {
    const password = process.env.PCS_FRONTEND_IDAM_USER_TEMP_PASSWORD as string;
    const userData = buildUserDataWithRole(role, password);
    await createAccount(userData);
    return { userData, password };
}

export async function createAccount(userData: UserData): Promise<Response | unknown> {
  try {
    const authToken = await getAccessTokenFromIdam();

    return retriedRequest(
      `${testConfig.idamTestingSupportUrl}/test/idam/users`,
      { 'Content-Type': 'application/json', Authorization: `Bearer ${authToken}` },
      JSON.stringify(userData) as BodyInit
    ).then(response => {
      return response.json();
    });
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error('Error creating account:', error);
    throw error;
  }
}

export async function deleteAccount(email: string): Promise<void> {
  try {
    const method = 'DELETE';
    await request(
      `${testConfig.idamTestingSupportUrl}/testing-support/accounts/${email}`,
      { 'Content-Type': 'application/json' },
      undefined,
      method
    );
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error('Error deleting account:', error);
    throw error;
  }
}

export async function getAccessTokenFromIdam(): Promise<string> {

  const details = {
    username,
    password,
    grant_type: testConfig.grantType,
    scope: testConfig.scope,
    client_id: testConfig.clientId,
    client_secret: clientSecret,
  };
  const body = new URLSearchParams();
  for (const property in details) {
    // @ts-ignore
    const value = details[property];
    if (value !== undefined) {
      body.append(property, value);
    }
  }
  const headers = {
    'Content-Type': 'application/x-www-form-urlencoded',
  };
  const url = `${testConfig.idamUrl}/${testConfig.loginEndpoint}`; // https://idam-api.aat.platform.hmcts.net/o/token
  return request(url, headers, body)
    .then(response => response.json())
    .then((data: TokenEndpointResponse) => {
      return data.access_token;
    });

}
