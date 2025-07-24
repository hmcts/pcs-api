// src/e2eTest/utils/helpers/caseApi.ts
import Axios, {AxiosInstance, AxiosResponse} from 'axios';
import {
  getIdamAuthToken,
  getServiceAuthToken
} from '../helpers/idam-helpers/idam.helper';
import {TestConfig} from 'config/test.config';
import caseDataJson from '../../data/case.data.json';


export interface CaseWithId {
  id: string;
  state: string;
  ccd_case_number?: string;
  case_reference?: string;
}

export interface CcdTokenResponse {
  token: string;
}

export interface CcdV2Response {
  id: string;
  state: string;
  data: any;
}

export class CaseApi {
  constructor(
    private readonly axios: AxiosInstance,
  ) {
  }

  async createCase(caseType: string, appId: string): Promise<CaseWithId> {
    console.log("axios baseURL : " + this.axios.defaults.baseURL);
    console.log("resource Path : " + `/case-types/${caseType}/event-triggers/${appId}`);
    const tokenResponse: AxiosResponse<CcdTokenResponse> = await this.axios.get(
      `/case-types/${caseType}/event-triggers/${appId}`
    );
    const token = tokenResponse.data.token;
    const event = {id: `${appId}`};
    const data = caseDataJson;

    console.log("Data to be sent: ", data);
    console.log("Event to be sent: ", event);
    try {
      const response = await this.axios.post<CcdV2Response>(`/case-types/${caseType}/cases`, {
        data: caseDataJson.data,
        event: event,
        event_token: token,
      });
      return {
        id: response.data.id,
        state: response.data.state
      };
    } catch (err) {
      console.error("Error creating case: ", err);
      throw new Error('Case could not be created.');
    }
  }
}

export const caseApi = (url: string): CaseApi => {
  let accessToken = getIdamAuthToken();
  let serviceAuthToken = getServiceAuthToken();
  console.log("accessToken: ", accessToken);
  console.log("serviceAuthToken: ", serviceAuthToken);
  return new CaseApi(
    Axios.create({
      baseURL: `${url}`,
      headers: {
        Authorization: `Bearer ${getIdamAuthToken()}`,
        ServiceAuthorization: `Bearer ${serviceAuthToken}`,
        'Content-Type': 'application/json',
        'experimental': 'experimental',
        'Accept': '*/*',
      },
    }),
  );
};
