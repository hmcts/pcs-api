import Axios from 'axios';
import {ServiceAuthUtils} from '@hmcts/playwright-common';
import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
import {createCase, addressDetails, housingPossessionClaim, defendantDetails, claimantName, contactPreferences, mediationAndSettlement, tenancyLicenceDetails, resumeClaimOptions, rentDetails, accessTokenApiData, caseApiData, dailyRentAmount, reasonsForPossession, detailsOfRentArrears,
        claimantType, claimType, groundsForPossession, preActionProtocol, noticeOfYourIntention, borderPostcode, rentArrearsPossessionGrounds, rentArrearsOrBreachOfTenancy, noticeDetails, moneyJudgment, whatAreYourGroundsForPossession, languageUsed, defendantCircumstances, applications, claimantCircumstances,
        claimingCosts, alternativesToPossession, reasonsForRequestingADemotionOrder, statementOfExpressTerms, reasonsForRequestingASuspensionOrder, uploadAdditionalDocs, additionalReasonsForPossession, completeYourClaim, home, search, userIneligible,
        whatAreYourGroundsForPossessionWales, underlesseeOrMortgageeDetails, reasonsForRequestingASuspensionAndDemotionOrder, provideMoreDetailsOfClaim, addressCheckYourAnswers, statementOfTruth} from "@data/page-data";

export let caseInfo: { id: string; fid: string; state: string };
export let caseNumber: string;
export let claimantsName: string;
export let addressInfo: { buildingStreet: string; townCity: string; engOrWalPostcode: string };

