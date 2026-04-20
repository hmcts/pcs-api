import { test, expect, Page } from '@playwright/test';
import path from 'path';
import { performAction, performActions, performValidation } from '@utils/controller-enforcement';
import { IAction, actionRecord } from '@utils/interfaces/action.interface';
import {
  yourHCEO,
  evidenceUpload
} from '@data/page-data/page-data-enforcement';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { EnforcementCommonUtils } from '@utils/actions/element-actions/enforcementUtils.action';
import {
  propertyAccessDetails,
  livingInTheProperty,
  nameAndAddressForEviction,
  evictionRisksPosed,
  vulnerableAdultsChildren,
  enforcementApplication,
  moneyOwed,
  legalCosts,
  landRegistryFees,
  repayments,
  peopleWhoWillBeEvicted,
  changeNameAddress,
  languageUsed,
  peopleYouWantToEvict,
  confirmDefendantsDOB,
  knownDefendantsDOBInformation,
  suspendedOrder,
  statementOfTruth,
  confirmHCEOfficer
} from '@data/page-data-figma/page-data-enforcement-figma';

export const addressInfo = {
  buildingStreet: createCaseApiData.createCasePayload.propertyAddress.AddressLine1,
  addressLine2: createCaseApiData.createCasePayload.propertyAddress.AddressLine2,
  townCity: createCaseApiData.createCasePayload.propertyAddress.PostTown,
  engOrWalPostcode: createCaseApiData.createCasePayload.propertyAddress.PostCode
};

export let defendantDetails: string[] = [];
export const moneyMap = new Map<string, number>();
export const fieldsMap = new Map<string, string>();

