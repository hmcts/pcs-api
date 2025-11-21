import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
import {collectCYAAddressData, collectCYAData} from '@utils/cya/cya-field-collector';
import {createCase, addressDetails, housingPossessionClaim, defendantDetails, claimantName, contactPreferences, mediationAndSettlement,
        tenancyLicenceDetails, resumeClaimOptions, rentDetails, dailyRentAmount, reasonsForPossession, detailsOfRentArrears, claimantType, claimType,
        groundsForPossession, preActionProtocol, noticeOfYourIntention, borderPostcode, rentArrearsPossessionGrounds, rentArrearsOrBreachOfTenancy,
        noticeDetails, moneyJudgment, whatAreYourGroundsForPossession, languageUsed, defendantCircumstances, applications, claimantCircumstances,
        claimingCosts, alternativesToPossession, reasonsForRequestingADemotionOrder, statementOfExpressTerms, reasonsForRequestingASuspensionOrder,
        uploadAdditionalDocs, additionalReasonsForPossession, completeYourClaim, home, search, userIneligible, whatAreYourGroundsForPossessionWales,
        underlesseeOrMortgageeDetails, reasonsForRequestingASuspensionAndDemotionOrder, provideMoreDetailsOfClaim, addressCheckYourAnswers, statementOfTruth, propertyDetails} from '@data/page-data';

