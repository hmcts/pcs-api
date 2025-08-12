import Axios, { AxiosInstance, AxiosResponse } from 'axios';
import { TestConfig } from 'config/test.config';
import { getIdamAuthToken, getServiceAuthToken } from '../../helpers/idam-helpers/idam.helper';
import { actionData, IAction } from '../../interfaces/action.interface';
import { Page } from '@playwright/test';
import { initIdamAuthToken, initServiceAuthToken, getUser } from 'utils/helpers/idam-helpers/idam.helper';
import { performAction, performActions } from '@utils/controller';
import {createCase} from "@data/page-data/createCase.page.data";
import {addressDetails} from "@data/page-data/addressDetails.page.data";
import {claimantName} from "@data/page-data/claimantName.page.data";
import {contactPreferences} from "@data/page-data/contactPreferences.page.data";

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
      ['selectJurisdictionCaseTypeEvent', () => this.selectJurisdictionCaseTypeEvent()],
      ['enterTestAddressManually', () => this.enterTestAddressManually()],
      ['selectClaimType', () => this.selectClaimType(fieldName)],
      ['selectClaimantName', () => this.selectClaimantName(fieldName)],
      ['selectContactPreferences', () => this.selectContactPreferences(fieldName)]
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
    await performAction('clickRadioButton', caseData);
    await performAction('clickButton', 'Continue');
  }

  private async selectClaimantType(caseData: actionData) {
    await performAction('clickRadioButton', caseData);
    await performAction('clickButton', 'Continue');
  }

  private async selectClaimType(caseData: actionData) {
    await performAction('clickRadioButton', caseData);
    await performAction('clickButton', 'Continue');
  }

  private async selectClaimantName(caseData: actionData) {
    await performAction('clickRadioButton', caseData);
    if(caseData == claimantName.no){
      await performAction('inputText', claimantName.whatIsCorrectClaimantName, claimantName.correctClaimantName);
    }
    await performAction('clickButton', 'Continue');
  }

  private async selectContactPreferences(preferences: actionData) {
    const prefData = preferences as {
      notifications: { answer: string };
      correspondenceAddress: { answer: string };
      phoneNumber: { answer: string };
    };

    await performAction('clickRadioButton', {
      question: contactPreferences.notificationQuestion,
      option: prefData.notifications.answer
    });
    if(prefData.notifications.answer == 'No'){
      await performAction('inputText', 'Enter email address', contactPreferences.emailId);
    }

    await performAction('clickRadioButton', {
      question: contactPreferences.correspondenceAddressQuestion,
      option: prefData.correspondenceAddress.answer
    });
    if(prefData.correspondenceAddress.answer == 'No'){
      await performAction('selectAddress', {postcode: addressDetails.englandPostcode,
        addressIndex: addressDetails.addressIndex});
    }

    await performAction('clickRadioButton', {
      question: contactPreferences.phoneNumberQuestion,
      option: prefData.phoneNumber.answer
    });
    if(prefData.phoneNumber.answer == 'Yes'){
      await performAction('inputText', 'Enter phone number', contactPreferences.phoneNumber);
    }

    await performAction('clickButton', 'Continue');
  }

  private async selectJurisdictionCaseTypeEvent() {
    await performActions('Case option selection'
      , ['select', 'Jurisdiction', createCase.possessionsJurisdiction]
      , ['select', 'Case type', createCase.caseType.civilPossessions]
      , ['select', 'Event', createCase.makeAPossessionClaimEvent]);
    await performAction('clickButton', 'Start');
  }

  private async enterTestAddressManually() {
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
