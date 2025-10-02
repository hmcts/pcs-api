import Axios from 'axios';
import {ServiceAuthUtils} from '@hmcts/playwright-common';
import {actionData, IAction} from '../../interfaces/action.interface';
import {Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
import {createCase} from '@data/page-data/createCase.page.data';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {housingPossessionClaim} from '@data/page-data/housingPossessionClaim.page.data';
import {defendantDetails} from "@data/page-data/defendantDetails.page.data";
import {claimantName} from '@data/page-data/claimantName.page.data';
import {contactPreferences} from '@data/page-data/contactPreferences.page.data';
import {mediationAndSettlement} from '@data/page-data/mediationAndSettlement.page.data';
import {tenancyLicenceDetails} from '@data/page-data/tenancyLicenceDetails.page.data';
import {groundsForPossession} from '@data/page-data/groundsForPossession.page.data';
import {resumeClaimOptions} from '@data/page-data/resumeClaimOptions.page.data';
import {rentDetails} from '@data/page-data/rentDetails.page.data';
import {accessTokenApiData} from '@data/api-data/accessToken.api.data';
import {caseApiData} from '@data/api-data/case.api.data';
import {dailyRentAmount} from '@data/page-data/dailyRentAmount.page.data';
import {reasonsForPossession} from '@data/page-data/reasonsForPossession.page.data';
import {detailsOfRentArrears} from '@data/page-data/detailsOfRentArrears.page.data';
import {additionalReasonsForPossession} from '@data/page-data/additionalReasonsForPossession.page.data';
import {claimingCosts} from '@data/page-data/claimingCosts.page.data';
import {alternativesToPossession} from '@data/page-data/alternativesToPossession.page.data';
import {reasonsForRequestingASuspensionOrder} from "@data/page-data/reasonsForRequestingASuspensionOrder.page.data";

export let caseInfo: { id: string; fid: string; state: string };
let caseNumber: string;

export class CreateCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createCase', () => this.createCaseAction(fieldName)],
      ['housingPossessionClaim', () => this.housingPossessionClaim()],
      ['selectAddress', () => this.selectAddress(fieldName)],
      ['selectResumeClaimOption', () => this.selectResumeClaimOption(fieldName)],
      ['extractCaseIdFromAlert', () => this.extractCaseIdFromAlert(page)],
      ['selectClaimantType', () => this.selectClaimantType(fieldName)],
      ['reloginAndFindTheCase', () => this.reloginAndFindTheCase(fieldName)],
      ['defendantDetails', () => this.defendantDetails(fieldName)],
      ['selectJurisdictionCaseTypeEvent', () => this.selectJurisdictionCaseTypeEvent()],
      ['enterTestAddressManually', () => this.enterTestAddressManually()],
      ['selectClaimType', () => this.selectClaimType(fieldName)],
      ['selectClaimantName', () => this.selectClaimantName(fieldName)],
      ['selectContactPreferences', () => this.selectContactPreferences(fieldName)],
      ['selectRentArrearsPossessionGround', () => this.selectRentArrearsPossessionGround(fieldName)],
      ['selectGroundsForPossession', () => this.selectGroundsForPossession(fieldName)],
      ['selectPreActionProtocol', () => this.selectPreActionProtocol(fieldName)],
      ['selectMediationAndSettlement', () => this.selectMediationAndSettlement(fieldName)],
      ['selectNoticeOfYourIntention', () => this.selectNoticeOfYourIntention(fieldName)],
      ['selectNoticeDetails', () => this.selectNoticeDetails(fieldName)],
      ['selectCountryRadioButton', () => this.selectCountryRadioButton(fieldName)],
      ['selectTenancyOrLicenceDetails', () => this.selectTenancyOrLicenceDetails(fieldName)],
      ['selectOtherGrounds', () => this.selectYourPossessionGrounds(fieldName)],
      ['selectYourPossessionGrounds', () => this.selectYourPossessionGrounds(fieldName)],
      ['enterReasonForPossession', () => this.enterReasonForPossession(fieldName)],
      ['selectRentArrearsOrBreachOfTenancy', () => this.selectRentArrearsOrBreachOfTenancy(fieldName)],
      ['provideRentDetails', () => this.provideRentDetails(fieldName)],
      ['selectDailyRentAmount', () => this.selectDailyRentAmount(fieldName)],
      ['provideDetailsOfRentArrears', () => this.provideDetailsOfRentArrears(fieldName)],
      ['selectClaimForMoney', () => this.selectClaimForMoney(fieldName)],
      ['selectAlternativesToPossession', () => this.selectAlternativesToPossession(fieldName)],
      ['selectHousingAct', () => this.selectHousingAct(fieldName)],
      ['enterReasonForRequestingASuspensionOrder', () => this.enterReasonForRequestingASuspensionOrder(fieldName)],
      ['selectClaimingCosts', () => this.selectClaimingCosts(fieldName)],
      ['selectAdditionalReasonsForPossession', ()=> this.selectAdditionalReasonsForPossession(fieldName)],
      ['selectClaimingCosts', () => this.selectClaimingCosts(fieldName)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
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
    const possessionGrounds = caseData as {
      groundsRadioInput: string;
      grounds?: string[];
    };
    await performAction('clickRadioButton', possessionGrounds.groundsRadioInput);
    if (possessionGrounds.groundsRadioInput == 'Yes') {
      if (possessionGrounds.grounds) {
        await performAction('check', possessionGrounds.grounds);
        if (possessionGrounds.grounds.includes('Other')) {
          await performAction('inputText', 'Enter your grounds for possession', groundsForPossession.enterYourGroundsForPossessionInput);
        }
      }
    }
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

  private async selectRentArrearsPossessionGround(rentArrearsPossessionGrounds: actionData) {
    const rentArrearsGrounds = rentArrearsPossessionGrounds as {
      rentArrears: string[];
      otherGrounds: string;
    };
    await performAction('check', rentArrearsGrounds.rentArrears);
    await performAction('clickRadioButton', rentArrearsGrounds.otherGrounds);
    await performAction('clickButton', 'Continue');
  }

  private async selectTenancyOrLicenceDetails(tenancyData: actionData) {
    const tenancyLicenceData = tenancyData as {
      tenancyOrLicenceType: string;
      day?: string;
      month?: string;
      year?: string;
      files?: string[];
    };
    await performAction('clickRadioButton', tenancyLicenceData.tenancyOrLicenceType);
    if (tenancyLicenceData.tenancyOrLicenceType === 'Other') {
      await performAction('inputText', 'Give details of the type of tenancy or licence agreement that\'s in place', tenancyLicenceDetails.detailsOfLicence);
    }
    if (tenancyLicenceData.day && tenancyLicenceData.month && tenancyLicenceData.year) {
      await performActions(
        'Enter Date',
        ['inputText', 'Day', tenancyLicenceData.day],
        ['inputText', 'Month', tenancyLicenceData.month],
        ['inputText', 'Year', tenancyLicenceData.year]);
    }
    if (tenancyLicenceData.files) {
      for (const file of tenancyLicenceData.files) {
        await performAction('clickButton', 'Add new');
        await performAction('uploadFile', file);
      }
    }
    await performAction('clickButton', 'Continue');
  }
  private async selectYourPossessionGrounds(possessionGrounds: actionData) {
    const grounds = possessionGrounds as {
      mandatory?: string[];
      mandatoryAccommodation?: string[];
      discretionary?: string[];
      discretionaryAccommodation?: string[];
    };
    if (grounds.discretionary) {
      await performAction('check', grounds.discretionary);
    }
    if (grounds.mandatory) {
      await performAction('check', grounds.mandatory);
    }
    if (grounds.mandatoryAccommodation) {
      await performAction('check', grounds.mandatoryAccommodation);
    }
    if (grounds.discretionaryAccommodation) {
      await performAction('check', grounds.discretionaryAccommodation);
    }
    await performAction('clickButton', 'Continue');
  }

  private async selectRentArrearsOrBreachOfTenancy(grounds: actionData) {
    const rentArrearsOrBreachOfTenancyGrounds = grounds as {
      rentArrearsOrBreach: string[];
    }
    await performAction('check', rentArrearsOrBreachOfTenancyGrounds.rentArrearsOrBreach);
    await performAction('clickButton', 'Continue');
  }

  private async enterReasonForPossession(reasons: actionData) {
    if (!Array.isArray(reasons)) {
      throw new Error(`EnterReasonForPossession expected an array, but received ${typeof reasons}`);
    }
    for (let n = 0; n < reasons.length; n++) {
      await performAction('inputText',  {text:reasons[n],index: n}, reasonsForPossession.detailsAboutYourReason);
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
      index: string,
      day?: string;
      month?: string;
      year?: string;
      files?: string
    };
    await performAction('clickRadioButton', noticeDetailsData.howDidYouServeNotice);
    if (noticeDetailsData.day && noticeDetailsData.month && noticeDetailsData.year) {
      await performActions('Enter Date',
        ['inputText', {text: 'Day', index: noticeDetailsData.index}, noticeDetailsData.day],
        ['inputText', {text: 'Month', index: noticeDetailsData.index}, noticeDetailsData.month],
        ['inputText', {text: 'Year', index: noticeDetailsData.index}, noticeDetailsData.year]);
      await performAction('uploadFile', noticeDetailsData.files);
    }
    await performAction('clickButton', 'Continue');
  }

  private async provideRentDetails(rentFrequency: actionData) {
    const rentData = rentFrequency as {
      rentFrequencyOption: string;
      rentAmount?: string;
      unpaidRentAmountPerDay?: string,
      inputFrequency?: string
    };
    await performAction('inputText', rentDetails.HowMuchRentLabel, rentData.rentAmount);
    await performAction('clickRadioButton', rentData.rentFrequencyOption);
    if(rentData.rentFrequencyOption == 'Other'){
      await performAction('inputText', rentDetails.rentFrequencyLabel, rentData.inputFrequency);
      await performAction('inputText', rentDetails.amountPerDayInputLabel, rentData.unpaidRentAmountPerDay);
    }
    await performAction('clickButton', 'Continue');
  }

  private async selectDailyRentAmount(dailyRentAmountData: actionData) {
    const rentAmount = dailyRentAmountData as {
      calculateRentAmount: string,
      unpaidRentInteractiveOption: string,
      unpaidRentAmountPerDay?: string
    };
    await performValidation('text', {
      text: dailyRentAmount.basedOnPreviousAnswers + `${rentAmount.calculateRentAmount}`,
      elementType: 'paragraph'
    });
    await performAction('clickRadioButton', rentAmount.unpaidRentInteractiveOption);
    if(rentAmount.unpaidRentInteractiveOption == 'No'){
      await performAction('inputText', dailyRentAmount.enterAmountPerDayLabel, rentAmount.unpaidRentAmountPerDay);
    }
    await performAction('clickButton', 'Continue');
  }

  private async provideDetailsOfRentArrears(rentArrears: actionData) {
    const rentArrearsData = rentArrears as {
      files?: string[],
      rentArrearsAmountOnStatement: string,
      rentPaidByOthersOption: string;
      paymentOptions?: string[];
    };
    await performAction('uploadFile', rentArrearsData.files);
    await performAction('inputText', detailsOfRentArrears.totalRentArrearsLabel, rentArrearsData.rentArrearsAmountOnStatement);
    await performAction('clickRadioButton', {
      question: detailsOfRentArrears.periodShownOnRentStatementLabel,
      option: rentArrearsData.rentPaidByOthersOption
    });
    if (rentArrearsData.rentPaidByOthersOption == 'Yes') {
      await performAction('check', rentArrearsData.paymentOptions);
      if (rentArrearsData.paymentOptions?.includes('Other')) {
        await performAction('inputText', detailsOfRentArrears.paymentSourceLabel, detailsOfRentArrears.paymentOptionOtherInput);
      }
      await performAction('clickButton', 'Continue');
    }
  }

  private async selectClaimForMoney(option: actionData) {
    await performAction('clickRadioButton', option);
    await performAction('clickButton', 'Continue');
  }

  private async selectClaimingCosts(option: actionData) {
    await performAction('clickRadioButton', option);
    await performAction('clickButton', claimingCosts.continue);
  }

  private async selectAlternativesToPossession(alternatives: actionData) {
    await performAction('check', alternatives);
    await performAction('clickButton', alternativesToPossession.continue);
  }

  private async selectHousingAct(option: actionData) {
    await performAction('clickRadioButton', option);
    await performAction('clickButton', alternativesToPossession.continue);
  }

  private async enterReasonForRequestingASuspensionOrder(reason: actionData) {
    await performAction('inputText',  reason, reasonsForRequestingASuspensionOrder.SampleTestReason);
    await performAction('clickButton', reasonsForRequestingASuspensionOrder.continue);
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
      , ['inputText', 'Address Line 2 (Optional)', addressDetails.addressLine2]
      , ['inputText', 'Address Line 3 (Optional)', addressDetails.addressLine3]
      , ['inputText', 'Town or City', addressDetails.townOrCity]
      , ['inputText', 'County (Optional)', addressDetails.walesCounty]
      , ['inputText', 'Postcode', addressDetails.walesCourtAssignedPostcode]
      , ['inputText', 'Country (Optional)', addressDetails.country]
    );
    await performAction('clickButton', 'Submit');
  }

  private async selectAdditionalReasonsForPossession(reasons: actionData) {
    await performAction('clickRadioButton', reasons);
    if(reasons == additionalReasonsForPossession.yes){
      await performAction('inputText', additionalReasonsForPossession.additionalReasonsForPossessionLabel, additionalReasonsForPossession.additionalReasonsForPossessionSampleText);
    }
    await performAction('clickButton', additionalReasonsForPossession.continue);
  }

  private async reloginAndFindTheCase(userInfo: actionData) {
    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
    await performAction('login', userInfo);
    await performAction('clickButton', 'Find case');
    await performAction('select', 'Jurisdiction', createCase.possessionsJurisdiction);
    await performAction('select', 'Case type', createCase.caseType.civilPossessions);
    await performAction('inputText', 'Case Number', caseNumber);
    await performAction('clickButton', 'Apply');
    await performAction('clickButton', caseNumber);
  }

  private async createCaseAction(caseData: actionData): Promise<void> {
    process.env.S2S_URL = accessTokenApiData.s2sUrl;
    process.env.SERVICE_AUTH_TOKEN = await new ServiceAuthUtils().retrieveToken({microservice: caseApiData.microservice});
    process.env.IDAM_AUTH_TOKEN = (await Axios.create().post(accessTokenApiData.accessTokenApiEndPoint, accessTokenApiData.accessTokenApiPayload)).data.access_token;
    const createCaseApi = Axios.create(caseApiData.createCaseApiInstance);
    process.env.EVENT_TOKEN = (await createCaseApi.get(caseApiData.eventTokenApiEndPoint)).data.token;
    const payloadData = typeof caseData === 'object' && 'data' in caseData ? caseData.data : caseData;
    try {
      const response = await createCaseApi.post(caseApiData.createCaseApiEndPoint,
        {
          data: payloadData,
          event: {id: `${caseApiData.eventName}`},
          event_token: process.env.EVENT_TOKEN,
        }
      );
      caseInfo.id = response.data.id,
      caseInfo.fid =  response.data.id.replace(/(.{4})(?=.)/g, '$1-'),
      caseInfo.state = response.data.state
    }
    catch (error) {
      throw new Error('Case could not be created.');
    }
  }
}
