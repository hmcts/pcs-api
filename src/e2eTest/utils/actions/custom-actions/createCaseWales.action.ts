import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
import {addressInfo, caseNumber, CreateCaseAction} from "@utils/actions/custom-actions/createCase.action";
import {
  // migration (page-data → page-data-figma)
  claimantDetailsWales,
  occupationLicenceDetailsWales,
  prohibitedConductWales
} from '@data/page-data-figma';
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
    if (claimant.option1 == claimantDetailsWales.yesRadioOption) {
      await performAction('inputText', claimantDetailsWales.whatsYourRegistrationNumberHiddenTextLabel, claimantDetailsWales.whatsYourRegistrationNumberHiddenTextInput);
    }
    await performAction('clickRadioButton', {question: claimant.question2, option: claimant.option2});
    if (claimant.option2 == claimantDetailsWales.yesRadioOption) {
      await performAction('inputText', claimantDetailsWales.whatsYourLicenceNumberHiddenTextLabel, claimantDetailsWales.whatsYourLicenceNumberHiddenTextInput);
    }
    await performAction('clickRadioButton', {question: claimant.question3, option: claimant.option3});
    if (claimant.option3 == claimantDetailsWales.yesRadioOption) {
      await performAction('inputText', claimantDetailsWales.agentsFirstNameHiddenTextLabel, claimantDetailsWales.agentsFirstNameHiddenTextInput);
      await performAction('inputText', claimantDetailsWales.agentsLastNameHiddenTextLabel, claimantDetailsWales.agentsLastNameHiddenTextInput);
      await performAction('inputText', claimantDetailsWales.agentsLicenceNumberHiddenTextLabel, claimantDetailsWales.agentsLicenceNumberHiddenTextInput);
      await performActions('Enter Date',
        ['inputText', claimantDetailsWales.dayHiddenTextLabel, claimantDetailsWales.dayHiddenTextInput],
        ['inputText', claimantDetailsWales.monthHiddenTextLabel, claimantDetailsWales.monthHiddenTextInput],
        ['inputText', claimantDetailsWales.yearHiddenTextLabel, claimantDetailsWales.yearHiddenTextInput]);
    }
    await performAction('clickButton', claimantDetailsWales.continueButton);
  }

  private async selectOccupationContractOrLicenceDetails(occupationContractData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {
      question: occupationContractData.occupationContractQuestion,
      option: occupationContractData.occupationContractType
    });
    if (occupationContractData.occupationContractType === occupationLicenceDetailsWales.otherRadioOption) {
      await performAction('inputText', occupationLicenceDetailsWales.GiveDetailsAboutHiddenTextLabel, occupationLicenceDetailsWales.GiveDetailsAboutHiddenTextInput);
    }
    if (occupationContractData.day && occupationContractData.month && occupationContractData.year) {
      await performActions(
        'Enter Date',
        ['inputText', occupationLicenceDetailsWales.dayTextLabel, occupationContractData.day],
        ['inputText', occupationLicenceDetailsWales.monthTextLabel, occupationContractData.month],
        ['inputText', occupationLicenceDetailsWales.yearTextLabel, occupationContractData.year]);
    }
    if (occupationContractData.files) {
      await performAction('uploadFile', occupationContractData.files);
    }
    await performAction('clickButton', occupationLicenceDetailsWales.continueButton);
  }

  private async selectProhibitedConductStandardContract(prohibitedConduct: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: prohibitedConduct.question1, option: prohibitedConduct.option1});
    if (prohibitedConduct.option1 == prohibitedConductWales.yesRadioOption) {
      await performAction('inputText', prohibitedConduct.label1, prohibitedConduct.input1);
      await performAction('clickRadioButton', {question: prohibitedConduct.question2, option: prohibitedConduct.option2});
      if (prohibitedConduct.option2 == prohibitedConductWales.yesRadioOption) {
        await performAction('inputText', prohibitedConduct.label2, prohibitedConduct.input2);
      }
    }
    await performAction('clickButton', prohibitedConductWales.continueButton);
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
