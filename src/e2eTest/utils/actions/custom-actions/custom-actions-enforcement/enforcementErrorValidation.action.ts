import { evidenceUpload, yourHCEO } from '@data/page-data/page-data-enforcement';
import { Page } from '@playwright/test';
import { performAction } from '@utils/controller-enforcement';
import { IAction, actionData, actionRecord } from '@utils/interfaces/action.interface';
import { defendantDetails } from './enforcement.action';
import { confirmDefendantsDOB, confirmHCEOfficer, enforcementApplication, evictionRisksPosed, explainHowDefendantsReturned, knownDefendantsDOBInformation, landRegistryFees, languageUsed, legalCosts, moneyOwed, nameAndAddressForEviction, peopleWhoWillBeEvicted, peopleYouWantToEvict, repayments, statementOfTruth, suspendedOrder, violentAggressiveRisk, vulnerableAdultsChildren } from '@data/page-data-figma/page-data-enforcement-figma';

export class ErrorValidationAction implements IAction {
  async execute(page: Page, action: string, errorFlag: string | actionRecord, roles?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['errorValidationYourApplicationPage', () => this.errorValidationYourApplicationPage(errorFlag as string)],
      ['errorValidationNameAndAddressForEvictionPage', () => this.errorValidationNameAndAddressForEvictionPage(errorFlag as string)],
      ['errorValidationConfirmDefendantsDOBPage', () => this.errorValidationConfirmDefendantsDOBPage(errorFlag as string)],
      ['errorValidationEnterDefendantsDOBPage', () => this.errorValidationEnterDefendantsDOBPage(errorFlag as string)],
      ['errorValidationRiskPosedByEveryonePage', () => this.errorValidationRiskPosedByEveryonePage(errorFlag as string)],
      ['errorValidationViolentOrAggressiveBehaviourPage', () => this.errorValidationViolentOrAggressiveBehaviourPage(errorFlag as string)],
      ['errorValidationMoneyOwedPage', () => this.errorValidationMoneyOwedPage(errorFlag as string)],
      ['errorValidationLegalCostsPage', () => this.errorValidationLegalCostsPage(errorFlag as string)],
      ['errorValidationLandRegistryFeePage', () => this.errorValidationLandRegistryFeePage(errorFlag as string)],
      ['errorValidationRepaymentsPage', () => this.errorValidationRepaymentsPage(errorFlag as string)],
      ['errorValidationLanguageUsedPage', () => this.errorValidationLanguageUsedPage(errorFlag as string)],
      ['errorValidationVulnerablePeoplePage', () => this.errorValidationVulnerablePeoplePage(errorFlag as string)],
      ['errorValidationSuspendOrderPage', () => this.errorValidationSuspendOrderPage(errorFlag as string)],
      ['errorValidationSOT1Page', () => this.errorValidationSOT1Page(errorFlag as string)],
      ['errorValidationSOT2Page', () => this.errorValidationSOT2Page(errorFlag as string)],
      ['errorValidationSOTWritPage', () => this.errorValidationSOTWritPage(errorFlag as string)],
      ['errorValidationConfirmHCEOHiredPage', () => this.errorValidationConfirmHCEOHiredPage(errorFlag as string)],
      ['errorValidationYourHCEOPage', () => this.errorValidationYourHCEOPage(errorFlag as string)],
      ['errorValidationHowDefendantsEnteredPage', () => this.errorValidationHowDefendantsEnteredPage(errorFlag as string)],
      ['errorValidationExplainHowDefendantsEnteredPage', () => this.errorValidationExplainHowDefendantsEnteredPage(errorFlag as string)],
      ['errorValidationPeopleYouWantToEvictPage', () => this.errorValidationPeopleYouWantToEvictPage(errorFlag as string)],
      ['errorValidationPeopleWhoWillBeEvictedPage', () => this.errorValidationPeopleWhoWillBeEvictedPage(errorFlag as string)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async errorValidationYourApplicationPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: enforcementApplication.errorValidationType.three,
        inputArray: enforcementApplication.errorValidationField.errorRadioOption,
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.warrantOfPossessionRadioOption,
        button: enforcementApplication.continueButton
      });
    }
  }
  private async errorValidationNameAndAddressForEvictionPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: nameAndAddressForEviction.errorValidationType.three,
        inputArray: nameAndAddressForEviction.errorValidationField.errorRadioOption,
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.yesRadioOption,
        button: nameAndAddressForEviction.continueButton
      });
    }
  };

  private async errorValidationConfirmDefendantsDOBPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: confirmDefendantsDOB.errorValidationType.three,
        inputArray: confirmDefendantsDOB.errorValidationField.errorRadioOption,
        question: confirmDefendantsDOB.defendantsDOBQuestion,
        option: confirmDefendantsDOB.yesRadioOption,
        button: confirmDefendantsDOB.continueButton
      });
    }
  }

  private async errorValidationEnterDefendantsDOBPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: knownDefendantsDOBInformation.errorValidationType.two,
        inputArray: knownDefendantsDOBInformation.errorValidationField.errorTextField,
        header: knownDefendantsDOBInformation.eventCouldNotBeCreatedErrorMessageHeader,
        label: knownDefendantsDOBInformation.defendantsDOBQuestion,
        button: knownDefendantsDOBInformation.continueButton
      });
    }
  }

  private async errorValidationRiskPosedByEveryonePage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: evictionRisksPosed.errorValidationType.four,
        inputArray: evictionRisksPosed.errorValidationField.errorCheckBoxOption,
        label: evictionRisksPosed.kindOfRiskQuestion,
        checkBox: evictionRisksPosed.violentOrAggressiveBehaviourCheckbox,
        button: evictionRisksPosed.continueButton
      });
    }
  }

  private async errorValidationViolentOrAggressiveBehaviourPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: violentAggressiveRisk.errorValidationType.two,
        inputArray: violentAggressiveRisk.errorValidationField.errorTextField,
        header: violentAggressiveRisk.eventCouldNotBeCreatedErrorMessageHeader,
        label: violentAggressiveRisk.howHaveTheyBeenViolentAndAggressiveQuestion,
        button: violentAggressiveRisk.continueButton
      });
    }
  }

  private async errorValidationVulnerablePeoplePage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: vulnerableAdultsChildren.errorValidationType.three,
        inputArray: vulnerableAdultsChildren.errorValidationField.errorRadioOption1,
        question: vulnerableAdultsChildren.IsAnyOneLivingAtThePropertyQuestion,
        option: vulnerableAdultsChildren.yesRadioOption,
        button: vulnerableAdultsChildren.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: vulnerableAdultsChildren.errorValidationType.three,
        inputArray: vulnerableAdultsChildren.errorValidationField.errorRadioOption2,
        question: vulnerableAdultsChildren.confirmVulnerablePeopleHiddenQuestion,
        option: vulnerableAdultsChildren.vulnerableAdultsHiddenRadioOption,
        button: vulnerableAdultsChildren.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: vulnerableAdultsChildren.errorValidationType.two,
        inputArray: vulnerableAdultsChildren.errorValidationField.errorTextField,
        header: vulnerableAdultsChildren.eventCouldNotBeCreatedErrorMessageHeader,
        label: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextLabel,
        button: vulnerableAdultsChildren.continueButton
      });
    }
  }

  private async errorValidationMoneyOwedPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: moneyOwed.errorValidationType.one,
        inputArray: moneyOwed.errorValidationField.errorMoneyField,
        label: moneyOwed.totalAmountOwedTextLabel,
        button: moneyOwed.continueButton
      });
    }
  }

  private async errorValidationLegalCostsPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: legalCosts.errorValidationType.three,
        inputArray: legalCosts.errorValidationField.errorRadioOption,
        question: legalCosts.reclaimLegalCostsQuestion,
        option: legalCosts.yesRadioOption,
        button: legalCosts.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: legalCosts.errorValidationType.five,
        inputArray: legalCosts.errorValidationField.errorMoneyField,
        question: legalCosts.reclaimLegalCostsQuestion,
        option: legalCosts.yesRadioOption,
        option2: legalCosts.noRadioOption,
        label: legalCosts.howMuchYouWantToReclaimTextLabelHidden,
        button: legalCosts.continueButton
      });
    }
  }

  private async errorValidationLandRegistryFeePage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: landRegistryFees.errorValidationType.three,
        inputArray: landRegistryFees.errorValidationField.errorRadioOption,
        question: landRegistryFees.landRegistryFeeQuestion,
        option: landRegistryFees.yesRadioOption,
        button: landRegistryFees.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: landRegistryFees.errorValidationType.five,
        inputArray: landRegistryFees.errorValidationField.errorMoneyField,
        question: landRegistryFees.landRegistryFeeQuestion,
        option: landRegistryFees.yesRadioOption,
        option2: landRegistryFees.noRadioOption,
        label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabelHidden,
        button: landRegistryFees.continueButton
      });
    }
  }

  private async errorValidationRepaymentsPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: repayments.errorValidationType.three,
        inputArray: repayments.errorValidationField.errorRadioOption,
        question: repayments.rePaymentQuestion,
        option: repayments.someRadioOptions,
        button: repayments.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: repayments.errorValidationType.five,
        inputArray: repayments.errorValidationField.errorMoneyField,
        question: repayments.rePaymentQuestion,
        option: repayments.someRadioOptions,
        option2: repayments.noneRadioOptions,
        label: repayments.enterTheAmountTextLabelHidden,
        button: repayments.continueButton
      });

    }
  }

  private async errorValidationLanguageUsedPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: languageUsed.errorValidationType.three,
        inputArray: languageUsed.errorValidationField.errorRadioOption,
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.englishRadioOption,
        button: languageUsed.continueButton
      });

    }
  }

  private async errorValidationSuspendOrderPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: suspendedOrder.errorValidationType.three,
        inputArray: suspendedOrder.errorValidationField.errorRadioOption,
        question: suspendedOrder.suspendedOrderQuestion,
        option: suspendedOrder.yesRadioOption,
        button: suspendedOrder.continueButton
      });

    }
  }

  private async errorValidationSOT1Page(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.four,
        inputArray: statementOfTruth.errorValidationField.errorCheckBoxOption,
        label: statementOfTruth.checkBoxGenericErrorMessageHeader,
        checkBox: statementOfTruth.iCertifyCheckboxDynamic,
        button: statementOfTruth.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.three,
        inputArray: statementOfTruth.errorValidationField.errorRadioOption,
        question: statementOfTruth.completedByLabel,
        option: statementOfTruth.claimantRadioOption,
        button: statementOfTruth.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.four,
        inputArray: statementOfTruth.errorValidationField.errorCheckBoxOption,
        label: statementOfTruth.checkBoxGenericErrorMessageHeader,
        checkBox: statementOfTruth.iBelieveTheFactsHiddenCheckbox,
        button: statementOfTruth.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.two,
        inputArray: statementOfTruth.errorValidationField.errorTextField1,
        header: statementOfTruth.thereIsAProblemErrorMessageHeader,
        label: statementOfTruth.fullNameHiddenTextLabel,
        button: statementOfTruth.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.two,
        inputArray: statementOfTruth.errorValidationField.errorTextField3,
        header: statementOfTruth.thereIsAProblemErrorMessageHeader,
        label: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
        button: statementOfTruth.continueButton
      });
    }
  }

  private async errorValidationSOT2Page(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.four,
        inputArray: statementOfTruth.errorValidationField.errorCheckBoxOption,
        label: statementOfTruth.checkBoxGenericErrorMessageHeader,
        header: statementOfTruth.thereIsAProblemErrorMessageHeader,
        checkBox: statementOfTruth.iCertifyCheckboxDynamic,
        button: statementOfTruth.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.three,
        inputArray: statementOfTruth.errorValidationField.errorRadioOption,
        question: statementOfTruth.completedByLabel,
        option: statementOfTruth.claimantLegalRepresentativeRadioOption,
        button: statementOfTruth.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.four,
        inputArray: statementOfTruth.errorValidationField.errorCheckBoxOption,
        label: statementOfTruth.checkBoxGenericErrorMessageHeader,
        header: statementOfTruth.thereIsAProblemErrorMessageHeader,
        checkBox: statementOfTruth.signThisStatementHiddenCheckbox,
        button: statementOfTruth.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.two,
        inputArray: statementOfTruth.errorValidationField.errorTextField1,
        header: statementOfTruth.thereIsAProblemErrorMessageHeader,
        label: statementOfTruth.fullNameHiddenTextLabel,
        button: statementOfTruth.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.two,
        inputArray: statementOfTruth.errorValidationField.errorTextField2,
        header: statementOfTruth.thereIsAProblemErrorMessageHeader,
        label: statementOfTruth.nameOfFirmHiddenTextLabel,
        button: statementOfTruth.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.two,
        inputArray: statementOfTruth.errorValidationField.errorTextField3,
        header: statementOfTruth.thereIsAProblemErrorMessageHeader,
        label: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
        button: statementOfTruth.continueButton
      });
    }
  }

  private async errorValidationSOTWritPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.three,
        inputArray: statementOfTruth.errorValidationField.errorRadioOption,
        question: statementOfTruth.completedByLabel,
        option: statementOfTruth.claimantRadioOption,
        button: statementOfTruth.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.four,
        inputArray: statementOfTruth.errorValidationField.errorCheckBoxOption,
        label: statementOfTruth.checkBoxGenericErrorMessageHeader,
        checkBox: statementOfTruth.iBelieveTheFactsHiddenCheckbox,
        button: statementOfTruth.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.two,
        inputArray: statementOfTruth.errorValidationField.errorTextField1,
        header: statementOfTruth.thereIsAProblemErrorMessageHeader,
        label: statementOfTruth.fullNameHiddenTextLabel,
        button: statementOfTruth.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruth.errorValidationType.two,
        inputArray: statementOfTruth.errorValidationField.errorTextField3,
        header: statementOfTruth.thereIsAProblemErrorMessageHeader,
        label: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
        button: statementOfTruth.continueButton
      });
    }
  }

  private async errorValidationConfirmHCEOHiredPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: confirmHCEOfficer.errorValidationType.three,
        inputArray: confirmHCEOfficer.errorValidationField.errorRadioOption,
        question: confirmHCEOfficer.haveYouHiredHCEOQuestion,
        option: confirmHCEOfficer.yesRadioOption,
        button: confirmHCEOfficer.continueButton
      });
    }
  }

  private async errorValidationYourHCEOPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: yourHCEO.errorValidationType.two,
        inputArray: yourHCEO.errorValidationField.errorTextField,
        header: yourHCEO.thereIsAProblemErrorMessageHeader,
        label: yourHCEO.nameOfYourHCEOTextLabel,
        button: yourHCEO.continueButton
      });
    }
  }

  private async errorValidationHowDefendantsEnteredPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: explainHowDefendantsReturned.errorValidationType.two,
        inputArray: explainHowDefendantsReturned.errorValidationField.errorTextField,
        header: explainHowDefendantsReturned.eventCouldNotBeCreatedErrorMessageHeader,
        label: explainHowDefendantsReturned.howDidTheDefendantsReturnToThePropertyTextLabel,
        button: explainHowDefendantsReturned.continueButton
      });
    }
  }

  private async errorValidationExplainHowDefendantsEnteredPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: evidenceUpload.errorValidationType.seven,
        inputArray: evidenceUpload.errorValidationField.errorAddDocument,
        button: evidenceUpload.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: evidenceUpload.errorValidationType.eight,
        inputArray: evidenceUpload.errorValidationField.errorDropDown,
        docType: evidenceUpload.typeOfDocumentHiddenTextLabel,
        type: evidenceUpload.witnessStatementDropDownInput,
        button: evidenceUpload.continueButton
      });
      await performAction('inputErrorValidation', {
        validationType: evidenceUpload.errorValidationType.nine,
        inputArray: evidenceUpload.errorValidationField.errorUpload,
        docType: evidenceUpload.typeOfDocumentHiddenTextLabel,
        type: evidenceUpload.witnessStatementDropDownInput,
        label: evidenceUpload.documentUploadHiddenTextLabel,
        button: evidenceUpload.continueButton
      });

      await performAction('inputErrorValidation', {
        validationType: evidenceUpload.errorValidationType.two,
        inputArray: evidenceUpload.errorValidationField.errorTextField,
        header: evidenceUpload.thereIsAProblemErrorMessageHeader,
        label: evidenceUpload.shortDescriptionHiddenTextLabel,
        button: evidenceUpload.continueButton,
        buttonRemove: evidenceUpload.removeButton
      });
    }
  }

  private async errorValidationPeopleYouWantToEvictPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: peopleYouWantToEvict.errorValidationType.six,
        inputArray: peopleYouWantToEvict.errorValidationField.errorCheckBoxOption,
        label: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
        header: peopleYouWantToEvict.thereIsAProblemErrorMessageHeader,
        checkBox: defendantDetails[0],
        button: peopleYouWantToEvict.continueButton
      });
    }
  }

  private async errorValidationPeopleWhoWillBeEvictedPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: peopleWhoWillBeEvicted.errorValidationType.three,
        inputArray: peopleWhoWillBeEvicted.errorValidationField.errorRadioOption,
        question: peopleWhoWillBeEvicted.evictEveryOneQuestion,
        option: peopleWhoWillBeEvicted.yesRadioOption,
        button: peopleWhoWillBeEvicted.continueButton
      });
    }
  }
}
