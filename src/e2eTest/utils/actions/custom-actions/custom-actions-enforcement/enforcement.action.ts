import { expect, Page } from '@playwright/test';
import { performAction, performValidation } from '@utils/controller-enforcement';
import { IAction, actionData, actionRecord } from '@utils/interfaces/action.interface';
import {
  yourApplication,
  nameAndAddressForEviction,
  everyoneLivingAtTheProperty,
  vulnerableAdultsAndChildren,
  violentOrAggressiveBehaviour,
  firearmPossession,
  criminalOrAntisocialBehaviour,
  riskPosedByEveryoneAtProperty,
  verbalOrWrittenThreats,
  groupProtestsEviction,
  policeOrSocialServiceVisit,
  animalsAtTheProperty,
  anythingElseHelpWithEviction,
  accessToTheProperty,
  peopleWillBeEvicted,
  youNeedPermission,
  legalCosts,
  landRegistryFees,
  rePayments,
  peopleYouWantToEvict,
  moneyOwed,
  languageUsed
} from '@data/page-data/page-data-enforcement';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';

export const addressInfo = {
  buildingStreet: createCaseApiData.createCasePayload.propertyAddress.AddressLine1,
  townCity: createCaseApiData.createCasePayload.propertyAddress.PostTown,
  engOrWalPostcode: createCaseApiData.createCasePayload.propertyAddress.PostCode
};

