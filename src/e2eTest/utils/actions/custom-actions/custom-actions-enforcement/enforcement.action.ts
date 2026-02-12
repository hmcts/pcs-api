import { expect, Page } from '@playwright/test';
import { performAction, performValidation } from '@utils/controller-enforcement';
import { IAction, actionData, actionRecord } from '@utils/interfaces/action.interface';
import {
  yourApplication,
  nameAndAddressForEviction,
  confirmDefendantsDOB,
  everyoneLivingAtTheProperty,
  vulnerableAdultsAndChildren,
  riskPosedByEveryoneAtProperty,
  anythingElseHelpWithEviction,
  accessToTheProperty,
  peopleWillBeEvicted,
  youNeedPermission,
  legalCosts,
  landRegistryFees,
  rePayments,
  peopleYouWantToEvict,
  moneyOwed,
  languageUsed,
  statementOfTruthOne,
  enterDefendantsDOB,
  suspendedOrder,
  confirmHCEOHired,
  yourHCEO,
  claimSentToHighCourt
} from '@data/page-data/page-data-enforcement';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { LONG_TIMEOUT, SHORT_TIMEOUT, VERY_LONG_TIMEOUT } from 'playwright.config';

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
  async execute(page: Page, action: string, fieldName: string | actionRecord, data?: actionData): Promise<void> {
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
      ['selectVulnerablePeopleInTheProperty', () => this.selectVulnerablePeopleInTheProperty(fieldName as actionRecord, page)],
      ['provideDetailsAnythingElseHelpWithEviction', () => this.provideDetailsAnythingElseHelpWithEviction(fieldName as actionRecord, page)],
      ['accessToProperty', () => this.accessToProperty(fieldName as actionRecord, page)],
      ['provideMoneyOwed', () => this.provideMoneyOwed(fieldName as actionRecord, page)],
      ['provideLegalCosts', () => this.provideLegalCosts(fieldName as actionRecord, page)],
      ['provideLandRegistryFees', () => this.provideLandRegistryFees(fieldName as actionRecord, page)],
      ['provideAmountToRePay', () => this.provideAmountToRePay(fieldName as actionRecord, page)],
      ['validateAmountToRePayTable', () => this.validateAmountToRePayTable(fieldName as actionRecord)],
      ['selectLanguageUsed', () => this.selectLanguageUsed(fieldName as actionRecord, page)],
      ['confirmSuspendedOrder', () => this.confirmSuspendedOrder(fieldName as actionRecord, page)],
      ['selectStatementOfTruth', () => this.selectStatementOfTruth(fieldName as actionRecord, page)],
      ['selectStatementOfTruthWrit', () => this.selectStatementOfTruthWrit(fieldName as actionRecord, page)],
      ['inputErrorValidation', () => this.inputErrorValidation(page, fieldName as actionRecord)],
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
      ? yourApplication.typeofFee.warrantOfPossessionFee
      : yourApplication.typeofFee.writOfPossessionFee;

    const writOrWarrantFeeAmt = warrantJourney ? await this.retrieveAmountFromString(summaryOption.text1 as string) : await this.retrieveAmountFromString(summaryOption.text2 as string);

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
    await this.reTryOnCallBackError(page, yourApplication.continueButton, applicationType.nextPage as string);
  }

  private async confirmClaimTransferredToHighCourt(transfer: actionRecord, page: Page) {
    await this.addFieldsToMap(transfer);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: transfer.question, option: transfer.option });
    await this.reTryOnCallBackError(page, claimSentToHighCourt.continueButton, transfer.nextPage as string);
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
    await this.reTryOnCallBackError(page, confirmHCEOHired.continueButton, haveYouHired.nextPage as string);
  }

  private async nameYourHCEO(nameHCEO: actionRecord, page: Page) {
    await this.addFieldsToMap(nameHCEO);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', nameHCEO.label, nameHCEO.input);
    await this.reTryOnCallBackError(page, yourHCEO.continueButton, nameHCEO.nextPage as string);
  }

  private async selectNameAndAddressForEviction(nameAndAddress: actionRecord, page: Page) {
    await this.addFieldsToMap(nameAndAddress);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    if (nameAndAddress.defendant1NameKnown === 'YES' && defendantDetails.length) {
      await performValidation('formLabelValue', nameAndAddressForEviction.subHeaderDefendants, defendantDetails.sort().join(' '));
    }

    defendantDetails = defendantDetails.map(fullName =>
      fullName?.trim().toLowerCase() === 'null null' ? 'Name not known' : fullName
    );

    await performValidation('formLabelValue', nameAndAddressForEviction.subHeaderAddress, `${addressInfo.buildingStreet} ${addressInfo.addressLine2} ${addressInfo.townCity} ${addressInfo.engOrWalPostcode}`);
    await performAction('clickRadioButton', { question: nameAndAddress.question, option: nameAndAddress.option });
    await this.reTryOnCallBackError(page, nameAndAddressForEviction.continueButton, nameAndAddress.nextPage as string);
  }

  private async confirmDefendantsDOB(confirmDefendantsDateOfBirth: actionRecord, page: Page) {
    await this.addFieldsToMap(confirmDefendantsDateOfBirth);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: confirmDefendantsDateOfBirth.question, option: confirmDefendantsDateOfBirth.option });
    await this.reTryOnCallBackError(page, confirmDefendantsDOB.continueButton, confirmDefendantsDateOfBirth.nextPage as string);
  }

  private async enterDefendantsDOB(page: Page, defendantsDOB: actionRecord) {
    await this.addFieldsToMap(defendantsDOB);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', defendantsDOB.label, await this.inputDOB(defendantsDOB.input as Array<string>));
    fieldsMap.set(defendantsDOB.label as string, await page.getByLabel(enterDefendantsDOB.defendantsDOBTextLabel).inputValue());
    await this.reTryOnCallBackError(page, enterDefendantsDOB.continueButton, defendantsDOB.nextPage as string);
  }

  private async selectPeopleWhoWillBeEvicted(evictPeople: actionRecord, page: Page) {
    await this.addFieldsToMap(evictPeople);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: evictPeople.question, option: evictPeople.option });
    await this.reTryOnCallBackError(page, peopleWillBeEvicted.continueButton, evictPeople.nextPage as string);
  }

  private async selectPeopleYouWantToEvict(peopleYouWantEvicted: actionRecord, page: Page) {
    await this.addFieldsToMap(peopleYouWantEvicted);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('check', { question: peopleYouWantEvicted.question, option: peopleYouWantEvicted.option });
    await this.reTryOnCallBackError(page, peopleYouWantToEvict.continueButton, peopleYouWantEvicted.nextPage as string);
  }

  private async selectPermissionFromJudge(page: Page) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    const [generalApplicationPage] = await Promise.all([
      page.waitForEvent('popup'),
      await performAction('clickButton', youNeedPermission.askTheJudgeLink)
    ]);
    await generalApplicationPage.waitForLoadState();
    await expect(generalApplicationPage).toHaveTitle(youNeedPermission.title);
    await performValidation('text', {
      'text': youNeedPermission.mainHeader,
      'elementType': 'heading'
    });
    await generalApplicationPage.close();
  }

  private async selectEveryoneLivingAtTheProperty(riskToBailiff: actionRecord, page: Page) {
    await this.addFieldsToMap(riskToBailiff);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: riskToBailiff.question, option: riskToBailiff.option });
    await this.reTryOnCallBackError(page, everyoneLivingAtTheProperty.continueButton, riskToBailiff.nextPage as string);
  }

  private async selectRiskPosedByEveryoneAtProperty(riskCategory: actionRecord, page: Page) {
    await this.addFieldsToMap(riskCategory);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('check', { question: riskCategory.question, option: riskCategory.option });
    await this.reTryOnCallBackError(page, riskPosedByEveryoneAtProperty.continueButton, riskCategory.nextPage as string);
  }

  private async provideRiskPosedByEveryoneAtProperty(provideRiskPosed: actionRecord, page: Page) {
    await this.addFieldsToMap(provideRiskPosed);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', provideRiskPosed.label, provideRiskPosed.input);
    await this.reTryOnCallBackError(page, !provideRiskPosed?.button ? provideRiskPosed.button = 'Continue' : provideRiskPosed.button as string, provideRiskPosed.nextPage as string);
  }

  private async selectVulnerablePeopleInTheProperty(vulnerablePeople: actionRecord, page: Page) {
    await this.addFieldsToMap(vulnerablePeople);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: vulnerablePeople.question, option: vulnerablePeople.option });
    if (vulnerablePeople.option === vulnerableAdultsAndChildren.yesRadioOption) {
      await performAction('clickRadioButton', { question: vulnerablePeople.confirm, option: vulnerablePeople.peopleOption });
      await performAction('inputText', vulnerablePeople.label, vulnerablePeople.input);
    }
    await this.reTryOnCallBackError(page, vulnerableAdultsAndChildren.continueButton, vulnerablePeople.nextPage as string);
  }
  private async provideDetailsAnythingElseHelpWithEviction(anythingElse: actionRecord, page: Page) {
    await this.addFieldsToMap(anythingElse);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: anythingElse.question, option: anythingElse.option });
    if (anythingElse.option === anythingElseHelpWithEviction.yesRadioOption) {
      await performAction('inputText', anythingElse.label, anythingElse.input);
    };
    await this.reTryOnCallBackError(page, anythingElseHelpWithEviction.continueButton, anythingElse.nextPage as string);
  }
  private async accessToProperty(accessToProperty: actionRecord, page: Page) {
    await this.addFieldsToMap(accessToProperty);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: accessToProperty.question, option: accessToProperty.option });
    if (accessToProperty.option === accessToTheProperty.yesRadioOption) {
      await performAction('inputText', accessToProperty.label, accessToProperty.input);
    };
    await this.reTryOnCallBackError(page, accessToTheProperty.continueButton, accessToProperty.nextPage as string);
  }

  private async provideMoneyOwed(totalMoneyOwed: actionRecord, page: Page) {
    await this.addFieldsToMap(totalMoneyOwed);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', totalMoneyOwed.label, totalMoneyOwed.input);
    const moneyOwedAmt = await this.retrieveAmountFromString(totalMoneyOwed.input as string);
    moneyMap.set(moneyOwed.arrearsAndOtherCosts, moneyOwedAmt);
    await this.reTryOnCallBackError(page, moneyOwed.continueButton, totalMoneyOwed.nextPage as string);
  }

  private async provideLegalCosts(legalCost: actionRecord, page: Page) {
    await this.addFieldsToMap(legalCost);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: legalCost.question, option: legalCost.option });
    if (legalCost.option === legalCosts.yesRadioOption) {
      await performAction('inputText', legalCost.label, legalCost.input);
      const legalCostAmt = await this.retrieveAmountFromString(legalCost.input as string);
      moneyMap.set(legalCosts.legalCostsFee, legalCostAmt);
    } else {
      moneyMap.set(legalCosts.legalCostsFee, 0);
    }
    await this.reTryOnCallBackError(page, legalCosts.continueButton, legalCost.nextPage as string);
  }

  private async provideLandRegistryFees(landRegistry: actionRecord, page: Page) {
    await this.addFieldsToMap(landRegistry);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: landRegistry.question, option: landRegistry.option });
    if (landRegistry.option === accessToTheProperty.yesRadioOption) {
      await performAction('inputText', landRegistry.label, landRegistry.input);
      const landRegistryFeeAmt = await this.retrieveAmountFromString(landRegistry.input as string);
      moneyMap.set(landRegistryFees.landRegistryFee, landRegistryFeeAmt);
    } else {
      moneyMap.set(landRegistryFees.landRegistryFee, 0);
    }
    await this.reTryOnCallBackError(page, landRegistryFees.continueButton, landRegistry.nextPage as string);
  }

  private async validateAmountToRePayTable(header: actionRecord) {

    if (header.headerName === rePayments.mainHeader) {
      const totalAmt = Array.from(moneyMap.values()).reduce((a, b) => a + b, 0);
      moneyMap.set(rePayments.totalAmt, totalAmt);
    };
    for (const [moneyField, amount] of moneyMap) {
      await performValidation('formLabelValue', moneyField, `${await this.convertCurrencyToString(amount)}`);
    }
  }
  private async provideAmountToRePay(amtToPay: actionRecord, page: Page) {
    await this.addFieldsToMap(amtToPay);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: amtToPay.question, option: amtToPay.option });
    if (amtToPay.option === rePayments.rePaymentRadioOptions.some) {
      await performAction('inputText', amtToPay.label, amtToPay.input);
    };
    await this.reTryOnCallBackError(page, rePayments.continueButton, amtToPay.nextPage as string);
  }

  private async selectLanguageUsed(languageDetails: actionRecord, page: Page) {
    await this.addFieldsToMap(languageDetails);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: languageDetails.question, option: languageDetails.option });
    await this.reTryOnCallBackError(page, languageUsed.continueButton, languageDetails.nextPage as string);
  }

  private async confirmSuspendedOrder(suspendedOrderPara: actionRecord, page: Page) {
    await this.addFieldsToMap(suspendedOrderPara);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: suspendedOrderPara.question, option: suspendedOrderPara.option });
    await this.reTryOnCallBackError(page, suspendedOrder.continueButton, suspendedOrderPara.nextPage as string);
  }

  private async selectStatementOfTruth(claimantDetails: actionRecord, page: Page) {
    await this.addFieldsToMap(claimantDetails);
    await performAction('check', claimantDetails.selectCheckbox);
    await performAction('clickRadioButton', { question: claimantDetails.question, option: claimantDetails.option });
    if (claimantDetails.option === statementOfTruthOne.claimantRadioOption) {
      await performAction('check', claimantDetails.option1);
      await performAction('inputText', claimantDetails.label, !claimantDetails.input ? submitCaseApiData.submitCasePayload.claimantName : claimantDetails.input);
      await performAction('inputText', claimantDetails.label1, claimantDetails.input1);
    }
    if (claimantDetails.option === statementOfTruthOne.claimantLegalRepresentativeRadioOption) {
      await performAction('check', claimantDetails.option1);
      await performAction('inputText', claimantDetails.label, claimantDetails.input);
      await performAction('inputText', claimantDetails.label1, claimantDetails.input1);
      await performAction('inputText', claimantDetails.label2, claimantDetails.input2);
    }
    await this.reTryOnCallBackError(page, statementOfTruthOne.continueButton, claimantDetails.nextPage as string);
  }

  private async selectStatementOfTruthWrit(claimantSOT: actionRecord, page: Page) {
    await this.addFieldsToMap(claimantSOT);
    await performAction('clickRadioButton', { question: claimantSOT.question, option: claimantSOT.option });
    if (claimantSOT.option === statementOfTruthOne.claimantRadioOption) {
      await performAction('check', claimantSOT.option1);
      await performAction('inputText', claimantSOT.label, !claimantSOT.input ? submitCaseApiData.submitCasePayload.claimantName : claimantSOT.input);
      await performAction('inputText', claimantSOT.label1, claimantSOT.input1);
    }
    if (claimantSOT.option === statementOfTruthOne.claimantLegalRepresentativeRadioOption) {
      await performAction('check', claimantSOT.option1);
      await performAction('inputText', claimantSOT.label, claimantSOT.input);
      await performAction('inputText', claimantSOT.label1, claimantSOT.input1);
      await performAction('inputText', claimantSOT.label2, claimantSOT.input2);
    }
    await this.reTryOnCallBackError(page, statementOfTruthOne.continueButton, claimantSOT.nextPage as string);
  }

  private async inputErrorValidation(page: Page, validationArr: actionRecord) {

    if (validationArr.validationReq === 'YES') {

      if (Array.isArray(validationArr.inputArray)) {
        for (const item of validationArr.inputArray) {
          switch (validationArr.validationType) {
            case 'moneyFieldAndRadioOption':
              await performAction('clickRadioButton', { question: validationArr.question, option: validationArr.option });
              await performAction('inputText', validationArr.label, item.type === 'moreThanTotal' ? String((moneyMap.get(rePayments.totalAmt) as number) + 10) : item.input);
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
              await performAction('inputText', validationArr.label, await this.generateMoreThanMaxString(page, validationArr.label as string, item.input));
              await performAction('clickButton', validationArr.button);
              if (item.type === 'moreThanMax') {
                await performValidation('errorMessage', { header: validationArr.header, message: item.errMessage });
              } else {
                await performValidation('inputError', validationArr.label, item.errMessage);
                await performValidation('errorMessage', validationArr.label, item.errMessage)
              }
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

            default:
              throw new Error(`Validation type :"${validationArr.validationType}" is not valid`);
          };
        }
      }
    }
  }

  private async generateMoreThanMaxString(page: Page, label: string, input: string | number): Promise<string> {

    let length: number;

    if (input === 'MAXPLUS') {
      const hintText = await page
        .locator(`//span[text()="${label}"]/ancestor::div[contains(@class,'form-group')]//span[contains(@class,'form-hint')]`)
        .innerText();

      const limit = await this.retrieveAmountFromString(hintText);
      if (limit === 0) return '';

      length = limit + 1;

    } else if (typeof input === 'number') {
      length = input + 1;

    } else {
      return '';
    }

    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let finalString = '';
    for (let i = 0; i < length; i++) {
      finalString += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return finalString;
  }

  private async retrieveAmountFromString(input: string): Promise<number> {
    const getCharCount = input.split('You can enter').map(str => str.trim()).filter(str => str.length > 0);
    const charLimitInfo = getCharCount[getCharCount.length - 1].match(/[-+]?(?:\d{1,3}(?:,\d{3})+|\d+)(?:\.\d+)?/);
    const amount = charLimitInfo ? Number(charLimitInfo[0].replace(/,/g, "")) : 0;
    return Number(amount.toFixed(2));
  }

  private async convertCurrencyToString(amount: number): Promise<string> {

    const cents = Math.round(amount * 100);
    const hasZeroDecimals = cents % 100 === 0;

    const amtString =
      new Intl.NumberFormat('en-GB', {
        style: 'currency',
        currency: 'GBP',
        minimumFractionDigits: hasZeroDecimals ? 0 : 2,
        maximumFractionDigits: hasZeroDecimals ? 0 : 2,
      });
    return amtString.format(cents / 100);
  }

  private async addFieldsToMap(fields: actionRecord) {

    const setIfKeyExists = (key?: string, value?: string) => {
      if (key) fieldsMap.set(key, value ?? "");
    };
    setIfKeyExists(fields.question as string, fields.option as string);
    setIfKeyExists(fields.label as string, fields.input as string);
    setIfKeyExists(fields.confirm as string, fields.peopleOption as string);
  }

  private async inputDOB(inputArray: string[]): Promise<string> {
    return inputArray.map((item) => item + " - " + this.getRandomDOBFromPast(18, 30)).join('\n');
  }

  private getRandomDOBFromPast(date1: number, date2: number): string {
    const today = new Date();
    const maxDate = new Date(
      today.getFullYear() - date1,
      today.getMonth(),
      today.getDate()
    );

    const minDate = new Date(
      today.getFullYear() - date2,
      today.getMonth(),
      today.getDate()
    );
    const randomTime = minDate.getTime() + Math.random() * (maxDate.getTime() - minDate.getTime());
    const randomDate = new Date(randomTime);

    const day = String(randomDate.getDate()).padStart(2, '0');
    const month = String(randomDate.getMonth() + 1).padStart(2, '0');
    const year = randomDate.getFullYear();

    return `${day} ${month} ${year}`;
  }

  /* This retry method is called when there is a call back error after the event of button click - with lesser 'Expect' timeout */
  private async reTryOnCallBackError(page: Page, button: string, nextPage: string) {
    await expect(async () => {
      await performAction('clickButton', button);
      await expect(page.locator(`//h1[text()="${nextPage}"]`), `If the ${nextPage} page is not loaded on the initial attempt,then this retry logic will be activated =>`).toBeVisible({ timeout: SHORT_TIMEOUT });
    }).toPass({
      timeout: LONG_TIMEOUT,
    });
  }
}