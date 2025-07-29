import Axios, {AxiosInstance, AxiosResponse} from 'axios';
import {TestConfig} from 'config/test.config';
import {
  getIdamAuthToken,
  getServiceAuthToken
} from '../../helpers/idam-helpers/idam.helper';
import {actionData, IAction} from '../../interfaces/action.interface';
import {Page} from '@playwright/test';
import {initIdamAuthToken, initServiceAuthToken, getUser} from 'utils/helpers/idam-helpers/idam.helper';


let caseInfo: { id: string; fid: string, state: string };
const testConfig = TestConfig.ccdCase;

export class CreateCaseAction implements IAction {
  private eventToken?: string;

  constructor(
    private readonly axios: AxiosInstance,
  ) {
  }

  async execute(page:Page,fieldName?: actionData, value?: actionData): Promise<void> {
    if (!fieldName) throw new Error('Missing fieldName');
    const dataStoreApiInstance = await dataStoreApi();
    caseInfo = await dataStoreApiInstance.createCase(fieldName);
  }

  async getEventToken(): Promise<string> {
    if (!this.eventToken) {
      const tokenResponse: AxiosResponse<{ token: string }> = await this.axios.get(
        `/case-types/${testConfig.caseType}/event-triggers/${testConfig.eventName}`
      );
      this.eventToken = tokenResponse.data.token;
    }
    return this.eventToken;
  }

  async createCase(caseData: actionData): Promise<{ id: string; fid: string; state: string }> {
    const eventToken = await this.getEventToken();
    const event = {id: `${testConfig.eventName}`};
    const payloadData = typeof caseData === 'object' && 'data' in caseData ? caseData.data : caseData;
    try {
      const response = await this.axios.post(
        `/case-types/${testConfig.caseType}/cases`,
        {
          data: payloadData,
          event: event,
          event_token: eventToken,
        }
      );
      return {
        id: response.data.id,
        fid: formatCaseNumber(response.data.id),
        state: response.data.state,
      };
    } catch (err) {
      throw err
      throw new Error('Case could not be created.');
    }
  }
}
//setup for the DataStoreApi to use the Axios instance with the correct headers and base URL
export const dataStoreApi = async (): Promise<CreateCaseAction> => {
  const userCreds = getUser('exuiUser');
  await initIdamAuthToken(userCreds?.email ?? '', userCreds?.password ?? '');
  await initServiceAuthToken();
  return new CreateCaseAction(
    Axios.create({
      baseURL: `${testConfig.url}`,
      headers: {
        Authorization: `Bearer ${getIdamAuthToken()}`,
        ServiceAuthorization: `Bearer ${getServiceAuthToken()}`,
        'Content-Type': 'application/json',
        'experimental': 'experimental',
        'Accept': '*/*',
      },
    }),
  );
};

export const getCaseInfo = (): { id: string; fid: string, state: string } => {
  if (!caseInfo) {
    throw new Error('Case information is not available. Ensure that a case has been created.');
  }
  return caseInfo;
}
export const formatCaseNumber = (id: string): string => {
  return id.replace(/(.{4})(?=.)/g, '$1-');
}