export let defendantDetails: string[] = [];
export class EnforcementAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['validateWritOrWarrantFeeAmount', () => this.validateWritOrWarrantFeeAmount(fieldName as actionRecord)],
      ['validateGetQuoteFromBailiffLink', () => this.validateGetQuoteFromBailiffLink(fieldName as actionRecord)],
      ['selectApplicationType', () => this.selectApplicationType(fieldName as actionRecord)],
      ['selectNameAndAddressForEviction', () => this.selectNameAndAddressForEviction(page, fieldName as actionRecord)],
      ['selectEveryoneLivingAtTheProperty', () => this.selectEveryoneLivingAtTheProperty(fieldName as actionRecord)],
      ['selectPermissionFromJudge', () => this.selectPermissionFromJudge(page)],
      ['getDefendantDetails', () => this.getDefendantDetails(fieldName as actionRecord)],
      ['selectPeopleWhoWillBeEvicted', () => this.selectPeopleWhoWillBeEvicted(fieldName as actionRecord)],
      ['selectPeopleYouWantToEvict', () => this.selectPeopleYouWantToEvict(fieldName as actionRecord)],
      ['selectRiskPosedByEveryoneAtProperty', () => this.selectRiskPosedByEveryoneAtProperty(fieldName as actionRecord)],
      ['provideDetailsViolentOrAggressiveBehaviour', () => this.provideDetailsViolentOrAggressiveBehaviour(fieldName as actionRecord)],
      ['provideDetailsFireArmPossession', () => this.provideDetailsFireArmPossession(fieldName as actionRecord)],
      ['provideDetailsCriminalOrAntisocialBehavior', () => this.provideDetailsCriminalOrAntisocialBehavior(fieldName as actionRecord)],
      ['provideDetailsVerbalOrWrittenThreats', () => this.provideDetailsVerbalOrWrittenThreats(fieldName as actionRecord)],
      ['provideDetailsGroupProtestsEviction', () => this.provideDetailsGroupProtestsEviction(fieldName as actionRecord)],
      ['provideDetailsPoliceOrSocialServiceVisits', () => this.provideDetailsPoliceOrSocialServiceVisits(fieldName as actionRecord)],
      ['provideDetailsAnimalsAtTheProperty', () => this.provideDetailsAnimalsAtTheProperty(fieldName as actionRecord)],
      ['selectVulnerablePeopleInTheProperty', () => this.selectVulnerablePeopleInTheProperty(fieldName as actionRecord)],
      ['provideDetailsAnythingElseHelpWithEviction', () => this.provideDetailsAnythingElseHelpWithEviction(fieldName as actionRecord)],
      ['accessToProperty', () => this.accessToProperty(fieldName as actionRecord)],
      ['provideMoneyOwed', () => this.provideMoneyOwed(fieldName as actionRecord)],
      ['provideLegalCosts', () => this.provideLegalCosts(fieldName as actionRecord)],
      ['provideLandRegistryFees', () => this.provideLandRegistryFees(fieldName as actionRecord)],
      ['selectLanguageUsed', () => this.selectLanguageUsed(fieldName as actionRecord)],
      ['inputErrorValidation', () => this.inputErrorValidation(fieldName as actionRecord)],
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
  }

  private async validateGetQuoteFromBailiffLink(bailiffQuote: actionRecord) {
    await performAction('expandSummary', bailiffQuote.type);
    await performAction('clickLinkAndVerifyNewTabTitle', bailiffQuote.link, bailiffQuote.newPage);
    await performAction('expandSummary', bailiffQuote.type);
  }

  private async selectApplicationType(applicationType: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: applicationType.question, option: applicationType.option });
    await performAction('clickButton', yourApplication.continueButton);
  }

  private async getDefendantDetails(defendantsDetails: actionRecord) {

    if (defendantsDetails.defendant1NameKnown === 'YES') {
      defendantDetails.push(
        `${submitCaseApiData.submitCasePayload.defendant1.firstName} ${submitCaseApiData.submitCasePayload.defendant1.lastName}`
      );
    };

    if (defendantsDetails.additionalDefendants === 'YES') {
      submitCaseApiData.submitCasePayload.additionalDefendants.forEach(defendant => {
        if (defendant.value.nameKnown === 'YES') {
          defendantDetails.push(`${defendant.value.firstName} ${defendant.value.lastName}`);
        }
      });
    };

  }

  private async selectNameAndAddressForEviction(page: Page, nameAndAddress: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    if (nameAndAddress.defendant1NameKnown === 'YES' && defendantDetails.length) {
      await performValidation('formLabelValue', nameAndAddressForEviction.subHeaderDefendants, defendantDetails.join(' '));
    }
    await performValidation('formLabelValue', nameAndAddressForEviction.subHeaderAddress, `${addressInfo.buildingStreet}${addressInfo.townCity}${addressInfo.engOrWalPostcode}`);
    await performAction('clickRadioButton', { question: nameAndAddress.question, option: nameAndAddress.option });
    await performAction('clickButton', nameAndAddressForEviction.continueButton);
  }

  private async selectPeopleWhoWillBeEvicted(evictPeople: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: evictPeople.question, option: evictPeople.option });
    await performAction('clickButton', peopleWillBeEvicted.continueButton);
  }

  private async selectPeopleYouWantToEvict(peopleYouWantEvicted: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('check', peopleYouWantEvicted.defendants);
    await performAction('clickButton', peopleYouWantToEvict.continueButton);
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

  private async selectEveryoneLivingAtTheProperty(riskToBailiff: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: riskToBailiff.question, option: riskToBailiff.option });
    await performAction('clickButton', everyoneLivingAtTheProperty.continueButton);
  }

  private async selectRiskPosedByEveryoneAtProperty(riskCategory: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('check', riskCategory.riskTypes);
    await performAction('clickButton', riskPosedByEveryoneAtProperty.continueButton);
  }

  private async provideDetailsViolentOrAggressiveBehaviour(violentAggressiveBehaviour: actionRecord) {
    await performValidation('mainHeader', violentOrAggressiveBehaviour.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', violentAggressiveBehaviour.label, violentAggressiveBehaviour.input);
    await performAction('clickButton', violentOrAggressiveBehaviour.continue);
  }

  private async provideDetailsFireArmPossession(firearm: actionRecord) {
    await performValidation('mainHeader', firearmPossession.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', firearm.label, firearm.input);
    await performAction('clickButton', firearmPossession.continue);
  }

  private async provideDetailsCriminalOrAntisocialBehavior(criminalAntisocialBehaviour: actionRecord) {
    await performValidation('mainHeader', criminalOrAntisocialBehaviour.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', criminalAntisocialBehaviour.label, criminalAntisocialBehaviour.input);
    await performAction('clickButton', criminalOrAntisocialBehaviour.continue);
  }

  private async provideDetailsVerbalOrWrittenThreats(verbalWritten: actionRecord) {
    await performValidation('mainHeader', verbalOrWrittenThreats.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', verbalWritten.label, verbalWritten.input);
    await performAction('clickButton', verbalOrWrittenThreats.continue);
  }

  private async provideDetailsGroupProtestsEviction(protestGroup: actionRecord) {
    await performValidation('mainHeader', groupProtestsEviction.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', protestGroup.label, protestGroup.input);
    await performAction('clickButton', groupProtestsEviction.continue);
  }

  private async provideDetailsPoliceOrSocialServiceVisits(policeOrSSVisit: actionRecord) {
    await performValidation('mainHeader', policeOrSocialServiceVisit.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', policeOrSSVisit.label, policeOrSSVisit.input);
    await performAction('clickButton', policeOrSocialServiceVisit.continue);
  }

  private async provideDetailsAnimalsAtTheProperty(theAnimalsAtTheProperty: actionRecord) {
    await performValidation('mainHeader', animalsAtTheProperty.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', theAnimalsAtTheProperty.label, theAnimalsAtTheProperty.input);
    await performAction('clickButton', animalsAtTheProperty.continue);
  }

  private async selectVulnerablePeopleInTheProperty(vulnerablePeople: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: vulnerablePeople.question, option: vulnerablePeople.option });
    if (vulnerablePeople.option === vulnerableAdultsAndChildren.yesRadioOption) {
      await performAction('clickRadioButton', { question: vulnerablePeople.confirm, option: vulnerablePeople.peopleOption });
      await performAction('inputText', vulnerablePeople.label, vulnerablePeople.input);
    }
    await performAction('clickButton', vulnerableAdultsAndChildren.continueButton);
  }
  private async provideDetailsAnythingElseHelpWithEviction(anythingElse: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: anythingElse.question, option: anythingElse.option });
    if (anythingElse.option === anythingElseHelpWithEviction.yesRadioOption) {
      await performAction('inputText', anythingElse.label, anythingElse.input);
    };
    await performAction('clickButton', anythingElseHelpWithEviction.continueButton);
  }
  private async accessToProperty(accessToProperty: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: accessToProperty.question, option: accessToProperty.option });
    if (accessToProperty.option === accessToTheProperty.yesRadioOption) {
      await performAction('inputText', accessToProperty.label, accessToProperty.input);
    };
    await performAction('clickButton', accessToTheProperty.continueButton);
  }

  private async provideMoneyOwed(totalMoneyOwed: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('inputText', totalMoneyOwed.label, totalMoneyOwed.input);
    await performAction('clickButton', moneyOwed.continueButton);
  }

  private async provideLegalCosts(legalCost: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: legalCost.question, option: legalCost.option });
    if (legalCost.option === accessToTheProperty.yesRadioOption) {
      await performAction('inputText', legalCost.label, legalCost.input);
    };
    await performAction('clickButton', legalCosts.continueButton);
  }

  private async provideLandRegistryFees(langRegistry: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: langRegistry.question, option: langRegistry.option });
    if (langRegistry.option === accessToTheProperty.yesRadioOption) {
      await performAction('inputText', langRegistry.label, langRegistry.input);
    };
    await performAction('clickButtonAndVerifyPageNavigation', landRegistryFees.continueButton, rePayments.mainHeader);
  }

  private async selectLanguageUsed(languageDetails: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + caseInfo.fid });
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
    await performAction('clickRadioButton', { question: languageDetails.question, option: languageDetails.option });
    await performAction('clickButton', languageUsed.continueButton);
  }

  private async inputErrorValidation(validationArr: actionRecord) {

    if (validationArr.validationReq === 'YES') {

      if (Array.isArray(validationArr.inputArray)) {
        for (const item of validationArr.inputArray) {
          switch (validationArr.validationType) {
            case 'moneyFieldAndRadioOption':
              await performAction('clickRadioButton', { question: validationArr.question, option: validationArr.option });
              if (validationArr.option === 'Yes') {
                await performAction('inputText', validationArr.label, item.input);
                await performAction('clickButton', validationArr.button);
                await performValidation('inputError', validationArr.label, item.errMessage);
              };
              await performAction('clickRadioButton', { question: validationArr.question, option: 'No' });
              break;

            case 'radioOptions':
              await performAction('clickButton', validationArr.button);
              await performValidation('inputError', validationArr.label, item.errMessage);
              break;

            case 'moneyField':
              await performAction('inputText', validationArr.label, item.input);
              await performAction('clickButton', validationArr.button);
              //below line be uncommented after the bug https://tools.hmcts.net/jira/browse/HDPI-3396 is resolved
              //await performValidation('errorMessage', validationArr.label, item.errMessage);
              await performValidation('inputError', validationArr.label, item.errMessage);
              break;

            default:
              throw new Error(`Validation type :"${validationArr.validationType}" is not valid`);
          };
        }
      }
    }
  }
}
