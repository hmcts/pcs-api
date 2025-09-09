import Axios, { AxiosInstance, AxiosResponse } from 'axios';
import { TestConfig } from 'config/test.config';
import { getIdamAuthToken, getServiceAuthToken } from '../../helpers/idam-helpers/idam.helper';
import { actionData, IAction } from '../../interfaces/action.interface';
import { Page } from '@playwright/test';
import { getUser, initIdamAuthToken, initServiceAuthToken } from 'utils/helpers/idam-helpers/idam.helper';
import { performAction, performActions, performValidation } from '@utils/controller';
import { createCase } from '@data/page-data/createCase.page.data';
import { addressDetails } from '@data/page-data/addressDetails.page.data';
import { housingPossessionClaim } from '@data/page-data/housingPossessionClaim.page.data';
import { defendantDetails } from "@data/page-data/defendantDetails.page.data";
import { claimantName } from '@data/page-data/claimantName.page.data';
import { contactPreferences } from '@data/page-data/contactPreferences.page.data';
import { mediationAndSettlement } from '@data/page-data/mediationAndSettlement.page.data';
import { resumeClaimOptions } from "@data/page-data/resumeClaimOptions.page.data";
import { rentDetails } from '@data/page-data/rentDetails.page.data';
import configData from '@config/test.config';

let caseInfo: { id: string; fid: string; state: string };
let caseNumber: string;
const testConfig = TestConfig.ccdCase;

export class CreateCaseAction implements IAction {
  private eventToken?: string;

  constructor(private readonly axios: AxiosInstance = Axios.create()) {
  }

