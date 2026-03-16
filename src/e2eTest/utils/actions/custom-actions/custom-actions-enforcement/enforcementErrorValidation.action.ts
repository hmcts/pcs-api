import { confirmDefendantsDOB, confirmHCEOHired, enterDefendantsDOB, evidenceUpload, explainHowDefendantsReturned, landRegistryFees, languageUsed, legalCosts, moneyOwed, nameAndAddressForEviction, peopleYouWantToEvict, rePayments, riskPosedByEveryoneAtProperty, statementOfTruthOne, statementOfTruthTwo, suspendedOrder, violentOrAggressiveBehaviour, vulnerableAdultsAndChildren, yourApplication, yourHCEO } from '@data/page-data/page-data-enforcement';
import { expect, Page } from '@playwright/test';
import { performAction, performValidation } from '@utils/controller-enforcement';
import { IAction, actionData, actionRecord } from '@utils/interfaces/action.interface';
import { defendantDetails } from './enforcement.action';

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
      ['errorValidationConfirmHCEOHiredPage', () => this.errorValidationConfirmHCEOHiredPage(errorFlag as string)],
      ['errorValidationYourHCEOPage', () => this.errorValidationYourHCEOPage(errorFlag as string)],
      ['errorValidationHowDefendantsEnteredPage', () => this.errorValidationHowDefendantsEnteredPage(errorFlag as string)],
      ['errorValidationExplainHowDefendantsEnteredPage', () => this.errorValidationExplainHowDefendantsEnteredPage(errorFlag as string)],
      ['errorValidationPeopleYouWantToEvictPage', () => this.errorValidationPeopleYouWantToEvictPage(errorFlag as string)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async errorValidationYourApplicationPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {
        validationType: yourApplication.errorValidationType.three,
        inputArray: yourApplication.errorValidationField.errorRadioOption,
        question: yourApplication.typeOfApplicationQuestion,
        option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
        button: yourApplication.continueButton
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

        validationType: enterDefendantsDOB.errorValidationType.two,
        inputArray: enterDefendantsDOB.errorValidationField.errorTextField,
        header: enterDefendantsDOB.eventCouldNotBeCreatedErrorMessage,
        label: enterDefendantsDOB.defendantsDOBTextLabel,
        button: enterDefendantsDOB.continueButton
      });
    }
  }

  private async errorValidationRiskPosedByEveryonePage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: riskPosedByEveryoneAtProperty.errorValidationType.four,
        inputArray: riskPosedByEveryoneAtProperty.errorValidationField.errorCheckBoxOption,
        label: riskPosedByEveryoneAtProperty.kindOfRiskQuestion,
        checkBox: riskPosedByEveryoneAtProperty.violentOrAggressiveBehaviourCheckbox,
        button: riskPosedByEveryoneAtProperty.continueButton
      });
    }
  }

  private async errorValidationViolentOrAggressiveBehaviourPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: violentOrAggressiveBehaviour.errorValidationType.two,
        inputArray: violentOrAggressiveBehaviour.errorValidationField.errorTextField,
        header: violentOrAggressiveBehaviour.eventCouldNotBeCreatedErrorMessage,
        label: violentOrAggressiveBehaviour.howHaveTheyBeenViolentAndAggressiveTextLabel,
        button: violentOrAggressiveBehaviour.continueButton
      });
    }
  }

  private async errorValidationVulnerablePeoplePage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: vulnerableAdultsAndChildren.errorValidationType.three,
        inputArray: vulnerableAdultsAndChildren.errorValidationField.errorRadioOption1,
        question: vulnerableAdultsAndChildren.IsAnyOneLivingAtThePropertyQuestion,
        option: vulnerableAdultsAndChildren.yesRadioOption,
        button: vulnerableAdultsAndChildren.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: vulnerableAdultsAndChildren.errorValidationType.three,
        inputArray: vulnerableAdultsAndChildren.errorValidationField.errorRadioOption2,
        question: vulnerableAdultsAndChildren.confirmVulnerablePeopleQuestion,
        option: vulnerableAdultsAndChildren.vulnerableAdultsRadioOption,
        button: vulnerableAdultsAndChildren.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: vulnerableAdultsAndChildren.errorValidationType.two,
        inputArray: vulnerableAdultsAndChildren.errorValidationField.errorTextField,
        header: vulnerableAdultsAndChildren.eventCouldNotBeCreatedErrorMessage,
        label: vulnerableAdultsAndChildren.howAreTheyVulnerableTextLabel,
        button: vulnerableAdultsAndChildren.continueButton
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
        label: legalCosts.howMuchYouWantToReclaimTextLabel,
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
        label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabel,
        button: landRegistryFees.continueButton
      });
    }
  }

  private async errorValidationRepaymentsPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: rePayments.errorValidationType.three,
        inputArray: rePayments.errorValidationField.errorRadioOption,
        question: rePayments.rePaymentQuestion,
        option: rePayments.rePaymentRadioOptions.some,
        button: rePayments.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: rePayments.errorValidationType.five,
        inputArray: rePayments.errorValidationField.errorMoneyField,
        question: rePayments.rePaymentQuestion,
        option: rePayments.rePaymentRadioOptions.some,
        option2: rePayments.rePaymentRadioOptions.none,
        label: rePayments.enterTheAmountTextLabel,
        button: rePayments.continueButton
      });

    }
  }

  private async errorValidationLanguageUsedPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: languageUsed.errorValidationType.three,
        inputArray: languageUsed.errorValidationField.errorRadioOption,
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.languageUsedRadioOptions.englishRadioOption,
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

        validationType: statementOfTruthOne.errorValidationType.four,
        inputArray: statementOfTruthOne.errorValidationField.errorCheckBoxOption,
        label: statementOfTruthOne.checkBoxGenericErrorLabel,
        checkBox: statementOfTruthOne.iCertifyCheckbox,
        button: statementOfTruthOne.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruthOne.errorValidationType.three,
        inputArray: statementOfTruthOne.errorValidationField.errorRadioOption,
        question: statementOfTruthOne.completedByLabel,
        option: statementOfTruthOne.claimantRadioOption,
        button: statementOfTruthOne.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruthOne.errorValidationType.four,
        inputArray: statementOfTruthOne.errorValidationField.errorCheckBoxOption,
        label: statementOfTruthOne.checkBoxGenericErrorLabel,
        checkBox: statementOfTruthOne.iBelieveTheFactsHiddenCheckbox,
        button: statementOfTruthOne.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruthOne.errorValidationType.two,
        inputArray: statementOfTruthOne.errorValidationField.errorTextField1,
        header: statementOfTruthOne.thereIsAProblemErrorMessageHeader,
        label: statementOfTruthOne.fullNameHiddenTextLabel,
        button: statementOfTruthOne.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruthOne.errorValidationType.two,
        inputArray: statementOfTruthOne.errorValidationField.errorTextField3,
        header: statementOfTruthOne.thereIsAProblemErrorMessageHeader,
        label: statementOfTruthOne.positionOrOfficeHeldHiddenTextLabel,
        button: statementOfTruthOne.continueButton
      });
    }
  }

  private async errorValidationSOT2Page(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: statementOfTruthTwo.errorValidationType.four,
        inputArray: statementOfTruthTwo.errorValidationField.errorCheckBoxOption,
        label: statementOfTruthTwo.checkBoxGenericErrorLabel,
        header: statementOfTruthTwo.thereIsAProblemErrorMessageHeader,
        checkBox: statementOfTruthTwo.iCertifyCheckbox,
        button: statementOfTruthTwo.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruthTwo.errorValidationType.three,
        inputArray: statementOfTruthTwo.errorValidationField.errorRadioOption,
        question: statementOfTruthTwo.completedByLabel,
        option: statementOfTruthTwo.claimantLegalRepresentativeRadioOption,
        button: statementOfTruthTwo.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruthTwo.errorValidationType.four,
        inputArray: statementOfTruthTwo.errorValidationField.errorCheckBoxOption,
        label: statementOfTruthTwo.checkBoxGenericErrorLabel,
        header: statementOfTruthTwo.thereIsAProblemErrorMessageHeader,
        checkBox: statementOfTruthTwo.signThisStatementHiddenCheckbox,
        button: statementOfTruthTwo.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruthTwo.errorValidationType.two,
        inputArray: statementOfTruthTwo.errorValidationField.errorTextField1,
        header: statementOfTruthTwo.thereIsAProblemErrorMessageHeader,
        label: statementOfTruthTwo.fullNameHiddenTextLabel,
        button: statementOfTruthTwo.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruthTwo.errorValidationType.two,
        inputArray: statementOfTruthTwo.errorValidationField.errorTextField2,
        header: statementOfTruthTwo.thereIsAProblemErrorMessageHeader,
        label: statementOfTruthTwo.nameOfFirmHiddenTextLabel,
        button: statementOfTruthTwo.continueButton
      });
      await performAction('inputErrorValidation', {

        validationType: statementOfTruthTwo.errorValidationType.two,
        inputArray: statementOfTruthTwo.errorValidationField.errorTextField3,
        header: statementOfTruthTwo.thereIsAProblemErrorMessageHeader,
        label: statementOfTruthTwo.positionOrOfficeHeldHiddenTextLabel,
        button: statementOfTruthTwo.continueButton
      });
    }
  }

  private async errorValidationConfirmHCEOHiredPage(validationReq: string) {
    if (validationReq === 'YES') {
      await performAction('inputErrorValidation', {

        validationType: confirmHCEOHired.errorValidationType.three,
        inputArray: confirmHCEOHired.errorValidationField.errorRadioOption,
        question: confirmHCEOHired.haveYouHiredHCEOQuestion,
        option: confirmHCEOHired.yesRadioOption,
        button: confirmHCEOHired.continueButton
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
        header: explainHowDefendantsReturned.eventCouldNotBeCreatedErrorMessage,
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
}
