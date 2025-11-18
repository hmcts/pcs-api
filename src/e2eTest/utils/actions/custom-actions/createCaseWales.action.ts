import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {Page} from '@playwright/test';
import {performAction, performActions, performValidation} from '@utils/controller';
import {claimantDetailsWales} from '@data/page-data/claimantDetailsWales.page.data';
import {addressInfo, caseNumber, CreateCaseAction} from "@utils/actions/custom-actions/createCase.action";
import {prohibitedConductStandardContractWales} from '@data/page-data/prohibitedConductStandardContractWales.page.data';
import {occupationContractOrLicenceDetailsWales} from '@data/page-data/occupationContractOrLicenceDetailsWales.page.data';
import {getAutoCollector} from '@utils/cya/auto-data-collector';
import {asbQuestionsWales} from '@data/page-data/asbQuestionsWales.page.data';

export class CreateCaseWalesAction extends CreateCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    // Set page in parent class (parent has private page property)
    (this as any).page = page;
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectClaimantDetails', () => this.selectClaimantDetails(fieldName as actionRecord)],
      ['selectProhibitedConductStandardContract', () => this.selectProhibitedConductStandardContract(fieldName as actionRecord)],
      ['selectOccupationContractOrLicenceDetails', () => this.selectOccupationContractOrLicenceDetails(fieldName as actionRecord)],
      ['selectAsbQuestions', () => this.selectAsbQuestions(fieldName as actionRecord)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectClaimantDetails(claimant: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: claimant.question1, option: claimant.option1});
    // Auto-collect question 1
    await getAutoCollector().collectRadioButtonAnswer((this as any).page, 'selectClaimantDetails', {option: claimant.option1}, claimant.question1 as string, claimantDetailsWales);
    if (claimant.option1 == claimantDetailsWales.yes) {
      await performAction('inputText', claimantDetailsWales.whatsYourRegistrationNumber, claimantDetailsWales.sampleTestRegistrationNumberInput);
      // Auto-collect conditional registration number
      await getAutoCollector().collectTextInputAnswer((this as any).page, 'selectClaimantDetails', claimantDetailsWales.whatsYourRegistrationNumber, claimantDetailsWales.sampleTestRegistrationNumberInput, claimantDetailsWales);
    }
    await performAction('clickRadioButton', {question: claimant.question2, option: claimant.option2});
    // Auto-collect question 2
    await getAutoCollector().collectRadioButtonAnswer((this as any).page, 'selectClaimantDetails', {option: claimant.option2}, claimant.question2 as string, claimantDetailsWales);
    if (claimant.option2 == claimantDetailsWales.yes) {
      await performAction('inputText', claimantDetailsWales.whatsYourLicenseNumber, claimantDetailsWales.sampleTestLicenseNumberInput);
      // Auto-collect conditional license number
      await getAutoCollector().collectTextInputAnswer((this as any).page, 'selectClaimantDetails', claimantDetailsWales.whatsYourLicenseNumber, claimantDetailsWales.sampleTestLicenseNumberInput, claimantDetailsWales);
    }
    await performAction('clickRadioButton', {question: claimant.question3, option: claimant.option3});
    // Auto-collect question 3
    await getAutoCollector().collectRadioButtonAnswer((this as any).page, 'selectClaimantDetails', {option: claimant.option3}, claimant.question3 as string, claimantDetailsWales);
    if (claimant.option3 == claimantDetailsWales.yes) {
      await performAction('inputText', claimantDetailsWales.agentsFirstnameLabel, claimantDetailsWales.agentsFirstnameInput);
      // Auto-collect agent first name
      await getAutoCollector().collectTextInputAnswer((this as any).page, 'selectClaimantDetails', claimantDetailsWales.agentsFirstnameLabel, claimantDetailsWales.agentsFirstnameInput, claimantDetailsWales);
      await performAction('inputText', claimantDetailsWales.agentsLastnameLabel, claimantDetailsWales.agentsLastnameInput);
      // Auto-collect agent last name
      await getAutoCollector().collectTextInputAnswer((this as any).page, 'selectClaimantDetails', claimantDetailsWales.agentsLastnameLabel, claimantDetailsWales.agentsLastnameInput, claimantDetailsWales);
      await performAction('inputText', claimantDetailsWales.agentsLicenseNumberLabel, claimantDetailsWales.agentsLicenseNumberInput);
      // Auto-collect agent license number
      await getAutoCollector().collectTextInputAnswer((this as any).page, 'selectClaimantDetails', claimantDetailsWales.agentsLicenseNumberLabel, claimantDetailsWales.agentsLicenseNumberInput, claimantDetailsWales);
      await performActions('Enter Date',
        ['inputText', claimantDetailsWales.dayLabel, claimantDetailsWales.dayInput],
        ['inputText', claimantDetailsWales.monthLabel, claimantDetailsWales.monthInput],
        ['inputText', claimantDetailsWales.yearLabel, claimantDetailsWales.yearInput]);
      // Auto-collect date
      await getAutoCollector().collectDateAnswer((this as any).page, 'selectClaimantDetails', claimantDetailsWales.dayInput, claimantDetailsWales.monthInput, claimantDetailsWales.yearInput, 'Date', claimantDetailsWales);
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
    // Auto-collect occupation contract type
    await getAutoCollector().collectRadioButtonAnswer((this as any).page, 'selectOccupationContractOrLicenceDetails', {option: occupationContractData.occupationContractType}, occupationContractData.occupationContractQuestion as string, occupationContractOrLicenceDetailsWales);
    if (occupationContractData.occupationContractType === occupationContractOrLicenceDetailsWales.other) {
      await performAction('inputText', occupationContractOrLicenceDetailsWales.giveDetailsOfTypeOfOccupationContractAgreementLabel, occupationContractOrLicenceDetailsWales.detailsOfLicenceInput);
      // Auto-collect conditional details
      await getAutoCollector().collectTextInputAnswer((this as any).page, 'selectOccupationContractOrLicenceDetails', occupationContractOrLicenceDetailsWales.giveDetailsOfTypeOfOccupationContractAgreementLabel, occupationContractOrLicenceDetailsWales.detailsOfLicenceInput, occupationContractOrLicenceDetailsWales);
    }
    if (occupationContractData.day && occupationContractData.month && occupationContractData.year) {
      await performActions(
        'Enter Date',
        ['inputText', occupationContractOrLicenceDetailsWales.dayLabel, occupationContractData.day],
        ['inputText', occupationContractOrLicenceDetailsWales.monthLabel, occupationContractData.month],
        ['inputText', occupationContractOrLicenceDetailsWales.yearLabel, occupationContractData.year]);
      // Auto-collect date
      await getAutoCollector().collectDateAnswer((this as any).page, 'selectOccupationContractOrLicenceDetails', occupationContractData.day as string | number, occupationContractData.month as string | number, occupationContractData.year as string | number, 'When did the occupation contract begin?', occupationContractOrLicenceDetailsWales);
    }
    if (occupationContractData.files) {
      await performAction('uploadFile', occupationContractData.files);
      // Auto-collect file uploads
      const files = Array.isArray(occupationContractData.files) ? occupationContractData.files : [occupationContractData.files as string];
      await getAutoCollector().collectFileUploadAnswer((this as any).page, 'selectOccupationContractOrLicenceDetails', files, 'Uploaded documents', occupationContractOrLicenceDetailsWales);
    }
    await performAction('clickButton', occupationContractOrLicenceDetailsWales.continue);
  }

  private async selectProhibitedConductStandardContract(prohibitedConduct: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + caseNumber});
    await performValidation('text', {elementType: 'paragraph', text: 'Property address: '+addressInfo.buildingStreet+', '+addressInfo.townCity+', '+addressInfo.engOrWalPostcode});
    await performAction('clickRadioButton', {question: prohibitedConduct.question1, option: prohibitedConduct.option1});
    // Auto-collect question 1
    await getAutoCollector().collectRadioButtonAnswer((this as any).page, 'selectProhibitedConductStandardContract', {option: prohibitedConduct.option1}, prohibitedConduct.question1 as string, prohibitedConductStandardContractWales);
    if (prohibitedConduct.option1 == prohibitedConductStandardContractWales.yes) {
      await performAction('inputText', prohibitedConduct.label1, prohibitedConduct.input1);
      // Auto-collect conditional input 1
      await getAutoCollector().collectTextInputAnswer((this as any).page, 'selectProhibitedConductStandardContract', prohibitedConduct.label1 as string, prohibitedConduct.input1 as string, prohibitedConductStandardContractWales);
      await performAction('clickRadioButton', {question: prohibitedConduct.question2, option: prohibitedConduct.option2});
      // Auto-collect question 2
      await getAutoCollector().collectRadioButtonAnswer((this as any).page, 'selectProhibitedConductStandardContract', {option: prohibitedConduct.option2}, prohibitedConduct.question2 as string, prohibitedConductStandardContractWales);
      if (prohibitedConduct.option2 == prohibitedConductStandardContractWales.yes) {
        await performAction('inputText', prohibitedConduct.label2, prohibitedConduct.input2);
        // Auto-collect conditional input 2
        await getAutoCollector().collectTextInputAnswer((this as any).page, 'selectProhibitedConductStandardContract', prohibitedConduct.label2 as string, prohibitedConduct.input2 as string, prohibitedConductStandardContractWales);
      }
    }
    await performAction('clickButton', claimantDetailsWales.continue);
  }

  private async selectAsbQuestions(asbQuestions: actionRecord) {
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
