import Axios from 'axios';
import {ServiceAuthUtils} from '@hmcts/playwright-common';
import {actionData, actionRecord, IAction} from '../../interfaces/action.interface';
import {Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
import {createCase} from '@data/page-data/createCase.page.data';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {housingPossessionClaim} from '@data/page-data/housingPossessionClaim.page.data';
import {defendantDetails} from '@data/page-data/defendantDetails.page.data';
import {claimantName} from '@data/page-data/claimantName.page.data';
import {contactPreferences} from '@data/page-data/contactPreferences.page.data';
import {mediationAndSettlement} from '@data/page-data/mediationAndSettlement.page.data';
import {tenancyLicenceDetails} from '@data/page-data/tenancyLicenceDetails.page.data';
import {resumeClaimOptions} from '@data/page-data/resumeClaimOptions.page.data';
import {rentDetails} from '@data/page-data/rentDetails.page.data';
import {accessTokenApiData} from '@data/api-data/accessToken.api.data';
import {caseApiData} from '@data/api-data/case.api.data';
import {dailyRentAmount} from '@data/page-data/dailyRentAmount.page.data';
import {reasonsForPossession} from '@data/page-data/reasonsForPossession.page.data';
import {detailsOfRentArrears} from '@data/page-data/detailsOfRentArrears.page.data';
import {claimantType} from '@data/page-data/claimantType.page.data';
import {claimType} from '@data/page-data/claimType.page.data';
import {groundsForPossession} from '@data/page-data/groundsForPossession.page.data';
import {preActionProtocol} from '@data/page-data/preActionProtocol.page.data';
import {noticeOfYourIntention} from '@data/page-data/noticeOfYourIntention.page.data';
import {borderPostcode} from '@data/page-data/borderPostcode.page.data';
import {rentArrearsPossessionGrounds} from '@data/page-data/rentArrearsPossessionGrounds.page.data';
import {rentArrearsOrBreachOfTenancy} from '@data/page-data/rentArrearsOrBreachOfTenancy.page.data';
import {noticeDetails} from '@data/page-data/noticeDetails.page.data';
import {moneyJudgment} from '@data/page-data/moneyJudgment.page.data';
import {whatAreYourGroundsForPossession} from '@data/page-data/whatAreYourGroundsForPossession.page.data';
import {languageUsed} from '@data/page-data/languageUsed.page.data';
import {defendantCircumstances} from '@data/page-data/defendantCircumstances.page.data';
import {applications} from '@data/page-data/applications.page.data';
import {claimantCircumstances} from '@data/page-data/claimantCircumstances.page.data';
import {claimingCosts} from '@data/page-data/claimingCosts.page.data';
import {alternativesToPossession} from '@data/page-data/alternativesToPossession.page.data';
import {reasonsForRequestingADemotionOrder} from '@data/page-data/reasonsForRequestingADemotionOrder.page.data';
import {statementOfExpressTerms} from '@data/page-data/statementOfExpressTerms.page.data';
import {reasonsForRequestingASuspensionOrder} from '@data/page-data/reasonsForRequestingASuspensionOrder.page.data';
import {uploadAdditionalDocs} from '@data/page-data/uploadAdditionalDocs.page.data';
import {additionalReasonsForPossession} from '@data/page-data/additionalReasonsForPossession.page.data';
import {completeYourClaim} from '@data/page-data/completeYourClaim.page.data';
import {home} from '@data/page-data/home.page.data';
import {search} from '@data/page-data/search.page.data';
import {userIneligible} from '@data/page-data/userIneligible.page.data';
import {reasonsForRequestingASuspensionAndDemotionOrder} from '@data/page-data/reasonsForRequestingASuspensionAndDemotionOrder.page.data';

export let caseInfo: { id: string; fid: string; state: string };
export let caseNumber: string;
export let claimantsName: string;

export class CreateCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
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
      ['selectClaimantName', () => this.selectClaimantName(page,fieldName)],
      ['selectContactPreferences', () => this.selectContactPreferences(fieldName)],
      ['selectRentArrearsPossessionGround', () => this.selectRentArrearsPossessionGround(fieldName)],
      ['selectGroundsForPossession', () => this.selectGroundsForPossession(fieldName)],
      ['selectPreActionProtocol', () => this.selectPreActionProtocol(fieldName)],
      ['selectMediationAndSettlement', () => this.selectMediationAndSettlement(fieldName)],
      ['selectNoticeOfYourIntention', () => this.selectNoticeOfYourIntention(fieldName as actionRecord)],
      ['selectNoticeDetails', () => this.selectNoticeDetails(fieldName)],
      ['selectBorderPostcode', () => this.selectBorderPostcode(fieldName)],
      ['selectTenancyOrLicenceDetails', () => this.selectTenancyOrLicenceDetails(fieldName)],
      ['selectOtherGrounds', () => this.selectYourPossessionGrounds(fieldName)],
      ['selectYourPossessionGrounds', () => this.selectYourPossessionGrounds(fieldName)],
      ['enterReasonForPossession', () => this.enterReasonForPossession(fieldName)],
      ['selectRentArrearsOrBreachOfTenancy', () => this.selectRentArrearsOrBreachOfTenancy(fieldName)],
      ['provideRentDetails', () => this.provideRentDetails(fieldName)],
      ['selectDailyRentAmount', () => this.selectDailyRentAmount(fieldName)],
      ['selectClaimantCircumstances', () => this.selectClaimantCircumstances(fieldName)],
      ['provideDetailsOfRentArrears', () => this.provideDetailsOfRentArrears(fieldName)],
      ['selectAlternativesToPossession', () => this.selectAlternativesToPossession(fieldName as actionRecord)],
      ['selectHousingAct', () => this.selectHousingAct(fieldName)],
      ['selectStatementOfExpressTerms', () => this.selectStatementOfExpressTerms(fieldName)],
      ['enterReasonForSuspensionOrder', () => this.enterReasonForSuspensionOrder(fieldName)],
      ['enterReasonForDemotionOrder', () => this.enterReasonForDemotionOrder(fieldName)],
      ['enterReasonForSuspensionAndDemotionOrder', () => this.enterReasonForSuspensionAndDemotionOrder(fieldName as actionRecord)],
      ['selectMoneyJudgment', () => this.selectMoneyJudgment(fieldName)],
      ['selectLanguageUsed', () => this.selectLanguageUsed(fieldName as actionRecord)],
      ['selectDefendantCircumstances', () => this.selectDefendantCircumstances(fieldName)],
      ['selectApplications', () => this.selectApplications(fieldName)],
      ['selectClaimingCosts', () => this.selectClaimingCosts(fieldName)],
      ['completingYourClaim', () => this.completingYourClaim(fieldName)],
      ['selectAdditionalReasonsForPossession', ()=> this.selectAdditionalReasonsForPossession(fieldName)],
      ['wantToUploadDocuments', () => this.wantToUploadDocuments(fieldName as actionRecord)],
      ['uploadAdditionalDocs', () => this.uploadAdditionalDocs(fieldName as actionRecord)]
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
    await performValidation('text', {
      'text': housingPossessionClaim.claimFeeText,
      'elementType': 'paragraph'
    });
    await performAction('clickButton', housingPossessionClaim.continue);
  }

  private async selectAddress(caseData: actionData) {
    const address = caseData as { postcode: string; addressIndex: number };
    await performActions(
      'Find Address based on postcode',
      ['inputText', addressDetails.enterUKPostcodeLabel, address.postcode],
      ['clickButton', addressDetails.findAddressLabel],
      ['select', addressDetails.selectAddressLabel, address.addressIndex]
    );
    await performAction('clickButton', addressDetails.submit);
  }

  private async extractCaseIdFromAlert(page: Page): Promise<void> {
    const text = await page.locator('div.alert-message').innerText();
    caseNumber = text.match(/#([\d-]+)/)?.[1] as string;
    if (!caseNumber) {
      throw new Error(`Case ID not found in alert message: "${text}"`);
    }
  }

  private async selectResumeClaimOption(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', caseData);
    await performAction('clickButtonAndVerifyPageNavigation', resumeClaimOptions.continue, claimantType.mainHeader);
  }

  private async selectClaimantType(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', caseData);
    if(caseData === claimantType.england.registeredProviderForSocialHousing || caseData === claimantType.wales.communityLandlord){
      await performAction('clickButtonAndVerifyPageNavigation', claimantType.continue, claimType.mainHeader);
    }
    else{
      await performAction('clickButtonAndVerifyPageNavigation', claimantType.continue, userIneligible.mainHeader);
    }
  }

  private async selectClaimType(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', caseData);
    if(caseData === claimType.no){
      await performAction('clickButtonAndVerifyPageNavigation', claimType.continue, claimantName.mainHeader);
    }
    else{
      await performAction('clickButtonAndVerifyPageNavigation', claimantType.continue, userIneligible.mainHeader);
    }
  }

  private async extractClaimantName(page: Page, caseData: string): Promise<string> {
    const loc = page.locator(`dl.case-field > dt.case-field__label:has-text("${caseData}")`)
      .locator('xpath=../..')
      .locator('span.text-16');
    return await loc.innerText();
  }

  private async selectGroundsForPossession(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    const possessionGrounds = caseData as {
      groundsRadioInput: string;
      grounds?: string[];
    };
    await performAction('clickRadioButton', possessionGrounds.groundsRadioInput);
    if (possessionGrounds.groundsRadioInput == groundsForPossession.yes) {
      if (possessionGrounds.grounds) {
        await performAction('check', possessionGrounds.grounds);
        if (possessionGrounds.grounds.includes(groundsForPossession.other)) {
          await performAction('inputText', groundsForPossession.enterGroundsForPossessionLabel, groundsForPossession.enterYourGroundsForPossessionInput);
        }
      }
    }
    await performAction('clickButton', groundsForPossession.continue);
  }

  private async selectPreActionProtocol(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', caseData);
    await performAction('clickButton', preActionProtocol.continue);
  }

  private async selectNoticeOfYourIntention(caseData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performAction('clickRadioButton', caseData);
    if ( caseData.option === noticeOfYourIntention.yes && caseData.typeOfNotice) {
      await performAction('inputText', noticeOfYourIntention.typeOfNotice, noticeOfYourIntention.typeOfNoticeInput);
    }
    await performAction('clickButton', noticeOfYourIntention.continue);
  }

  private async selectBorderPostcode(option: actionData) {
    await performAction('clickRadioButton', option);
    await performAction('clickButton', borderPostcode.submit);
  }

  private async selectClaimantName(page: Page, caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', caseData);
    if(caseData == claimantName.no){
      await performAction('inputText', claimantName.whatIsCorrectClaimantName, claimantName.correctClaimantNameInput);
    }
    claimantsName = caseData == "No" ? claimantName.correctClaimantNameInput : await this.extractClaimantName(page, claimantName.yourClaimantNameRegisteredWithHMCTS);
  }

  private async selectContactPreferences(preferences: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    const prefData = preferences as {
      notifications: string;
      correspondenceAddress: string;
      phoneNumber: string;
    };
    await performAction('clickRadioButton', {
      question: contactPreferences.emailAddressForNotifications,
      option: prefData.notifications
    });
    if (prefData.notifications === contactPreferences.no) {
      await performAction('inputText', contactPreferences.enterEmailAddressLabel, contactPreferences.emailIdInput);
    }
    await performAction('clickRadioButton', {
      question: contactPreferences.doYouWantDocumentsToBeSentToAddress,
      option: prefData.correspondenceAddress
    });
    if (prefData.correspondenceAddress === contactPreferences.no) {
      await performActions(
        'Find Address based on postcode',
          ['inputText', addressDetails.enterUKPostcodeLabel, addressDetails.englandCourtAssignedPostcode],
          ['clickButton', addressDetails.findAddressLabel],
          ['select', addressDetails.selectAddressLabel, addressDetails.addressIndex]
      );
    }
    await performAction('clickRadioButton', {
      question: contactPreferences.provideContactPhoneNumber,
      option: prefData.phoneNumber
    });
    if (prefData.phoneNumber === contactPreferences.yes) {
      await performAction('inputText', contactPreferences.enterPhoneNumberLabel, contactPreferences.phoneNumberInput);
    }
    await performAction('clickButton', contactPreferences.continue);
  }

  private async defendantDetails(defendantVal: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
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
    if (defendantData.name === defendantDetails.yes) {
      await performAction('inputText', defendantDetails.defendantFirstName, defendantDetails.firstNameInput);
      await performAction('inputText', defendantDetails.defendantLastName, defendantDetails.lastNameInput);
    }
    await performAction('clickRadioButton', {
      question: defendantDetails.defendantCorrespondenceAddress,
      option: defendantData.correspondenceAddress
    });
    if (defendantData.correspondenceAddress === defendantDetails.yes) {
      await performAction('clickRadioButton', {
        question: defendantDetails.isCorrespondenceAddressSame,
        option: defendantData.correspondenceAddressSame
      });
      if (defendantData.correspondenceAddressSame === defendantDetails.no) {
        await performActions(
            'Find Address based on postcode',
            ['inputText', addressDetails.enterUKPostcodeLabel, addressDetails.englandCourtAssignedPostcode],
            ['clickButton', addressDetails.findAddressLabel],
            ['select', addressDetails.selectAddressLabel, addressDetails.addressIndex]
        );
      }
    }
    await performAction('clickRadioButton', {
      question: defendantDetails.defendantEmailAddress,
      option: defendantData.email
    });
    if (defendantData.email === defendantDetails.yes) {
      await performAction('inputText', defendantDetails.enterEmailAddress, defendantDetails.emailIdInput);
    }
    await performAction('clickButton', defendantDetails.continue);
  }

  private async selectRentArrearsPossessionGround(rentArrearsPossession: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    const rentArrearsGrounds = rentArrearsPossession as {
      rentArrears: string[];
      otherGrounds: string;
    };
    await performAction('check', rentArrearsGrounds.rentArrears);
    await performAction('clickRadioButton', rentArrearsGrounds.otherGrounds);
    await performAction('clickButton', rentArrearsPossessionGrounds.continue);
  }

  private async selectTenancyOrLicenceDetails(tenancyData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    const tenancyLicenceData = tenancyData as {
      tenancyOrLicenceType: string;
      day?: string;
      month?: string;
      year?: string;
      files?: string[];
    };
    await performAction('clickRadioButton', tenancyLicenceData.tenancyOrLicenceType);
    if (tenancyLicenceData.tenancyOrLicenceType === tenancyLicenceDetails.other) {
      await performAction('inputText', tenancyLicenceDetails.giveDetailsOfTypeOfTenancyOrLicenceAgreement, tenancyLicenceDetails.detailsOfLicence);
    }
    if (tenancyLicenceData.day && tenancyLicenceData.month && tenancyLicenceData.year) {
      await performActions(
        'Enter Date',
        ['inputText', tenancyLicenceDetails.dayLabel, tenancyLicenceData.day],
        ['inputText', tenancyLicenceDetails.monthLabel, tenancyLicenceData.month],
        ['inputText', tenancyLicenceDetails.yearLabel, tenancyLicenceData.year]);
    }
    if (tenancyLicenceData.files) {
      for (const file of tenancyLicenceData.files) {
        await performAction('clickButton', tenancyLicenceDetails.addNew);
        await performAction('uploadFile', file);
      }
    }
    await performAction('clickButton', tenancyLicenceDetails.continue);
  }
  private async selectYourPossessionGrounds(possessionGrounds: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
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
    await performAction('clickButton', whatAreYourGroundsForPossession.continue);
  }

  private async selectRentArrearsOrBreachOfTenancy(grounds: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    const rentArrearsOrBreachOfTenancyGrounds = grounds as {
      rentArrearsOrBreach: string[];
    }
    await performAction('check', rentArrearsOrBreachOfTenancyGrounds.rentArrearsOrBreach);
    await performAction('clickButton', rentArrearsOrBreachOfTenancy.continue);
  }

  private async enterReasonForPossession(reasons: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    if (!Array.isArray(reasons)) {
      throw new Error(`EnterReasonForPossession expected an array, but received ${typeof reasons}`);
    }
    for (let n = 0; n < reasons.length; n++) {
      await performAction('inputText',  {text:reasons[n],index: n}, reasonsForPossession.detailsAboutYourReason);
    }
    await performAction('clickButton', reasonsForPossession.continue);
  }

  private async selectMediationAndSettlement(mediationSettlement: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    const prefData = mediationSettlement as {
      attemptedMediationWithDefendantsOption: string;
      settlementWithDefendantsOption: string;
    };
    await performAction('clickRadioButton', {
      question: mediationAndSettlement.attemptedMediationWithDefendants,
      option: prefData.attemptedMediationWithDefendantsOption
    });
    if (prefData.attemptedMediationWithDefendantsOption == mediationAndSettlement.yes) {
      await performAction('inputText', mediationAndSettlement.attemptedMediationTextAreaLabel, mediationAndSettlement.attemptedMediationInputData);
    }
    await performAction('clickRadioButton', {
      question: mediationAndSettlement.settlementWithDefendants,
      option: prefData.settlementWithDefendantsOption
    });
    if (prefData.settlementWithDefendantsOption == mediationAndSettlement.yes) {
      await performAction('inputText', mediationAndSettlement.settlementWithDefendantsTextAreaLabel, mediationAndSettlement.settlementWithDefendantsInputData);
    }
    await performAction('clickButton', mediationAndSettlement.continue);
  }

  private async selectNoticeDetails(noticeData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    const noticeDetailsData = noticeData as {
      howDidYouServeNotice: string;
      explanationLabel?: string;
      explanation?: string,
      day?: string;
      month?: string;
      year?: string;
      hour?: string;
      minute?: string;
      second?: string;
      files?: string
    };
    await performAction('clickRadioButton', noticeDetailsData.howDidYouServeNotice);
    if (noticeDetailsData.explanationLabel && noticeDetailsData.explanation) {
      await performAction('inputText', noticeDetailsData.explanationLabel, noticeDetailsData.explanation);
    }
    if (noticeDetailsData.day && noticeDetailsData.month && noticeDetailsData.year) {
      await performActions('Enter Date',
        ['inputText', noticeDetails.dayLabel, noticeDetailsData.day],
        ['inputText', noticeDetails.monthLabel, noticeDetailsData.month],
        ['inputText', noticeDetails.yearLabel, noticeDetailsData.year]);
    }
    if (noticeDetailsData.hour && noticeDetailsData.minute && noticeDetailsData.second) {
      await performActions('Enter Time',
        ['inputText', noticeDetails.hourLabel, noticeDetailsData.hour],
        ['inputText', noticeDetails.minuteLabel, noticeDetailsData.minute],
        ['inputText', noticeDetails.secondLabel, noticeDetailsData.second]);
    }
    if (noticeDetailsData.files) {
      await performAction('uploadFile', noticeDetailsData.files);
    }
    await performAction('clickButton', noticeDetails.continue);
  }

  private async provideRentDetails(rentFrequency: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    const rentData = rentFrequency as {
      rentFrequencyOption: string;
      rentAmount?: string;
      unpaidRentAmountPerDay?: string,
      inputFrequency?: string
    };
    await performAction('inputText', rentDetails.HowMuchRentLabel, rentData.rentAmount);
    await performAction('clickRadioButton', rentData.rentFrequencyOption);
    if(rentData.rentFrequencyOption == rentDetails.other){
      await performAction('inputText', rentDetails.rentFrequencyLabel, rentData.inputFrequency);
      await performAction('inputText', rentDetails.amountPerDayInputLabel, rentData.unpaidRentAmountPerDay);
    }
    await performAction('clickButton', rentDetails.continue);
  }

  private async selectDailyRentAmount(dailyRentAmountData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
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
    if(rentAmount.unpaidRentInteractiveOption == dailyRentAmount.no){
      await performAction('inputText', dailyRentAmount.enterAmountPerDayLabel, rentAmount.unpaidRentAmountPerDay);
    }
    await performAction('clickButton', dailyRentAmount.continue);
  }

  private async selectClaimantCircumstances(claimantCircumstance: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    const claimData = claimantCircumstance as {
      circumstanceOption: string,
      claimantInput: string
    };
    //As discussed with pod1 team, part of HDPI-2011, Below steps will be enabled back when dynamic organisation name handled in new ticket on claimant circumstances page.
    //const nameClaimant = claimantsName.substring(claimantsName.length - 1) == 's' ? `${claimantsName}'` : `${claimantsName}'s`;
    const claimOption = claimData.circumstanceOption;
    /*await performAction('clickRadioButton', {
     // question: claimantCircumstances.claimantCircumstanceInfo.replace("Claimants", nameClaimant),
      question: claimantCircumstances.claimantCircumstanceInfo,
      option: claimOption
    }
    );*/
    await performAction('clickRadioButton', claimData.circumstanceOption);
    if (claimOption == claimantCircumstances.yes) {
      //await performAction('inputText', claimantCircumstances.claimantCircumstanceInfoTextAreaLabel.replace("Claimants", nameClaimant), claimData.claimantInput);
      await performAction('inputText', claimantCircumstances.claimantCircumstanceInfoTextAreaLabel, claimData.claimantInput);
    }
    await performAction('clickButton', claimantCircumstances.continue);
  }

  private async provideDetailsOfRentArrears(rentArrears: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
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
    if (rentArrearsData.rentPaidByOthersOption == detailsOfRentArrears.yes) {
      await performAction('check', rentArrearsData.paymentOptions);
      if (rentArrearsData.paymentOptions?.includes(detailsOfRentArrears.other)) {
        await performAction('inputText', detailsOfRentArrears.paymentSourceLabel, detailsOfRentArrears.paymentOptionOtherInput);
      }
      await performAction('clickButton', detailsOfRentArrears.continue);
    }
  }

  private async selectMoneyJudgment(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', option);
    await performAction('clickButton', moneyJudgment.continue);
  }

  private async selectClaimingCosts(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', option);
    await performAction('clickButton', claimingCosts.continue);
  }

  private async selectAlternativesToPossession(alternatives: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    if(alternatives){
      await performAction('check', {question: alternatives.question, option: alternatives.option});
    }
    await performAction('clickButton', alternativesToPossession.continue);
  }

  private async selectHousingAct(housingAct: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    if(Array.isArray(housingAct)) {
      for (const act of housingAct) {
        await performAction('clickRadioButton', {question: act.question, option: act.option});
      }
    }
    await performAction('clickButton', alternativesToPossession.continue);
  }

  private async selectStatementOfExpressTerms(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', {
      question: statementOfExpressTerms.statementOfExpressTermsQuestion,
      option: option
    });
    if(option == statementOfExpressTerms.yes){
      await performAction('inputText', statementOfExpressTerms.giveDetailsOfTermsLabel, statementOfExpressTerms.sampleTestReason);
    }
    await performAction('clickButton', statementOfExpressTerms.continue);
  }

  private async enterReasonForDemotionOrder(reason: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('inputText', reason, reasonsForRequestingADemotionOrder.sampleTestReason);
    await performAction('clickButton', reasonsForRequestingADemotionOrder.continue);
  }

  private async enterReasonForSuspensionOrder(reason: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('inputText', reason, reasonsForRequestingASuspensionOrder.sampleTestReason);
    await performAction('clickButton', reasonsForRequestingASuspensionOrder.continue);
  }

  private async enterReasonForSuspensionAndDemotionOrder(reason: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('inputText', reason.suspension, reasonsForRequestingASuspensionOrder.sampleTestReason);
    await performAction('inputText', reason.demotion, reasonsForRequestingADemotionOrder.sampleTestReason);
    await performAction('clickButton', reasonsForRequestingASuspensionAndDemotionOrder.continue);
  }

  private async selectApplications(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', option);
    await performAction('clickButton', applications.continue);
  }

  private async wantToUploadDocuments(documentsData: actionRecord) {
    await performAction('clickRadioButton', {
      question: documentsData.question,
      option: documentsData.option
    });
    await performAction('clickButton', uploadAdditionalDocs.continue);
  }

  private async uploadAdditionalDocs(documentsData: actionRecord) {
    if (Array.isArray(documentsData.documents)) {
      for (const document of documentsData.documents) {
        await performActions(
          'Add Document',
          ['uploadFile', document.fileName],
          ['select', uploadAdditionalDocs.typeOfDocument, document.type],
          ['inputText', uploadAdditionalDocs.shortDescriptionLabel, document.description]
        );
      }
      await performAction('clickButton', uploadAdditionalDocs.continue);
    }
  }

  private async completingYourClaim(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', option);
    await performAction('clickButton', completeYourClaim.continue);
  }

  private async selectJurisdictionCaseTypeEvent() {
    await performActions('Case option selection'
      , ['select', createCase.jurisdictionLabel, createCase.possessionsJurisdiction]
      , ['select', createCase.caseTypeLabel, createCase.caseType.civilPossessions]
      , ['select', createCase.eventLabel, createCase.makeAPossessionClaimEvent]);
    await performAction('clickButton', createCase.start);
  }

  private async selectLanguageUsed(languageDetails: actionRecord) {
    await performAction('clickRadioButton', {question: languageDetails.question, option: languageDetails.option});
    await performAction('clickButton', languageUsed.continue);
  }

  private async selectDefendantCircumstances(defendantDetails: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', defendantDetails);
    if(defendantDetails == defendantCircumstances.yes){
      await performAction('inputText', defendantCircumstances.defendantCircumstancesLabel, defendantCircumstances.defendantCircumstancesSampleData);
    }
    await performAction('clickButton', defendantCircumstances.continue);
  }

  private async enterTestAddressManually() {
    await performActions(
      'Enter Address Manually'
      , ['clickButton', addressDetails.cantEnterUKPostcodeLabel]
      , ['inputText', addressDetails.buildingAndStreetLabel, addressDetails.buildingAndStreet]
      , ['inputText', addressDetails.addressLine2Label, addressDetails.addressLine2]
      , ['inputText', addressDetails.addressLine3Label, addressDetails.addressLine3]
      , ['inputText', addressDetails.townOrCityLabel, addressDetails.townOrCity]
      , ['inputText', addressDetails.countyLabel, addressDetails.walesCounty]
      , ['inputText', addressDetails.postcodeLabel, addressDetails.walesCourtAssignedPostcode]
      , ['inputText', addressDetails.countryLabel, addressDetails.country]
    );
    await performAction('clickButton', addressDetails.submit);
  }

  private async selectAdditionalReasonsForPossession(reasons: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', reasons);
    if(reasons == additionalReasonsForPossession.yes){
      await performAction('inputText', additionalReasonsForPossession.additionalReasonsForPossessionLabel, additionalReasonsForPossession.additionalReasonsForPossessionSampleText);
    }
    await performAction('clickButton', additionalReasonsForPossession.continue);
  }

  private async reloginAndFindTheCase(userInfo: actionData) {
    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
    await performAction('login', userInfo);
    await performAction('clickButton', home.findCaseTab);
    await performAction('select', search.jurisdictionLabel, search.possessionsJurisdiction);
    await performAction('select', search.caseTypeLabel, search.caseType.civilPossessions);
    await performAction('inputText', search.caseNumberLabel, caseNumber);
    await performAction('clickButton', search.apply);
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
      caseInfo.id = response.data.id;
      caseInfo.fid =  response.data.id.replace(/(.{4})(?=.)/g, '$1-');
      caseInfo.state = response.data.state;
    }
    catch (error) {
      throw new Error('Case could not be created.');
    }
  }
}