export class EnforcementAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, data?: actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['validateWritOrWarrantFeeAmount', () => this.validateWritOrWarrantFeeAmount(fieldName as actionRecord)],
      ['validateGetQuoteFromBailiffLink', () => this.validateGetQuoteFromBailiffLink(fieldName as actionRecord)],
      ['selectApplicationType', () => this.selectApplicationType(fieldName as actionRecord, page)],
      ['confirmClaimTransferredToHighCourt', () => this.confirmClaimTransferredToHighCourt(fieldName as actionRecord, page)],
      ['selectHaveHiredHCEO', () => this.selectHaveHiredHCEO(fieldName as actionRecord, page)],
      ['nameYourHCEO', () => this.nameYourHCEO(fieldName as actionRecord, page)],
      ['selectNameAndAddressForEviction', () => this.selectNameAndAddressForEviction(fieldName as actionRecord, page)],
      ['confirmDefendantsDOB', () => this.confirmDefendantsDOB(fieldName as actionRecord, page)],
      ['enterDefendantsDOB', () => this.enterDefendantsDOB(page, fieldName as actionRecord)],
      ['selectEveryoneLivingAtTheProperty', () => this.selectEveryoneLivingAtTheProperty(fieldName as actionRecord, page)],
      ['selectPermissionFromJudge', () => this.selectPermissionFromJudge(page)],
      ['getDefendantDetails', () => this.getDefendantDetails(fieldName as actionRecord)],
      ['selectPeopleWhoWillBeEvicted', () => this.selectPeopleWhoWillBeEvicted(fieldName as actionRecord, page)],
      ['selectPeopleYouWantToEvict', () => this.selectPeopleYouWantToEvict(fieldName as actionRecord, page)],
      ['selectRiskPosedByEveryoneAtProperty', () => this.selectRiskPosedByEveryoneAtProperty(fieldName as actionRecord, page)],
      ['provideRiskPosedByEveryoneAtProperty', () => this.provideRiskPosedByEveryoneAtProperty(fieldName as actionRecord, page)],
      ['provideHowDefendantReturnToProperty', () => this.provideHowDefendantReturnToProperty(fieldName as actionRecord, page)],
      ['selectVulnerablePeopleInTheProperty', () => this.selectVulnerablePeopleInTheProperty(fieldName as actionRecord, page)],
      ['provideDetailsBasedOnRadioOptionSelection', () => this.provideDetailsBasedOnRadioOptionSelection(fieldName as actionRecord, page)],
      ['provideMoneyOwed', () => this.provideMoneyOwed(fieldName as actionRecord, page)],
      ['provideLegalCosts', () => this.provideLegalCosts(fieldName as actionRecord, page)],
      ['provideLandRegistryFees', () => this.provideLandRegistryFees(fieldName as actionRecord, page)],
      ['provideAmountToRePay', () => this.provideAmountToRePay(fieldName as actionRecord, page)],
      ['validateAmountToRePayTable', () => this.validateAmountToRePayTable(fieldName as actionRecord)],
      ['selectLanguageUsed', () => this.selectLanguageUsed(fieldName as actionRecord, page)],
      ['confirmSuspendedOrder', () => this.confirmSuspendedOrder(fieldName as actionRecord, page)],
      ['selectStatementOfTruth', () => this.selectStatementOfTruth(fieldName as actionRecord, page)],
      ['selectStatementOfTruthWrit', () => this.selectStatementOfTruthWrit(fieldName as actionRecord, page)],
      ['uploadEvidenceThatDefendantsAreAtProperty', () => this.uploadEvidenceThatDefendantsAreAtProperty(fieldName as actionRecord, page)],
      ['inputErrorValidation', () => this.inputErrorValidation(page, fieldName as actionRecord)],
      ['validatePrePopulatedData', () => this.validatePrePopulatedData(fieldName as actionRecord)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async validateWritOrWarrantFeeAmount(summaryOption: actionRecord) {
    await performAction('expandSummary', summaryOption.type);
    await performValidation('formLabelValue', summaryOption.label1, summaryOption.text1);
    await performValidation('formLabelValue', summaryOption.label2, summaryOption.text2);
    await performAction('expandSummary', summaryOption.type);
    const warrantJourney = summaryOption.journey === 'Warrant of possession';
    const feeType = warrantJourney
      ? enforcementApplication.typeofFee.warrantOfPossessionFee
      : enforcementApplication.typeofFee.writOfPossessionFee;

    const writOrWarrantFeeAmt = warrantJourney ? EnforcementCommonUtils.retrieveAmountFromString(summaryOption.text1 as string) : EnforcementCommonUtils.retrieveAmountFromString(summaryOption.text2 as string);

    moneyMap.set(feeType, writOrWarrantFeeAmt);
  }

  private async validateGetQuoteFromBailiffLink(bailiffQuote: actionRecord) {
    await performAction('expandSummary', bailiffQuote.type);
    await performAction('clickLinkAndVerifyNewTabTitle', bailiffQuote.link, bailiffQuote.newPage);
    await performAction('expandSummary', bailiffQuote.type);
  }

  private async selectApplicationType(applicationType: actionRecord, page: Page) {
    await this.addFieldsToMap(applicationType);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: applicationType.question, option: applicationType.option });
    if (applicationType.option === 'Writ of possession' && applicationType.option1 !== 'No') {
      await this.checkClaimTransferredToHighCourt(applicationType.question1 as string, applicationType.question2 as string);
      await performAction('reTryOnCallBackError', enforcementApplication.continueButton, applicationType.nextPage as string);
    } else if (applicationType.option === 'Warrant of possession' || applicationType.option === 'Warrant of restitution') {
      await performAction('reTryOnCallBackError', enforcementApplication.continueButton, applicationType.nextPage as string);
    }

  }

  private async checkClaimTransferredToHighCourt(question1: string, question2: string) {
    await performAction('clickRadioButton', { question: question1, option: enforcementApplication.yesRadioOptionHidden });
    await performAction('clickRadioButton', { question: question2, option: enforcementApplication.noRadioOptionHidden });
    await performAction('clickButton', enforcementApplication.continueButton);
    await performValidation('errorMessage', { header: enforcementApplication.errorErrorMessageHeader, message: enforcementApplication.errMessageDynamic });
    await performAction('clickButton', enforcementApplication.continueButton);
    await performAction('clickRadioButton', { question: question1, option: enforcementApplication.yesRadioOptionHidden });
    await performAction('clickRadioButton', { question: question2, option: enforcementApplication.yesRadioOptionHidden });
  }

  private async confirmClaimTransferredToHighCourt(transfer: actionRecord, page: Page) {
    await this.addFieldsToMap(transfer);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: transfer.question, option: transfer.option });
    await performAction('reTryOnCallBackError', enforcementApplication.continueButton, transfer.nextPage as string);
  }

  private async getDefendantDetails(defendantsDetails: actionRecord) {

    let originalDefendantDetails: string[] = [];
    const payLoad = defendantsDetails.payLoad as Record<string, any>;
    if (defendantsDetails.defendant1NameKnown === 'YES') {
      originalDefendantDetails.push(
        `${payLoad.defendant1.firstName} ${payLoad.defendant1.lastName}`
      );
    } else {
      originalDefendantDetails.push(
        `null null`
      );
    }

    if (defendantsDetails.additionalDefendants === 'YES') {

      for (const defendant of payLoad.additionalDefendants) {
        if (defendant.value.nameKnown === 'YES') {
          originalDefendantDetails.push(`${defendant.value.firstName} ${defendant.value.lastName}`);
        } else {
          originalDefendantDetails.push(
            `null null`
          );
        };
      };
    }
    defendantDetails = [...new Set(originalDefendantDetails.filter(n => n.trim().toLowerCase() !== "null null")),
    ...originalDefendantDetails.filter(n => n.trim().toLowerCase() === "null null")
    ];

  }

  private async selectHaveHiredHCEO(haveYouHired: actionRecord, page: Page) {
    await this.addFieldsToMap(haveYouHired);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: haveYouHired.question, option: haveYouHired.option });
    await performAction('reTryOnCallBackError', confirmHCEOfficer.continueButton, haveYouHired.nextPage as string);
  }

  private async nameYourHCEO(nameHCEO: actionRecord, page: Page) {
    await this.addFieldsToMap(nameHCEO);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', nameHCEO.label, nameHCEO.input);
    await performAction('reTryOnCallBackError', yourHCEO.continueButton, nameHCEO.nextPage as string);
  }

  private async selectNameAndAddressForEviction(nameAndAddress: actionRecord, page: Page) {
    await this.addFieldsToMap(nameAndAddress);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    if (nameAndAddress.defendant1NameKnown === 'YES' && defendantDetails.length) {
      await performValidation('formLabelValue', nameAndAddressForEviction.defendantsTableHeader, defendantDetails.join(' '));
    }

    defendantDetails = defendantDetails.map(fullName =>
      fullName?.trim().toLowerCase() === 'null null' ? 'Name not known' : fullName
    );

    await performValidation('formLabelValue', nameAndAddressForEviction.addressTableHeader, `${addressInfo.buildingStreet} ${addressInfo.addressLine2} ${addressInfo.townCity} ${addressInfo.engOrWalPostcode}`);
    await performAction('clickRadioButton', { question: nameAndAddress.question, option: nameAndAddress.option });
    await performAction('reTryOnCallBackError', nameAndAddressForEviction.continueButton, nameAndAddress.nextPage as string);
  }

  private async confirmDefendantsDOB(confirmDefendantsDateOfBirth: actionRecord, page: Page) {
    await this.addFieldsToMap(confirmDefendantsDateOfBirth);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: confirmDefendantsDateOfBirth.question, option: confirmDefendantsDateOfBirth.option });
    await performAction('reTryOnCallBackError', confirmDefendantsDOB.continueButton, confirmDefendantsDateOfBirth.nextPage as string);
  }

  private async enterDefendantsDOB(page: Page, defendantsDOB: actionRecord) {
    await this.addFieldsToMap(defendantsDOB);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', defendantsDOB.label, EnforcementCommonUtils.inputDOB(defendantsDOB.input as Array<string>));
    fieldsMap.set(defendantsDOB.label as string, await page.getByLabel(knownDefendantsDOBInformation.defendantsDOBQuestion).inputValue());
    await performAction('reTryOnCallBackError', knownDefendantsDOBInformation.continueButton, defendantsDOB.nextPage as string);
  }

  private async selectPeopleWhoWillBeEvicted(evictPeople: actionRecord, page: Page) {
    await this.addFieldsToMap(evictPeople);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: evictPeople.question, option: evictPeople.option });
    await performAction('reTryOnCallBackError', peopleWhoWillBeEvicted.continueButton, evictPeople.nextPage as string);
  }

  private async selectPeopleYouWantToEvict(peopleYouWantEvicted: actionRecord, page: Page) {
    await this.addFieldsToMap(peopleYouWantEvicted);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('check', { question: peopleYouWantEvicted.question, option: peopleYouWantEvicted.option });
    await performAction('reTryOnCallBackError', peopleYouWantToEvict.continueButton, peopleYouWantEvicted.nextPage as string);
  }

  private async selectPermissionFromJudge(page: Page) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    const [generalApplicationPage] = await Promise.all([
      page.waitForEvent('popup'),
      await performAction('clickButton', changeNameAddress.askTheJudgeParagraph)
    ]);
    await generalApplicationPage.waitForLoadState();
    await expect(generalApplicationPage).toHaveTitle(changeNameAddress.titleDynamic);
    await performValidation('text', {
      'text': changeNameAddress.mainHeader,
      'elementType': 'heading'
    });
    await generalApplicationPage.close();
  }

  private async selectEveryoneLivingAtTheProperty(riskToBailiff: actionRecord, page: Page) {
    await this.addFieldsToMap(riskToBailiff);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: riskToBailiff.question, option: riskToBailiff.option });
    await performAction('reTryOnCallBackError', livingInTheProperty.continueButton, riskToBailiff.nextPage as string);
  }

  private async selectRiskPosedByEveryoneAtProperty(riskCategory: actionRecord, page: Page) {
    await this.addFieldsToMap(riskCategory);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('check', { question: riskCategory.question, option: riskCategory.option });
    await performAction('reTryOnCallBackError', evictionRisksPosed.continueButton, riskCategory.nextPage as string);
  }

  private async provideRiskPosedByEveryoneAtProperty(provideRiskPosed: actionRecord, page: Page) {
    await this.addFieldsToMap(provideRiskPosed);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    const testInput = await EnforcementCommonUtils.generateMoreThanMaxString(page, provideRiskPosed.label as string, provideRiskPosed.input as number);
    await performAction('inputText', provideRiskPosed.label, testInput);
    fieldsMap.set(provideRiskPosed.label as string, testInput);
    await performAction('reTryOnCallBackError', !provideRiskPosed?.button ? provideRiskPosed.button = 'Continue' : provideRiskPosed.button as string, provideRiskPosed.nextPage as string);
  }

  private async selectVulnerablePeopleInTheProperty(vulnerablePeople: actionRecord, page: Page) {
    await this.addFieldsToMap(vulnerablePeople);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: vulnerablePeople.question, option: vulnerablePeople.option });
    if (vulnerablePeople.option === vulnerableAdultsChildren.yesRadioOption) {
      const testInput = await EnforcementCommonUtils.generateMoreThanMaxString(page, vulnerablePeople.label as string, vulnerablePeople.input as number);
      await performAction('clickRadioButton', { question: vulnerablePeople.confirm, option: vulnerablePeople.peopleOption });
      await performAction('inputText', vulnerablePeople.label, testInput);
      fieldsMap.set(vulnerablePeople.label as string, testInput);
    }
    await performAction('reTryOnCallBackError', enforcementApplication.continueButton, vulnerablePeople.nextPage as string);
  }

  private async provideDetailsBasedOnRadioOptionSelection(userInput: actionRecord, page: Page) {
    await this.addFieldsToMap(userInput);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: userInput.question, option: userInput.option });
    if (userInput.option === 'Yes') {
      const testInput = await EnforcementCommonUtils.generateMoreThanMaxString(page, userInput.label as string, userInput.input as number);
      await performAction('inputText', userInput.label, testInput);
      fieldsMap.set(userInput.label as string, testInput);
    };
    await performAction('reTryOnCallBackError', !userInput?.button ? userInput.button = 'Continue' : userInput.button as string, userInput.nextPage as string);
  }

  private async provideMoneyOwed(totalMoneyOwed: actionRecord, page: Page) {
    await this.addFieldsToMap(totalMoneyOwed);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    const moneyOwedEntered = EnforcementCommonUtils.getRandomElementForAnArray(totalMoneyOwed.input as Array<string>)
    await performAction('inputText', totalMoneyOwed.label, moneyOwedEntered);
    const moneyOwedAmt = EnforcementCommonUtils.retrieveAmountFromString(moneyOwedEntered as string);
    moneyMap.set(moneyOwed.arrearsAndOtherCostsDynamic, moneyOwedAmt);
    fieldsMap.set(totalMoneyOwed.label as string, moneyOwedEntered as string);
    await performAction('reTryOnCallBackError', moneyOwed.continueButton, totalMoneyOwed.nextPage as string);
  }

  private async provideLegalCosts(legalCost: actionRecord, page: Page) {
    await this.addFieldsToMap(legalCost);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: legalCost.question, option: legalCost.option });
    if (legalCost.option === legalCosts.yesRadioOption) {
      const legalCostEntered = EnforcementCommonUtils.getRandomElementForAnArray(legalCost.input as Array<string>)
      await performAction('inputText', legalCost.label, legalCostEntered);
      const legalCostAmt = EnforcementCommonUtils.retrieveAmountFromString(legalCostEntered as string);
      moneyMap.set(legalCosts.legalCostsFeeDynamic, legalCostAmt);
      fieldsMap.set(legalCost.label as string, legalCostEntered as string);
    } else {
      moneyMap.set(legalCosts.legalCostsFeeDynamic, 0);
    }
    await performAction('reTryOnCallBackError', legalCosts.continueButton, legalCost.nextPage as string);
  }

  private async provideLandRegistryFees(landRegistry: actionRecord, page: Page) {
    await this.addFieldsToMap(landRegistry);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: landRegistry.question, option: landRegistry.option });
    if (landRegistry.option === propertyAccessDetails.yesRadioOption) {
      const langRegistryAmtEntered = EnforcementCommonUtils.getRandomElementForAnArray(landRegistry.input as Array<string>)
      await performAction('inputText', landRegistry.label, langRegistryAmtEntered);
      const landRegistryFeeAmt = EnforcementCommonUtils.retrieveAmountFromString(langRegistryAmtEntered as string);
      moneyMap.set(landRegistryFees.landRegistryFeeDynamic, landRegistryFeeAmt);
      fieldsMap.set(landRegistry.label as string, langRegistryAmtEntered as string);
    } else {
      moneyMap.set(landRegistryFees.landRegistryFeeDynamic, 0);
    }
    await performAction('reTryOnCallBackError', landRegistryFees.continueButton, landRegistry.nextPage as string);
  }

  private async validateAmountToRePayTable(header: actionRecord) {

    if (header.headerName === repayments.mainHeader) {
      const totalAmt = Array.from(moneyMap.values()).reduce((a, b) => a + b, 0);
      moneyMap.set(repayments.totalTableHeader, totalAmt);
    };
    for (const [moneyField, amount] of moneyMap) {
      await performValidation('formLabelValue', moneyField, `${EnforcementCommonUtils.convertCurrencyToString(amount)}`);
    }
  }
  private async provideAmountToRePay(amtToPay: actionRecord, page: Page) {
    await this.addFieldsToMap(amtToPay);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: amtToPay.question, option: amtToPay.option });
    if (amtToPay.option === repayments.someRadioOptions) {
      const amtToRepayEntered = EnforcementCommonUtils.getRandomElementForAnArray(amtToPay.input as Array<string>)
      await performAction('inputText', amtToPay.label, amtToRepayEntered);
      fieldsMap.set(amtToPay.label as string, amtToRepayEntered as string);
    };
    await performAction('reTryOnCallBackError', repayments.continueButton, amtToPay.nextPage as string);
  }

  private async provideHowDefendantReturnToProperty(provideEvidence: actionRecord, page: Page) {
    await this.addFieldsToMap(provideEvidence);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    const testInput = await EnforcementCommonUtils.generateMoreThanMaxString(page, provideEvidence.label as string, provideEvidence.input as number);
    await performAction('inputText', provideEvidence.label, testInput);
    fieldsMap.set(provideEvidence.label as string, testInput);
    await performAction('reTryOnCallBackError', !provideEvidence?.button ? provideEvidence.button = 'Continue' : provideEvidence.button as string, provideEvidence.nextPage as string);
  }

  private async selectLanguageUsed(languageDetails: actionRecord, page: Page) {
    await this.addFieldsToMap(languageDetails);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: languageDetails.question, option: languageDetails.option });
    await performAction('reTryOnCallBackError', languageUsed.continueButton, languageDetails.nextPage as string);
  }

  private async confirmSuspendedOrder(suspendedOrderPara: actionRecord, page: Page) {
    await this.addFieldsToMap(suspendedOrderPara);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: suspendedOrderPara.question, option: suspendedOrderPara.option });
    await performAction('reTryOnCallBackError', suspendedOrder.continueButton, suspendedOrderPara.nextPage as string);
  }

  private async selectStatementOfTruth(claimantDetails: actionRecord, page: Page) {
    await this.addFieldsToMap(claimantDetails);
    await performAction('check', claimantDetails.selectCheckbox);
    await performAction('clickRadioButton', { question: claimantDetails.question, option: claimantDetails.option });
    if (claimantDetails.option === statementOfTruth.claimantRadioOption) {
      await performAction('check', claimantDetails.option1);
      await performAction('inputText', claimantDetails.label, !claimantDetails.input ? submitCaseApiData.submitCasePayload.claimantName : claimantDetails.input);
      await performAction('inputText', claimantDetails.label1, claimantDetails.input1);
    }
    if (claimantDetails.option === statementOfTruth.claimantLegalRepresentativeRadioOption) {
      await performAction('check', claimantDetails.option1);
      await performAction('inputText', claimantDetails.label, claimantDetails.input);
      await performAction('inputText', claimantDetails.label1, claimantDetails.input1);
      await performAction('inputText', claimantDetails.label2, claimantDetails.input2);
    }
    await performAction('reTryOnCallBackError', statementOfTruth.continueButton, claimantDetails.nextPage as string);
  }

  private async selectStatementOfTruthWrit(claimantSOT: actionRecord, page: Page) {
    await this.addFieldsToMap(claimantSOT);
    await performAction('clickRadioButton', { question: claimantSOT.question, option: claimantSOT.option });
    if (claimantSOT.option === statementOfTruth.claimantRadioOption) {
      await performAction('check', claimantSOT.option1);
      await performAction('inputText', claimantSOT.label, !claimantSOT.input ? submitCaseApiData.submitCasePayload.claimantName : claimantSOT.input);
      await performAction('inputText', claimantSOT.label1, claimantSOT.input1);
    }
    if (claimantSOT.option === statementOfTruth.claimantLegalRepresentativeRadioOption) {
      await performAction('check', claimantSOT.option1);
      await performAction('inputText', claimantSOT.label, claimantSOT.input);
      await performAction('inputText', claimantSOT.label1, claimantSOT.input1);
      await performAction('inputText', claimantSOT.label2, claimantSOT.input2);
    }
    await performAction('reTryOnCallBackError', statementOfTruth.continueButton, claimantSOT.nextPage as string);
  }

  private async uploadEvidenceThatDefendantsAreAtProperty(uploadEvidence: actionRecord, page: Page) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    if (Array.isArray(uploadEvidence.documents)) {
      for (let fileIndex = 0; fileIndex < uploadEvidence.documents.length; fileIndex++) {
        const document = uploadEvidence.documents[fileIndex];
        const testInput = await EnforcementCommonUtils.generateMoreThanMaxString(page, document.label as string, document.description as number);
        await performActions(
          'Add Document',
          ['uploadFile', document.fileName],
          ['select', { dropdown: document.docType, index: fileIndex }, document.type],
          ['inputText', { text: document.label, index: fileIndex }, testInput]
        );
      }
    }
    await performAction('reTryOnCallBackError', evidenceUpload.continueButton, uploadEvidence.nextPage as string);

  }

  private async validatePrePopulatedData(prePopulatedData: actionRecord) {

     await test.step(`PrePopulated data validation`, async () => {
      const page = prePopulatedData?.testPage ?? 'Unknown';
      const count = Array.isArray(prePopulatedData?.inputData)
        ? prePopulatedData.inputData.length
        : prePopulatedData?.inputData ? 1 : 0;
      expect(page, `Validation of prepopulated data started for page => [${page}] and the number of elements getting validated is : [${count}]`).not.toBe('Unknown');
    });
    const items = Array.isArray(prePopulatedData.inputData)
      ? prePopulatedData.inputData
      : [prePopulatedData.inputData];

    for (const item of items) {
      switch (item.type) {
        case 'radio':
          await performValidation('validateRadioButtonValues', { question: item.inputRadioQuestion }, { expected: item.expectedAnswer });
          break;

        case 'inputText':
          await performValidation('validateInputTextValues', { textLabel: item.inputTextLabel }, { expected: item.expectedAnswer });
          break;

        case 'checkBox':
          await performValidation('validateCheckBoxSelection', { question: item.inputCheckBoxQuestion }, { expected: item.expectedAnswer });
          break;

        default:
          break;
      }
    }

  }

  private async inputErrorValidation(page: Page, validationArr: actionRecord) {


      if (Array.isArray(validationArr.inputArray)) {
        for (const item of validationArr.inputArray) {
          switch (validationArr.validationType) {
            case 'moneyFieldAndRadioOption':
              await performAction('clickRadioButton', { question: validationArr.question, option: validationArr.option });
              await performAction('inputText', validationArr.label, item.type === 'moreThanTotal' ? String((moneyMap.get(repayments.totalTableHeader) as number) + 10) : item.input);
              await expect(async () => {
                await performAction('clickButton', validationArr.button);
                // await performValidation('errorMessage', { header: !validationArr?.header ? validationArr.header = 'The event could not be created' : validationArr.header, message: item.errMessage });
                await performValidation('inputError', validationArr.label, item.errMessage);
              }).toPass({
                timeout: VERY_LONG_TIMEOUT,
              });
              await performAction('clickRadioButton', { question: validationArr.question, option: validationArr.option2 });
              break;

            case 'radioOptions':
              await performAction('clickButton', validationArr.button);
              await performValidation('inputError', !validationArr?.label ? validationArr.question : validationArr.label, item.errMessage);
              await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
              await performAction('clickRadioButton', { question: validationArr.question, option: validationArr.option });
              break;

            case 'moneyField':
              await performAction('inputText', validationArr.label, item.input);
              await expect(async () => {
                await performAction('clickButton', validationArr.button);
                //await performValidation('errorMessage', { header: !validationArr?.header ? validationArr.header = 'The event could not be created' : validationArr.header, message: item.errMessage });
                await performValidation('inputError', validationArr.label, item.errMessage);
              }).toPass({
                timeout: VERY_LONG_TIMEOUT,
              });
              break;

            case 'textField':
              await performAction('inputText', validationArr.label, await EnforcementCommonUtils.generateMoreThanMaxString(page, validationArr.label as string, item.input));
              await expect(async () => {
                await performAction('clickButton', validationArr.button);
                if (item.type === 'moreThanMax') {
                  await performValidation('errorMessage', { header: validationArr.header, message: item.errMessage });
                } else {
                  await performValidation('inputError', validationArr.label, item.errMessage);
                  await performValidation('errorMessage', validationArr.label, item.errMessage);
                }
              }).toPass({
                timeout: VERY_LONG_TIMEOUT,
              });
              break;

            case 'checkBox':
              await performAction('clickButton', validationArr.button);
              await performValidation('inputError', !validationArr?.label ? validationArr.question : validationArr.label, item.errMessage);
              await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
              await performAction('check', validationArr.checkBox);
              break;

            case 'checkBoxPageLevel':
              await performAction('clickButton', validationArr.button);
              await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
              await performAction('check', validationArr.checkBox);
              break;

            case 'addDocument':
              await expect(async () => {
                await performAction('clickButton', validationArr.button);
                await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
              }).toPass({
                timeout: VERY_LONG_TIMEOUT,
              });
              await performAction('clickButton', 'Add new');
              break;

            case 'dropDown':
              await performAction('clickButton', validationArr.button);
              await expect(async () => {
                await performAction('clickButton', validationArr.button);
                await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
              }).toPass({
                timeout: VERY_LONG_TIMEOUT,
              });
              await performAction('select', validationArr.docType, validationArr.type);
              break;

            case 'upLoad':
              await expect(async () => {
                await performAction('clickButton', validationArr.button);
                if (item.type === 'invalid') {
                  const fileInput = page.locator('input[type="file"].form-control.bottom-30');
                  const filePath = path.resolve(__dirname, '../../../../data/inputFiles', item.file);
                  await fileInput.last().setInputFiles(filePath);
                  await performValidation('inputError', validationArr.label, item.errMessage);
                } else {
                  await performValidation('errorMessage', !validationArr?.header ? validationArr.header = 'There is a problem' : validationArr.header, item.errMessage);
                }
              }).toPass({
                timeout: VERY_LONG_TIMEOUT,
              });
              break;

            default:
              throw new Error(`Validation type :"${validationArr.validationType}" is not valid`);
          };
        }
      }
    if (validationArr.buttonRemove) {
      await performAction('removeFile');
      await page.waitForTimeout(6000);
    }
  }

  private async addFieldsToMap(fields: actionRecord) {

    const setIfKeyExists = (key?: string, value?: string) => {
      if (key) fieldsMap.set(key, value ?? "");
    };
    setIfKeyExists(fields.question as string, fields.option as string);
    setIfKeyExists(fields.label as string, fields.input as string);
    setIfKeyExists(fields.confirm as string, fields.peopleOption as string);
  }
}
