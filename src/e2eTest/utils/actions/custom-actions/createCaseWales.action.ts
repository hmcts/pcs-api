import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
import {claimantDetailsWales} from '@data/page-data/claimantDetailsWales.page.data';
import {addressInfo, caseNumber, CreateCaseAction} from "@utils/actions/custom-actions/createCase.action";
import {prohibitedConductStandardContractWales} from '@data/page-data/prohibitedConductStandardContractWales.page.data';
import {occupationContractOrLicenceDetailsWales} from '@data/page-data/occupationContractOrLicenceDetailsWales.page.data';
import {asbQuestionsWales} from '@data/page-data/asbQuestionsWales.page.data';

export class CreateCaseWalesAction extends CreateCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectClaimantDetails', () => this.selectClaimantDetails(fieldName as actionRecord)],
      ['selectProhibitedConductStandardContract', () => this.selectProhibitedConductStandardContract(fieldName as actionRecord)],
      ['selectOccupationContractOrLicenceDetails', () => this.selectOccupationContractOrLicenceDetails(fieldName as actionRecord)],
      ['selectAsb', () => this.selectAsb(fieldName as actionRecord)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectClaimantDetails(claimant: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: claimant.question1, option: claimant.option1});
    if (claimant.option1 == claimantDetailsWales.yes) {
      await performAction('inputText', claimantDetailsWales.whatsYourRegistrationNumber, claimantDetailsWales.sampleTestRegistrationNumberInput);
    }
    await performAction('clickRadioButton', {question: claimant.question2, option: claimant.option2});
    if (claimant.option2 == claimantDetailsWales.yes) {
      await performAction('inputText', claimantDetailsWales.whatsYourLicenseNumber, claimantDetailsWales.sampleTestLicenseNumberInput);
    }
    await performAction('clickRadioButton', {question: claimant.question3, option: claimant.option3});
    if (claimant.option3 == claimantDetailsWales.yes) {
      await performAction('inputText', claimantDetailsWales.agentsFirstnameLabel, claimantDetailsWales.agentsFirstnameInput);
      await performAction('inputText', claimantDetailsWales.agentsLastnameLabel, claimantDetailsWales.agentsLastnameInput);
      await performAction('inputText', claimantDetailsWales.agentsLicenseNumberLabel, claimantDetailsWales.agentsLicenseNumberInput);
      await performActions('Enter Date',
        ['inputText', claimantDetailsWales.dayLabel, claimantDetailsWales.dayInput],
        ['inputText', claimantDetailsWales.monthLabel, claimantDetailsWales.monthInput],
        ['inputText', claimantDetailsWales.yearLabel, claimantDetailsWales.yearInput]);
    }
    await performAction('clickButton', claimantDetailsWales.continue);
  }

  private async selectOccupationContractOrLicenceDetails(occupationContractData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: occupationContractData.occupationContractQuestion,
      option: occupationContractData.occupationContractType
    });
    if (occupationContractData.occupationContractType === occupationContractOrLicenceDetailsWales.other) {
      await performAction('inputText', occupationContractOrLicenceDetailsWales.giveDetailsOfTypeOfOccupationContractAgreementLabel, occupationContractOrLicenceDetailsWales.detailsOfLicenceInput);
    }
    if (occupationContractData.day && occupationContractData.month && occupationContractData.year) {
      await performActions(
        'Enter Date',
        ['inputText', occupationContractOrLicenceDetailsWales.dayLabel, occupationContractData.day],
        ['inputText', occupationContractOrLicenceDetailsWales.monthLabel, occupationContractData.month],
        ['inputText', occupationContractOrLicenceDetailsWales.yearLabel, occupationContractData.year]);
    }
    if (occupationContractData.files) {
      await performAction('uploadFile', occupationContractData.files);
    }
    await performAction('clickButton', occupationContractOrLicenceDetailsWales.continue);
  }

  private async selectProhibitedConductStandardContract(prohibitedConduct: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: prohibitedConduct.question1, option: prohibitedConduct.option1});
    if (prohibitedConduct.option1 == prohibitedConductStandardContractWales.yes) {
      await performAction('inputText', prohibitedConduct.label1, prohibitedConduct.input1);
      await performAction('clickRadioButton', {question: prohibitedConduct.question2, option: prohibitedConduct.option2});
      if (prohibitedConduct.option2 == prohibitedConductStandardContractWales.yes) {
        await performAction('inputText', prohibitedConduct.label2, prohibitedConduct.input2);
      }
    }
    await performAction('clickButton', claimantDetailsWales.continue);
  }

  private async selectAsb(asbQuestions: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {
      elementType: 'paragraph',
      text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}`
    });
    await performAction('clickRadioButton', {
      question: asbQuestionsWales.isThereActualOrThreatenedAsbQuestion,
      option: asbQuestions.asbChoice
    });
    if (asbQuestions.asbChoice == asbQuestionsWales.yesRadioOption) {
      await performAction('inputText', asbQuestionsWales.giveDetailsOfAsbHiddenTextLabel, asbQuestions.giveDetailsOfAsb);
    }
    await performAction('clickRadioButton', {
      question: asbQuestionsWales.isThereActualIllegalPurposesQuestion,
      option: asbQuestions.illegalPurposesChoice
    });
    if (asbQuestions.illegalPurposesChoice === asbQuestionsWales.yesRadioOption) {
      await performAction('inputText', asbQuestionsWales.giveDetailsOfIllegalHiddenTextLabel, asbQuestions.giveDetailsOfIllegal);
    }
    await performAction('clickRadioButton', {
      question: asbQuestionsWales.hasThereBeenOtherProhibitedQuestion,
      option: asbQuestions.prohibitedConductChoice
    });
    if (asbQuestions.prohibitedConductChoice === asbQuestionsWales.yesRadioOption) {
      await performAction('inputText', asbQuestionsWales.giveDetailsOfTheOtherHiddenTextLabel, asbQuestions.giveDetailsOfTheOther);
    }
    await performAction('clickButton', asbQuestionsWales.continueButton);
  }
}
