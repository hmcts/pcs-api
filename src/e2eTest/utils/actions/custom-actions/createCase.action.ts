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
      ['defendantDetails', () => this.defendantDetails(fieldName as actionRecord)],
      ['selectJurisdictionCaseTypeEvent', () => this.selectJurisdictionCaseTypeEvent()],
      ['enterTestAddressManually', () => this.enterTestAddressManually()],
      ['selectClaimType', () => this.selectClaimType(fieldName)],
      ['selectClaimantName', () => this.selectClaimantName(page,fieldName)],
      ['selectContactPreferences', () => this.selectContactPreferences(fieldName as actionRecord)],
      ['selectRentArrearsPossessionGround', () => this.selectRentArrearsPossessionGround(fieldName as actionRecord)],
      ['selectGroundsForPossession', () => this.selectGroundsForPossession(fieldName as actionRecord)],
      ['selectPreActionProtocol', () => this.selectPreActionProtocol(fieldName)],
      ['selectMediationAndSettlement', () => this.selectMediationAndSettlement(fieldName as actionRecord)],
      ['selectNoticeOfYourIntention', () => this.selectNoticeOfYourIntention(fieldName as actionRecord)],
      ['selectNoticeDetails', () => this.selectNoticeDetails(fieldName as actionRecord)],
      ['selectBorderPostcode', () => this.selectBorderPostcode(fieldName)],
      ['selectTenancyOrLicenceDetails', () => this.selectTenancyOrLicenceDetails(fieldName as actionRecord)],
      ['selectOtherGrounds', () => this.selectYourPossessionGrounds(fieldName as actionRecord)],
      ['selectYourPossessionGrounds', () => this.selectYourPossessionGrounds(fieldName as actionRecord)],
      ['enterReasonForPossession', () => this.enterReasonForPossession(fieldName)],
      ['selectRentArrearsOrBreachOfTenancy', () => this.selectRentArrearsOrBreachOfTenancy(fieldName)],
      ['provideRentDetails', () => this.provideRentDetails(fieldName as actionRecord)],
      ['selectDailyRentAmount', () => this.selectDailyRentAmount(fieldName as actionRecord)],
      ['selectClaimantCircumstances', () => this.selectClaimantCircumstances(fieldName as actionRecord)],
      ['provideDetailsOfRentArrears', () => this.provideDetailsOfRentArrears(fieldName as actionRecord)],
      ['selectAlternativesToPossession', () => this.selectAlternativesToPossession(fieldName as actionRecord)],
      ['selectHousingAct', () => this.selectHousingAct(fieldName)],
      ['selectStatementOfExpressTerms', () => this.selectStatementOfExpressTerms(fieldName)],
      ['enterReasonForSuspensionOrder', () => this.enterReasonForSuspensionOrder(fieldName)],
      ['enterReasonForDemotionOrder', () => this.enterReasonForDemotionOrder(fieldName)],
      ['enterReasonForSuspensionAndDemotionOrder', () => this.enterReasonForSuspensionAndDemotionOrder(fieldName as actionRecord)],
      ['selectMoneyJudgment', () => this.selectMoneyJudgment(fieldName)],
      ['selectLanguageUsed', () => this.selectLanguageUsed(fieldName as actionRecord)],
      ['selectDefendantCircumstances', () => this.selectDefendantCircumstances(fieldName as actionRecord)],
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

  private async selectGroundsForPossession(possessionGrounds: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', possessionGrounds.groundsRadioInput);
    if (possessionGrounds.groundsRadioInput == groundsForPossession.yes) {
      if (possessionGrounds.grounds) {
        await performAction('check', possessionGrounds.grounds);
        if ((possessionGrounds.grounds as Array<string>).includes(groundsForPossession.other)) {
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

  private async selectContactPreferences(preferences: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', {
      question: contactPreferences.emailAddressForNotifications,
      option: preferences.notifications
    });
    if (preferences.notifications === contactPreferences.no) {
      await performAction('inputText', contactPreferences.enterEmailAddressLabel, contactPreferences.emailIdInput);
    }
    await performAction('clickRadioButton', {
      question: contactPreferences.doYouWantDocumentsToBeSentToAddress,
      option: preferences.correspondenceAddress
    });
    if (preferences.correspondenceAddress === contactPreferences.no) {
      await performActions(
        'Find Address based on postcode',
        ['inputText', addressDetails.enterUKPostcodeLabel, addressDetails.englandCourtAssignedPostcode],
        ['clickButton', addressDetails.findAddressLabel],
        ['select', addressDetails.selectAddressLabel, addressDetails.addressIndex]
      );
    }
    await performAction('clickRadioButton', {
      question: contactPreferences.provideContactPhoneNumber,
      option: preferences.phoneNumber
    });
    if (preferences.phoneNumber === contactPreferences.yes) {
      await performAction('inputText', contactPreferences.enterPhoneNumberLabel, contactPreferences.phoneNumberInput);
    }
    await performAction('clickButton', contactPreferences.continue);
  }

  private async defendantDetails(defendantVal: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', {
      question: defendantDetails.doYouKnowTheDefendantName,
      option: defendantVal.name
    });
    if (defendantVal.name === defendantDetails.yes) {
      await performAction('inputText', defendantDetails.defendantFirstName, defendantDetails.firstNameInput);
      await performAction('inputText', defendantDetails.defendantLastName, defendantDetails.lastNameInput);
    }
    await performAction('clickRadioButton', {
      question: defendantDetails.defendantCorrespondenceAddress,
      option: defendantVal.correspondenceAddress
    });
    if (defendantVal.correspondenceAddress === defendantDetails.yes) {
      await performAction('clickRadioButton', {
        question: defendantDetails.isCorrespondenceAddressSame,
        option: defendantVal.correspondenceAddressSame
      });
      if (defendantVal.correspondenceAddressSame === defendantDetails.no) {
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
      option: defendantVal.email
    });
    if (defendantVal.email === defendantDetails.yes) {
      await performAction('inputText', defendantDetails.enterEmailAddress, defendantDetails.emailIdInput);
    }
    await performAction('clickButton', defendantDetails.continue);
  }

  private async selectRentArrearsPossessionGround(rentArrearsPossession: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('check', rentArrearsPossession.rentArrears);
    await performAction('clickRadioButton', rentArrearsPossession.otherGrounds);
    await performAction('clickButton', rentArrearsPossessionGrounds.continue);
  }

  private async selectTenancyOrLicenceDetails(tenancyData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', tenancyData.tenancyOrLicenceType);
    if (tenancyData.tenancyOrLicenceType === tenancyLicenceDetails.other) {
      await performAction('inputText', tenancyLicenceDetails.giveDetailsOfTypeOfTenancyOrLicenceAgreement, tenancyLicenceDetails.detailsOfLicence);
    }
    if (tenancyData.day && tenancyData.month && tenancyData.year) {
      await performActions(
        'Enter Date',
        ['inputText', tenancyLicenceDetails.dayLabel, tenancyData.day],
        ['inputText', tenancyLicenceDetails.monthLabel, tenancyData.month],
        ['inputText', tenancyLicenceDetails.yearLabel, tenancyData.year]);
    }
    if (tenancyData.files) {
      for (const file of tenancyData.files as Array<string>) {
        await performAction('clickButton', tenancyLicenceDetails.addNew);
        await performAction('uploadFile', file);
      }
    }
    await performAction('clickButton', tenancyLicenceDetails.continue);
  }
  private async selectYourPossessionGrounds(possessionGrounds: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    if (possessionGrounds.discretionary) {
      await performAction('check', possessionGrounds.discretionary);
    }
    if (possessionGrounds.mandatory) {
      await performAction('check', possessionGrounds.mandatory);
    }
    if (possessionGrounds.mandatoryAccommodation) {
      await performAction('check', possessionGrounds.mandatoryAccommodation);
    }
    if (possessionGrounds.discretionaryAccommodation) {
      await performAction('check', possessionGrounds.discretionaryAccommodation);
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

  private async selectMediationAndSettlement(mediationSettlement: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', {
      question: mediationAndSettlement.attemptedMediationWithDefendants,
      option: mediationSettlement.attemptedMediationWithDefendantsOption
    });
    if (mediationSettlement.attemptedMediationWithDefendantsOption == mediationAndSettlement.yes) {
      await performAction('inputText', mediationAndSettlement.attemptedMediationTextAreaLabel, mediationAndSettlement.attemptedMediationInputData);
    }
    await performAction('clickRadioButton', {
      question: mediationAndSettlement.settlementWithDefendants,
      option: mediationSettlement.settlementWithDefendantsOption
    });
    if (mediationSettlement.settlementWithDefendantsOption == mediationAndSettlement.yes) {
      await performAction('inputText', mediationAndSettlement.settlementWithDefendantsTextAreaLabel, mediationAndSettlement.settlementWithDefendantsInputData);
    }
    await performAction('clickButton', mediationAndSettlement.continue);
  }

  private async selectNoticeDetails(noticeData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('clickRadioButton', noticeData.howDidYouServeNotice);
    if (noticeData.explanationLabel && noticeData.explanation) {
      await performAction('inputText', noticeData.explanationLabel, noticeData.explanation);
    }
    if (noticeData.day && noticeData.month && noticeData.year) {
      await performActions('Enter Date',
        ['inputText', noticeDetails.dayLabel, noticeData.day],
        ['inputText', noticeDetails.monthLabel, noticeData.month],
        ['inputText', noticeDetails.yearLabel, noticeData.year]);
    }
    if (noticeData.hour && noticeData.minute && noticeData.second) {
      await performActions('Enter Time',
        ['inputText', noticeDetails.hourLabel, noticeData.hour],
        ['inputText', noticeDetails.minuteLabel, noticeData.minute],
        ['inputText', noticeDetails.secondLabel, noticeData.second]);
    }
    if (noticeData.files) {
      await performAction('uploadFile', noticeData.files);
    }
    await performAction('clickButton', noticeDetails.continue);
  }

  private async provideRentDetails(rentFrequency: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('inputText', rentDetails.HowMuchRentLabel, rentFrequency.rentAmount);
    await performAction('clickRadioButton', rentFrequency.rentFrequencyOption);
    if(rentFrequency.rentFrequencyOption == rentDetails.other){
      await performAction('inputText', rentDetails.rentFrequencyLabel, rentFrequency.inputFrequency);
      await performAction('inputText', rentDetails.amountPerDayInputLabel, rentFrequency.unpaidRentAmountPerDay);
    }
    await performAction('clickButton', rentDetails.continue);
  }

  private async selectDailyRentAmount(dailyRentAmountData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {
      text: dailyRentAmount.basedOnPreviousAnswers + `${dailyRentAmountData.calculateRentAmount}`,
      elementType: 'paragraph'
    });
    await performAction('clickRadioButton', dailyRentAmountData.unpaidRentInteractiveOption);
    if(dailyRentAmountData.unpaidRentInteractiveOption == dailyRentAmount.no){
      await performAction('inputText', dailyRentAmount.enterAmountPerDayLabel, dailyRentAmountData.unpaidRentAmountPerDay);
    }
    await performAction('clickButton', dailyRentAmount.continue);
  }

  private async selectClaimantCircumstances(claimantCircumstance: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    //As discussed with pod1 team, part of HDPI-2011, Below steps will be enabled back when dynamic organisation name handled in new ticket on claimant circumstances page.
    //const nameClaimant = claimantsName.substring(claimantsName.length - 1) == 's' ? `${claimantsName}'` : `${claimantsName}'s`;
    const claimOption = claimantCircumstance.circumstanceOption;
    /*await performAction('clickRadioButton', {
     // question: claimantCircumstances.claimantCircumstanceInfo.replace("Claimants", nameClaimant),
      question: claimantCircumstances.claimantCircumstanceInfo,
      option: claimOption
    }
    );*/
    await performAction('clickRadioButton', claimantCircumstance.circumstanceOption);
    if (claimOption == claimantCircumstances.yes) {
      //await performAction('inputText', claimantCircumstances.claimantCircumstanceInfoTextAreaLabel.replace("Claimants", nameClaimant), claimData.claimantInput);
      await performAction('inputText', claimantCircumstances.claimantCircumstanceInfoTextAreaLabel, claimantCircumstance.claimantInput);
    }
    await performAction('clickButton', claimantCircumstances.continue);
  }

  private async provideDetailsOfRentArrears(rentArrears: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performAction('uploadFile', rentArrears.files);
    await performAction('inputText', detailsOfRentArrears.totalRentArrearsLabel, rentArrears.rentArrearsAmountOnStatement);
    await performAction('clickRadioButton', {
      question: detailsOfRentArrears.periodShownOnRentStatementLabel,
      option: rentArrears.rentPaidByOthersOption
    });
    if (rentArrears.rentPaidByOthersOption == detailsOfRentArrears.yes) {
      await performAction('check', rentArrears.paymentOptions);
      if ((rentArrears.paymentOptions as Array<string>).includes(detailsOfRentArrears.other)) {
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

  private async statementOfTruth(option: actionData) {
    // await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
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