  async execute(page: Page, action: string, fieldName: actionData, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createCase', () => this.createCaseAction(page, action, fieldName, data)],
      ['housingPossessionClaim', () => this.housingPossessionClaim()],
      ['selectAddress', () => this.selectAddress(fieldName)],
      ['selectResumeClaimOption', () => this.selectResumeClaimOption(fieldName)],
      ['extractCaseIdFromAlert', () => this.extractCaseIdFromAlert(page)],
      ['selectClaimantType', () => this.selectClaimantType(fieldName)],
      ['reloginAndFindTheCase', () => this.reloginAndFindTheCase()],
      ['defendantDetails', () => this.defendantDetails(fieldName)],
      ['selectJurisdictionCaseTypeEvent', () => this.selectJurisdictionCaseTypeEvent()],
      ['enterTestAddressManually', () => this.enterTestAddressManually()],
      ['selectClaimType', () => this.selectClaimType(fieldName)],
      ['selectClaimantName', () => this.selectClaimantName(fieldName)],
      ['selectContactPreferences', () => this.selectContactPreferences(fieldName)],
      ['selectGroundsForPossession', () => this.selectGroundsForPossession(fieldName)],
      ['selectPreActionProtocol', () => this.selectPreActionProtocol(fieldName)],
      ['selectMediationAndSettlement', () => this.selectMediationAndSettlement(fieldName)],
      ['selectNoticeOfYourIntention', () => this.selectNoticeOfYourIntention(fieldName)],
      ['selectNoticeDetails', () => this.selectNoticeDetails(fieldName)],
      ['selectCountryRadioButton', () => this.selectCountryRadioButton(fieldName)],
      ['provideRentDetails', () => this.provideRentDetails(fieldName)]
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

  private async housingPossessionClaim() {
    /* The performValidation call below needs to be updated to:
   await performValidation('mainHeader', housingPossessionClaim.mainHeader);
   once we get the new story, as the previous story (HDPI-1254) has been implemented with 2-page headers. */
    await performValidation('text', {
      'text': housingPossessionClaim.mainHeader,
      'elementType': 'heading'
    });
    await performAction('clickButton', housingPossessionClaim.continue);
  }

  private async selectAddress(caseData: actionData) {
    const addressDetails = caseData as { postcode: string; addressIndex: number };
    await performActions(
      'Find Address based on postcode',
      ['inputText', 'Enter a UK postcode', addressDetails.postcode],
      ['clickButton', 'Find address'],
      ['select', 'Select an address', addressDetails.addressIndex]
    );
    await performAction('clickButton', 'Submit');
  }

  private async extractCaseIdFromAlert(page: Page): Promise<void> {
    const text = await page.locator('div.alert-message').innerText();
    caseNumber = text.match(/#([\d-]+)/)?.[1] as string;
    if (!caseNumber) {
      throw new Error(`Case ID not found in alert message: "${text}"`);
    }
  }

  private async selectResumeClaimOption(caseData: actionData) {
    await performAction('clickRadioButton', caseData);
    await performAction('clickButton', resumeClaimOptions.continue);
  }

  private async selectClaimantType(caseData: actionData) {
    await performAction('clickRadioButton', caseData);
    await performAction('clickButton', 'Continue');
  }

  private async selectClaimType(caseData: actionData) {
    await performAction('clickRadioButton', caseData);
    await performAction('clickButton', 'Continue');
  }

  private async selectGroundsForPossession(caseData: actionData) {
    await performAction('clickRadioButton', caseData);
    await performAction('clickButton', 'Continue');
  }

  private async selectPreActionProtocol(caseData: actionData) {
    await performAction('clickRadioButton', caseData);
    await performAction('clickButton', 'Continue');
  }

  private async selectNoticeOfYourIntention(caseData: actionData) {
    await performAction('clickRadioButton', caseData);
    await performAction('clickButton', 'Continue');
  }

  private async selectCountryRadioButton(option: actionData) {
    await performAction('clickRadioButton', option);
    await performAction('clickButton', 'Submit');
  }

  private async selectClaimantName(caseData: actionData) {
    await performAction('clickRadioButton', caseData);
    if(caseData == claimantName.no){
      await performAction('inputText', claimantName.whatIsCorrectClaimantName, claimantName.correctClaimantNameInput);
    }
    await performAction('clickButton', 'Continue');
  }

  private async selectContactPreferences(preferences: actionData) {
    const prefData = preferences as {
      notifications: string;
      correspondenceAddress: string;
      phoneNumber: string;
    };
    await performAction('clickRadioButton', {
      question: contactPreferences.emailAddressForNotifications,
      option: prefData.notifications
    });
    if (prefData.notifications === 'No') {
      await performAction('inputText', 'Enter email address', contactPreferences.emailIdInput);
    }
    await performAction('clickRadioButton', {
      question: contactPreferences.doYouWantDocumentsToBeSentToAddress,
      option: prefData.correspondenceAddress
    });
    if (prefData.correspondenceAddress === 'No') {
      await performActions(
          'Find Address based on postcode',
          ['inputText', 'Enter a UK postcode', addressDetails.englandCourtAssignedPostcode],
          ['clickButton', 'Find address'],
          ['select', 'Select an address', addressDetails.addressIndex]
      );
    }
    await performAction('clickRadioButton', {
      question: contactPreferences.provideContactPhoneNumber,
      option: prefData.phoneNumber
    });
    if (prefData.phoneNumber === 'Yes') {
      await performAction('inputText', 'Enter phone number', contactPreferences.phoneNumberInput);
    }
    await performAction('clickButton', 'Continue');
  }

private async defendantDetails(defendantVal: actionData) {
    const defendantData = defendantVal as {
      name: string;
      correspondenceAddress: string;
      email: string;
      correspondenceAddressSame?: string
    };
    await performAction('clickRadioButton', {
      question: defendantDetails.doYouKnowTheDefendantName,
      option: defendantData.name
    });
    if (defendantData.name === 'Yes') {
      await performAction('inputText', defendantDetails.defendantFirstName, defendantDetails.firstNameInput);
      await performAction('inputText', defendantDetails.defendantLastName, defendantDetails.lastNameInput);
    }
    await performAction('clickRadioButton', {
      question: defendantDetails.defendantCorrespondenceAddress,
      option: defendantData.correspondenceAddress
    });
    if (defendantData.correspondenceAddress === 'Yes') {
      await performAction('clickRadioButton', {
        question: defendantDetails.isCorrespondenceAddressSame,
        option: defendantData.correspondenceAddressSame
      });
      if (defendantData.correspondenceAddressSame === 'No') {
        await performActions(
            'Find Address based on postcode',
            ['inputText', 'Enter a UK postcode', addressDetails.englandCourtAssignedPostcode],
            ['clickButton', 'Find address'],
            ['select', 'Select an address', addressDetails.addressIndex]
        );
      }
    }
    await performAction('clickRadioButton', {
      question: defendantDetails.defendantEmailAddress,
      option: defendantData.email
    });
    if (defendantData.email === 'Yes') {
      await performAction('inputText', defendantDetails.enterEmailAddress, defendantDetails.emailIdInput);
    }
    await performAction('clickButton', 'Continue');
  }

  private async selectMediationAndSettlement(mediationSettlement: actionData) {
    const prefData = mediationSettlement as {
      attemptedMediationWithDefendantsOption: string;
      settlementWithDefendantsOption: string;
    };
    await performAction('clickRadioButton', {
      question: mediationAndSettlement.attemptedMediationWithDefendants,
      option: prefData.attemptedMediationWithDefendantsOption
    });
    if (prefData.attemptedMediationWithDefendantsOption == 'Yes') {
      await performAction('inputText', mediationAndSettlement.attemptedMediationTextAreaLabel, mediationAndSettlement.attemptedMediationInputData);
    }
    await performAction('clickRadioButton', {
      question: mediationAndSettlement.settlementWithDefendants,
      option: prefData.settlementWithDefendantsOption
    });
    if (prefData.settlementWithDefendantsOption == 'Yes') {
      await performAction('inputText', mediationAndSettlement.settlementWithDefendantsTextAreaLabel, mediationAndSettlement.settlementWithDefendantsInputData);
    }
    await performAction('clickButton', 'Continue');
  }

  private async selectNoticeDetails(noticeData: actionData) {
    const noticeDetailsData = noticeData as {
      howDidYouServeNotice: string;
      day?: string;
      month?: string;
      year?: string;
    };
    await performAction('clickRadioButton', noticeDetailsData.howDidYouServeNotice);
    if(noticeDetailsData.day && noticeDetailsData.month &&  noticeDetailsData.year) {
      await performActions('Enter Day Month Year'
        ,['inputText', 'Day', noticeDetailsData.day]
        ,['inputText', 'Month', noticeDetailsData.month]
        ,['inputText', 'Year', noticeDetailsData.year]);
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
      , ['inputText', 'Postcode/Zipcode', addressDetails.walesCourtAssignedPostcode]
      , ['inputText', 'Country', addressDetails.country]
    );
    await performAction('clickButton', 'Submit');
  }

  private async reloginAndFindTheCase() {
    await performAction('navigateToUrl', configData.manageCasesBaseURL);
    await performAction('login')
    await performAction('inputText', '16-digit case reference:', caseNumber);
    await performAction('clickButton', 'Find');
  }

  private async provideRentDetails(rentFrequency: actionData) {
    const rentData = rentFrequency as {
      rentFrequencyOption: string;
      rentAmount?: string;
      unpaidRentAmountPerDay?: string,
      inputFrequency?: string
    };
    await performAction('clickRadioButton', rentData.rentFrequencyOption);
    if(rentData.rentFrequencyOption == 'Other'){
      await performAction('inputText', rentDetails.rentFrequencyLabel, rentData.inputFrequency);
      await performAction('inputText', rentDetails.amountPerDayInputLabel, rentData.unpaidRentAmountPerDay);
    } else {
      await performAction('inputText', rentDetails.HowMuchRentLabel, rentData.rentAmount);
    }
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
