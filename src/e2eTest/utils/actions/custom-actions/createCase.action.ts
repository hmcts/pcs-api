import Axios, { AxiosInstance, AxiosResponse } from 'axios';
import { TestConfig } from 'config/test.config';
import { getIdamAuthToken, getServiceAuthToken } from '../../helpers/idam-helpers/idam.helper';
import { actionData, IAction } from '../../interfaces/action.interface';
import { Page } from '@playwright/test';
import { initIdamAuthToken, initServiceAuthToken, getUser } from 'utils/helpers/idam-helpers/idam.helper';
import { performAction, performActions } from '@utils/controller';

let caseInfo: { id: string; fid: string; state: string };
const testConfig = TestConfig.ccdCase;

export class CreateCaseAction implements IAction {
  private eventToken?: string;
  constructor(private readonly axios: AxiosInstance = Axios.create()) {}

  async execute(page: Page, action: string, fieldName: actionData, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createCase', () => this.createCaseAction(page, action, fieldName, data)],
      ['selectAddress', () => this.selectAddress(fieldName)],
      ['selectLegislativeCountry', () => this.selectLegislativeCountry(fieldName)],
      ['selectClaimantType', () => this.selectClaimantType(fieldName)],
      ['selectCaseOptions', () => this.selectCaseOptions(fieldName)],
      ['enterAddress', () => this.enterAddress(fieldName)]

    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async createCaseAction(page: Page, action: string, fieldName: actionData, data?: actionData) {
    const dataStoreApiInstance = await dataStoreApi();
    await dataStoreApiInstance.execute(page, action, fieldName, data);
    caseInfo = await dataStoreApiInstance.createCase(fieldName as string);
  }

  private async selectAddress(caseData: actionData) {
    const addressDetails = caseData as { postcode: string; addressIndex: number };
    await performActions(
      'Find Address based on postcode',
      ['inputText', 'Enter a UK postcode', addressDetails.postcode],
      ['clickButton', 'Find address'],
      ['select', 'Select an address', addressDetails.addressIndex]
    );
    await performAction('clickButton', 'Continue');
  }

  private async selectLegislativeCountry(caseData: actionData) {
    await performAction('clickRadioButton', (caseData as { country: string }).country);
    await performAction('clickButton', 'Continue');
  }

  private async selectClaimantType(caseData: actionData) {
    await performAction('clickRadioButton', (caseData as { claimantType: string }).claimantType);
    await performAction('clickButton', 'Continue');
  }

  private async selectCaseOptions(caseData: actionData) {
    await performActions('Case option selection'
      , ['select', 'Jurisdiction', (caseData as { jurisdiction: string }).jurisdiction]
      , ['select', 'Case type', (caseData as { caseType: string }).caseType]
      , ['select', 'Event', (caseData as { event: string }).event]);
    await performAction('clickButton', 'Start');
  }

  private async enterAddress(caseData: actionData) {
    const addressDetails = caseData as { postcode: string; country: string, walesCounty:string, townOrCity:string, addressLine3:string
      addressLine2:string, buildingAndStreet:string };
    await performActions(
      'Enter Address Manually'
      , ['clickButton', "I can't enter a UK postcode"]
      , ['inputText', 'Building and Street', addressDetails.buildingAndStreet]
      , ['inputText', 'Address Line 2', addressDetails.addressLine2]
      , ['inputText', 'Address Line 3', addressDetails.addressLine3]
      , ['inputText', 'Town or City', addressDetails.townOrCity]
      , ['inputText', 'County', addressDetails.walesCounty]
      , ['inputText', 'Postcode/Zipcode', addressDetails.postcode]
      , ['inputText', 'Country', addressDetails.country]
    );
    await performAction('clickButton', 'Continue');
  }

  async getEventToken(): Promise<string> {
    if (!this.eventToken) {
      const res: AxiosResponse<{ token: string }> = await this.axios.get(
        `/case-types/${testConfig.caseType}/event-triggers/${testConfig.eventName}`
      );
      this.eventToken = res.data.token;
    }
    return this.eventToken;
  }

  async createCase(caseData: actionData): Promise<{ id: string; fid: string; state: string }> {
    const eventToken = await this.getEventToken();
    const event = { id: `${testConfig.eventName}` };
    const payload = typeof caseData === 'object' && 'data' in caseData ? caseData.data : caseData;
    try {
      const res = await this.axios.post(`/case-types/${testConfig.caseType}/cases`, {
        data: payload, event, event_token: eventToken
      });
      return { id: res.data.id, fid: formatCaseNumber(res.data.id), state: res.data.state };
    } catch {
      throw new Error('Case could not be created.');
    }
  }
}

export const dataStoreApi = async (): Promise<CreateCaseAction> => {
  const user = getUser('exuiUser');
  await initIdamAuthToken(user?.email ?? '', user?.password ?? '');
  await initServiceAuthToken();
  return new CreateCaseAction(
    Axios.create({
      baseURL: `${testConfig.url}`,
      headers: {
        Authorization: `Bearer ${getIdamAuthToken()}`,
        ServiceAuthorization: `Bearer ${getServiceAuthToken()}`,
        'Content-Type': 'application/json',
        experimental: 'experimental',
        Accept: '*/*',
      },
    })
  );
};

export const getCaseInfo = (): { id: string; fid: string; state: string } => {
  if (!caseInfo) throw new Error('Case information is not available. Ensure that a case has been created.');
  return caseInfo;
};

export const formatCaseNumber = (id: string): string => id.replace(/(.{4})(?=.)/g, '$1-');
