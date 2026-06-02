import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {expect, Page} from '@playwright/test';
import {getCaseTypeId} from '@utils/common/caseType.utils';
import {performAction, performActions, performValidation} from '@utils/controller';
import {
  createCase,
  addressDetails,
  housingPossessionClaim,
  resumeClaimOptions,
  reasonsForPossession,
  borderPostcode,
  whatAreYourGroundsForPossession,
  userIneligible,
  whatAreYourGroundsForPossessionWales,
  addressCheckYourAnswers
} from '@data/page-data';
import {
  claimantType,
  claimType,
  claimantInformation,
  defendantDetails,
  contactPreferences,
  tenancyLicenceDetails,
  groundsForPossession,
  introductoryDemotedOrOtherGroundsForPossession,
  groundsForPossessionRentArrears,
  preactionProtocol,
  mediationAndSettlement,
  rentDetails,
  dailyRentAmount,
  noticeDetails,
  moneyJudgment,
  defendantCircumstances,
  claimantCircumstances,
  alternativesToPossession,
  provideMoreDetailsOfClaim,
  checkingNotice,
  additionalReasonsForPossession,
  generalApplication,
  completingYourClaim,
  rentArrears,
  claimLanguageUsed,
  confirm,
  statementOfTruth,
  uploadAdditionalDocuments,
  demotionOfTenancyOrderReason,
  rentArrearsOrBreachOfTenancyGround,
  statementOfExpressTerms,
  suspensionOfRightToBuyOrderReason,
  suspensionToBuyDemotionOfTenancyOrderReasons,
  underlesseeMortgageeDetails,
  checkingNoticeWales,
  addCaseNote,
} from '@data/page-data-figma';
import {MEDIUM_TIMEOUT, VERY_LONG_TIMEOUT} from 'playwright.config';
import {compareMaps} from '@utils/common/compareMaps.util';
import {caseInfo} from './createCaseAPI.action';
import {createCaseApiData} from '@data/api-data';
import {formatCurrency, formatDate, formatDateTime, formatText, formatWord} from '@utils/common/string.utils';
export let caseNumber: string;
export let claimantsName: string;
export let addressInfo: { buildingStreet: string; townCity: string; engOrWalPostcode: string };

export const addressInfoCaseTab = {
  buildingStreet: createCaseApiData.createCasePayload.propertyAddress.AddressLine1,
  addressLine2: createCaseApiData.createCasePayload.propertyAddress.AddressLine2,
  townCity: createCaseApiData.createCasePayload.propertyAddress.PostTown,
  engOrWalPostcode: createCaseApiData.createCasePayload.propertyAddress.PostCode
};
export const caseTabMap = new Map<string, string>();

