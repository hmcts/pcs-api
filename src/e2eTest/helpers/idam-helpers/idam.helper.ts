import ConfigData from "@data/config.data";
import { TokenEndpointResponse } from 'oauth4webapi';

import { request, retriedRequest } from './rest.helper';
import { UserData } from './testConfig';

const testConfig = ConfigData.iDam;
const username = process.env.IDAM_SYSTEM_USERNAME as string;
const password = process.env.IDAM_SYSTEM_USER_PASSWORD as string;
const clientSecret = process.env.PCS_API_IDAM_SECRET as string;

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
  //let responsePromise = await retriedRequest(url, headers, body, 'POST', 200);
  return request(url, headers, body)
    .then(response => response.json())
    .then((data: TokenEndpointResponse) => {
      return data.access_token;
    });
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
    // eslint-disable-next-line no-console
    console.log('Account deleted post test completion: ' + email);
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error('Error deleting account:', error);
    throw error;
  }
}

export default deleteAccount;
