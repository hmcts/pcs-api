import * as fs from 'fs';
import * as path from 'path';
import TestConfig from "../../../config/test.config";
import {TokenEndpointResponse} from 'oauth4webapi';
import {request, retriedRequest} from './rest.helper';
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
  const userData = buildUserDataWithRole(roles, tempPassword, userKey);
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
        deleteTempUser(key);
      } catch (err) {
      }
    }
  }
}

export async function createAccount(userData: UserData): Promise<Response | unknown> {
  try {
    const authToken = await getAccessTokenFromIdam(username, password);
    return retriedRequest(
      `${testConfig.idamTestingSupportUrl}/test/idam/users`,
      {'Content-Type': 'application/json', Authorization: `Bearer ${authToken}`},
      JSON.stringify(userData) as BodyInit
    ).then(response => {
      return response.json();
    });
  } catch (error) {
    throw error;
  }
}


export async function getAccessTokenFromIdam(username: string,password:string): Promise<string> {
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


export interface UserDetails {
  email: string;
  password: string;
  temp?: boolean;
  roles: string[];
}

const storePath = path.resolve(__dirname, './../../../data/.temp-users.data.json');

let tempUsers: Record<string, UserDetails> = {};

if (fs.existsSync(storePath)) {
  const data = fs.readFileSync(storePath, 'utf-8');
  tempUsers = JSON.parse(data);
}

function saveTempUsers() {
  fs.writeFileSync(storePath, JSON.stringify(tempUsers, null, 2));
}

export function setTempUser(key: string, creds: UserDetails) {
  tempUsers[key] = creds;
  saveTempUsers();
}

export function deleteTempUser(key: string) {
  delete tempUsers[key];
  saveTempUsers();
}

export function getUser(key: string): UserDetails | undefined {
  return tempUsers[key] || permanentUsersData[key];
}

export function getAllUsers(): Record<string, UserDetails> {
  return {...permanentUsersData, ...tempUsers};
}

let result: string;

export async function getS2SToken(): Promise<string> {
  const testConfig = TestConfig.authProvider;
  const body = JSON.stringify({microservice: `${testConfig.microservice}`});
  const headers = {
    'Content-Type': 'application/json',
  };
  const url = `${testConfig.url}/${testConfig.endPoint}`;
  return request(url, headers, body).then(async response => {
    if (response.status !== 200) {
      throw new Error(`Failed to get S2S token, status code: ${response.status}`);
    }
    const reader = response.body?.getReader();
    const decoder = new TextDecoder();
    result = '';
    let done = false;

    while (!done) {
      const {value, done: streamDone} = await reader?.read()!;
      done = streamDone;
      if (value) {
        result += decoder.decode(value, {stream: true});
      }
    }
    return result;
  });

};

let authToken: string = '';
export const initIdamAuthToken = async (email: string,pwd:string): Promise<void> => {
  authToken = await getAccessTokenFromIdam(email,pwd);
};

export const getIdamAuthToken = (): string => {
  console.log("idam token: " + authToken);
  return authToken;

};
let s2sToken: string = '';
export const initServiceAuthToken = async (): Promise<void> => {
  s2sToken = await getS2SToken();
};
export const getServiceAuthToken = (): string => {
  return s2sToken;
};