export class CreateCaseAction implements IAction {
  private page: Page | null = null;

  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    this.page = page;
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createCase', () => this.createCaseAction(fieldName)],
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
      townCity: await page.getByLabel(addressDetails.townOrCityTextLabel).inputValue(),
      engOrWalPostcode: address.postcode
    };
    // Auto-collect address data for address CYA page
    await getAutoCollector().collectAddressAnswer(this.page, addressInfo);
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
    // Auto-collect claimant type
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectClaimantType', caseData as string, 'Who is the claimant in this case?', claimantType);
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
    // Auto-collect grounds for possession
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectGroundsForPossession', possessionGrounds.groundsRadioInput as string, 'Are you claiming possession because of rent arrears or breach of the tenancy?', groundsForPossession);
    if (possessionGrounds.groundsRadioInput == groundsForPossession.yes) {
      if (possessionGrounds.grounds) {
        await performAction('check', possessionGrounds.grounds);
        // Auto-collect checkbox selections
        await getAutoCollector().collectCheckboxAnswer(this.page, 'selectGroundsForPossession', 'Grounds for possession', possessionGrounds.grounds as string[], groundsForPossession);
        if ((possessionGrounds.grounds as Array<string>).includes(groundsForPossession.other)) {
          await performAction('inputText', groundsForPossession.enterGroundsForPossessionLabel, groundsForPossession.enterYourGroundsForPossessionInput);
          await getAutoCollector().collectTextInputAnswer(this.page, 'selectGroundsForPossession', 'Enter your grounds for possession', groundsForPossession.enterYourGroundsForPossessionInput, groundsForPossession);
        }
      }
    }
    await performAction('clickButton', groundsForPossession.continue);
  }

  private async selectPreActionProtocol(caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', caseData);
    // Auto-collect pre-action protocol
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectPreActionProtocol', caseData as string, 'Have you followed the pre-action protocol?', preActionProtocol);
    await performAction('clickButton', preActionProtocol.continue);
  }

  private async selectNoticeOfYourIntention(caseData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', caseData);
    // Auto-collect notice data
    const questionText = (caseData.question as string) || 'Have you served notice to the defendants?' || noticeOfYourIntention.servedNoticeInteractiveText;
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectNoticeOfYourIntention', caseData, questionText, noticeOfYourIntention);
    if ( caseData.option === noticeOfYourIntention.yes && caseData.typeOfNotice) {
      await performAction('inputText', noticeOfYourIntention.typeOfNotice, noticeOfYourIntention.typeOfNoticeInput);
      await getAutoCollector().collectTextInputAnswer(this.page, 'selectNoticeOfYourIntention', 'What type of notice did you serve?', noticeOfYourIntention.typeOfNoticeInput, noticeOfYourIntention);
    }
    await performAction('clickButton', noticeOfYourIntention.continue);
  }

  private async selectBorderPostcode(option: actionData) {
    await performAction('clickRadioButton', option);
    // Auto-collect country/England-Wales selection for address CYA page
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectBorderPostcode', option as string, 'Is the property located in England or Wales?', borderPostcode);
    await performAction('clickButton', borderPostcode.continueButton);
  }

  private async selectClaimantName(page: Page, caseData: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', caseData);
    // Auto-collect claimant name answer
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectClaimantName', caseData as string, 'Is this the correct claimant name?', claimantName);
 if(caseData == claimantName.no){
      await performAction('inputText', claimantName.whatIsCorrectClaimantName, claimantName.correctClaimantNameInput);
      // Auto-collect conditional text input
      await getAutoCollector().collectTextInputAnswer(this.page, 'selectClaimantName', 'What is the correct claimant name?', claimantName.correctClaimantNameInput, claimantName);
    }
    claimantsName = caseData == "No" ? claimantName.correctClaimantNameInput : await this.extractClaimantName(page, claimantName.yourClaimantNameRegisteredWithHMCTS);
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
      question: contactPreferences.emailAddressForNotifications,
      option: preferences.notifications
    });
    // Auto-collect contact preferences
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectContactPreferences', {option: preferences.notifications}, 'Do you want to use this email address for notifications?', contactPreferences);
    if (preferences.notifications === contactPreferences.no) {
      await performAction('inputText', contactPreferences.enterEmailAddressLabel, contactPreferences.emailIdInput);
      // Auto-collect conditional email input
      await getAutoCollector().collectTextInputAnswer(this.page, 'selectContactPreferences', 'Enter email address', contactPreferences.emailIdInput, contactPreferences);
    }
    await performAction('clickRadioButton', {
      question: contactPreferences.doYouWantDocumentsToBeSentToAddress,
      option: preferences.correspondenceAddress
    });
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectContactPreferences', {option: preferences.correspondenceAddress}, 'Do you want documents to be sent to this address?', contactPreferences);
    if (preferences.correspondenceAddress === contactPreferences.no) {
      await performActions(
        'Find Address based on postcode',
        ['inputText', addressDetails.enterUKPostcodeTextLabel, addressDetails.englandCourtAssignedPostcodeTextInput],
        ['clickButton', addressDetails.findAddressButton],
        ['select', addressDetails.addressSelectLabel, addressDetails.addressIndex]
      );
      // Note: Address collection would happen in selectAddress action if called separately
    }
    if(prefData.phoneNumber) {
      await performAction('clickRadioButton', {
        question: contactPreferences.provideContactPhoneNumber,
        option: prefData.phoneNumber
      });
      await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectContactPreferences', {option: prefData.phoneNumber}, 'Do you want to provide a contact phone number?', contactPreferences);
      if (prefData.phoneNumber === contactPreferences.yes) {
        await performAction('inputText', contactPreferences.enterPhoneNumberLabel, contactPreferences.phoneNumberInput);
        // Auto-collect conditional phone number input
        await getAutoCollector().collectTextInputAnswer(this.page, 'selectContactPreferences', 'Enter phone number', contactPreferences.phoneNumberInput, contactPreferences);
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
    if (defendantData.nameOption === defendantDetails.yesRadioOption) {
      await performAction('inputText', defendantDetails.defendantFirstNameTextLabel, defendantData.firstName);
      await performAction('inputText', defendantDetails.defendantLastNameTextLabel, defendantData.lastName);
    }
    await performAction('clickRadioButton', {
      question: defendantDetails.defendantCorrespondenceAddressQuestion,
      option: defendantData.correspondenceAddressOption,
    });
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
    await performAction('clickButton', defendantDetails.continueButton);
  }

  private async selectRentArrearsPossessionGround(rentArrearsPossession: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('check', rentArrearsPossession.rentArrears);
    // Auto-collect rent arrears (checkboxes)
    const rentArrears = Array.isArray(rentArrearsPossession.rentArrears) ? rentArrearsPossession.rentArrears : [rentArrearsPossession.rentArrears as string];
    await getAutoCollector().collectCheckboxAnswer(this.page, 'selectRentArrearsPossessionGround', 'Rent arrears', rentArrears, rentArrearsPossessionGrounds);
    await performAction('clickRadioButton', rentArrearsPossession.otherGrounds);
    // Auto-collect other grounds
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectRentArrearsPossessionGround', rentArrearsPossession.otherGrounds as string, undefined, rentArrearsPossessionGrounds);
    await performAction('clickButton', rentArrearsPossessionGrounds.continue);
  }

  private async selectTenancyOrLicenceDetails(tenancyData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', tenancyData.tenancyOrLicenceType);
    // Auto-collect tenancy type
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectTenancyOrLicenceDetails', tenancyData.tenancyOrLicenceType as string, 'What type of tenancy or licence is in place?', tenancyLicenceDetails);
    if (tenancyData.tenancyOrLicenceType === tenancyLicenceDetails.other) {
      await performAction('inputText', tenancyLicenceDetails.giveDetailsOfTypeOfTenancyOrLicenceAgreement, tenancyLicenceDetails.detailsOfLicence);
      await getAutoCollector().collectTextInputAnswer(this.page, 'selectTenancyOrLicenceDetails', tenancyLicenceDetails.giveDetailsOfTypeOfTenancyOrLicenceAgreement, tenancyLicenceDetails.detailsOfLicence, tenancyLicenceDetails);
    }
    if (tenancyData.day && tenancyData.month && tenancyData.year) {
      await performActions(
        'Enter Date',
        ['inputText', tenancyLicenceDetails.dayLabel, tenancyData.day],
        ['inputText', tenancyLicenceDetails.monthLabel, tenancyData.month],
        ['inputText', tenancyLicenceDetails.yearLabel, tenancyData.year]);
      // Auto-collect date
      await getAutoCollector().collectDateAnswer(this.page, 'selectTenancyOrLicenceDetails', tenancyData.day as string | number, tenancyData.month as string | number, tenancyData.year as string | number, 'When did the tenancy begin?', tenancyLicenceDetails);
    }
    if (tenancyData.files) {
      const files = tenancyData.files as Array<string>;
      for (const file of files) {
        await performAction('clickButton', tenancyLicenceDetails.addNew);
        await performAction('uploadFile', file);
      }
      // Auto-collect file uploads
      await getAutoCollector().collectFileUploadAnswer(this.page, 'selectTenancyOrLicenceDetails', files, 'Uploaded documents', tenancyLicenceDetails);
    }
    await performAction('clickButton', tenancyLicenceDetails.continue);
  }

  private async selectYourPossessionGrounds(possessionGrounds: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if (!possessionGrounds) {
      await performAction('clickButton', whatAreYourGroundsForPossession.continue);
      return;
    }
    const allSelectedGrounds: string[] = [];
    for (const key of Object.keys(possessionGrounds)) {
      switch (key) {
        case 'discretionary':
          await performAction('check', possessionGrounds.discretionary);
          if (Array.isArray(possessionGrounds.discretionary)) {
            allSelectedGrounds.push(...possessionGrounds.discretionary);
          }
          if (
            (possessionGrounds.discretionary as Array<string>).includes(
              whatAreYourGroundsForPossessionWales.discretionary.estateManagementGrounds
            )
          ) {
            await performAction('check', possessionGrounds.discretionaryEstateGrounds);
            if (Array.isArray(possessionGrounds.discretionaryEstateGrounds)) {
              allSelectedGrounds.push(...possessionGrounds.discretionaryEstateGrounds);
            }
          }
          break;
        case 'mandatory':
          await performAction('check', possessionGrounds.mandatory);
          if (Array.isArray(possessionGrounds.mandatory)) {
            allSelectedGrounds.push(...possessionGrounds.mandatory);
          }
          break;
        case 'mandatoryAccommodation':
          await performAction('check', possessionGrounds.mandatoryAccommodation);
          if (Array.isArray(possessionGrounds.mandatoryAccommodation)) {
            allSelectedGrounds.push(...possessionGrounds.mandatoryAccommodation);
          }
          break;
        case 'discretionaryAccommodation':
          await performAction('check', possessionGrounds.discretionaryAccommodation);
          if (Array.isArray(possessionGrounds.discretionaryAccommodation)) {
            allSelectedGrounds.push(...possessionGrounds.discretionaryAccommodation);
          }
          break;
      }
    }
    // Auto-collect possession grounds (all selected options)
    if (allSelectedGrounds.length > 0) {
      await getAutoCollector().collectCheckboxAnswer(this.page, 'selectYourPossessionGrounds', 'What are your grounds for possession?', allSelectedGrounds, whatAreYourGroundsForPossession);
    }
    await performAction('clickButton', whatAreYourGroundsForPossession.continue);
  }

  private async selectRentArrearsOrBreachOfTenancy(grounds: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    const rentArrearsOrBreachOfTenancyGrounds = grounds as {
      rentArrearsOrBreach: string[];
    }
    await performAction('check', rentArrearsOrBreachOfTenancyGrounds.rentArrearsOrBreach);
    // Auto-collect rent arrears or breach (checkboxes)
    await getAutoCollector().collectCheckboxAnswer(this.page, 'selectRentArrearsOrBreachOfTenancy', 'Rent arrears or breach of tenancy', rentArrearsOrBreachOfTenancyGrounds.rentArrearsOrBreach, rentArrearsOrBreachOfTenancy);
    await performAction('clickButton', rentArrearsOrBreachOfTenancy.continue);
  }

  private async enterReasonForPossession(reasons: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if (!Array.isArray(reasons)) {
      throw new Error(`EnterReasonForPossession expected an array, but received ${typeof reasons}`);
    }
    const allReasons: string[] = [];
    for (let n = 0; n < reasons.length; n++) {
      await performAction('inputText',  {text:reasons[n],index: n}, reasonsForPossession.detailsAboutYourReason);
      allReasons.push(reasons[n] as string);
    }
    // Auto-collect reasons for possession (combine all reasons)
    if (allReasons.length > 0) {
      await getAutoCollector().collectTextInputAnswer(this.page, 'enterReasonForPossession', reasonsForPossession.detailsAboutYourReason, allReasons.join('; '), reasonsForPossession);
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
    // Auto-collect mediation data
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectMediationAndSettlement', {option: mediationSettlement.attemptedMediationWithDefendantsOption}, 'Have you attempted mediation with the defendants?', mediationAndSettlement);
    if (mediationSettlement.attemptedMediationWithDefendantsOption == mediationAndSettlement.yes) {
      await performAction('inputText', mediationAndSettlement.attemptedMediationTextAreaLabel, mediationAndSettlement.attemptedMediationInputData);
      // Auto-collect conditional text input
      await getAutoCollector().collectTextInputAnswer(this.page, 'selectMediationAndSettlement', 'Give details about the attempted mediation and what the outcome was', mediationAndSettlement.attemptedMediationInputData, mediationAndSettlement);
    }
    await performAction('clickRadioButton', {
      question: mediationAndSettlement.settlementWithDefendants,
      option: mediationSettlement.settlementWithDefendantsOption
    });
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectMediationAndSettlement', {option: mediationSettlement.settlementWithDefendantsOption}, 'Have you tried to reach a settlement with the defendants?', mediationAndSettlement);
    if (mediationSettlement.settlementWithDefendantsOption == mediationAndSettlement.yes) {
      await performAction('inputText', mediationAndSettlement.settlementWithDefendantsTextAreaLabel, mediationAndSettlement.settlementWithDefendantsInputData);
      // Auto-collect conditional text input
      await getAutoCollector().collectTextInputAnswer(this.page, 'selectMediationAndSettlement', 'Explain what steps you\'ve taken to reach a settlement', mediationAndSettlement.settlementWithDefendantsInputData, mediationAndSettlement);
    }
    await performAction('clickButton', mediationAndSettlement.continue);
  }

  private async selectNoticeDetails(noticeData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', noticeData.howDidYouServeNotice);
    // Auto-collect how notice was served
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectNoticeDetails', noticeData.howDidYouServeNotice as string, 'How did you serve the notice?', noticeDetails);
    if (noticeData.explanationLabel && noticeData.explanation) {
      await performAction('inputText', noticeData.explanationLabel, noticeData.explanation);
      await getAutoCollector().collectTextInputAnswer(this.page, 'selectNoticeDetails', 'Explain how it was served', noticeData.explanation as string, noticeDetails);
    }
    if (noticeData.day && noticeData.month && noticeData.year) {
      await performActions('Enter Date',
        ['inputText', noticeDetails.dayLabel, noticeData.day],
        ['inputText', noticeDetails.monthLabel, noticeData.month],
        ['inputText', noticeDetails.yearLabel, noticeData.year]);
      // Auto-collect date
      await getAutoCollector().collectDateAnswer(this.page, 'selectNoticeDetails', noticeData.day as string | number, noticeData.month as string | number, noticeData.year as string | number, 'Date notice was served', noticeDetails);
    }
    if (noticeData.hour && noticeData.minute && noticeData.second) {
      await performActions('Enter Time',
        ['inputText', noticeDetails.hourLabel, noticeData.hour],
        ['inputText', noticeDetails.minuteLabel, noticeData.minute],
        ['inputText', noticeDetails.secondLabel, noticeData.second]);
      // Auto-collect time
      await getAutoCollector().collectTimeAnswer(this.page, 'selectNoticeDetails', noticeData.hour as string | number, noticeData.minute as string | number, noticeData.second as string | number, 'Time notice was served', noticeDetails);
    }
    if (noticeData.files) {
      await performAction('uploadFile', noticeData.files);
      const files = Array.isArray(noticeData.files) ? noticeData.files : [noticeData.files as string];
      await getAutoCollector().collectFileUploadAnswer(this.page, 'selectNoticeDetails', files, 'Uploaded notice documents', noticeDetails);
    }
    await performAction('clickButton', noticeDetails.continue);
  }

  private async provideRentDetails(rentFrequency: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('inputText', rentDetails.HowMuchRentLabel, rentFrequency.rentAmount);
    // Auto-collect rent amount
    await getAutoCollector().collectTextInputAnswer(this.page, 'provideRentDetails', 'How much is the rent?', rentFrequency.rentAmount as string, rentDetails);
    await performAction('clickRadioButton', rentFrequency.rentFrequencyOption);
    // Auto-collect rent frequency
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'provideRentDetails', rentFrequency.rentFrequencyOption as string, 'Enter frequency', rentDetails);
    if(rentFrequency.rentFrequencyOption == rentDetails.other){
      await performAction('inputText', rentDetails.rentFrequencyLabel, rentFrequency.inputFrequency);
      await getAutoCollector().collectTextInputAnswer(this.page, 'provideRentDetails', rentDetails.rentFrequencyLabel, rentFrequency.inputFrequency as string, rentDetails);
      await performAction('inputText', rentDetails.amountPerDayInputLabel, rentFrequency.unpaidRentAmountPerDay);
      await getAutoCollector().collectTextInputAnswer(this.page, 'provideRentDetails', 'Enter the amount per day that unpaid rent should be charged at', rentFrequency.unpaidRentAmountPerDay as string, rentDetails);
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
    // Auto-collect daily rent amount option
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectDailyRentAmount', dailyRentAmountData.unpaidRentInteractiveOption as string, 'Is the amount per day correct?', dailyRentAmount);
    if(dailyRentAmountData.unpaidRentInteractiveOption == dailyRentAmount.no){
      await performAction('inputText', dailyRentAmount.enterAmountPerDayLabel, dailyRentAmountData.unpaidRentAmountPerDay);
      // Auto-collect conditional text input
      await getAutoCollector().collectTextInputAnswer(this.page, 'selectDailyRentAmount', 'Enter amount per day that unpaid rent should be charged at', dailyRentAmountData.unpaidRentAmountPerDay as string, dailyRentAmount);
    }
    await performAction('clickButton', dailyRentAmount.continue);
  }

  private async selectClaimantCircumstances(claimantCircumstance: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    //As discussed with pod1 team, part of HDPI-2011, Below steps will be enabled back when dynamic organisation name handled in new ticket on claimant circumstances page.
    //const nameClaimant = claimantsName.substring(claimantsName.length - 1) == 's' ? `${claimantsName}'` : `${claimantsName}'s`;
    const claimOption = claimantCircumstance.circumstanceOption;
    // Auto-collect claimant circumstances
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectClaimantCircumstances', claimOption as string, 'Is there any information you\'d like to provide about Claimants circumstances?', claimantCircumstances);
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
      // Auto-collect conditional text input
      await getAutoCollector().collectTextInputAnswer(this.page, 'selectClaimantCircumstances', 'Give details about circumstances', claimantCircumstance.claimantInput as string, claimantCircumstances);
    }
    await performAction('clickButton', claimantCircumstances.continue);
  }

  private async provideDetailsOfRentArrears(rentArrears: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('uploadFile', rentArrears.files);
    // Auto-collect file upload - uses cyaQuestions from pageData
    const files = Array.isArray(rentArrears.files) ? rentArrears.files : [rentArrears.files as string];
    await getAutoCollector().collectFileUploadAnswer(this.page, 'provideDetailsOfRentArrears', files, 'Uploaded rent statement', detailsOfRentArrears);
    await performAction('inputText', detailsOfRentArrears.totalRentArrearsLabel, rentArrears.rentArrearsAmountOnStatement);
    // Auto-collect rent arrears amount
    await getAutoCollector().collectTextInputAnswer(this.page, 'provideDetailsOfRentArrears', 'Total rent arrears', rentArrears.rentArrearsAmountOnStatement as string, detailsOfRentArrears);
    await performAction('clickRadioButton', {
      question: detailsOfRentArrears.periodShownOnRentStatementLabel,
      option: rentArrears.rentPaidByOthersOption
    });
    // Auto-collect period shown on statement
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'provideDetailsOfRentArrears', {option: rentArrears.rentPaidByOthersOption}, 'For the period shown on the rent statement, have any rent payments been paid by someone other than the defendants?', detailsOfRentArrears);
    if (rentArrears.rentPaidByOthersOption == detailsOfRentArrears.yes) {
      await performAction('check', rentArrears.paymentOptions);
      // Auto-collect payment options (checkboxes)
      if (Array.isArray(rentArrears.paymentOptions)) {
        const paymentOptions: string[] = rentArrears.paymentOptions.map(opt => String(opt));
        await getAutoCollector().collectCheckboxAnswer(this.page, 'provideDetailsOfRentArrears', 'Payment options', paymentOptions, detailsOfRentArrears);
      }
      if ((rentArrears.paymentOptions as Array<string>).includes(detailsOfRentArrears.other)) {
        await performAction('inputText', detailsOfRentArrears.paymentSourceLabel, detailsOfRentArrears.paymentOptionOtherInput);
        await getAutoCollector().collectTextInputAnswer(this.page, 'provideDetailsOfRentArrears', 'Payment source', detailsOfRentArrears.paymentOptionOtherInput, detailsOfRentArrears);
      }
      await performAction('clickButton', detailsOfRentArrears.continue);
    }
  }

  private async selectMoneyJudgment(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', option);
    // Auto-collect money judgment
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectMoneyJudgment', option as string, 'Are you claiming a money judgment?', moneyJudgment);
    await performAction('clickButton', moneyJudgment.continue);
  }

  private async selectClaimingCosts(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', option);
    // Auto-collect claiming costs
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectClaimingCosts', option as string, 'Are you claiming costs?', claimingCosts);
    await performAction('clickButton', claimingCosts.continue);
  }

  private async selectAlternativesToPossession(alternatives: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if(alternatives){
      await performAction('check', {question: alternatives.question, option: alternatives.option});
      // Auto-collect alternatives (checkboxes)
      const options = Array.isArray(alternatives.option) ? alternatives.option : [alternatives.option as string];
      await getAutoCollector().collectCheckboxAnswer(this.page, 'selectAlternativesToPossession', 'In the alternative to possession, would you like to claim suspension of right to buy or demotion of tenancy?', options, alternativesToPossession);
    }
    await performAction('clickButton', alternativesToPossession.continue);
  }

  private async selectHousingAct(housingAct: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    if(Array.isArray(housingAct)) {
      for (const act of housingAct) {
        await performAction('clickRadioButton', {question: act.question, option: act.option});
        // Auto-collect each housing act question
        await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectHousingAct', {option: act.option}, act.question, alternativesToPossession);
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
    // Auto-collect statement of express terms
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectStatementOfExpressTerms', option as string, statementOfExpressTerms.statementOfExpressTermsQuestion, statementOfExpressTerms);
    if(option == statementOfExpressTerms.yes){
      await performAction('inputText', statementOfExpressTerms.giveDetailsOfTermsLabel, statementOfExpressTerms.sampleTestReason);
      // Auto-collect conditional text input
      await getAutoCollector().collectTextInputAnswer(this.page, 'selectStatementOfExpressTerms', statementOfExpressTerms.giveDetailsOfTermsLabel, statementOfExpressTerms.sampleTestReason, statementOfExpressTerms);
    }
    await performAction('clickButton', statementOfExpressTerms.continue);
  }

  private async enterReasonForDemotionOrder(reason: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('inputText', reason, reasonsForRequestingADemotionOrder.sampleTestReason);
    // Auto-collect reason for demotion order
    await getAutoCollector().collectTextInputAnswer(this.page, 'enterReasonForDemotionOrder', reason as string, reasonsForRequestingADemotionOrder.sampleTestReason, reasonsForRequestingADemotionOrder);
    await performAction('clickButton', reasonsForRequestingADemotionOrder.continue);
  }

  private async enterReasonForSuspensionOrder(reason: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('inputText', reason, reasonsForRequestingASuspensionOrder.sampleTestReason);
    // Auto-collect reason for suspension order
    await getAutoCollector().collectTextInputAnswer(this.page, 'enterReasonForSuspensionOrder', reason as string, reasonsForRequestingASuspensionOrder.sampleTestReason, reasonsForRequestingASuspensionOrder);
    await performAction('clickButton', reasonsForRequestingASuspensionOrder.continue);
  }

  private async enterReasonForSuspensionAndDemotionOrder(reason: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('inputText', reason.suspension, reasonsForRequestingASuspensionOrder.sampleTestReason);
    // Auto-collect suspension reason
    await getAutoCollector().collectTextInputAnswer(this.page, 'enterReasonForSuspensionAndDemotionOrder', reason.suspension as string, reasonsForRequestingASuspensionOrder.sampleTestReason, reasonsForRequestingASuspensionOrder);
    await performAction('inputText', reason.demotion, reasonsForRequestingADemotionOrder.sampleTestReason);
    // Auto-collect demotion reason
    await getAutoCollector().collectTextInputAnswer(this.page, 'enterReasonForSuspensionAndDemotionOrder', reason.demotion as string, reasonsForRequestingADemotionOrder.sampleTestReason, reasonsForRequestingADemotionOrder);
    await performAction('clickButton', reasonsForRequestingASuspensionAndDemotionOrder.continue);
  }

  private async selectApplications(option: actionData) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', option);
    // Auto-collect applications data
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectApplications', option as string, 'Are you making any applications?', applications);
    await performAction('clickButton', applications.continue);
  }

  private async wantToUploadDocuments(documentsData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: documentsData.question,
      option: documentsData.option
    });
    // Auto-collect upload documents data
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'wantToUploadDocuments', documentsData, 'Do you want to upload any additional documents?', wantToUploadDocuments);
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
    // Auto-collect completion option
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'completingYourClaim', option as string, 'How would you like to complete your claim?', completeYourClaim);
    await performAction('clickButton', completeYourClaim.continue);
  }

  private async selectJurisdictionCaseTypeEvent() {
    await performActions('Case option selection'
      , ['select', createCase.jurisdictionLabel, createCase.possessionsJurisdiction]
      , ['select', createCase.caseTypeLabel, createCase.caseType.civilPossessions]
      , ['select', createCase.eventLabel, createCase.makeAPossessionClaimEvent]);
    // Auto-collect jurisdiction, case type, and event selections
    await getAutoCollector().collectSelectAnswer(this.page, 'selectJurisdictionCaseTypeEvent', createCase.jurisdictionLabel, createCase.possessionsJurisdiction, createCase);
    await getAutoCollector().collectSelectAnswer(this.page, 'selectJurisdictionCaseTypeEvent', createCase.caseTypeLabel, createCase.caseType.civilPossessions, createCase);
    await getAutoCollector().collectSelectAnswer(this.page, 'selectJurisdictionCaseTypeEvent', createCase.eventLabel, createCase.makeAPossessionClaimEvent, createCase);
    await performAction('clickButton', createCase.start);
  }

  private async selectLanguageUsed(languageDetails: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: languageDetails.question, option: languageDetails.option});
    // Auto-collect language used data
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectLanguageUsed', languageDetails, 'Which language did you use to complete this service?', languageUsed);
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
      townCity: await page.getByLabel(addressDetails.townOrCityTextLabel).inputValue(),
      engOrWalPostcode: address.postcode.toString(),
    };
    // Auto-collect manually entered address
    await getAutoCollector().collectAddressAnswer(this.page, addressInfo);
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
    // Auto-collect additional reasons
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectAdditionalReasonsForPossession', reasons as string, 'Is there any other information you\'d like to provide about your reasons for possession?', additionalReasonsForPossession);
    if(reasons == additionalReasonsForPossession.yes){
      await performAction('inputText', additionalReasonsForPossession.additionalReasonsForPossessionLabel, additionalReasonsForPossession.additionalReasonsForPossessionSampleText);
      // Auto-collect conditional text input
      await getAutoCollector().collectTextInputAnswer(this.page, 'selectAdditionalReasonsForPossession', 'Additional reasons for possession', additionalReasonsForPossession.additionalReasonsForPossessionSampleText, additionalReasonsForPossession);
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
    // Auto-collect underlessee data
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectUnderlesseeOrMortgageeEntitledToClaim', underlesseeOrMortgageeEntitledToClaim, 'Is there an underlessee or mortgagee entitled to claim relief against forfeiture?', underlesseeOrMortgageeEntitledToClaim);
    await performAction('clickButton', underlesseeOrMortgageeDetails.continueButton);
  }

  private async selectUnderlesseeOrMortgageeDetails(underlesseeOrMortgageeDetail: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: underlesseeOrMortgageeDetails.doYouKnowTheNameQuestion,
      option: underlesseeOrMortgageeDetail.nameOption
    });
    // Auto-collect name question
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectUnderlesseeOrMortgageeDetails', {option: underlesseeOrMortgageeDetail.nameOption}, underlesseeOrMortgageeDetail.nameQuestion as string, underlesseeOrMortgageeDetails);
    if (underlesseeOrMortgageeDetail.nameOption === underlesseeOrMortgageeDetails.yesRadioOption) {
      await performAction('inputText', underlesseeOrMortgageeDetails.doYouKnowTheNameTextLabel, underlesseeOrMortgageeDetail.name);
    }
    await performAction('clickRadioButton', {
      question: underlesseeOrMortgageeDetails.doYouKnowTheAddressQuestion,
      option: underlesseeOrMortgageeDetail.addressOption
    });
    // Auto-collect address question
    await getAutoCollector().collectRadioButtonAnswer(this.page, 'selectUnderlesseeOrMortgageeDetails', {option: underlesseeOrMortgageeDetail.addressOption}, underlesseeOrMortgageeDetail.addressQuestion as string, underlesseeOrMortgageeDetails);
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
