import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
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
      await performAction('collectCYAAddressData', {actionName: 'selectAddress', question: propertyDetails.buildingAndStreetLabel, answer: addressInfo.buildingStreet});
    }
    if (addressInfo.addressLine2) {
      await performAction('collectCYAAddressData', {actionName: 'selectAddress', question: propertyDetails.addressLine2Label, answer: addressInfo.addressLine2});
    }
    if (addressInfo.addressLine3) {
      await performAction('collectCYAAddressData', {actionName: 'selectAddress', question: propertyDetails.addressLine3Label, answer: addressInfo.addressLine3});
    }
    if (addressInfo.townCity) {
      await performAction('collectCYAAddressData', {actionName: 'selectAddress', question: propertyDetails.townOrCityLabel, answer: addressInfo.townCity});
    }
    if (addressInfo.country) {
      await performAction('collectCYAAddressData', {actionName: 'selectAddress', question: propertyDetails.countryLabel, answer: addressInfo.country});
    }
    if (addressInfo.engOrWalPostcode) {
      await performAction('collectCYAAddressData', {actionName: 'selectAddress', question: propertyDetails.postcodeLabel, answer: addressInfo.engOrWalPostcode});
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
    // Collect CYA data - caseData is already the displayed answer text
    await performAction('collectCYAData', {actionName: 'selectClaimantType', question: claimantType.whoIsTheClaimantQuestion, answer: caseData});
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
    // Collect CYA data - caseData is already the displayed answer text
    await performAction('collectCYAData', {actionName: 'selectClaimType', question: claimType.isThisAClaimAgainstTrespassersQuestion, answer: caseData});
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
    // Collect CYA data - groundsRadioInput is already the displayed answer text
    await performAction('collectCYAData', {actionName: 'selectGroundsForPossession', question: groundsForPossession.areYouClaimingPossessionBecauseOfRentArrearsQuestion, answer: possessionGrounds.groundsRadioInput});
    if (possessionGrounds.groundsRadioInput == groundsForPossession.yes) {
      if (possessionGrounds.grounds) {
        await performAction('check', possessionGrounds.grounds);
        const groundsArray = possessionGrounds.grounds as string[];
        await performAction('collectCYAData', {actionName: 'selectGroundsForPossession', question: 'What are your grounds for possession?', answer: groundsArray.join(', ')});
        if (groundsArray.includes(groundsForPossession.other)) {
          await performAction('inputText', groundsForPossession.enterGroundsForPossessionLabel, groundsForPossession.enterYourGroundsForPossessionInput);
          await performAction('collectCYAData', {actionName: 'selectGroundsForPossession', question: groundsForPossession.enterGroundsForPossessionLabel, answer: groundsForPossession.enterYourGroundsForPossessionInput});
        }
      }
    }
    await performAction('clickButton', groundsForPossession.continue);
  }

  private async selectPreActionProtocol(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', caseData);
    // Collect CYA data - caseData is already the displayed answer text
    await performAction('collectCYAData', {actionName: 'selectPreActionProtocol', question: preActionProtocol.haveYouFollowedPreActionProtocolQuestion, answer: caseData});
    await performAction('clickButton', preActionProtocol.continue);
  }

  private async selectNoticeOfYourIntention(caseData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', caseData);
    // Collect CYA data - caseData.option is already the displayed answer text
    await performAction('collectCYAData', {actionName: 'selectNoticeOfYourIntention', question: caseData.question, answer: caseData.option});
    if ( caseData.option === noticeOfYourIntention.yes && caseData.typeOfNotice) {
      await performAction('inputText', noticeOfYourIntention.typeOfNotice, noticeOfYourIntention.typeOfNoticeInput);
      await performAction('collectCYAData', {actionName: 'selectNoticeOfYourIntention', question: noticeOfYourIntention.typeOfNotice, answer: noticeOfYourIntention.typeOfNoticeInput});
    }
    await performAction('clickButton', noticeOfYourIntention.continue);
  }

  private async selectBorderPostcode(option: actionData) {
    await performAction('clickRadioButton', option);
    // Collect CYA data (Address CYA - this question appears on Address CYA page)
    await performAction('collectCYAAddressData', {actionName: 'selectBorderPostcode', question: borderPostcode.englandWalesInlineContent, answer: option});
    await performAction('clickButton', borderPostcode.continueButton);
  }

  private async selectClaimantName(page: Page, caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', caseData);
    // Collect CYA data - caseData is already the displayed answer text
    await performAction('collectCYAData', {actionName: 'selectClaimantName', question: claimantName.isThisTheCorrectClaimantNameQuestion, answer: caseData});
    if(caseData == claimantName.no){
      await performAction('inputText', claimantName.whatIsCorrectClaimantNameQuestion, claimantName.correctClaimantNameInput);
      await performAction('collectCYAData', {actionName: 'selectClaimantName', question: claimantName.whatIsCorrectClaimantNameQuestion, answer: claimantName.correctClaimantNameInput});
    }
    claimantsName = caseData == "No" ? claimantName.correctClaimantNameInput : await this.extractClaimantName(page, claimantName.yourClaimantNameRegisteredWithHMCTS);
  }

  private async selectContactPreferences(prefData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: contactPreferences.emailAddressForNotifications,
      option: prefData.notifications
    });
    // Collect CYA data - prefData.notifications is already the displayed answer text
    await performAction('collectCYAData', {actionName: 'selectContactPreferences', question: contactPreferences.emailAddressForNotifications, answer: prefData.notifications});
    if (prefData.notifications === contactPreferences.no) {
      await performAction('inputText', contactPreferences.enterEmailAddressLabel, contactPreferences.emailIdInput);
      await performAction('collectCYAData', {actionName: 'selectContactPreferences', question: contactPreferences.enterEmailAddressLabel, answer: contactPreferences.emailIdInput});
    }
    await performAction('clickRadioButton', {
      question: contactPreferences.doYouWantDocumentsToBeSentToAddress,
      option: prefData.correspondenceAddress
    });
    // Collect CYA data - prefData.correspondenceAddress is already the displayed answer text
    await performAction('collectCYAData', {actionName: 'selectContactPreferences', question: contactPreferences.doYouWantDocumentsToBeSentToAddress, answer: prefData.correspondenceAddress});
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
      // Collect CYA data - prefData.phoneNumber is already the displayed answer text
      await performAction('collectCYAData', {actionName: 'selectContactPreferences', question: contactPreferences.provideContactPhoneNumber, answer: prefData.phoneNumber});
      if (prefData.phoneNumber === contactPreferences.yes) {
        await performAction('inputText', contactPreferences.enterPhoneNumberLabel, contactPreferences.phoneNumberInput);
        await performAction('collectCYAData', {actionName: 'selectContactPreferences', question: contactPreferences.enterPhoneNumberLabel, answer: contactPreferences.phoneNumberInput});
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
    await performAction('collectCYAData', {actionName: 'selectContactPreferences', question: defendantDetails.doYouKnowTheDefendantNameQuestion, answer: defendantData.nameOption});
    if (defendantData.nameOption === defendantDetails.yesRadioOption) {
      await performAction('inputText', defendantDetails.defendantFirstNameTextLabel, defendantData.firstName);
      await performAction('inputText', defendantDetails.defendantLastNameTextLabel, defendantData.lastName);
    }
    await performAction('clickRadioButton', {
      question: defendantDetails.defendantCorrespondenceAddressQuestion,
      option: defendantData.correspondenceAddressOption,
    });
    await performAction('collectCYAData', {actionName: 'selectContactPreferences', question: defendantDetails.defendantCorrespondenceAddressQuestion, answer: defendantData.correspondenceAddressOption});
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
    await performAction('collectCYAData', {actionName: 'selectContactPreferences', question: defendantDetails.additionalDefendantsQuestion, answer: defendantData.addAdditionalDefendantsOption});
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
    await performAction('collectCYAData', {actionName: 'addDefendantDetails', question: defendantDetails.doYouKnowTheDefendantNameQuestion, answer: defendantData.nameOption});
    if (defendantData.nameOption === defendantDetails.yesRadioOption) {
      await performAction('collectCYAData', {actionName: 'addDefendantDetails', question: 'Defendant\'s name', answer: `${defendantData.firstName} ${defendantData.lastName}`});
    }
    await performAction('collectCYAData', {actionName: 'addDefendantDetails', question: defendantDetails.defendantCorrespondenceAddressQuestion, answer: defendantData.correspondenceAddressOption});
    if (defendantData.correspondenceAddressOption === defendantDetails.yesRadioOption) {
      await performAction('collectCYAData', {actionName: 'addDefendantDetails', question: defendantDetails.isCorrespondenceAddressSameQuestion, answer: defendantData.correspondenceAddressSameOption});
    }
    await performAction('collectCYAData', {actionName: 'addDefendantDetails', question: defendantDetails.additionalDefendantsQuestion, answer: defendantData.addAdditionalDefendantsOption});
    await performAction('clickButton', defendantDetails.continueButton);
  }

  private async selectRentArrearsPossessionGround(rentArrearsPossession: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('check', rentArrearsPossession.rentArrears);
    await performAction('clickRadioButton', rentArrearsPossession.otherGrounds);
    // Collect CYA data
    const rentArrearsArray = rentArrearsPossession.rentArrears as string[];
    await performAction('collectCYAData', {actionName: 'selectRentArrearsPossessionGround', question: 'What are your grounds for possession?', answer: rentArrearsArray.join(', ')});
    await performAction('clickButton', rentArrearsPossessionGrounds.continue);
  }

  private async selectTenancyOrLicenceDetails(tenancyData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', tenancyData.tenancyOrLicenceType);
    await performAction('collectCYAData', {actionName: 'selectTenancyOrLicenceDetails', question: tenancyLicenceDetails.tenancyOrLicenceType, answer: tenancyData.tenancyOrLicenceType});
    if (tenancyData.tenancyOrLicenceType === tenancyLicenceDetails.other) {
      await performAction('inputText', tenancyLicenceDetails.giveDetailsOfTypeOfTenancyOrLicenceAgreement, tenancyLicenceDetails.detailsOfLicence);
      await performAction('collectCYAData', {actionName: 'selectTenancyOrLicenceDetails', question: tenancyLicenceDetails.giveDetailsOfTypeOfTenancyOrLicenceAgreement, answer: tenancyLicenceDetails.detailsOfLicence});
    }
    if (tenancyData.day && tenancyData.month && tenancyData.year) {
      await performActions(
        'Enter Date',
        ['inputText', tenancyLicenceDetails.dayLabel, tenancyData.day],
        ['inputText', tenancyLicenceDetails.monthLabel, tenancyData.month],
        ['inputText', tenancyLicenceDetails.yearLabel, tenancyData.year]);
      const dateStr = `${tenancyData.day}/${tenancyData.month}/${tenancyData.year}`;
      await performAction('collectCYAData', {actionName: 'selectTenancyOrLicenceDetails', question: 'When did the tenancy or licence start?', answer: dateStr});
    }
    if (tenancyData.files) {
        await performAction('uploadFile', tenancyData.files);
        const filesArray = tenancyData.files as string[];
        await performAction('collectCYAData', {actionName: 'selectTenancyOrLicenceDetails', question: 'Upload documents', answer: filesArray.map(f => f.split('/').pop() || f).join(', ')});
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
          await performAction('collectCYAData', {actionName: 'selectYourPossessionGrounds', question: 'Discretionary grounds', answer: discretionaryArray.join(', ')});
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
          await performAction('collectCYAData', {actionName: 'selectYourPossessionGrounds', question: 'Mandatory grounds', answer: mandatoryArray.join(', ')});
          break;
        case 'mandatoryAccommodation':
          await performAction('check', possessionGrounds.mandatoryAccommodation);
          const mandatoryAccommodationArray = possessionGrounds.mandatoryAccommodation as string[];
          await performAction('collectCYAData', {actionName: 'selectYourPossessionGrounds', question: 'Mandatory grounds', answer: mandatoryAccommodationArray.join(', ')});
          break;
        case 'discretionaryAccommodation':
          await performAction('check', possessionGrounds.discretionaryAccommodation);
          const discretionaryAccommodationArray = possessionGrounds.discretionaryAccommodation as string[];
          await performAction('collectCYAData', {actionName: 'selectYourPossessionGrounds', question: 'What are your grounds for possession?', answer: discretionaryAccommodationArray.join(', ')});
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
      await performAction('collectCYAData', {actionName: 'selectMediationAndSettlement', question: reasonsForPossession.giveDetailsAboutYourReasonsForPossession + ' (' + reasons[n] + ')', answer: reasonsForPossession.detailsAboutYourReason});
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
    // Collect CYA data - attemptedMediationWithDefendantsOption is already the displayed answer text
    await performAction('collectCYAData', {actionName: 'selectMediationAndSettlement', question: mediationAndSettlement.attemptedMediationWithDefendants, answer: mediationSettlement.attemptedMediationWithDefendantsOption});
    if (mediationSettlement.attemptedMediationWithDefendantsOption == mediationAndSettlement.yes) {
      await performAction('inputText', mediationAndSettlement.attemptedMediationTextAreaLabel, mediationAndSettlement.attemptedMediationInputData);
      await performAction('collectCYAData', {actionName: 'selectMediationAndSettlement', question: mediationAndSettlement.attemptedMediationTextAreaLabel, answer: mediationAndSettlement.attemptedMediationInputData});
    }
    await performAction('clickRadioButton', {
      question: mediationAndSettlement.settlementWithDefendants,
      option: mediationSettlement.settlementWithDefendantsOption
    });
    // Collect CYA data - settlementWithDefendantsOption is already the displayed answer text
    await performAction('collectCYAData', {actionName: 'selectMediationAndSettlement', question: mediationAndSettlement.settlementWithDefendants, answer: mediationSettlement.settlementWithDefendantsOption});
    if (mediationSettlement.settlementWithDefendantsOption == mediationAndSettlement.yes) {
      await performAction('inputText', mediationAndSettlement.settlementWithDefendantsTextAreaLabel, mediationAndSettlement.settlementWithDefendantsInputData);
      await performAction('collectCYAData', {actionName: 'selectMediationAndSettlement', question: mediationAndSettlement.settlementWithDefendantsTextAreaLabel, answer: mediationAndSettlement.settlementWithDefendantsInputData});
    }
    await performAction('clickButton', mediationAndSettlement.continue);
  }

  private async selectNoticeDetails(noticeData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', noticeData.howDidYouServeNotice);
    await performAction('collectCYAData', {actionName: 'selectNoticeDetails', question: noticeDetails.howDidYouServeNotice, answer: noticeData.howDidYouServeNotice});
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
    if (noticeData.day && noticeData.month && noticeData.year) {
      const dateStr = `${noticeData.day}/${noticeData.month}/${noticeData.year}`;
      await performAction('collectCYAData', {actionName: 'selectNoticeDetails', question: 'When did you serve notice?', answer: dateStr});
    }
    if (noticeData.hour && noticeData.minute) {
      const timeStr = `${noticeData.hour}:${noticeData.minute}`;
      await performAction('collectCYAData', {actionName: 'selectNoticeDetails', question: 'What time did you serve notice?', answer: timeStr});
    }
    if (noticeData.explanation) {
      await performAction('collectCYAData', {actionName: 'selectNoticeDetails', question: noticeData.explanationLabel as string, answer: noticeData.explanation});
    }
    if (noticeData.files) {
      const fileName = (noticeData.files as string).split('/').pop() || noticeData.files as string;
      await performAction('collectCYAData', {actionName: 'selectNoticeDetails', question: 'Upload documents', answer: fileName});
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
    await performAction('collectCYAData', {actionName: 'provideRentDetails', question: rentDetails.HowMuchRentLabel, answer: rentFrequency.rentAmount});
    await performAction('collectCYAData', {actionName: 'provideRentDetails', question: 'How often is rent paid?', answer: rentFrequency.rentFrequencyOption});
    if(rentFrequency.rentFrequencyOption == rentDetails.other){
      await performAction('collectCYAData', {actionName: 'provideRentDetails', question: rentDetails.amountPerDayInputLabel, answer: rentFrequency.unpaidRentAmountPerDay});
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
    await performAction('collectCYAData', {actionName: 'selectDailyRentAmount', question: 'What is the daily rent amount?', answer: dailyRentAmountData.calculateRentAmount});
    if(dailyRentAmountData.unpaidRentInteractiveOption == dailyRentAmount.no){
      await performAction('collectCYAData', {actionName: 'selectDailyRentAmount', question: dailyRentAmount.enterAmountPerDayLabel, answer: dailyRentAmountData.unpaidRentAmountPerDay});
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
    await performAction('collectCYAData', {actionName: 'selectClaimantCircumstances', question: claimantCircumstances.isThereAnyInformationYouWouldLikeToProvideQuestion, answer: claimantCircumstance.circumstanceOption});
    if (claimOption == claimantCircumstances.yes) {
      //await performAction('inputText', claimantCircumstances.claimantCircumstanceInfoTextAreaLabel.replace("Claimants", nameClaimant), claimData.claimantInput);
      await performAction('inputText', claimantCircumstances.claimantCircumstanceInfoTextAreaLabel, claimantCircumstance.claimantInput);
      await performAction('collectCYAData', {actionName: 'selectClaimantCircumstances', question: claimantCircumstances.claimantCircumstanceInfoTextAreaLabel, answer: claimantCircumstance.claimantInput});
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
    await performAction('collectCYAData', {actionName: 'selectMoneyJudgment', question: moneyJudgment.mainHeader, answer: option});
    await performAction('clickButton', moneyJudgment.continue);
  }

  private async selectClaimingCosts(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', option);
    // Collect CYA data
    await performAction('collectCYAData', {actionName: 'selectClaimingCosts', question: claimingCosts.doYouWantToAskForYourCostsBack, answer: option});
    await performAction('clickButton', claimingCosts.continue);
  }

  private async selectAlternativesToPossession(alternatives: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if(alternatives){
      await performAction('check', {question: alternatives.question, option: alternatives.option});
      await performAction('selectAlternativesToPossession', alternatives.question, alternatives.option);
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
    await performAction('collectCYAData', {actionName: 'selectApplications', question: applications.areYouPlanningToMakeAnApplicationQuestion, answer: option});
    await performAction('clickButton', applications.continue);
  }

  private async wantToUploadDocuments(documentsData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: documentsData.question,
      option: documentsData.option
    });
    await performAction('collectCYAData', {actionName: 'wantToUploadDocuments', question: documentsData.question, answer: documentsData.option});
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
    await performAction('collectCYAData', {actionName: 'completingYourClaim', question: completeYourClaim.whatWouldYouLikeToDoNextQuestion, answer: option});
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
    await performAction('collectCYAData', {actionName: 'selectLanguageUsed', question: languageDetails.question, answer: languageDetails.option});
    await performAction('clickButton', languageUsed.continue);
  }

  private async selectDefendantCircumstances(defendantDetails: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseNumber });
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', defendantDetails.defendantCircumstance);
    await performAction('collectCYAData', {actionName: 'selectDefendantCircumstances', question: defendantCircumstances.defendantCircumstancesQuestion, answer: defendantDetails.defendantCircumstance});
    if (defendantDetails.defendantCircumstance == defendantCircumstances.yesRadioOption) {
      if (defendantDetails.additionalDefendants == true) {
        await performAction('inputText', defendantCircumstances.defendantCircumstancesPluralTextLabel, defendantCircumstances.defendantCircumstancesTextInput);
      } else {
        await performAction('inputText', defendantCircumstances.defendantCircumstancesSingularTextLabel, defendantCircumstances.defendantCircumstancesTextInput);
        await performAction('collectCYAData', {actionName: 'selectDefendantCircumstances', question: defendantCircumstances.defendantCircumstancesSingularTextLabel, answer: defendantCircumstances.defendantCircumstancesTextInput});
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
      engOrWalPostcode: await page.getByLabel(addressDetails.postcodeTextLabel, { exact: true }).inputValue(),
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
    await performAction('collectCYAData', {actionName: 'selectAdditionalReasonsForPossession', question: additionalReasonsForPossession.isThereAnyOtherInformationQuestion, answer: reasons})
    if(reasons == additionalReasonsForPossession.yes){
      await performAction('inputText', additionalReasonsForPossession.additionalReasonsForPossessionLabel, additionalReasonsForPossession.additionalReasonsForPossessionSampleText);
      await performAction('collectCYAData', {actionName: 'selectAdditionalReasonsForPossession', question: additionalReasonsForPossession.additionalReasonsForPossessionLabel, answer: additionalReasonsForPossession.additionalReasonsForPossessionSampleText});
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
    await performAction('collectCYAData', {actionName: 'selectUnderlesseeOrMortgageeEntitledToClaim', question: underlesseeOrMortgageeEntitledToClaim.question, answer: underlesseeOrMortgageeEntitledToClaim.option});
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
    await performAction('collectCYAData', {actionName: 'selectStatementOfTruth', question: statementOfTruth.completedByLabel, answer: claimantDetails.completedBy});
    if(claimantDetails.completedBy == statementOfTruth.claimantRadioOption){
      await performAction('check', claimantDetails.iBelieveCheckbox);
      await performAction('inputText', statementOfTruth.fullNameHiddenTextLabel, claimantDetails.fullNameTextInput);
      await performAction('inputText', statementOfTruth.positionOrOfficeHeldHiddenTextLabel, claimantDetails.positionOrOfficeTextInput);
      await performAction('collectCYAData', {actionName: 'selectStatementOfTruth', question: '', answer: claimantDetails.iBelieveCheckbox});
      await performAction('collectCYAData', {actionName: 'selectStatementOfTruth', question: statementOfTruth.fullNameHiddenTextLabel, answer: claimantDetails.fullNameTextInput});
      await performAction('collectCYAData', {actionName: 'selectStatementOfTruth', question: statementOfTruth.positionOrOfficeHeldHiddenTextLabel, answer: claimantDetails.positionOrOfficeTextInput});
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