export class CreateCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['housingPossessionClaim', () => this.housingPossessionClaim()],
      ['selectAddress', () => this.selectAddress(page, fieldName)],
      ['submitAddressCheckYourAnswers', () => this.submitAddressCheckYourAnswers()],
      ['provideMoreDetailsOfClaim', () => this.provideMoreDetailsOfClaim(page)],
      ['selectResumeClaimOption', () => this.selectResumeClaimOption(fieldName)],
      ['extractCaseIdFromAlert', () => this.extractCaseIdFromAlert(page)],
      ['selectClaimantType', () => this.selectClaimantType(fieldName)],
      ['reloginAndFindTheCase', () => this.reloginAndFindTheCase(fieldName)],
      ['addDefendantDetails', () => this.addDefendantDetails(fieldName as actionRecord)],
      ['selectJurisdictionCaseTypeEvent', () => this.selectJurisdictionCaseTypeEvent(page)],
      ['enterTestAddressManually', () => this.enterTestAddressManually(page, fieldName as actionRecord)],
      ['selectClaimType', () => this.selectClaimType(fieldName)],
      ['selectClaimantName', () => this.selectClaimantName(page, fieldName)],
      ['selectContactPreferences', () => this.selectContactPreferences(fieldName as actionRecord)],
      ['selectRentArrearsPossessionGround', () => this.selectRentArrearsPossessionGround(fieldName as actionRecord)],
      ['selectGroundsForPossession', () => this.selectGroundsForPossession(fieldName as actionRecord)],
      ['selectPreActionProtocol', () => this.selectPreActionProtocol(fieldName)],
      ['selectMediationAndSettlement', () => this.selectMediationAndSettlement(fieldName as actionRecord)],
      ['selectNoticeOfYourIntention', () => this.selectNoticeOfYourIntention(fieldName as actionRecord)],
      ['selectNoticeDetails', () => this.selectNoticeDetails(fieldName as actionRecord)],
      ['selectNoticeDetailsWales', () => this.selectNoticeDetailsWales(fieldName as actionRecord)],
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
      ['completingYourClaim', () => this.completingYourClaim(fieldName)],
      ['selectAdditionalReasonsForPossession', () => this.selectAdditionalReasonsForPossession(fieldName)],
      ['selectUnderlesseeOrMortgageeEntitledToClaim', () => this.selectUnderlesseeOrMortgageeEntitledToClaim(fieldName as actionRecord)],
      ['selectUnderlesseeMortgageeDetails', () => this.selectUnderlesseeMortgageeDetails(fieldName as actionRecord)],
      ['wantToUploadDocuments', () => this.wantToUploadDocuments(fieldName as actionRecord)],
      ['uploadAdditionalDocs', () => this.uploadAdditionalDocs(fieldName as actionRecord)],
      ['selectStatementOfTruth', () => this.selectStatementOfTruth(fieldName as actionRecord)],
      ['claimSaved', () => this.claimSaved()],
      ['payClaimFee', () => this.payClaimFee()],
      ['validateDefendantDetails', () => this.validateDefendantDetails(page, fieldName as actionRecord)],
      ['validateClaimantDetails', () => this.validateClaimantDetails(page, fieldName as actionRecord)],
      ['addCaseNotes', () => this.addCaseNotes(fieldName as actionRecord)],
      ['validateCaseNotesDetails', () => this.validateCaseNotesDetails(page, fieldName as actionRecord)],
      ['validateCaseSummaryDetails', () => this.validateCaseSummaryDetails(page, fieldName as actionRecord)],
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

  private async selectJurisdictionCaseTypeEvent(page: Page) {
    await performActions('Case option selection'
      , ['select', createCase.jurisdictionLabel, createCase.possessionsJurisdiction]
      , ['select', createCase.caseTypeLabel, createCase.caseType.civilPossessions]
      , ['select', createCase.eventLabel, createCase.makeAPossessionClaimEvent]);
    await page.waitForLoadState('load');
    await page.locator('.spinner-container').waitFor({ state: 'detached', timeout: MEDIUM_TIMEOUT }).catch(() => {});
    await performAction('clickButtonAndVerifyPageNavigation', createCase.start, housingPossessionClaim.mainHeader);
  }

  private async selectAddress(page: Page, caseData: actionData) {
    const address = caseData as { postcode: string; addressIndex: number };
    await performActions(
      'Find Address based on postcode',
      ['inputText', addressDetails.enterUKPostcodeTextLabel, address.postcode],
      ['clickButton', addressDetails.findAddressButton],
      ['select', addressDetails.addressSelectLabel, address.addressIndex]
    );
    addressInfo = {
      buildingStreet: await page.getByLabel(addressDetails.buildingAndStreetTextLabel).inputValue(),
      townCity: await page.getByLabel(addressDetails.townOrCityTextLabel).inputValue(),
      engOrWalPostcode: address.postcode
    };
    await performAction('clickButton', addressDetails.continueButton);
  }

  private async submitAddressCheckYourAnswers() {
    await performAction('clickButton', addressCheckYourAnswers.saveAndContinueButton);
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
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question:resumeClaimOptions.resumeClaimQuestion, option: caseData});
    await performAction('clickButtonAndVerifyPageNavigation', resumeClaimOptions.continue, claimantInformation.mainHeader);
  }

  private async selectClaimantType(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question:claimantType.whoIsTheClaimantQuestion, option: caseData});
    if(caseData === claimantType.englandRegisteredProviderForSocialHousingDynamicRadioOption || caseData === claimantType.walesCommunityLandlordDynamicRadioOption){
      await performAction('clickButtonAndVerifyPageNavigation', claimantType.continueButton, claimType.mainHeader);
    }
    else{
      await performAction('clickButtonAndVerifyPageNavigation', claimantType.continueButton, userIneligible.mainHeader);
    }
  }

  private async selectClaimType(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question:claimType.isThisAClaimAgainstQuestion, option: caseData});
    if(caseData === claimType.yesRadioOption){    
      await performAction('clickButtonAndVerifyPageNavigation', claimType.continueButton, userIneligible.mainHeader);
    } else {
      await performAction('clickButton', claimType.continueButton);
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
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    let groundsForPossessionQuestion = groundsForPossession.areYouClaimingPossessionQuestion;
    if (possessionGrounds.rentArrears == groundsForPossession.noRadioOption) {
      groundsForPossessionQuestion = introductoryDemotedOrOtherGroundsForPossession.doYouHaveGroundsForPossessionQuestion;
    }
    await performAction('clickRadioButton', {question:groundsForPossessionQuestion, option: possessionGrounds.groundsRadioInput});
    if (possessionGrounds.groundsRadioInput == groundsForPossession.yesRadioOption) {
      if (possessionGrounds.grounds) {
        await performAction('check', {question: groundsForPossessionRentArrears.whatAreYourGroundsForPossessionQuestion, option: possessionGrounds.grounds});
        if ((possessionGrounds.grounds as Array<string>).includes(introductoryDemotedOrOtherGroundsForPossession.otherHiddenCheckbox)) {
          await performAction('inputText', introductoryDemotedOrOtherGroundsForPossession.enterYourGroundsHiddenTextLabel, introductoryDemotedOrOtherGroundsForPossession.enterYourGroundsInput);
        }
      }
    }
    await performAction('clickButton', groundsForPossession.continueButton);
  }

  private async selectPreActionProtocol(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question:preactionProtocol.haveYouFollowedThePreactionQuestion, option: caseData});
    if(caseData === 'No' && addressInfo.townCity !== addressDetails.walesTownOrCityTextInput){
      await performAction('inputText', preactionProtocol.explainWhyYouHaveNoFollowedHiddenTextLabel, preactionProtocol.explainWhyYouHaveNoFollowedHiddenTextInput);
    }
    await performAction('clickButton', preactionProtocol.continueButton);
  }

  private async selectNoticeOfYourIntention(caseData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question:caseData.question, option: caseData.option});
    if ( caseData.option === checkingNotice.yesRadioOption && caseData.typeOfNotice) {
      await performAction('inputText', checkingNotice.typeOfNoticeHiddenTextLabel, checkingNotice.typeOfNoticeHiddenTextInput);
    }
    await performAction('clickButton', checkingNotice.continueButton);
  }

  private async selectBorderPostcode(option: actionData) {
    await performAction('clickRadioButton', {question:borderPostcode.isProtpertyLocatedInEnglandOrWalesQuestion, option: option});
    await performAction('clickButton', borderPostcode.continueButton);
  }

  private async selectClaimantName(page: Page, caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question:claimantInformation.IsCorrectClaimantNameQuestion, option: caseData});
    if(caseData == claimantInformation.noRadioOption){
      await performAction('inputText', claimantInformation.whatIsCorrectClaimantNameHiddenQuestion, claimantInformation.ClaimantNameTextInput);
    }
    claimantsName = caseData == "No" ? claimantInformation.ClaimantNameTextInput : await this.extractClaimantName(page, claimantInformation.yourClaimantNameRegisteredParagraph);
    await performAction('clickButton', claimantInformation.continueButton);
  }

  private async selectContactPreferences(preferences: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    const prefData = preferences as {
      notifications: string;
      correspondenceAddress: string;
      phoneNumber?: string;
    };
    await performAction('clickRadioButton', {
      question: contactPreferences.doYouWantToUseQuestion,
      option: preferences.notifications
    });
    if (preferences.notifications === contactPreferences.noRadioOption) {
      await performAction('inputText', contactPreferences.enterEmailAddressHiddenTextLabel, contactPreferences.enterEmailAddressTextInput);
    }
    await performAction('clickRadioButton', {
      question: contactPreferences.doYouWantDocumentsToQuestion,
      option: preferences.correspondenceAddress
    });
    if (preferences.correspondenceAddress === contactPreferences.noRadioOption) {
      await performActions(
        'Find Address based on postcode',
        ['inputText', addressDetails.enterUKPostcodeTextLabel, addressDetails.englandCourtAssignedPostcodeTextInput],
        ['clickButton', addressDetails.findAddressButton],
        ['select', addressDetails.addressSelectLabel, addressDetails.addressIndex]
      );
    }
    if(prefData.phoneNumber) {
      await performAction('clickRadioButton', {
        question: contactPreferences.doYouWantToProvideQuestion,
        option: prefData.phoneNumber
      });
      if (prefData.phoneNumber === contactPreferences.yesRadioOption) {
        await performAction('inputText', contactPreferences.enterPhoneNumberHiddenTextLabel, contactPreferences.enterPhoneNumberTextInput);
      }
    }
    await performAction('clickButton', contactPreferences.continueButton);
  }

  private async addDefendantDetails(defendantData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: defendantDetails.doYouKnowTheDefendantsNameQuestion,
      option: defendantData.nameOption,
    });
    if (defendantData.nameOption === defendantDetails.yesRadioOption) {
      await performAction('inputText', defendantDetails.defendantsFirstNameHiddenTextLabel, defendantData.firstName);
      await performAction('inputText', defendantDetails.defendantsLastNameHiddenTextLabel, defendantData.lastName);
    }
    await performAction('clickRadioButton', {
      question: defendantDetails.doYouKnowTheDefendantsQuestion,
      option: defendantData.correspondenceAddressOption,
    });
    if (defendantData.correspondenceAddressOption === defendantDetails.yesRadioOption) {
      await performAction('clickRadioButton', {
        question: defendantDetails.isTheDefendantsAddressForServiceHiddenQuestion,
        option: defendantData.correspondenceAddressSameOption,
      });
      if (defendantData.correspondenceAddressSameOption === defendantDetails.noRadioOption) {
        await performActions(
          'Find Address based on postcode',
          ['inputText', addressDetails.enterUKPostcodeTextLabel, defendantData.address],
          ['clickButton', addressDetails.findAddressButton],
          ['select', addressDetails.addressSelectLabel, addressDetails.addressIndex]
        );
      }
    }
    const numAdditionalDefendants = Number(defendantData.numberOfDefendants) || 0;
    await performAction('clickRadioButton', {
      question: defendantDetails.doYouNeedToAddQuestion,
      option: defendantData.addAdditionalDefendantsOption,
    });
    if (defendantData.addAdditionalDefendantsOption === defendantDetails.yesRadioOption && numAdditionalDefendants > 0) {
      for (let i = 0; i < numAdditionalDefendants; i++) {
        await performAction('clickButton', defendantDetails.addNewHiddenButton);
        const index = i + 1;
        const nameQuestion = defendantDetails.doYouKnowTheDefendantsNameQuestion;
        const nameOption = defendantData[`name${index}Option`] || defendantDetails.noRadioOption;
        await performAction('clickRadioButton', {
          question: nameQuestion,
          option: nameOption,
          index,
        });
        await performAction('clickRadioButton', {
          question: nameQuestion,
          option: nameOption,
          index,
        });
        if (nameOption === defendantDetails.yesRadioOption) {
          await performAction('inputText', {text: defendantDetails.defendantsFirstNameHiddenTextLabel, index: index}, `${defendantData.firstName}${index}`);
          await performAction('inputText', {text: defendantDetails.defendantsLastNameHiddenTextLabel, index:index}, `${defendantData.lastName}${index}`
          );
        }
        const addressQuestion = defendantDetails.doYouKnowTheDefendantsQuestion;
        const correspondenceAddressOption =
          defendantData[`correspondenceAddress${index}Option`] || defendantDetails.noRadioOption;
        await performAction('clickRadioButton', {
          question: addressQuestion,
          option: correspondenceAddressOption,
          index,
        });
        const correspondenceAddressSameOption =
          defendantData[`correspondenceAddressSame${index}Option`] || defendantDetails.noRadioOption;
        if (correspondenceAddressOption === defendantDetails.yesRadioOption) {
          await performAction('clickRadioButton', {
            question: defendantDetails.isTheDefendantsAddressForServiceHiddenQuestion,
            option: correspondenceAddressSameOption,
            index,
          });
        }
      }
    }
    await performAction('clickButton', defendantDetails.continueButton);
  }

  private async selectRentArrearsPossessionGround(rentArrearsPossession: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('check', {question: groundsForPossessionRentArrears.whatAreYourGroundsForPossessionQuestion, option: rentArrearsPossession.rentArrears});
    await performAction('clickRadioButton', {question: groundsForPossessionRentArrears.doYouHaveAnyOtherAdditionalDynamicQuestion, option: rentArrearsPossession.otherGrounds});
    await performAction('clickButton', groundsForPossessionRentArrears.continueButton);
  }

  private async selectTenancyOrLicenceDetails(tenancyData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: tenancyLicenceDetails.whatTypeOfTenancyOrQuestion, option: tenancyData.tenancyOrLicenceType});
    if (tenancyData.tenancyOrLicenceType === tenancyLicenceDetails.otherRadioOption) {
      await performAction('inputText', tenancyLicenceDetails.giveDetailsOfTheTypeHiddenTextLabel, tenancyLicenceDetails.detailsOfLicenceTextInput);
    }
    if (tenancyData.day && tenancyData.month && tenancyData.year) {
      await performActions(
        'Enter Date',
        ['inputText', tenancyLicenceDetails.dayTextLabel, tenancyData.day],
        ['inputText', tenancyLicenceDetails.monthTextLabel, tenancyData.month],
        ['inputText', tenancyLicenceDetails.yearTextLabel, tenancyData.year]);
    }
    await performAction('clickRadioButton', {question: tenancyData.question, option: tenancyData.option});
    if (tenancyData.files) {
      await performAction('uploadFile', tenancyData.files);
    }
    if (tenancyData.reason) {
      await performAction('inputText', tenancyLicenceDetails.explainWhyHiddenTextLabel, tenancyData.reason);
    }
    await performAction('clickButton', tenancyLicenceDetails.continueButton);
  }

  private async selectYourPossessionGrounds(possessionGrounds: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if (!possessionGrounds) {
      await performAction('clickButton', whatAreYourGroundsForPossession.continueButton);
      return;
    }
    for (const key of Object.keys(possessionGrounds)) {
      switch (key) {
        case 'discretionary':
          await performAction('check', {question: whatAreYourGroundsForPossession.discretionary.discretionaryGroundsCategoryQuestion, option: possessionGrounds.discretionary});
          if (
            (possessionGrounds.discretionary as Array<string>).includes(
              whatAreYourGroundsForPossessionWales.discretionary.estateManagementGrounds
            )
          ) {
              await performAction('check', {question: whatAreYourGroundsForPossessionWales.discretionary.discretionaryGroundsCategoryQuestion, option: possessionGrounds.discretionaryEstateGrounds});
          }
          break;
        case 'mandatory':
          await performAction('check', {question: whatAreYourGroundsForPossession.mandatory.mandatoryGroundsCategoryQuestion, option: possessionGrounds.mandatory});
          if (String(possessionGrounds.mandatory) === 'Antisocial behaviour') {
            await performAction('check', {question: whatAreYourGroundsForPossession.mandatoryAbsoluteGrounds.absoluteGroundQuestion, option: possessionGrounds.mandatoryAbsolute});
          }
          break;
        case 'mandatoryAccommodation':
          await performAction('check', {question: whatAreYourGroundsForPossession.mandatoryWithAccommodation.mandatoryWithAccommodationGroundsCategoryQuestion, option: possessionGrounds.mandatoryAccommodation});
          break;
        case 'discretionaryAccommodation':
          await performAction('check', {question: whatAreYourGroundsForPossession.discretionaryWithAccommodation.discretionaryWithAccommodationGroundsCategoryQuestion, option: possessionGrounds.discretionaryAccommodation});
          break;
        case 'other':
          await performAction('check', {question: whatAreYourGroundsForPossession.additionalGrounds, option: possessionGrounds.other});
          await performAction('inputText', whatAreYourGroundsForPossession.giveDetailsHiddenTextLabel, whatAreYourGroundsForPossession.giveDetailsHiddenTextInput);
          break;
      }
    }
    await performAction('clickButton', whatAreYourGroundsForPossession.continueButton);
  }

  private async selectRentArrearsOrBreachOfTenancy(grounds: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    const rentArrearsOrBreachOfTenancyGrounds = grounds as {
      rentArrearsOrBreach: string[];
    }
    await performAction('check', rentArrearsOrBreachOfTenancyGrounds.rentArrearsOrBreach);
    await performAction('clickButton', rentArrearsOrBreachOfTenancyGround.continueButton);
  }

  private async enterReasonForPossession(reasons: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if (!Array.isArray(reasons)) {
      throw new Error(`EnterReasonForPossession expected an array, but received ${typeof reasons}`);
    }
    for (let n = 0; n < reasons.length; n++) {
      const reason = String(reasons[n]).trim();
      const needsGrounds = /^(other|no)$/i.test(reason);
      const reasonDisplay = needsGrounds ? `${reason} grounds` : reason;
      await performValidation('text', { text: reasonsForPossession.giveDetailsAboutYourReasonsForPossessionHintText, "elementType": 'paragraph', "index": n });
      await performAction('inputText', { text: `${reasonsForPossession.giveDetailsAboutYourReasonsForPossessionTextLabel} (${reasonDisplay})`, index: n }, reasonsForPossession.detailsAboutYourReason + "-" + reasons[n]);
    }
    await performAction('clickButton', reasonsForPossession.continue);
  }

  private async selectMediationAndSettlement(mediationSettlement: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: mediationAndSettlement.haveYouAttemptedMediationWithQuestion,
      option: mediationSettlement.attemptedMediationWithDefendantsOption
    });
    await performAction('clickRadioButton', {
      question: mediationAndSettlement.haveYouTriedToReachQuestion,
      option: mediationSettlement.settlementWithDefendantsOption
    });
    await performAction('clickButton', mediationAndSettlement.continueButton);
  }

  private async selectNoticeDetails(noticeData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: noticeDetails.howDidYouServeTheQuestion, option: noticeData.howDidYouServeNotice});
    if (noticeData.explanationLabel && noticeData.explanation) {
      await performAction('inputText', noticeData.explanationLabel, noticeData.explanation);
    }
    if (noticeData.day && noticeData.month && noticeData.year) {
      await performActions('Enter Date',
        ['inputText', noticeDetails.dayHiddenTextLabel, noticeData.day],
        ['inputText', noticeDetails.monthHiddenTextLabel, noticeData.month],
        ['inputText', noticeDetails.yearHiddenTextLabel, noticeData.year]);
    }
    if (noticeData.hour && noticeData.minute && noticeData.second) {
      await performActions('Enter Time',
        ['inputText', noticeDetails.hourHiddenTextLabel, noticeData.hour],
        ['inputText', noticeDetails.minuteHiddenTextLabel, noticeData.minute],
        ['inputText', noticeDetails.secondHiddenTextLabel, noticeData.second]);
    }
    if (noticeData.files) {
      await performAction('uploadFile', noticeData.files);
    }
    await performAction('clickButton', noticeDetails.continueButton);
  }

  private async selectNoticeDetailsWales(noticeData: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseNumber });
    await performValidation('text', { elementType: 'paragraph', text: 'Property address: ' + addressInfo.buildingStreet + ', ' + addressInfo.townCity + ', ' + addressInfo.engOrWalPostcode });
    await performAction('clickRadioButton', { question: noticeData.question, option: noticeData.haveYouServedNoticeToQuestion });
    if (noticeData.haveYouServedNoticeToQuestion === checkingNoticeWales.noRadioOption) {
      await performValidation('text', { "text": checkingNoticeWales.ifThisIsAPossessionParagraph, "elementType": "paragraph" });
      await performValidation('text', { "text": checkingNoticeWales.eachGroundRequiresParagraph, "elementType": "paragraph" });
      await performValidation('text', { "text": checkingNoticeWales.haveYouServedNoticeToQuestion, "elementType": "paragraph" });
      await performValidation('text', { "text": checkingNoticeWales.youMustMakeAStatementHiddenParagraph, "elementType": "paragraph" });
      await performValidation('text', { "text": checkingNoticeWales.characterLimitHiddenHintText, "elementType": "paragraph" });
      if (noticeData.walesNoticeStatement) {
        await performAction('inputText', checkingNoticeWales.enterStatementHiddenTextLabel, noticeData.walesNoticeStatement);
      }
    }
    await performAction('clickButton', checkingNoticeWales.continueButton);
  }

  private async provideRentDetails(rentFrequency: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('inputText', rentDetails.howMuchIsTheRentQuestion, rentFrequency.rentAmount);
    await performAction('clickRadioButton', {question: rentDetails.howFrequentlyShouldRentBePaidQuestion, option: rentFrequency.rentFrequencyOption});
    if(rentFrequency.rentFrequencyOption == rentDetails.otherRadioOption){
      await performAction('inputText', rentDetails.enterFrequencyHiddenTextLabel, rentFrequency.inputFrequency);
      await performAction('inputText', rentDetails.enterTheAmountPerDayHiddenTextLabel, rentFrequency.unpaidRentAmountPerDay);
    }
    await performAction('clickButton', rentDetails.continueButton);
  }

  private async selectDailyRentAmount(dailyRentAmountData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performValidation('text', {
      text: dailyRentAmount.basedOnYourPreviousAnswersParagraph + `${dailyRentAmountData.calculateRentAmount}`,
      elementType: 'paragraph'
    });
    await performAction('clickRadioButton', {question: dailyRentAmount.isTheAmountPerDayQuestion, option: dailyRentAmountData.unpaidRentInteractiveOption});
    if(dailyRentAmountData.unpaidRentInteractiveOption == dailyRentAmount.noRadioOption){
      await performAction('inputText', dailyRentAmount.enterAmountPerDayHiddenTextLabel, dailyRentAmountData.unpaidRentAmountPerDay);
    }
    await performAction('clickButton', dailyRentAmount.continueButton);
  }

  private async selectClaimantCircumstances(claimantCircumstance: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    const nameClaimant = claimantsName.substring(claimantsName.length - 1) == 's' ? `${claimantsName}’` : `${claimantsName}’s`;
    await performAction('clickRadioButton', {
      question: claimantCircumstances.isThereAnyInfoDynamicQuestion.replace("claimant", nameClaimant),
      option: claimantCircumstance.circumstanceOption
    }
    );
    if (claimantCircumstance.circumstanceOption == claimantCircumstances.yesRadioOption) {
      await performAction('inputText', claimantCircumstances.giveDetailsAboutHiddenDynamicParagraph.replace("claimant", nameClaimant), claimantCircumstance.claimantInput);
    }
    await performAction('clickButton', claimantCircumstances.continueButton);
  }

  private async selectDefendantCircumstances(
    defendantDetails: actionRecord
  ) {
    const hasAdditionalDefendants = defendantDetails.additionalDefendants === true;

    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});

    const config = hasAdditionalDefendants
      ? {question: defendantCircumstances.isThereAnyInformationMultipleDefendantsCircumstancesDynamicQuestion,
        guidance: defendantCircumstances.youCanUseThisSectionMultipleDynamicParagraph,
        hiddenLabel: defendantCircumstances.giveDetailsDefendantCircumstancesPluralHiddenTextLabel}
      : {question: defendantCircumstances.isThereAnyInformationSingleDefendantCircumstancesDynamicQuestion,
        guidance: defendantCircumstances.youCanUseThisSectionSingleDynamicParagraph,
        hiddenLabel: defendantCircumstances.giveDetailsDefendantCircumstancesSingularHiddenTextLabel
      };

    await performValidation('text', {elementType: 'paragraph', text: config.guidance});

    await performAction('clickRadioButton', {question: config.question, option: defendantDetails.defendantCircumstance});

    if (defendantDetails.defendantCircumstance === defendantCircumstances.yesRadioOption) {
      await performAction('inputText', config.hiddenLabel, defendantCircumstances.giveDetailsDefendantCircumstancesHiddenTextInput);
    }
    await performAction('clickButton', defendantCircumstances.continueButton);
  }

  private async provideDetailsOfRentArrears(rentArrearsData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('uploadFile', rentArrearsData.files);
    await performAction('inputText', rentArrears.totalRentArrearsTextLabel, rentArrearsData.rentArrearsAmountOnStatement);
    await performAction('clickRadioButton', {
      question: rentArrears.haveThereBeenPreviousStepsTakenQuestion,
      option: rentArrearsData.rentPaidByOthersOption
    });
    if (rentArrearsData.rentPaidByOthersOption == rentArrears.yesRadioOption) {

      await performAction('inputText', rentArrears.giveDetailsOfPreviousStepsTakenHiddenTextLabel, rentArrears.giveDetailsOfPreviousStepsTakenHiddenTextInput);
    }
    await performAction('clickButton', rentArrears.continueButton);
  }

  private async selectMoneyJudgment(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: moneyJudgment.doYouWantTheCourtQuestion, option: option});
    await performAction('clickButton', moneyJudgment.continueButton);
  }

  private async selectAlternativesToPossession(alternatives: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if(alternatives){
      await performAction('check', {question: alternatives.question, option: alternatives.option});
    }
    await performAction('clickButton', alternativesToPossession.continueButton);
  }

  private async selectHousingAct(housingAct: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if(Array.isArray(housingAct)) {
      for (const act of housingAct) {
        await performAction('clickRadioButton', {question: act.question, option: act.option});
      }
    }
    await performAction('clickButton', alternativesToPossession.continueButton);
  }

  private async selectStatementOfExpressTerms(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: statementOfExpressTerms.haveYouServedStatementQuestion,
      option: option
    });
    if(option == statementOfExpressTerms.yesRadioOption){
      await performAction('inputText', statementOfExpressTerms.giveDetailsOfTheTermsHiddenTextLabel, statementOfExpressTerms.giveDetailsOfTheTermsHiddenTextInput);
    }
    await performAction('clickButton', statementOfExpressTerms.continueButton);
  }

  private async enterReasonForDemotionOrder(reason: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('inputText', reason, demotionOfTenancyOrderReason.whyAreYouRequestingDemotionOrderInputText);
    await performAction('clickButton', demotionOfTenancyOrderReason.continueButton);
  }

  private async enterReasonForSuspensionOrder(reason: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('inputText', reason, suspensionOfRightToBuyOrderReason.whyAreYouRequestingSuspensionOrderInputText);
    await performAction('clickButton', suspensionOfRightToBuyOrderReason.continueButton);
  }

  private async enterReasonForSuspensionAndDemotionOrder(reason: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('inputText', reason.suspension, suspensionOfRightToBuyOrderReason.whyAreYouRequestingSuspensionOrderInputText);
    await performAction('inputText', reason.demotion, demotionOfTenancyOrderReason.whyAreYouRequestingDemotionOrderInputText);
    await performAction('clickButton', suspensionToBuyDemotionOfTenancyOrderReasons.continueButton);
  }

  private async selectApplications(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: generalApplication.areYouPlanningToMakeQuestion, option: option});
    await performAction('clickButton', generalApplication.continueButton);
  }

  private async wantToUploadDocuments(documentsData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: documentsData.question,
      option: documentsData.option
    });
    await performAction('clickButton', uploadAdditionalDocuments.continueButton);
  }

  private async uploadAdditionalDocs(documentsData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if (Array.isArray(documentsData.documents)) {
      for (let fileIndex = 0; fileIndex < documentsData.documents.length; fileIndex++) {
        const document = documentsData.documents[fileIndex]; await performActions(
          'Add Document',
          ['uploadFile', document.fileName],
          ['select', {dropdown: uploadAdditionalDocuments.typeOfDocumentHiddenTextLabel, index: fileIndex}, document.type],
          ['inputText', {text: uploadAdditionalDocuments.shortDescriptionHiddenTextLabel, index: fileIndex}, document.description]
        );
      }
    }
    await performAction('clickButton', uploadAdditionalDocuments.continueButton);
  }

  private async completingYourClaim(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: completingYourClaim.whatWouldYouLikeToDoNextQuestion, option: option});
    await performAction('clickButton', completingYourClaim.continueButton);
  }

  private async selectLanguageUsed(languageDetails: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: languageDetails.question, option: languageDetails.option});
    await performAction('clickButton', claimLanguageUsed.continueButton);
  }

  private async claimSaved() {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseNumber });
    await performValidation('text', { elementType: 'paragraph', text: 'Property address: ' + addressInfo.buildingStreet + ', ' + addressInfo.townCity + ', ' + addressInfo.engOrWalPostcode });
    await performValidation('mainHeader', confirm.makeAClaimDynamicHeader);
    await performValidation('text', { elementType: 'span', text: confirm.claimSavedDynamicLabel });
    await performValidation('text', { elementType: 'paragraph', text: confirm.aDraftOfYourClaimDynamicParagraph });
    await performValidation('text', { elementType: 'listItem', text: confirm.resumeYourClaimDynamicParagraph });
    await performValidation('text', { elementType: 'listItem', text: confirm.clickThroughTheQuestionsDynamicParagraph });
    await performValidation('text', { elementType: 'listItem', text: confirm.chooseTheSubmitDynamicParagraph });
    await performValidation('text', { elementType: 'listItem', text: confirm.selectThePayTheClaimFeeDynamicParagraph });
    await performAction('clickButton', confirm.closeAndReturnToCaseDetailsButton);
  }

  private async payClaimFee(params?: { clickLink?: boolean }) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performValidation('mainHeader',confirm.makeAClaimDynamicHeader);
    await performValidation('text', {elementType: 'subHeader', text: confirm.makeAPaymentDynamicSubHeader});
    await performValidation('text', {elementType: 'span', text: confirm.pay404ClaimFeeDynamicParagraph});
    if (params?.clickLink === true) {
      await performAction('clickButton', confirm.payTheClaimFeeDynamicLink);
    }
    await performAction('clickButton', confirm.closeAndReturnToCaseDetailsButton);
  }

  private async enterTestAddressManually(page: Page, address: actionRecord) {
    await performActions(
      'Enter Address Manually'
      , ['clickButton', addressDetails.cantEnterUKPostcodeLink]
      , ['inputText', addressDetails.buildingAndStreetTextLabel, address.buildingAndStreet]
      , ['inputText', addressDetails.addressLine2TextLabel, addressDetails.addressLine2TextInput]
      , ['inputText', addressDetails.addressLine3TextLabel, addressDetails.addressLine3TextInput]
      , ['inputText', addressDetails.townOrCityTextLabel, address.townOrCity]
      , ['inputText', addressDetails.countyTextLabel, address.county]
      , ['inputText', addressDetails.postcodeTextLabel, address.postcode]
      , ['inputText', addressDetails.countryTextLabel, address.country]
    );
    addressInfo = {
      buildingStreet: await page.getByLabel(addressDetails.buildingAndStreetTextLabel).inputValue(),
      townCity: await page.getByLabel(addressDetails.townOrCityTextLabel).inputValue(),
      engOrWalPostcode: address.postcode.toString(),
    };
    await performAction('clickButton', addressDetails.continueButton);
  }

  private async provideMoreDetailsOfClaim(page: Page) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await expect(async () => {
        await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Next%20steps`);
      }).toPass({
        timeout: VERY_LONG_TIMEOUT,
      });
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continueButton, claimantInformation.mainHeader);
  }

  private async selectAdditionalReasonsForPossession(reasons: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: additionalReasonsForPossession.isThereAnyOtherInformationQuestion, option: reasons});
    if(reasons == additionalReasonsForPossession.yesRadioOption){
      await performAction('inputText', additionalReasonsForPossession.additionalReasonsForPossessionHiddenTextLabel, additionalReasonsForPossession.additionalReasonsForPossessionHiddenTextInput);
    }
    await performAction('clickButton', additionalReasonsForPossession.continueButton);
  }

  private async selectUnderlesseeOrMortgageeEntitledToClaim(underlesseeOrMortgageeData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: underlesseeOrMortgageeData.question,
      option: underlesseeOrMortgageeData.option
    });
    await performAction('clickButton', underlesseeMortgageeDetails.continueButton);
  }

  private async selectUnderlesseeMortgageeDetails(underlesseeOrMortgageeDetail: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: underlesseeMortgageeDetails.doYouKnowTheNameQuestion,
      option: underlesseeOrMortgageeDetail.nameOption
    });
    if (underlesseeOrMortgageeDetail.nameOption === underlesseeMortgageeDetails.yesRadioOption) {
      await performAction('inputText', underlesseeMortgageeDetails.whatIsTheirNameHiddenTextLabel, underlesseeOrMortgageeDetail.name);
    }
    await performAction('clickRadioButton', {
      question: underlesseeMortgageeDetails.doYouKnowTheAddressQuestion,
      option: underlesseeOrMortgageeDetail.addressOption
    });
    if (underlesseeOrMortgageeDetail.addressOption === underlesseeMortgageeDetails.yesRadioOption) {
      await performActions(
        'Find Address based on postcode',
        ['inputText', addressDetails.enterUKPostcodeTextLabel, underlesseeOrMortgageeDetail.address],
        ['clickButton', addressDetails.findAddressButton],
        ['select', addressDetails.addressSelectLabel, addressDetails.addressIndex]
      );
    }
    await performAction('clickRadioButton', {
      question: underlesseeMortgageeDetails.doYouNeedToAddAnotherQuestion,
      option: underlesseeOrMortgageeDetail.anotherUnderlesseeOrMortgageeOption
    });
    const additionalUnderlesseeMortgagee = Number(underlesseeOrMortgageeDetail.additionalUnderlesseeMortgagees) || 0;
    if (underlesseeOrMortgageeDetail.anotherUnderlesseeOrMortgageeOption === underlesseeMortgageeDetails.yesRadioOption && additionalUnderlesseeMortgagee > 0) {
      for (let i = 0; i < additionalUnderlesseeMortgagee; i++) {
        await performAction('clickButton', underlesseeMortgageeDetails.addNewHiddenButton);
        const index = i + 1;
        const nameQuestion = underlesseeMortgageeDetails.doYouKnowTheNameQuestion;
        const nameOption = underlesseeOrMortgageeDetail[`name${index}Option`] || underlesseeMortgageeDetails.noRadioOption;
        await performAction('clickRadioButton', {
          question: nameQuestion,
          option: nameOption,
          index,
        });
        await performAction('clickRadioButton', {
          question: nameQuestion,
          option: nameOption,
          index,
        });
        if (nameOption === underlesseeMortgageeDetails.yesRadioOption) {
          await performAction('inputText', {text: underlesseeMortgageeDetails.whatIsTheirNameHiddenTextLabel, index: index}, `${underlesseeOrMortgageeDetail.name}${index}`);
        }
        const addressQuestion = underlesseeMortgageeDetails.doYouKnowTheAddressQuestion;
        const correspondenceAddressOption =
          underlesseeOrMortgageeDetail[`correspondenceAddress${index}Option`] || underlesseeMortgageeDetails.noRadioOption;
        await performAction('clickRadioButton', {
          question: addressQuestion,
          option: correspondenceAddressOption,
          index,
        });
      }
    }
    await performAction('clickButton', underlesseeMortgageeDetails.continueButton);
  }

  private async selectStatementOfTruth(claimantDetails: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('clickRadioButton', {
      question: statementOfTruth.completedByLabel,
      option: claimantDetails.completedBy
    });
    if(claimantDetails.completedBy == statementOfTruth.claimantRadioOption){
      await performAction('check', claimantDetails.iBelieveCheckbox);
      await performAction('inputText', statementOfTruth.fullNameHiddenTextLabel, claimantDetails.fullNameTextInput);
      await performAction('inputText', statementOfTruth.positionOrOfficeHeldHiddenTextLabel, claimantDetails.positionOrOfficeTextInput);
    }
    if(claimantDetails.completedBy == statementOfTruth.claimantLegalRepresentativeRadioOption){
      await performAction('check', claimantDetails.signThisStatementCheckbox);
      await performAction('inputText', statementOfTruth.fullNameHiddenTextLabel, claimantDetails.fullNameTextInput);
      await performAction('inputText', statementOfTruth.nameOfFirmHiddenTextLabel, claimantDetails.nameOfFirmTextInput);
      await performAction('inputText', statementOfTruth.positionOrOfficeHeldHiddenTextLabel, claimantDetails.positionOrOfficeTextInput);
    }
    await performAction('clickButton', statementOfTruth.continueButton);
  }

  private async reloginAndFindTheCase(userInfo: actionData) {
    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
    await performAction('login', userInfo);
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${caseNumber.replace(/-/g, '')}#Next%20steps`);
    //Skipping Find Case search as per the decision taken on https://tools.hmcts.net/jira/browse/HDPI-3317
    //await performAction('searchCaseFromFindCase', caseNumber);
  }

  private async addCaseNotes(caseNote: actionRecord){
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid});
    await performValidation('text', {elementType: 'paragraph', text: `Property address: ${addressInfoCaseTab.buildingStreet}, ${addressInfoCaseTab.townCity}, ${addressInfoCaseTab.engOrWalPostcode}`});
    await performAction('inputText', caseNote.label, caseNote.input);
    await performAction('clickButton', addCaseNote.continueButton);
  }

  private async validateDefendantDetails(page: Page, defendantsDetails: actionRecord) {

    const defendant = new Map<string, string>();
    const payLoad = defendantsDetails.payLoad as Record<string, any>;
    if (defendantsDetails.defendant1NameKnown === 'YES') {
      defendant.set(`Defendant's first name`, `${payLoad.defendant1.firstName}`);
      defendant.set(`Defendant's last name`, `${payLoad.defendant1.lastName}`);
    } else {
      defendant.set(`Defendant's first name`, `null`);
      defendant.set(`Defendant's last name`, `null`);
    }

    if (payLoad.defendant1.addressKnown === 'YES' && payLoad.defendant1.addressSameAsPossession === 'YES') {
      const address = payLoad.formattedClaimantContactAddress.split('<br>');
      defendant.set(`Building and Street`, address[0]);
      defendant.set(`Address Line 2`, address[1]);
      defendant.set(`Town or City`, address[2]);
      defendant.set(`Postcode/Zipcode`, address[3]);
      defendant.set('Country', 'United Kingdom')

    } else if (payLoad.defendant1.addressKnown === 'YES' && payLoad.defendant1.addressSameAsPossession === 'NO') {
      defendant.set(`Building and Street`, payLoad.defendant1.correspondenceAddress.AddressLine1);
      defendant.set(`Address Line 2`, payLoad.defendant1.correspondenceAddress.AddressLine2);
      defendant.set(`Town or City`, payLoad.defendant1.correspondenceAddress.PostTown);
      defendant.set(`Postcode/Zipcode`, payLoad.defendant1.correspondenceAddress.PostCode);
      defendant.set('Country', 'United Kingdom')
    }
    await this.caseTabTableData(page, defendantsDetails.table as string);

    const misMatchMap = compareMaps(defendant, caseTabMap, {
      name1: 'Defendant',
      name2: 'CaseParties',
    })
    
      if (misMatchMap.size > 0) {
        console.log(`\n❌ Differences found: ${misMatchMap.size}`);
        for (const [key, val] of misMatchMap) {
          const expectedValue = val.a === undefined ? '<missing>' : String(val.a);
          const actualValue = val.b === undefined ? '<missing>' : String(val.b);
          console.log('============================================================');
          console.log(`• key: "${String(key)}" → Expected: ${expectedValue} | Actual: ${actualValue}`);
        }
        console.log(`\n**********  END OF FAILURE LIST. ***************`);
        throw new Error(`Case Parties (Defendant) validations failed for ${misMatchMap.size} ${misMatchMap.size === 1 ? 'item' : 'items'}`);
      } else {
        console.log('\n✅ Case Parties (Defendant) VALIDATION PASSED!\n');
      }
    
    caseTabMap.clear();

  }

  private async validateClaimantDetails(page: Page, defendantsDetails: actionRecord) {

    const claimant = new Map<string, string>();
    const payLoad = defendantsDetails.payLoad as Record<string, any>;

    claimant.set(`Name`, payLoad.claimantName);
    claimant.set(`Email address`, payLoad.claimantContactEmail);
    if (payLoad.claimantProvidePhoneNumber === 'YES') {
      claimant.set(`Telephone number`, payLoad.claimantContactPhoneNumber);
    }

    if (payLoad.orgAddressFound === 'Yes') {
      claimant.set(`Building and Street`, payLoad.organisationAddress.AddressLine1);
      claimant.set(`Address Line 2`, payLoad.organisationAddress.AddressLine2);
      claimant.set(`Town or City`, payLoad.organisationAddress.PostTown);
      claimant.set(`Postcode/Zipcode`, payLoad.organisationAddress.PostCode);
      claimant.set('Country', payLoad.organisationAddress.Country)
    }
    await this.caseTabTableData(page, defendantsDetails.table as string);

    const misMatchMap = compareMaps(claimant, caseTabMap, {
      name1: 'Claimant',
      name2: 'CaseParties',
    })
    
      if (misMatchMap.size > 0) {
        console.log(`\n❌ Differences found: ${misMatchMap.size}`);
        for (const [key, val] of misMatchMap) {
          const expectedValue = val.a === undefined ? '<missing>' : String(val.a);
          const actualValue = val.b === undefined ? '<missing>' : String(val.b);
          console.log('============================================================');
          console.log(`• key: "${String(key)}" → Expected: ${expectedValue} | Actual: ${actualValue}`);
        }
        console.log(`\n**********  END OF FAILURE LIST. ***************`);
        throw new Error(`Case Parties (Claimant) validations failed for ${misMatchMap.size} ${misMatchMap.size === 1 ? 'item' : 'items'}`);
      } else {
        console.log('\n✅ Case Parties (Claimant) VALIDATION PASSED!\n');
      }
    
    caseTabMap.clear();

  }

  private async validateCaseNotesDetails(page: Page, caseNotes: actionRecord) {

    const caseNote = new Map<string, string>();
    caseNote.set(`Created by`, process.env.Display_NAME as string);
    caseNote.set(`Created on`, caseNotes.createdOn as string);
    caseNote.set(`Note`, caseNotes.userInput as string);

    await this.caseTabTableData(page, caseNotes.table as string);

    const misMatchMap = compareMaps(caseNote, caseTabMap, {
      name1: 'CaseNote',
      name2: 'CaseNotesTab',
    })

    if (misMatchMap.size > 0) {
      console.log(`\n❌ Differences found: ${misMatchMap.size}`);
      for (const [key, val] of misMatchMap) {
        const expectedValue = val.a === undefined ? '<missing>' : String(val.a);
        const actualValue = val.b === undefined ? '<missing>' : String(val.b);
        console.log('============================================================');
        console.log(`• key: "${String(key)}" → Expected: ${expectedValue} | Actual: ${actualValue}`);
      }
      console.log(`\n**********  END OF FAILURE LIST. ***************`);
      throw new Error(`Case Notes validations failed for ${misMatchMap.size} ${misMatchMap.size === 1 ? 'item' : 'items'}`);
    } else {
      console.log('\n✅ Case Notes VALIDATION PASSED!\n');
    }

    caseTabMap.clear();

  }

  private async validateCaseSummaryDetails(page: Page, caseSummarySection: actionRecord) {

    let caseSummary = new Map<string, string>();
    let submitPayLoad = caseSummarySection.submitPayload as Record<string, any>;
    let createPayLoad = caseSummarySection.createPayload as Record<string, any>;

    switch (caseSummarySection.section) {
      case 'Defendant details':
        if (submitPayLoad.defendant1.nameKnown === 'YES') {
          caseSummary.set(`First name`, submitPayLoad.defendant1.firstName);
          caseSummary.set(`Last name`, submitPayLoad.defendant1.lastName);
        }
        if (submitPayLoad.defendant1.addressKnown === 'YES' && submitPayLoad.defendant1.addressSameAsPossession === 'YES') {
          const address = submitPayLoad.formattedClaimantContactAddress.split('<br>');
          caseSummary.set(`Building and Street`, address[0]);
          caseSummary.set(`Address Line 2`, address[1]);
          caseSummary.set(`Town or City`, address[2]);
          caseSummary.set(`Postcode/Zipcode`, address[3]);
          caseSummary.set('Country', 'United Kingdom')

        } else if (submitPayLoad.defendant1.addressKnown === 'YES' && submitPayLoad.defendant1.addressSameAsPossession === 'NO') {
          caseSummary.set(`Building and Street`, submitPayLoad.defendant1.correspondenceAddress.AddressLine1);
          caseSummary.set(`Address Line 2`, submitPayLoad.defendant1.correspondenceAddress.AddressLine2);
          caseSummary.set(`Town or City`, submitPayLoad.defendant1.correspondenceAddress.PostTown);
          caseSummary.set(`Postcode/Zipcode`, submitPayLoad.defendant1.correspondenceAddress.PostCode);
          caseSummary.set('Country', 'United Kingdom')
        }
        break;

      case 'Defendant Case details':
        caseSummary.set(`Defendant 1’s name known?`, formatWord(submitPayLoad.defendant1.nameKnown));
        caseSummary.set(`First name`, submitPayLoad.defendant1.firstName);
        caseSummary.set(`Last name`, submitPayLoad.defendant1.lastName);

        if (submitPayLoad.defendant1.addressKnown === 'YES' && submitPayLoad.defendant1.addressSameAsPossession === 'YES') {
          const address = submitPayLoad.formattedClaimantContactAddress.split('<br>');
          caseSummary.set(`Defendant 1’s address for service known?`, formatWord(submitPayLoad.defendant1.addressKnown));
          caseSummary.set(`Building and Street`, address[0]);
          caseSummary.set(`Address Line 2`, address[1]);
          caseSummary.set(`Town or City`, address[2]);
          caseSummary.set(`Postcode/Zipcode`, address[3]);
          caseSummary.set('Country', 'United Kingdom')

        } else if (submitPayLoad.defendant1.addressKnown === 'YES' && submitPayLoad.defendant1.addressSameAsPossession === 'NO') {
          caseSummary.set(`Defendant 1’s address for service known?`, formatWord(submitPayLoad.defendant1.addressKnown));
          caseSummary.set(`Building and Street`, submitPayLoad.defendant1.correspondenceAddress.AddressLine1);
          caseSummary.set(`Address Line 2`, submitPayLoad.defendant1.correspondenceAddress.AddressLine2);
          caseSummary.set(`Town or City`, submitPayLoad.defendant1.correspondenceAddress.PostTown);
          caseSummary.set(`Postcode/Zipcode`, submitPayLoad.defendant1.correspondenceAddress.PostCode);
          caseSummary.set('Country', 'United Kingdom')
        }
        break;

      case 'Defendant circumstances':
        caseSummary.set(`Is there any information you’re required to provide, or you want to provide, about the defendants’ circumstances?`, formatWord(submitPayLoad.hasDefendantCircumstancesInfo));
        if (submitPayLoad.hasDefendantCircumstancesInfo === 'YES') {
          caseSummary.set(`Details of defendants’ circumstances`, submitPayLoad.defendantCircumstancesInfo);
        }
        break;

      case 'Address of property':
        caseSummary.set(`Building and Street`, createPayLoad.propertyAddress.AddressLine1);
        caseSummary.set(`Address Line 2`, createPayLoad.propertyAddress.AddressLine2);
        caseSummary.set(`Town or City`, createPayLoad.propertyAddress.PostTown);
        caseSummary.set(`Postcode/Zipcode`, createPayLoad.propertyAddress.PostCode);
        caseSummary.set('Country', createPayLoad.propertyAddress.Country);
        break;

      case 'Claimant details':
        caseSummary.set(`Claimant name`, submitPayLoad.claimantName);
        break;

      case 'Claimant address':
        caseSummary.set(`Building and Street`, submitPayLoad.organisationAddress.AddressLine1);
        caseSummary.set(`Address Line 2`, submitPayLoad.organisationAddress.AddressLine2);
        caseSummary.set(`Town or City`, submitPayLoad.organisationAddress.PostTown);
        caseSummary.set(`Postcode/Zipcode`, submitPayLoad.organisationAddress.PostCode);
        caseSummary.set('Country', submitPayLoad.organisationAddress.Country);
        break;

      case 'Claimant contact details':
        caseSummary.set(`Email address for notifications`, submitPayLoad.claimantContactEmail);
        caseSummary.set(`Do you want to provide a phone number for urgent updates about your case?`, formatWord(submitPayLoad.claimantProvidePhoneNumber));
        caseSummary.set(`Contact phone number`, submitPayLoad.claimantContactPhoneNumber);
        break;

      case 'Claimant circumstances':
        caseSummary.set(`Is there any information you’d like to provide about the claimant’s circumstances?`, formatWord(submitPayLoad.claimantCircumstancesSelect));
        if (submitPayLoad.claimantCircumstancesSelect === 'YES') {
          caseSummary.set(`Claimant circumstances`, submitPayLoad.claimantCircumstancesDetails);
        }
        break;

      case 'Tenancy and Occupation':
        caseSummary.set(`Tenancy, occupation contract or licence agreement type`, (submitPayLoad.tenancy_TypeOfTenancyLicence)
          .toLowerCase()
          .replace(/_/g, " ")
          .replace(/^\w/, (c: string) => c.toUpperCase())
        );
        caseSummary.set(`Tenancy, occupation contract or licence agreement start date`, formatDate(submitPayLoad.tenancy_TenancyLicenceDate, 'DD/MM/YYYY'));
        break;

      case 'Tenancy and Occupation Case details':
        caseSummary.set(`Tenancy, occupation contract or licence agreement type`, (submitPayLoad.tenancy_TypeOfTenancyLicence)
          .toLowerCase()
          .replace(/_/g, " ")
          .replace(/^\w/, (c: string) => c.toUpperCase())
        );
        caseSummary.set(`Tenancy, occupation contract or licence start date`, formatDate(submitPayLoad.tenancy_TenancyLicenceDate, 'DD/MONTH/YYYY'));
        caseSummary.set(`Do you have a copy of the tenancy or licence agreement?`, formatWord(submitPayLoad.tenancy_HasCopyOfTenancyLicence));
        caseSummary.set(`Details of why you do not have a copy`, submitPayLoad.tenancy_ReasonsForNoTenancyLicenceDocuments);
        break;

      case 'Grounds of possession':
        if (submitPayLoad.introGrounds_HasIntroductoryDemotedOtherGroundsForPossession === 'YES') {
          caseSummary.set(`Grounds`, submitPayLoad.introGrounds_IntroductoryDemotedOrOtherGrounds.map(formatText).
            map((item: string) =>
              item === "Anti social" ? "Antisocial behaviour" : item
            )
            .join(','));
        };
        break;

      case 'Rent arrears':
        caseSummary.set(`Rent amount`, formatCurrency(submitPayLoad.rentDetails_CurrentRent));
        caseSummary.set(`How rent is calculated`, formatWord(submitPayLoad.rentDetails_Frequency));
        caseSummary.set(`Daily rate`, formatCurrency(submitPayLoad.rentDetails_CalculatedDailyCharge));
        caseSummary.set(`Rent arrears total at the time of claim issue`, formatCurrency(submitPayLoad.rentArrears_Total));
        caseSummary.set(`Judgment requested for the outstanding arrears?`, formatWord(submitPayLoad.arrearsJudgmentWanted));
        break;

      case 'Rent arrears Case details':
        caseSummary.set(`Rent amount`, formatCurrency(submitPayLoad.rentDetails_CurrentRent));
        caseSummary.set(`How rent is calculated`, formatWord(submitPayLoad.rentDetails_Frequency));
        caseSummary.set(`Daily rate`, formatCurrency(submitPayLoad.rentDetails_CalculatedDailyCharge));
        caseSummary.set(`Previous steps taken to recover rent arrears?`, formatWord(submitPayLoad.rentArrears_RecoveryAttempted));
        caseSummary.set(`Rent statement`, submitPayLoad.rentArrears_StatementDocuments?.[0]?.value?.document_filename);
        caseSummary.set(`Rent arrears total at the time of claim issue`, formatCurrency(submitPayLoad.rentArrears_Total));
        caseSummary.set(`Judgment requested for the outstanding arrears?`, formatWord(submitPayLoad.arrearsJudgmentWanted));
        break;

      case 'Notice':
        const serviceMethod = submitPayLoad.notice_NoticeServiceMethod;
        const dateServed =
          serviceMethod === 'FIRST_CLASS_POST'
            ? submitPayLoad.notice_NoticePostedDate
            : serviceMethod === 'EMAIL'
              ? submitPayLoad.notice_NoticeEmailSentDateTime
              : null;

        if (dateServed) {
          const formattedDate =
            serviceMethod === 'FIRST_CLASS_POST'
              ? formatDate(dateServed,'DD/MM/YYYY')
              : formatDateTime(dateServed);

          caseSummary.set('Date notice was served', formattedDate);
        }
        break;

      case 'Notice Case details':
        const serviceMethodCaseDetails = submitPayLoad.notice_NoticeServiceMethod;
        const dateServedCaseDetails =
          serviceMethodCaseDetails === 'FIRST_CLASS_POST'
            ? submitPayLoad.notice_NoticePostedDate
            : serviceMethodCaseDetails === 'EMAIL'
              ? submitPayLoad.notice_NoticeEmailSentDateTime
              : null;
        const methodOfService = serviceMethodCaseDetails === 'FIRST_CLASS_POST' ? 'By first class post or other service which provides for delivery on the next business day' : serviceMethodCaseDetails === 'EMAIL' ? 'By Email' : null;
        caseSummary.set('Has notice been served?',formatWord(submitPayLoad.noticeServed));
        caseSummary.set('Method of service', methodOfService as string);

        if (dateServedCaseDetails) {
          const formattedDate =
            serviceMethodCaseDetails === 'FIRST_CLASS_POST'
              ? formatDate(dateServedCaseDetails, 'DD/MONTH/YYYY')
              : formatDateTime(dateServedCaseDetails);

          caseSummary.set('Date and time notice served (if applicable)', formattedDate);
        }
        break;

      case 'Claim details':
        caseSummary.set(`Claimant type`, submitPayLoad.claimantType.value.label);
        caseSummary.set(`Is your claim a trespass claim?`, formatWord(submitPayLoad.claimAgainstTrespassers));
        break;

      case 'Actions taken':
        caseSummary.set(`Pre-action protocol followed?`, formatWord(submitPayLoad.preActionProtocolCompleted));
        caseSummary.set(`Mediation attempted?`, formatWord(submitPayLoad.mediationAttempted));
        caseSummary.set(`Settlement attempted?`, formatWord(submitPayLoad.settlementAttempted));
        break;

      case 'Reasons for possession':
        if (submitPayLoad.introGrounds_IntroductoryDemotedOrOtherGrounds?.includes('ANTI_SOCIAL')) {
          caseSummary.set(`Reasons for claiming possession under Antisocial behaviour`, submitPayLoad.antiSocialBehaviourGround);
        }
        if (submitPayLoad.introGrounds_IntroductoryDemotedOrOtherGrounds?.includes('BREACH_OF_THE_TENANCY')) {
          caseSummary.set(`Reasons for claiming possession under Breach of the tenancy`, submitPayLoad.breachOfTheTenancyGround);
        }
        if (submitPayLoad.introGrounds_IntroductoryDemotedOrOtherGrounds?.includes('ABSOLUTE_GROUNDS')) {
          caseSummary.set(`Reasons for claiming possession under Absolute grounds`, submitPayLoad.absoluteGrounds);
        }
        caseSummary.set(`Do you have any additional reasons for possession?`, formatWord(submitPayLoad.additionalReasonsForPossession.hasReasons));
        if (submitPayLoad.additionalReasonsForPossession.hasReasons === 'YES') {
          caseSummary.set(`Details of additional reasons`, formatWord(submitPayLoad.additionalReasonsForPossession.reasons));
        }
        break;

      case 'Applications':
        caseSummary.set(`Are you planning to make an application at the same time as your claim?`, formatWord(submitPayLoad.applicationWithClaim));
        break;

      case 'Demotion of tenancy':
        if (submitPayLoad.alternativesToPossession?.includes('DEMOTION_OF_TENANCY')) {
          const sectionOfHousingAct = submitPayLoad.demotionOfTenancyActs === 'SECTION_82A_2' ? 'Section 82A(2) of the Housing Act 1985' : 'Section 6A(2) of the Housing Act 1988';
          caseSummary.set(`Section of the Housing Act demotion of tenancy claim made under`, sectionOfHousingAct);
          caseSummary.set(`Have you served the defendants with a statement of the express terms which will apply to the demoted tenancy?`, formatWord(submitPayLoad.demotionOfTenancy_StatementOfExpressTermsServed));
          if (submitPayLoad.demotionOfTenancy_StatementOfExpressTermsServed === 'YES') {
            caseSummary.set(`Details of terms`, submitPayLoad.demotionOfTenancy_StatementOfExpressTermsDetails)
          }
          caseSummary.set(`Reasons for requesting a demotion of tenancy order`, submitPayLoad.demotionOrderReason)
        }
        break;

      case 'Suspension of right to buy':
        if (submitPayLoad.alternativesToPossession?.includes('SUSPENSION_OF_RIGHT_TO_BUY')) {
          const sectionOfHousingActSuspension = submitPayLoad.suspensionOfRightToBuyActs === 'SECTION_82A_2' ? 'Section 82A(2) of the Housing Act 1985' : 'Section 6A(2) of the Housing Act 1988';
          caseSummary.set(`Section of the Housing Act suspension of right to buy claim made under`, sectionOfHousingActSuspension);
          caseSummary.set(`Reasons for requesting suspension of right to buy order`, submitPayLoad.suspensionOrderReason)
        }
        break;

      case 'Underlessee or mortgagee':
        if (submitPayLoad.hasUnderlesseeOrMortgagee === 'YES') {
          caseSummary.set(`Underlessee or mortgagee’s name known?`, formatWord(submitPayLoad.underlesseeOrMortgagee1.nameKnown));
          caseSummary.set(`Name`, submitPayLoad.underlesseeOrMortgagee1.nameKnown === 'YES' ? submitPayLoad.underlesseeOrMortgagee1.name : '');
          caseSummary.set(`Underlessee or mortgagee’s address for service known?`, formatWord(submitPayLoad.underlesseeOrMortgagee1.addressKnown));
          if (submitPayLoad.underlesseeOrMortgagee1.addressKnown === 'YES') {
            caseSummary.set(`Building and Street`, submitPayLoad.underlesseeOrMortgagee1.address.AddressLine1);
            caseSummary.set(`Address Line 2`, submitPayLoad.underlesseeOrMortgagee1.address.AddressLine2);
            caseSummary.set(`Town or City`, submitPayLoad.underlesseeOrMortgagee1.address.PostTown);
            caseSummary.set(`Postcode/Zipcode`, submitPayLoad.underlesseeOrMortgagee1.address.PostCode);
            caseSummary.set('Country', submitPayLoad.underlesseeOrMortgagee1.address.Country);
          }
        }
        break;

      default:
        break;
    };

    await this.caseTabTableData(page, caseSummarySection.table as string);

    const misMatchMap = compareMaps(caseSummary, caseTabMap, {
      name1: `${caseSummarySection.section}`,
      name2: 'CaseSummaryDetailsTab',
    })

    if (misMatchMap.size > 0) {
      console.log(`\n❌ Differences found: ${misMatchMap.size}`);
      for (const [key, val] of misMatchMap) {
        const expectedValue = val.a === undefined ? '<missing>' : String(val.a);
        const actualValue = val.b === undefined ? '<missing>' : String(val.b);
        console.log('============================================================');
        console.log(`• key: "${String(key)}" → Expected: ${expectedValue} | Actual: ${actualValue}`);
      }
      console.log(`\n**********  END OF FAILURE LIST. ***************`);
      throw new Error(`Case Summary/Details validations failed for ${misMatchMap.size} ${misMatchMap.size === 1 ? 'item' : 'items'} in "${caseSummarySection.section}" section`);
    } else {
      console.log(`\n✅ Case Summary/Details VALIDATION for section "${caseSummarySection.section}" PASSED!\n`);
    }

    caseTabMap.clear();

  }

  private async caseTabTableData(page: Page, table: string) {

    const tables = page.locator(`//span[text()="${table}"]/ancestor::div[1]/child::table[@aria-describedby="complex field table"]`);
    const tableCount = await tables.count();

    if (tableCount === 0) throw new Error(`the table ${table} not found. Exiting...`);

    for (let i = 0; i < tableCount; i++) {
      const table = tables.nth(i);
      await expect(table).toBeVisible();

      const rows = table.locator('tr');
      const rowCount = await rows.count();

      for (let j = 0; j < rowCount; j++) {
        const row = rows.nth(j);
        if (!(await row.isVisible())) continue;

        const keyQns = row.locator('th span, th');
        const valAns = row.locator('td.case-field-content, td');

        if ((await keyQns.count()) === 0 || (await valAns.count()) === 0) continue;

        const keyText = (await keyQns.first().innerText()).trim();
        let valText = (await valAns.first().innerText()).trim().replace(/\r?\n+/g, ',');

        if (keyText === "Created on") {
          valText = valText.replace(/:\d{2} /, " ");
        }

        if (keyText && keyText.length > 0) {
          caseTabMap.set(keyText ?? '', valText ?? '');
        }
      }
    }
  };
}