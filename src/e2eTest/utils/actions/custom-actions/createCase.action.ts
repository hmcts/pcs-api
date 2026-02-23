import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {expect, Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
import {
  createCase,
  addressDetails,
  housingPossessionClaim,
  resumeClaimOptions,
  reasonsForPossession,
  borderPostcode,
  rentArrearsOrBreachOfTenancy,
  whatAreYourGroundsForPossession,
  reasonsForRequestingADemotionOrder,
  statementOfExpressTerms,
  reasonsForRequestingASuspensionOrder,
  userIneligible,
  whatAreYourGroundsForPossessionWales,
  underlesseeOrMortgageeDetails,
  reasonsForRequestingASuspensionAndDemotionOrder,
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
  claimingCosts,
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
} from '@data/page-data-figma';
import {MEDIUM_TIMEOUT, VERY_LONG_TIMEOUT} from 'playwright.config';
export let caseNumber: string;
export let claimantsName: string;
export let addressInfo: { buildingStreet: string; townCity: string; engOrWalPostcode: string };

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
      ['selectAdditionalReasonsForPossession', () => this.selectAdditionalReasonsForPossession(fieldName)],
      ['selectUnderlesseeOrMortgageeEntitledToClaim', () => this.selectUnderlesseeOrMortgageeEntitledToClaim(fieldName as actionRecord)],
      ['selectUnderlesseeOrMortgageeDetails', () => this.selectUnderlesseeOrMortgageeDetails(fieldName as actionRecord)],
      ['wantToUploadDocuments', () => this.wantToUploadDocuments(fieldName as actionRecord)],
      ['uploadAdditionalDocs', () => this.uploadAdditionalDocs(fieldName as actionRecord)],
      ['selectStatementOfTruth', () => this.selectStatementOfTruth(fieldName as actionRecord)],
      ['claimSaved', () => this.claimSaved()],
      ['payClaimFee', () => this.payClaimFee()]
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
    //await performAction('clickButtonAndVerifyPageNavigation', createCase.start, housingPossessionClaim.mainHeader);
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
    await performAction('clickButtonAndVerifyPageNavigation', resumeClaimOptions.continue, claimantType.mainHeader);
  }

  private async selectClaimantType(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question:claimantType.whoIsTheClaimantQuestion, option: caseData});
    if(caseData === claimantType.england.registeredProviderForSocialHousing || caseData === claimantType.wales.communityLandlord){
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
    if(caseData === claimType.noRadioOption){
      await performAction('clickButtonAndVerifyPageNavigation', claimType.continueButton, claimantInformation.mainHeader);
    }
    else{
      await performAction('clickButtonAndVerifyPageNavigation', claimType.continueButton, userIneligible.mainHeader);
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
    await performValidation('text', {elementType: 'paragraph', text: preactionProtocol.ifYourClaimIsOnDynamicParagraph});
    await performAction('clickRadioButton', {question:preactionProtocol.haveYouFollowedThePreactionQuestion, option: caseData});
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
        question: defendantDetails.isTheDefendantsCorrespondenceAddressHiddenQuestion,
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
            question: defendantDetails.isTheDefendantsCorrespondenceAddressHiddenQuestion,
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
    if (tenancyData.files) {
      await performAction('uploadFile', tenancyData.files);
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
          break;
        case 'mandatoryAccommodation':
          await performAction('check', {question: whatAreYourGroundsForPossession.mandatoryWithAccommodation.mandatoryWithAccommodationGroundsCategoryQuestion, option: possessionGrounds.mandatoryAccommodation});
          break;
        case 'discretionaryAccommodation':
          await performAction('check', {question: whatAreYourGroundsForPossession.discretionaryWithAccommodation.discretionaryWithAccommodationGroundsCategoryQuestion, option: possessionGrounds.discretionaryAccommodation});
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
    await performAction('clickButton', rentArrearsOrBreachOfTenancy.continue);
  }

  private async enterReasonForPossession(reasons: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if (!Array.isArray(reasons)) {
      throw new Error(`EnterReasonForPossession expected an array, but received ${typeof reasons}`);
    }
    for (let n = 0; n < reasons.length; n++) {
      await performAction('inputText',  {text:reasons[n],index: n}, reasonsForPossession.detailsAboutYourReason+"-"+reasons[n]);
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
    if (mediationSettlement.attemptedMediationWithDefendantsOption == mediationAndSettlement.yesRadioOption) {
      await performAction('inputText', mediationAndSettlement.giveDetailsAboutTheAttemptedHiddenTextLabel, mediationAndSettlement.giveDetailsAboutTheAttemptedHiddenTextInput);
    }
    await performAction('clickRadioButton', {
      question: mediationAndSettlement.haveYouTriedToReachQuestion,
      option: mediationSettlement.settlementWithDefendantsOption
    });
    if (mediationSettlement.settlementWithDefendantsOption == mediationAndSettlement.yesRadioOption) {
      await performAction('inputText', mediationAndSettlement.explainWhatStepsYouHaveTakenHiddenTextLabel, mediationAndSettlement.explainWhatStepsYouHaveTakenHiddenTextInput);
    }
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
      question: rentArrears.forThePeriodShownOnTheRentStatementHaveAnyRentPaymentsBeenPaidBySomeoneOtherThanTheDefendantsQuestion,
      option: rentArrearsData.rentPaidByOthersOption
    });
    if (rentArrearsData.rentPaidByOthersOption == rentArrears.yesRadioOption) {
      await performAction('check', {question: rentArrears.whereHaveThePaymentsComeFromHiddenQuestion, option: rentArrearsData.paymentOptions});
      if ((rentArrearsData.paymentOptions as Array<string>).includes(rentArrears.otherHiddenCheckBox)) {
        await performAction('inputText', rentArrears.paymentSourceHiddenTextLabel, rentArrears.paymentSourceHiddenTextInput);
      }
      await performAction('clickButton', rentArrears.continueButton);
    }
  }

  private async selectMoneyJudgment(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: moneyJudgment.doYouWantTheCourtQuestion, option: option});
    await performAction('clickButton', moneyJudgment.continueButton);
  }

  private async selectClaimingCosts(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: claimingCosts.doYouWantToAskForYourCostBackQuestion, option: option});
    await performAction('clickButton', claimingCosts.continueButton);
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
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('inputText', reason, reasonsForRequestingADemotionOrder.sampleTestReason);
    await performAction('clickButton', reasonsForRequestingADemotionOrder.continue);
  }

  private async enterReasonForSuspensionOrder(reason: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('inputText', reason, reasonsForRequestingASuspensionOrder.sampleTestReason);
    await performAction('clickButton', reasonsForRequestingASuspensionOrder.continue);
  }

  private async enterReasonForSuspensionAndDemotionOrder(reason: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('inputText', reason.suspension, reasonsForRequestingASuspensionOrder.sampleTestReason);
    await performAction('inputText', reason.demotion, reasonsForRequestingADemotionOrder.sampleTestReason);
    await performAction('clickButton', reasonsForRequestingASuspensionAndDemotionOrder.continue);
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
      for (const document of documentsData.documents) {
        await performActions(
          'Add Document',
          ['uploadFile', document.fileName],
          ['select', uploadAdditionalDocuments.typeOfDocumentHiddenTextLabel, document.type],
          ['inputText', uploadAdditionalDocuments.shortDescriptionHiddenTextLabel, document.description]
        );
      }
      await performAction('clickButton', uploadAdditionalDocuments.continueButton);
    }
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
    await performValidation('mainHeader',confirm.makeAClaimDynamicHeader);
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
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continueButton, claimantType.mainHeader);
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
    await performAction('clickButton', underlesseeOrMortgageeDetails.continueButton);
  }

  private async selectUnderlesseeOrMortgageeDetails(underlesseeOrMortgageeDetail: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: underlesseeOrMortgageeDetails.doYouKnowTheNameQuestion,
      option: underlesseeOrMortgageeDetail.nameOption
    });
    if (underlesseeOrMortgageeDetail.nameOption === underlesseeOrMortgageeDetails.yesRadioOption) {
      await performAction('inputText', underlesseeOrMortgageeDetails.doYouKnowTheNameTextLabel, underlesseeOrMortgageeDetail.name);
    }
    await performAction('clickRadioButton', {
      question: underlesseeOrMortgageeDetails.doYouKnowTheAddressQuestion,
      option: underlesseeOrMortgageeDetail.addressOption
    });
    if (underlesseeOrMortgageeDetail.addressOption === underlesseeOrMortgageeDetails.yesRadioOption) {
      await performActions(
        'Find Address based on postcode',
        ['inputText', addressDetails.enterUKPostcodeTextLabel, underlesseeOrMortgageeDetail.address],
        ['clickButton', addressDetails.findAddressButton],
        ['select', addressDetails.addressSelectLabel, addressDetails.addressIndex]
      );
    }
    await performAction('clickRadioButton', {
      question: underlesseeOrMortgageeDetails.addAnotherUnderlesseeOrMortgageeQuestion,
      option: underlesseeOrMortgageeDetail.anotherUnderlesseeOrMortgageeOption
    });
    const additionalUnderlesseeMortgagee = Number(underlesseeOrMortgageeDetail.additionalUnderlesseeMortgagees) || 0;
    if (underlesseeOrMortgageeDetail.anotherUnderlesseeOrMortgageeOption === underlesseeOrMortgageeDetails.yesRadioOption && additionalUnderlesseeMortgagee > 0) {
      for (let i = 0; i < additionalUnderlesseeMortgagee; i++) {
        await performAction('clickButton', underlesseeOrMortgageeDetails.addNewButton);
        const index = i + 1;
        const nameQuestion = underlesseeOrMortgageeDetails.doYouKnowTheNameQuestion;
        const nameOption = underlesseeOrMortgageeDetail[`name${index}Option`] || underlesseeOrMortgageeDetails.noRadioOption;
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
        if (nameOption === underlesseeOrMortgageeDetails.yesRadioOption) {
          await performAction('inputText', {text: underlesseeOrMortgageeDetails.doYouKnowTheNameTextLabel, index: index}, `${underlesseeOrMortgageeDetail.name}${index}`);
        }
        const addressQuestion = underlesseeOrMortgageeDetails.doYouKnowTheAddressQuestion;
        const correspondenceAddressOption =
          underlesseeOrMortgageeDetail[`correspondenceAddress${index}Option`] || underlesseeOrMortgageeDetails.noRadioOption;
        await performAction('clickRadioButton', {
          question: addressQuestion,
          option: correspondenceAddressOption,
          index,
        });
      }
    }
    await performAction('clickButton', underlesseeOrMortgageeDetails.continueButton);
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
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/PCS${process.env.CHANGE_ID ? `-${process.env.CHANGE_ID}` : ''}/${caseNumber.replace(/-/g, '')}#Next%20steps`);
    //Skipping Find Case search as per the decision taken on https://tools.hmcts.net/jira/browse/HDPI-3317
    //await performAction('searchCaseFromFindCase', caseNumber);
  }
}
