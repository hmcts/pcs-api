import * as fs from 'fs';
import * as path from 'path';
import TestConfig from "../../../config/test.config";
import { TokenEndpointResponse } from 'oauth4webapi';
import { request, retriedRequest } from './rest.helper';
import {buildUserDataWithRole, UserData} from './testConfig';
import {permanentUsersData} from "@data/permanent-users.data";

const testConfig = TestConfig.iDam;
const username = process.env.IDAM_SYSTEM_USERNAME as string;
const password = process.env.IDAM_SYSTEM_USER_PASSWORD as string;
const clientSecret = process.env.PCS_API_IDAM_SECRET as string;

export async function createTempUser(
  userKey: string,
  roles: string[]
): Promise<void> {
  const tempPassword = process.env.PCS_IDAM_TEST_USER_PASSWORD || '';
  const userData = buildUserDataWithRole(roles, tempPassword,userKey);
  await createAccount(userData);

  setTempUser(userKey, {
    email: userData.user.email,
    password: tempPassword,
    temp: true,
    roles,
  });
}

export async function cleanupTempUsers(): Promise<void> {
  const all = getAllUsers();
  for (const [key, creds] of Object.entries(all)) {
    if (creds.temp) {
      try {
        await deleteAccount(creds.email);
        deleteTempUser(key);
      } catch (err) {
      }
    }
  }
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


export interface UserCredentials {
  email: string;
  password: string;
  temp?: boolean;
  roles: string[];
}

const storePath = path.resolve(__dirname, './../../../data/.temp-users.data.json');

let tempUsers: Record<string, UserCredentials> = {};

if (fs.existsSync(storePath)) {
  const data = fs.readFileSync(storePath, 'utf-8');
  tempUsers = JSON.parse(data);
}

function saveTempUsers() {
  fs.writeFileSync(storePath, JSON.stringify(tempUsers, null, 2));
}

export function setTempUser(key: string, creds: UserCredentials) {
  tempUsers[key] = creds;
  saveTempUsers();
}

export function deleteTempUser(key: string) {
  delete tempUsers[key];
  saveTempUsers();
}

export function getUser(key: string): UserCredentials | undefined {
  return tempUsers[key] || permanentUsersData[key];
}

export function getAllUsers(): Record<string, UserCredentials> {
  return { ...permanentUsersData, ...tempUsers };
}