export let caseNumber: string;
export let claimantsName: string;
export let addressInfo: { buildingStreet: string; addressLine2: string; addressLine3: string, townCity: string; country: string; engOrWalPostcode: string };

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
      ['selectJurisdictionCaseTypeEvent', () => this.selectJurisdictionCaseTypeEvent()],
      ['enterTestAddressManually', () => this.enterTestAddressManually(page, fieldName as actionRecord)],
      ['selectClaimType', () => this.selectClaimType(fieldName)],
      ['selectClaimantName', () => this.selectClaimantName(page,fieldName)],
      ['selectContactPreferences', () => this.selectContactPreferences(page, fieldName as actionRecord)],
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
      ['selectStatementOfTruth', () => this.selectStatementOfTruth(fieldName as actionRecord)]
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
      addressLine2: await page.getByLabel(addressDetails.addressLine2TextLabel).inputValue(),
      addressLine3: await page.getByLabel(addressDetails.addressLine3TextLabel).inputValue(),
      townCity: await page.getByLabel(addressDetails.townOrCityTextLabel).inputValue(),
      engOrWalPostcode: await page.getByLabel(addressDetails.postcodeTextLabel, { exact: true }).inputValue(),
      country: await page.getByLabel(addressDetails.countryTextLabel).inputValue(),
    };
    // Collect CYA data (Address CYA) - collect each address field separately
    // Each field appears as a separate Q&A on the Address CYA page
    if (addressInfo.buildingStreet) {
      await collectCYAAddressData('selectAddress', propertyDetails.buildingAndStreetLabel, addressInfo.buildingStreet);
    }
    if (addressInfo.addressLine2) {
      await collectCYAAddressData('selectAddress', propertyDetails.addressLine2Label, addressInfo.addressLine2);
    }
    if (addressInfo.addressLine3) {
      await collectCYAAddressData('selectAddress', 'Address Line 3', addressInfo.addressLine3);
    }
    if (addressInfo.townCity) {
      await collectCYAAddressData('selectAddress', propertyDetails.townOrCityLabel, addressInfo.townCity);
    }
    if (addressInfo.country) {
      await collectCYAAddressData('selectAddress', propertyDetails.countryLabel, addressInfo.country);
    }
    if (addressInfo.engOrWalPostcode) {
      await collectCYAAddressData('selectAddress', propertyDetails.postcodeZipcodeLabel, addressInfo.engOrWalPostcode);
    }
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
    await performAction('clickRadioButton', caseData);
    await performAction('clickButtonAndVerifyPageNavigation', resumeClaimOptions.continue, claimantType.mainHeader);
  }

  private async selectClaimantType(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', caseData);
    // Collect CYA data
    await collectCYAData('selectClaimantType', claimantType.whoIsTheClaimantQuestion, caseData);
    if(caseData === claimantType.england.registeredProviderForSocialHousing || caseData === claimantType.wales.communityLandlord){
      await performAction('clickButtonAndVerifyPageNavigation', claimantType.continue, claimType.mainHeader);
    }
    else{
      await performAction('clickButtonAndVerifyPageNavigation', claimantType.continue, userIneligible.mainHeader);
    }
  }

  private async selectClaimType(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', caseData);
    // Collect CYA data
    await collectCYAData('selectClaimType', claimType.isThisAClaimAgainstTrespassersQuestion, caseData);
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
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', possessionGrounds.groundsRadioInput);
    // Collect CYA data
    await collectCYAData('selectGroundsForPossession', groundsForPossession.groundsForPossessionSectionInlineText, possessionGrounds.groundsRadioInput);
    if (possessionGrounds.groundsRadioInput == groundsForPossession.yes) {
      if (possessionGrounds.grounds) {
        await performAction('check', possessionGrounds.grounds);
        const groundsArray = possessionGrounds.grounds as string[];
        await collectCYAData('selectGroundsForPossession', 'What are your grounds for possession?', groundsArray.join(', '));
        if (groundsArray.includes(groundsForPossession.other)) {
          await performAction('inputText', groundsForPossession.enterGroundsForPossessionLabel, groundsForPossession.enterYourGroundsForPossessionInput);
          await collectCYAData('selectGroundsForPossession', groundsForPossession.enterGroundsForPossessionLabel, groundsForPossession.enterYourGroundsForPossessionInput);
        }
      }
    }
    await performAction('clickButton', groundsForPossession.continue);
  }

  private async selectPreActionProtocol(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', caseData);
    // Collect CYA data
    await collectCYAData('selectPreActionProtocol', preActionProtocol.mainHeader, caseData);
    await performAction('clickButton', preActionProtocol.continue);
  }

  private async selectNoticeOfYourIntention(caseData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', caseData);
    if ( caseData.option === noticeOfYourIntention.yes && caseData.typeOfNotice) {
      await performAction('inputText', noticeOfYourIntention.typeOfNotice, noticeOfYourIntention.typeOfNoticeInput);
    }
    // Collect CYA data
    await collectCYAData('selectNoticeOfYourIntention', noticeOfYourIntention.servedNoticeInteractiveText, caseData.option);
    if (caseData.option === noticeOfYourIntention.yes && caseData.typeOfNotice) {
      await collectCYAData('selectNoticeOfYourIntention', noticeOfYourIntention.typeOfNotice, noticeOfYourIntention.typeOfNoticeInput);
    }
    await performAction('clickButton', noticeOfYourIntention.continue);
  }

  private async selectBorderPostcode(option: actionData) {
    await performAction('clickRadioButton', option);
    // Collect CYA data (Address CYA - this question appears on Address CYA page)
    await collectCYAAddressData('selectBorderPostcode', borderPostcode.isThePropertyLocatedInEnglandOrWalesQuestion, option);
    await performAction('clickButton', borderPostcode.continueButton);
  }

  private async selectClaimantName(page: Page, caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', caseData);
    await collectCYAData('selectClaimantName', claimantName.isThisTheCorrectClaimantNameQuestion, caseData);
    if(caseData == claimantName.no){
      await performAction('inputText', claimantName.whatIsCorrectClaimantNameQuestion, claimantName.correctClaimantNameInput);
      await collectCYAData('selectClaimantName', claimantName.whatIsCorrectClaimantNameQuestion, claimantName.correctClaimantNameInput);
    }
    claimantsName = caseData == "No" ? claimantName.correctClaimantNameInput : await this.extractClaimantName(page, claimantName.yourClaimantNameRegisteredWithHMCTS);
  }

  private async selectContactPreferences(page: Page, prefData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: contactPreferences.emailAddressForNotifications,
      option: prefData.notifications
    });
    await collectCYAData('selectContactPreferences', contactPreferences.emailAddressForNotifications, prefData.notifications);
    if (prefData.notifications === contactPreferences.no) {
      await performAction('inputText', contactPreferences.enterEmailAddressLabel, contactPreferences.emailIdInput);
      await collectCYAData('selectContactPreferences', contactPreferences.enterEmailAddressLabel, contactPreferences.emailIdInput);
    }
    await performAction('clickRadioButton', {
      question: contactPreferences.doYouWantDocumentsToBeSentToAddress,
      option: prefData.correspondenceAddress
    });
    await collectCYAData('selectContactPreferences', contactPreferences.doYouWantDocumentsToBeSentToAddress, prefData.correspondenceAddress);
    if (prefData.correspondenceAddress === contactPreferences.no) {
      await performActions(
        'Find Address based on postcode',
        ['inputText', addressDetails.enterUKPostcodeTextLabel, addressDetails.englandCourtAssignedPostcodeTextInput],
        ['clickButton', addressDetails.findAddressButton],
        ['select', addressDetails.addressSelectLabel, addressDetails.addressIndex]
      );
    }
    // await collectCYAData('selectContactPreferences', 'Enter address details', {
    //       buildingStreet: await page.getByLabel(addressDetails.buildingAndStreetTextLabel).inputValue(),
    //       addressLine2: await page.getByLabel(addressDetails.addressLine2TextLabel).inputValue(),
    //       addressLine3: await page.getByLabel(addressDetails.addressLine3TextLabel).inputValue(),
    //       townCity: await page.getByLabel(addressDetails.townOrCityTextLabel).inputValue(),
    //       engOrWalPostcode: await page.getByLabel(addressDetails.postcodeTextLabel, { exact: true }).inputValue(),
    //       country: await page.getByLabel(addressDetails.countryTextLabel).inputValue(),
    //     });
    if(prefData.phoneNumber) {
      await performAction('clickRadioButton', {
        question: contactPreferences.provideContactPhoneNumber,
        option: prefData.phoneNumber
      });
      await collectCYAData('selectContactPreferences', contactPreferences.provideContactPhoneNumber, prefData.phoneNumber);
      if (prefData.phoneNumber === contactPreferences.yes) {
        await performAction('inputText', contactPreferences.enterPhoneNumberLabel, contactPreferences.phoneNumberInput);
        await collectCYAData('selectContactPreferences', contactPreferences.enterPhoneNumberLabel, contactPreferences.phoneNumberInput);
      }
    }
    await performAction('clickButton', contactPreferences.continue);
  }

  private async addDefendantDetails(defendantData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: defendantDetails.doYouKnowTheDefendantNameQuestion,
      option: defendantData.nameOption,
    });
    await collectCYAData('selectContactPreferences', defendantDetails.doYouKnowTheDefendantNameQuestion, defendantData.nameOption);
    if (defendantData.nameOption === defendantDetails.yesRadioOption) {
      await performAction('inputText', defendantDetails.defendantFirstNameTextLabel, defendantData.firstName);
      await performAction('inputText', defendantDetails.defendantLastNameTextLabel, defendantData.lastName);
    }
    await performAction('clickRadioButton', {
      question: defendantDetails.defendantCorrespondenceAddressQuestion,
      option: defendantData.correspondenceAddressOption,
    });
    await collectCYAData('selectContactPreferences', defendantDetails.defendantCorrespondenceAddressQuestion, defendantData.correspondenceAddressOption);
    if (defendantData.correspondenceAddressOption === defendantDetails.yesRadioOption) {
      await performAction('clickRadioButton', {
        question: defendantDetails.isCorrespondenceAddressSameQuestion,
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
      question: defendantDetails.additionalDefendantsQuestion,
      option: defendantData.addAdditionalDefendantsOption,
    });
    await collectCYAData('selectContactPreferences', defendantDetails.additionalDefendantsQuestion, defendantData.addAdditionalDefendantsOption);
    if (defendantData.addAdditionalDefendantsOption === defendantDetails.yesRadioOption && numAdditionalDefendants > 0) {
      for (let i = 0; i < numAdditionalDefendants; i++) {
        await performAction('clickButton', defendantDetails.addNewButton);
        const index = i + 1;
        const nameQuestion = defendantDetails.doYouKnowTheDefendantNameQuestion;
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
          await performAction('inputText', {text: defendantDetails.defendantFirstNameTextLabel, index: index}, `${defendantData.firstName}${index}`);
          await performAction('inputText', {text: defendantDetails.defendantLastNameTextLabel, index:index}, `${defendantData.lastName}${index}`
          );
        }
        const addressQuestion = defendantDetails.defendantCorrespondenceAddressQuestion;
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
            question: defendantDetails.isCorrespondenceAddressSameQuestion,
            option: correspondenceAddressSameOption,
            index,
          });
        }
      }
    }
    // Collect CYA data
    await collectCYAData('addDefendantDetails', defendantDetails.doYouKnowTheDefendantNameQuestion, defendantData.nameOption);
    if (defendantData.nameOption === defendantDetails.yesRadioOption) {
      await collectCYAData('addDefendantDetails', 'Defendant\'s name', `${defendantData.firstName} ${defendantData.lastName}`);
    }
    await collectCYAData('addDefendantDetails', defendantDetails.defendantCorrespondenceAddressQuestion, defendantData.correspondenceAddressOption);
    if (defendantData.correspondenceAddressOption === defendantDetails.yesRadioOption) {
      await collectCYAData('addDefendantDetails', defendantDetails.isCorrespondenceAddressSameQuestion, defendantData.correspondenceAddressSameOption);
    }
    await collectCYAData('addDefendantDetails', defendantDetails.additionalDefendantsQuestion, defendantData.addAdditionalDefendantsOption);
    await performAction('clickButton', defendantDetails.continueButton);
  }

  private async selectRentArrearsPossessionGround(rentArrearsPossession: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('check', rentArrearsPossession.rentArrears);
    await performAction('clickRadioButton', rentArrearsPossession.otherGrounds);
    // Collect CYA data
    const rentArrearsArray = rentArrearsPossession.rentArrears as string[];
    await collectCYAData('selectRentArrearsPossessionGround', 'What are your grounds for possession?', rentArrearsArray.join(', '));
    await performAction('clickButton', rentArrearsPossessionGrounds.continue);
  }

  private async selectTenancyOrLicenceDetails(tenancyData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
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
        await performAction('uploadFile', tenancyData.files);
    }
    // Collect CYA data
    await collectCYAData('selectTenancyOrLicenceDetails', 'What type of tenancy or licence is in place?', tenancyData.tenancyOrLicenceType);
    if (tenancyData.tenancyOrLicenceType === tenancyLicenceDetails.other) {
      await collectCYAData('selectTenancyOrLicenceDetails', tenancyLicenceDetails.giveDetailsOfTypeOfTenancyOrLicenceAgreement, tenancyLicenceDetails.detailsOfLicence);
    }
    if (tenancyData.day && tenancyData.month && tenancyData.year) {
      const dateStr = `${tenancyData.day}/${tenancyData.month}/${tenancyData.year}`;
      await collectCYAData('selectTenancyOrLicenceDetails', 'When did the tenancy or licence start?', dateStr);
    }
    if (tenancyData.files) {
      const filesArray = tenancyData.files as string[];
      await collectCYAData('selectTenancyOrLicenceDetails', 'Upload documents', filesArray.map(f => f.split('/').pop() || f).join(', '));
    }
    await performAction('clickButton', tenancyLicenceDetails.continue);
  }

  private async selectYourPossessionGrounds(possessionGrounds: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if (!possessionGrounds) {
      await performAction('clickButton', whatAreYourGroundsForPossession.continueButton);
      return;
    }
    // Collect CYA data
    for (const key of Object.keys(possessionGrounds)) {
      switch (key) {
        case 'discretionary':
          await performAction('check', possessionGrounds.discretionary);
          const discretionaryArray = possessionGrounds.discretionary as string[];
          await collectCYAData('selectYourPossessionGrounds', 'What are your grounds for possession?', discretionaryArray.join(', '));
          if (
            discretionaryArray.includes(
              whatAreYourGroundsForPossessionWales.discretionary.estateManagementGrounds
            )
          ) {
            await performAction('check', possessionGrounds.discretionaryEstateGrounds);
          }
          break;
        case 'mandatory':
          await performAction('check', possessionGrounds.mandatory);
          const mandatoryArray = possessionGrounds.mandatory as string[];
          await collectCYAData('selectYourPossessionGrounds', 'What are your grounds for possession?', mandatoryArray.join(', '));
          break;
        case 'mandatoryAccommodation':
          await performAction('check', possessionGrounds.mandatoryAccommodation);
          const mandatoryAccommodationArray = possessionGrounds.mandatoryAccommodation as string[];
          await collectCYAData('selectYourPossessionGrounds', 'What are your grounds for possession?', mandatoryAccommodationArray.join(', '));
          break;
        case 'discretionaryAccommodation':
          await performAction('check', possessionGrounds.discretionaryAccommodation);
          const discretionaryAccommodationArray = possessionGrounds.discretionaryAccommodation as string[];
          await collectCYAData('selectYourPossessionGrounds', 'What are your grounds for possession?', discretionaryAccommodationArray.join(', '));
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
      await performAction('inputText',  {text:reasons[n],index: n}, reasonsForPossession.detailsAboutYourReason);
    }
    await performAction('clickButton', reasonsForPossession.continue);
  }

  private async selectMediationAndSettlement(mediationSettlement: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
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
    // Collect CYA data
    await collectCYAData('selectMediationAndSettlement', mediationAndSettlement.attemptedMediationWithDefendants, mediationSettlement.attemptedMediationWithDefendantsOption);
    if (mediationSettlement.attemptedMediationWithDefendantsOption == mediationAndSettlement.yes) {
      await collectCYAData('selectMediationAndSettlement', mediationAndSettlement.attemptedMediationTextAreaLabel, mediationAndSettlement.attemptedMediationInputData);
    }
    await collectCYAData('selectMediationAndSettlement', mediationAndSettlement.settlementWithDefendants, mediationSettlement.settlementWithDefendantsOption);
    if (mediationSettlement.settlementWithDefendantsOption == mediationAndSettlement.yes) {
      await collectCYAData('selectMediationAndSettlement', mediationAndSettlement.settlementWithDefendantsTextAreaLabel, mediationAndSettlement.settlementWithDefendantsInputData);
    }
    await performAction('clickButton', mediationAndSettlement.continue);
  }

  private async selectNoticeDetails(noticeData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
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
    // Collect CYA data
    await collectCYAData('selectNoticeDetails', 'How did you serve notice?', noticeData.howDidYouServeNotice);
    if (noticeData.day && noticeData.month && noticeData.year) {
      const dateStr = `${noticeData.day}/${noticeData.month}/${noticeData.year}`;
      await collectCYAData('selectNoticeDetails', 'When did you serve notice?', dateStr);
    }
    if (noticeData.hour && noticeData.minute) {
      const timeStr = `${noticeData.hour}:${noticeData.minute}`;
      await collectCYAData('selectNoticeDetails', 'What time did you serve notice?', timeStr);
    }
    if (noticeData.explanation) {
      await collectCYAData('selectNoticeDetails', noticeData.explanationLabel as string, noticeData.explanation);
    }
    if (noticeData.files) {
      const fileName = (noticeData.files as string).split('/').pop() || noticeData.files as string;
      await collectCYAData('selectNoticeDetails', 'Upload documents', fileName);
    }
    await performAction('clickButton', noticeDetails.continue);
  }

  private async provideRentDetails(rentFrequency: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('inputText', rentDetails.HowMuchRentLabel, rentFrequency.rentAmount);
    await performAction('clickRadioButton', rentFrequency.rentFrequencyOption);
    if(rentFrequency.rentFrequencyOption == rentDetails.other){
      await performAction('inputText', rentDetails.rentFrequencyLabel, rentFrequency.inputFrequency);
      await performAction('inputText', rentDetails.amountPerDayInputLabel, rentFrequency.unpaidRentAmountPerDay);
    }
    // Collect CYA data
    await collectCYAData('provideRentDetails', rentDetails.HowMuchRentLabel, rentFrequency.rentAmount);
    await collectCYAData('provideRentDetails', 'How often is rent paid?', rentFrequency.rentFrequencyOption);
    if(rentFrequency.rentFrequencyOption == rentDetails.other){
      await collectCYAData('provideRentDetails', rentDetails.amountPerDayInputLabel, rentFrequency.unpaidRentAmountPerDay);
    }
    await performAction('clickButton', rentDetails.continue);
  }

  private async selectDailyRentAmount(dailyRentAmountData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performValidation('text', {
      text: dailyRentAmount.basedOnPreviousAnswers + `${dailyRentAmountData.calculateRentAmount}`,
      elementType: 'paragraph'
    });
    await performAction('clickRadioButton', dailyRentAmountData.unpaidRentInteractiveOption);
    if(dailyRentAmountData.unpaidRentInteractiveOption == dailyRentAmount.no){
      await performAction('inputText', dailyRentAmount.enterAmountPerDayLabel, dailyRentAmountData.unpaidRentAmountPerDay);
    }
    // Collect CYA data
    await collectCYAData('selectDailyRentAmount', 'What is the daily rent amount?', dailyRentAmountData.calculateRentAmount);
    if(dailyRentAmountData.unpaidRentInteractiveOption == dailyRentAmount.no){
      await collectCYAData('selectDailyRentAmount', dailyRentAmount.enterAmountPerDayLabel, dailyRentAmountData.unpaidRentAmountPerDay);
    }
    await performAction('clickButton', dailyRentAmount.continue);
  }

  private async selectClaimantCircumstances(claimantCircumstance: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
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
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
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
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', option);
    // Collect CYA data
    await collectCYAData('selectMoneyJudgment', moneyJudgment.mainHeader, option);
    await performAction('clickButton', moneyJudgment.continue);
  }

  private async selectClaimingCosts(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', option);
    // Collect CYA data
    await collectCYAData('selectClaimingCosts', claimingCosts.mainHeader, option);
    await performAction('clickButton', claimingCosts.continue);
  }

  private async selectAlternativesToPossession(alternatives: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if(alternatives){
      await performAction('check', {question: alternatives.question, option: alternatives.option});
    }
    await performAction('clickButton', alternativesToPossession.continue);
  }

  private async selectHousingAct(housingAct: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if(Array.isArray(housingAct)) {
      for (const act of housingAct) {
        await performAction('clickRadioButton', {question: act.question, option: act.option});
      }
    }
    await performAction('clickButton', alternativesToPossession.continue);
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
    await performAction('clickRadioButton', option);
    // Collect CYA data
    await collectCYAData('selectApplications', applications.mainHeader, option);
    await performAction('clickButton', applications.continue);
  }

  private async wantToUploadDocuments(documentsData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: documentsData.question,
      option: documentsData.option
    });
    await performAction('clickButton', uploadAdditionalDocs.continue);
  }

  private async uploadAdditionalDocs(documentsData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
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
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
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
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: languageDetails.question, option: languageDetails.option});
    await performAction('clickButton', languageUsed.continue);
  }

  private async selectDefendantCircumstances(
    defendantDetails: actionRecord
  ) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseNumber });
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', defendantDetails.defendantCircumstance);
    if (defendantDetails.defendantCircumstance == defendantCircumstances.yesRadioOption) {
      if (defendantDetails.additionalDefendants == true) {
        await performAction('inputText', defendantCircumstances.defendantCircumstancesPluralTextLabel, defendantCircumstances.defendantCircumstancesTextInput);
      } else {
        await performAction('inputText', defendantCircumstances.defendantCircumstancesSingularTextLabel, defendantCircumstances.defendantCircumstancesTextInput);
      }
    }
    await performAction('clickButton', defendantCircumstances.continueButton);
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
      addressLine2: await page.getByLabel(addressDetails.addressLine2TextLabel).inputValue(),
      addressLine3: await page.getByLabel(addressDetails.addressLine3TextLabel).inputValue(),
      townCity: await page.getByLabel(addressDetails.townOrCityTextLabel).inputValue(),
      engOrWalPostcode: await page.getByLabel(addressDetails.postcodeTextLabel).inputValue(),
      country: await page.getByLabel(addressDetails.countryTextLabel).inputValue(),
    };
    await performAction('clickButton', addressDetails.continueButton);
  }

  private async provideMoreDetailsOfClaim(page: Page) {
    // Reloading to reset session/UI state before performing next step
    await page.reload();
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
  }

  private async selectAdditionalReasonsForPossession(reasons: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', reasons);
    if(reasons == additionalReasonsForPossession.yes){
      await performAction('inputText', additionalReasonsForPossession.additionalReasonsForPossessionLabel, additionalReasonsForPossession.additionalReasonsForPossessionSampleText);
    }
    await performAction('clickButton', additionalReasonsForPossession.continue);
  }

  private async selectUnderlesseeOrMortgageeEntitledToClaim(underlesseeOrMortgageeEntitledToClaim: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: underlesseeOrMortgageeEntitledToClaim.question,
      option: underlesseeOrMortgageeEntitledToClaim.option
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
    await performAction('clickButton', home.findCaseTab);
    await performAction('select', search.jurisdictionLabel, search.possessionsJurisdiction);
    await performAction('select', search.caseTypeLabel, search.caseType.civilPossessions);
    await performAction('inputText', search.caseNumberLabel, caseNumber);
    await performAction('clickButton', search.apply);
    await performAction('clickButton', caseNumber);
  }
}
