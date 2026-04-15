import { expect, test } from '@utils/test-fixtures';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { initializeExecutor } from '@utils/controller';
import { initializeEnforcementExecutor, performAction, performValidation } from '@utils/controller-enforcement';
import { caseSummary } from '@data/page-data';
import {
  checkYourAnswers
} from '@data/page-data/page-data-enforcement';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { defendantDetails, fieldsMap, moneyMap } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {
  aggressiveAnimalsRisk,
  additionalInformation,
  criminalAntisocialRisk,
  livingInTheProperty,
  evictionDelayWarning,
  firearmsPossessionRisk,
  protestorGroupRisk,
  propertyAccessDetails,
  nameAndAddressForEviction,
  policeOrSocialServicesRisk,
  evictionRisksPosed,
  verbalOrWrittenThreatsRisk,
  violentAggressiveRisk,
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
  statementOfTruth
} from '@data/page-data-figma/page-data-enforcement-figma';

test.beforeEach(async ({ page }, testInfo) => {
  initializeExecutor(page);
  initializeEnforcementExecutor(page);
  defendantDetails.length = 0;
  moneyMap.clear();
  fieldsMap.clear();
  if (testInfo.title.includes('@noDefendants')) {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadNoDefendants });
    await performAction('getDefendantDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadNoDefendants.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadNoDefendants.addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayloadNoDefendants
    });
  } else if (testInfo.title.includes('@onlyMain')) {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadOnlyMain });
    await performAction('getDefendantDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadOnlyMain.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadOnlyMain.addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayloadOnlyMain
    });
  } else {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
    await performAction('getDefendantDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayload.addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayload
    });
  }
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  // Login and cookie consent are handled globally via storageState in global-setup.config.ts
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
});

test.afterEach(async () => {
  defendantDetails.length = 0;
  moneyMap.clear();
  fieldsMap.clear();
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
  PageContentValidation.finaliseTest();
});
// Skipping this test case as the feature is not part of Release 1 to save execution time.
// It will be enabled once the feature is included in the execution scope.
test.describe.skip('[Enforcement - Warrant of Possession]', async () => {
  test('Warrant - Apply for a Warrant of Possession - risk to Bailiff [Yes] @enforcement @PR @regression',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', enforcementApplication.mainHeader);
      await performAction('validateWritOrWarrantFeeAmount', {
        journey: enforcementApplication.warrantOfPossessionRadioOption,
        type: enforcementApplication.summaryWritOrWarrantLink,
        label1: enforcementApplication.warrantFeeValidationLabelHidden,
        text1: enforcementApplication.warrantFeeValidationTextHidden,
        label2: enforcementApplication.writFeeValidationLabelHidden,
        text2: enforcementApplication.writFeeValidationTextHidden
      });
      await performAction('validateGetQuoteFromBailiffLink', {
        type: enforcementApplication.summaryWritOrWarrantLink,
        link: enforcementApplication.quoteFromBailiffLinkHidden,
        newPage: enforcementApplication.hceoPageTitleHidden
      });
      await performAction('expandSummary', enforcementApplication.summarySaveApplicationLink);
      await performAction('errorValidationYourApplicationPage', enforcementApplication.errorValidation);
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.warrantOfPossessionRadioOption,
        nextPage: nameAndAddressForEviction.mainHeader
      });
      await performAction('errorValidationNameAndAddressForEvictionPage', nameAndAddressForEviction.errorValidation);
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.yesRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
        nextPage: confirmDefendantsDOB.mainHeader
      });
      await performAction('errorValidationConfirmDefendantsDOBPage', confirmDefendantsDOB.errorValidation);
      await performAction('confirmDefendantsDOB', {
        question: confirmDefendantsDOB.defendantsDOBQuestion,
        option: confirmDefendantsDOB.yesRadioOption,
        nextPage: knownDefendantsDOBInformation.mainHeader
      });
      await performAction('errorValidationEnterDefendantsDOBPage', knownDefendantsDOBInformation.errorValidation);
      await performAction('enterDefendantsDOB', {
        label: knownDefendantsDOBInformation.defendantsDOBQuestion,
        input: defendantDetails,
        nextPage: peopleWhoWillBeEvicted.mainHeader
      });
      await performAction('errorValidationPeopleWhoWillBeEvictedPage', peopleWhoWillBeEvicted.errorValidation);
      await performAction('selectPeopleWhoWillBeEvicted', {
        question: peopleWhoWillBeEvicted.evictEveryOneQuestion,
        option: peopleWhoWillBeEvicted.yesRadioOption,
        nextPage: livingInTheProperty.mainHeader
      });
      await performAction('selectEveryoneLivingAtTheProperty', {
        question: livingInTheProperty.riskToBailiffQuestion,
        option: livingInTheProperty.yesRadioOption,
        nextPage: evictionRisksPosed.mainHeader
      });
      await performAction('errorValidationRiskPosedByEveryonePage', evictionRisksPosed.errorValidation);
      await performAction('selectRiskPosedByEveryoneAtProperty', {
        question: evictionRisksPosed.kindOfRiskQuestion,
        option: [
          evictionRisksPosed.violentOrAggressiveBehaviourCheckbox,
          evictionRisksPosed.historyOfFirearmPossessionCheckbox,
          evictionRisksPosed.criminalOrAntisocialBehaviourCheckbox,
          evictionRisksPosed.verbalOrWrittenThreatsCheckbox,
          evictionRisksPosed.protestGroupCheckbox,
          evictionRisksPosed.policeOrSocialServiceCheckbox,
          evictionRisksPosed.aggressiveAnimalsCheckbox,
        ],
        nextPage: violentAggressiveRisk.mainHeader
      });
      await performAction('errorValidationViolentOrAggressiveBehaviourPage', violentAggressiveRisk.errorValidation);
      await performAction('provideRiskPosedByEveryoneAtProperty', {
        label: violentAggressiveRisk.howHaveTheyBeenViolentAndAggressiveQuestion,
        input: violentAggressiveRisk.howHaveTheyBeenViolentAndAggressiveTextInput,
        nextPage: firearmsPossessionRisk.mainHeader
      });
      await performAction('provideRiskPosedByEveryoneAtProperty', {
        label: firearmsPossessionRisk.whatIsTheirHistoryOfFirearmPossessionQuestion,
        input: firearmsPossessionRisk.whatIsTheirHistoryOfFirearmPossessionTextInput,
        nextPage: criminalAntisocialRisk.mainHeader
      });
      await performAction('provideRiskPosedByEveryoneAtProperty', {
        label: criminalAntisocialRisk.whatIsTheirHistoryOfCriminalAntisocialBehaviourQuestion,
        input: criminalAntisocialRisk.whatIsTheirHistoryOfCriminalAntisocialBehaviourTextInput,
        nextPage: verbalOrWrittenThreatsRisk.mainHeader
      });
      await performAction('provideRiskPosedByEveryoneAtProperty', {
        label: verbalOrWrittenThreatsRisk.verbalOrWrittenThreatsMadeQuestion,
        input: verbalOrWrittenThreatsRisk.verbalOrWrittenThreatsMadeTextInput,
        nextPage: protestorGroupRisk.mainHeader
      });
      await performAction('provideRiskPosedByEveryoneAtProperty', {
        label: protestorGroupRisk.whichGroupMemberQuestion,
        input: protestorGroupRisk.whichGroupMemberTextInput,
        nextPage: policeOrSocialServicesRisk.mainHeader
      });
      await performAction('provideRiskPosedByEveryoneAtProperty', {
        label: policeOrSocialServicesRisk.whyDidThePoliceOrSSVisitThePropertyQuestion,
        input: policeOrSocialServicesRisk.whyDidThePoliceOrSSVisitThePropertyTextInput,
        nextPage: aggressiveAnimalsRisk.mainHeader
      });
      await performAction('provideRiskPosedByEveryoneAtProperty', {
        label: aggressiveAnimalsRisk.whatKindOfAnimalDoTheyHaveQuestion,
        input: aggressiveAnimalsRisk.whatKindOfAnimalDoTheyHaveTextInput,
        nextPage: vulnerableAdultsChildren.mainHeader
      });
      await performAction('errorValidationVulnerablePeoplePage', vulnerableAdultsChildren.errorValidation);
      await performAction('selectVulnerablePeopleInTheProperty', {
        question: vulnerableAdultsChildren.IsAnyOneLivingAtThePropertyQuestion,
        option: vulnerableAdultsChildren.yesRadioOption,
        confirm: vulnerableAdultsChildren.confirmVulnerablePeopleHiddenQuestion,
        peopleOption: vulnerableAdultsChildren.vulnerableAdultsHiddenRadioOption,
        label: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextLabel,
        input: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextInput,
        nextPage: propertyAccessDetails.mainHeader
      });
      await performAction('provideDetailsBasedOnRadioOptionSelection', {
        question: propertyAccessDetails.accessToThePropertyQuestion,
        option: propertyAccessDetails.yesRadioOption,
        label: propertyAccessDetails.whyItsDifficultToAccessToThePropertyTextLabelHidden,
        input: propertyAccessDetails.whyItsDifficultToAccessToThePropertyTextInputHidden,
        nextPage: additionalInformation.mainHeader
      });
      await performAction('provideDetailsBasedOnRadioOptionSelection', {
        question: additionalInformation.anythingElseQuestion,
        option: additionalInformation.yesRadioOption,
        label: additionalInformation.tellUsAnythingElseTextLabelHidden,
        input: additionalInformation.tellUsAnythingElseTextInput,
        nextPage: moneyOwed.mainHeader
      });
      await performAction('errorValidationMoneyOwedPage', moneyOwed.errorValidation);
      await performAction('provideMoneyOwed', {
        label: moneyOwed.totalAmountOwedTextLabel,
        input: moneyOwed.totalAmountOwedTextInput,
        nextPage: legalCosts.mainHeader
      });
      await performAction('errorValidationLegalCostsPage', legalCosts.errorValidation);
      await performAction('provideLegalCosts', {
        question: legalCosts.reclaimLegalCostsQuestion,
        option: legalCosts.yesRadioOption,
        label: legalCosts.howMuchYouWantToReclaimTextLabelHidden,
        input: legalCosts.howMuchYouWantToReclaimTextInputHidden,
        nextPage: landRegistryFees.mainHeader
      });
      await performAction('errorValidationLandRegistryFeePage', landRegistryFees.errorValidation);
      await performAction('provideLandRegistryFees', {
        question: landRegistryFees.landRegistryFeeQuestion,
        option: landRegistryFees.yesRadioOption,
        label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabelHidden,
        input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput,
        nextPage: repayments.mainHeader
      });
      await performAction('validateAmountToRePayTable', { headerName: repayments.mainHeader });
      await performAction('errorValidationRepaymentsPage', repayments.errorValidation);
      await performAction('provideAmountToRePay', {
        question: repayments.rePaymentQuestion,
        option: repayments.someRadioOptions,
        label: repayments.enterTheAmountTextLabelHidden,
        input: repayments.enterTheAmountTextInputHidden,
        nextPage: languageUsed.mainHeader
      });
      await performAction('errorValidationLanguageUsedPage', languageUsed.errorValidation);
      await performAction('selectLanguageUsed', {
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.englishRadioOption,
        nextPage: suspendedOrder.mainHeader
      });
      await performAction('errorValidationSuspendOrderPage', suspendedOrder.errorValidation);
      await performAction('confirmSuspendedOrder', {
        question: suspendedOrder.suspendedOrderQuestion,
        option: suspendedOrder.yesRadioOption,
        nextPage: statementOfTruth.mainHeader
      });
      await performAction('errorValidationSOT1Page', statementOfTruth.errorValidation);
      await performAction('validateAmountToRePayTable', { headerName: statementOfTruth.mainHeader });
      await performAction('selectStatementOfTruth', {
        selectCheckbox: statementOfTruth.iCertifyCheckboxDynamic,
        question: statementOfTruth.completedByLabel,
        option: statementOfTruth.claimantRadioOption,
        option1: statementOfTruth.iBelieveTheFactsHiddenCheckbox,
        label: statementOfTruth.fullNameHiddenTextLabel,
        input: statementOfTruth.fullNameHiddenTextInput,
        label1: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
        input1: statementOfTruth.positionOrOfficeHeldHiddenTextInput,
        label2: statementOfTruth.nameOfFirmHiddenTextLabel,
        input2: statementOfTruth.nameOfFirmHiddenTextInput,
        nextPage: checkYourAnswers.mainHeader
      });
    });

  test('Warrant - Apply for a Warrant of Possession - risk to Bailiff [No] @enforcement @PR @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', enforcementApplication.mainHeader);
    await performAction('validateWritOrWarrantFeeAmount', {
      journey: enforcementApplication.warrantOfPossessionRadioOption,
      type: enforcementApplication.summaryWritOrWarrantLink,
      label1: enforcementApplication.warrantFeeValidationLabelHidden,
      text1: enforcementApplication.warrantFeeValidationTextHidden,
      label2: enforcementApplication.writFeeValidationLabelHidden,
      text2: enforcementApplication.writFeeValidationTextHidden
    });
    await performAction('selectApplicationType', {
      question: enforcementApplication.typeOfApplicationQuestion,
      option: enforcementApplication.warrantOfPossessionRadioOption,
      nextPage: nameAndAddressForEviction.mainHeader
    });
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
      defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      nextPage: confirmDefendantsDOB.mainHeader
    });
    await performAction('confirmDefendantsDOB', {
      question: confirmDefendantsDOB.defendantsDOBQuestion,
      option: confirmDefendantsDOB.noRadioOption,
      nextPage: peopleWhoWillBeEvicted.mainHeader
    });
    await performAction('selectPeopleWhoWillBeEvicted', {
      question: peopleWhoWillBeEvicted.evictEveryOneQuestion,
      option: peopleWhoWillBeEvicted.noRadioOption,
      nextPage: peopleYouWantToEvict.mainHeader
    });
    await performAction('errorValidationPeopleYouWantToEvictPage', peopleYouWantToEvict.errorValidation);
    await performAction('selectPeopleYouWantToEvict', {
      question: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
      option: defendantDetails,
      nextPage: livingInTheProperty.mainHeader
    });
    await performAction('selectEveryoneLivingAtTheProperty', {
      question: livingInTheProperty.riskToBailiffQuestion,
      option: livingInTheProperty.noRadioOption,
      nextPage: vulnerableAdultsChildren.mainHeader
    });
    await performAction('selectVulnerablePeopleInTheProperty', {
      question: vulnerableAdultsChildren.IsAnyOneLivingAtThePropertyQuestion,
      option: vulnerableAdultsChildren.noRadioOption,
      confirm: vulnerableAdultsChildren.confirmVulnerablePeopleHiddenQuestion,
      peopleOption: vulnerableAdultsChildren.vulnerableAdultsHiddenRadioOption,
      label: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextLabel,
      input: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextInput,
      nextPage: propertyAccessDetails.mainHeader
    });
    await performAction('provideDetailsBasedOnRadioOptionSelection', {
      question: propertyAccessDetails.accessToThePropertyQuestion,
      option: propertyAccessDetails.noRadioOption,
      nextPage: additionalInformation.mainHeader
    });
    await performAction('provideDetailsBasedOnRadioOptionSelection', {
      question: additionalInformation.anythingElseQuestion,
      option: additionalInformation.noRadioOption,
      nextPage: moneyOwed.mainHeader
    });
    await performAction('provideMoneyOwed', {
      label: moneyOwed.totalAmountOwedTextLabel,
      input: moneyOwed.totalAmountOwedTextInput,
      nextPage: legalCosts.mainHeader
    });
    await performAction('provideLegalCosts', {
      question: legalCosts.reclaimLegalCostsQuestion,
      option: legalCosts.noRadioOption,
      label: legalCosts.howMuchYouWantToReclaimTextLabelHidden,
      input: legalCosts.howMuchYouWantToReclaimTextInputHidden,
      nextPage: landRegistryFees.mainHeader
    });
    await performAction('provideLandRegistryFees', {
      question: landRegistryFees.landRegistryFeeQuestion,
      option: landRegistryFees.noRadioOption,
      label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabelHidden,
      input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput,
      nextPage: repayments.mainHeader,
    });
    await performAction('validateAmountToRePayTable', { headerName: repayments.mainHeader });
    await performAction('provideAmountToRePay', {
      question: repayments.rePaymentQuestion,
      option: repayments.noneRadioOptions,
      label: repayments.enterTheAmountTextLabelHidden,
      input: repayments.enterTheAmountTextInputHidden,
      nextPage: languageUsed.mainHeader
    });
    await performAction('selectLanguageUsed', {
      question: languageUsed.whichLanguageUsedQuestion,
      option: languageUsed.englishRadioOption,
      nextPage: suspendedOrder.mainHeader
    });
    await performAction('confirmSuspendedOrder', {
      question: suspendedOrder.suspendedOrderQuestion,
      option: suspendedOrder.noRadioOption,
      nextPage: statementOfTruth.mainHeader
    });
    await performAction('validateAmountToRePayTable', { headerName: statementOfTruth.mainHeader });
    await performAction('selectStatementOfTruth', {
      selectCheckbox: statementOfTruth.iCertifyCheckboxDynamic,
      question: statementOfTruth.completedByLabel,
      option: statementOfTruth.claimantLegalRepresentativeRadioOption,
      option1: statementOfTruth.signThisStatementHiddenCheckbox,
      label: statementOfTruth.fullNameHiddenTextLabel,
      input: statementOfTruth.fullNameHiddenTextInput,
      label1: statementOfTruth.nameOfFirmHiddenTextLabel,
      input1: statementOfTruth.nameOfFirmHiddenTextInput,
      label2: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
      input2: statementOfTruth.positionOrOfficeHeldHiddenTextInput,
      nextPage: checkYourAnswers.mainHeader
    });
  });

  test('Warrant - Apply for a Warrant of Possession - risk to Bailiff [Not sure] @enforcement @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', enforcementApplication.mainHeader);
    await performAction('validateWritOrWarrantFeeAmount', {
      journey: enforcementApplication.warrantOfPossessionRadioOption,
      type: enforcementApplication.summaryWritOrWarrantLink,
      label1: enforcementApplication.warrantFeeValidationLabelHidden,
      text1: enforcementApplication.warrantFeeValidationTextHidden,
      label2: enforcementApplication.writFeeValidationLabelHidden,
      text2: enforcementApplication.writFeeValidationTextHidden
    });
    await performAction('selectApplicationType', {
      question: enforcementApplication.typeOfApplicationQuestion,
      option: enforcementApplication.warrantOfPossessionRadioOption,
      nextPage: nameAndAddressForEviction.mainHeader
    });
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
      defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      nextPage: confirmDefendantsDOB.mainHeader
    });
    await performAction('confirmDefendantsDOB', {
      question: confirmDefendantsDOB.defendantsDOBQuestion,
      option: confirmDefendantsDOB.noRadioOption,
      nextPage: peopleWhoWillBeEvicted.mainHeader
    });
    await performAction('selectPeopleWhoWillBeEvicted', {
      question: peopleWhoWillBeEvicted.evictEveryOneQuestion,
      option: peopleWhoWillBeEvicted.noRadioOption,
      nextPage: peopleYouWantToEvict.mainHeader
    });
    await performAction('selectPeopleYouWantToEvict', {
      question: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
      option: defendantDetails[0],
      nextPage: livingInTheProperty.mainHeader
    });
    await performAction('selectEveryoneLivingAtTheProperty', {
      question: livingInTheProperty.riskToBailiffQuestion,
      option: livingInTheProperty.notSureRadioOption,
      nextPage: evictionDelayWarning.mainHeader
    });
    await performAction('clickButton', evictionDelayWarning.continueButton);
    await performValidation('mainHeader', vulnerableAdultsChildren.mainHeader);
    await performAction('selectVulnerablePeopleInTheProperty', {
      question: vulnerableAdultsChildren.IsAnyOneLivingAtThePropertyQuestion,
      option: vulnerableAdultsChildren.notSureRadioOption,
      confirm: vulnerableAdultsChildren.confirmVulnerablePeopleHiddenQuestion,
      peopleOption: vulnerableAdultsChildren.vulnerableAdultsHiddenRadioOption,
      label: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextLabel,
      input: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextInput,
      nextPage: propertyAccessDetails.mainHeader
    });
    await performAction('provideDetailsBasedOnRadioOptionSelection', {
      question: propertyAccessDetails.accessToThePropertyQuestion,
      option: propertyAccessDetails.yesRadioOption,
      label: propertyAccessDetails.whyItsDifficultToAccessToThePropertyTextLabelHidden,
      input: propertyAccessDetails.whyItsDifficultToAccessToThePropertyTextInputHidden,
      nextPage: additionalInformation.mainHeader
    });
    await performAction('provideDetailsBasedOnRadioOptionSelection', {
      question: additionalInformation.anythingElseQuestion,
      option: additionalInformation.yesRadioOption,
      label: additionalInformation.tellUsAnythingElseTextLabelHidden,
      input: additionalInformation.tellUsAnythingElseTextInput,
      nextPage: moneyOwed.mainHeader
    });
    await performAction('provideMoneyOwed', {
      label: moneyOwed.totalAmountOwedTextLabel,
      input: moneyOwed.totalAmountOwedTextInput,
      nextPage: legalCosts.mainHeader
    });
    await performAction('provideLegalCosts', {
      question: legalCosts.reclaimLegalCostsQuestion,
      option: legalCosts.yesRadioOption,
      label: legalCosts.howMuchYouWantToReclaimTextLabelHidden,
      input: legalCosts.howMuchYouWantToReclaimTextInputHidden,
      nextPage: landRegistryFees.mainHeader
    });
    await performAction('provideLandRegistryFees', {
      question: landRegistryFees.landRegistryFeeQuestion,
      option: landRegistryFees.noRadioOption,
      label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabelHidden,
      input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput,
      nextPage: repayments.mainHeader,
    });
    await performAction('validateAmountToRePayTable', { headerName: repayments.mainHeader });
    await performAction('provideAmountToRePay', {
      question: repayments.rePaymentQuestion,
      option: repayments.allRadioOptions,
      label: repayments.enterTheAmountTextLabelHidden,
      input: repayments.enterTheAmountTextInputHidden,
      nextPage: languageUsed.mainHeader
    });
    await performAction('selectLanguageUsed', {
      question: languageUsed.whichLanguageUsedQuestion,
      option: languageUsed.englishRadioOption,
      nextPage: suspendedOrder.mainHeader
    });
    await performAction('confirmSuspendedOrder', {
      question: suspendedOrder.suspendedOrderQuestion,
      option: suspendedOrder.yesRadioOption,
      nextPage: statementOfTruth.mainHeader
    });
    await performAction('validateAmountToRePayTable', { headerName: statementOfTruth.mainHeader });
    await performAction('selectStatementOfTruth', {
      selectCheckbox: statementOfTruth.iCertifyCheckboxDynamic,
      question: statementOfTruth.completedByLabel,
      option: statementOfTruth.claimantRadioOption,
      option1: statementOfTruth.iBelieveTheFactsHiddenCheckbox,
      label: statementOfTruth.fullNameHiddenTextLabel,
      input: statementOfTruth.fullNameHiddenTextInput,
      label1: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
      input1: statementOfTruth.positionOrOfficeHeldHiddenTextInput,
      label2: statementOfTruth.nameOfFirmHiddenTextLabel,
      input2: statementOfTruth.nameOfFirmHiddenTextInput,
      nextPage: checkYourAnswers.mainHeader
    });
  });

  test('Warrant - Apply for a Warrant of Possession [General application journey] - risk to Bailiff [Yes]', {
    annotation: {
      type: 'issue',
      description: 'General application journey is a placeholder for now,this test will be fully etched out when this is ready to be developed',
    },
  },
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', enforcementApplication.mainHeader);
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.warrantOfPossessionRadioOption,
        nextPage: nameAndAddressForEviction.mainHeader
      });
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.noRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
        nextPage: changeNameAddress.mainHeader
      });
      await performAction('clickButton', changeNameAddress.continueButton);
      await performValidation('errorMessage', { header: changeNameAddress.eventCouldNotBeCreatedErrorMessageHeader, message: changeNameAddress.errMessageDynamic });
    });

  test('Warrant - Apply for a Warrant of Possession - risk to Bailiff [No] no defendants added @noDefendants @enforcement',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', enforcementApplication.mainHeader);
      await performAction('validateWritOrWarrantFeeAmount', {
        journey: enforcementApplication.warrantOfPossessionRadioOption,
        type: enforcementApplication.summaryWritOrWarrantLink,
        label1: enforcementApplication.warrantFeeValidationLabelHidden,
        text1: enforcementApplication.warrantFeeValidationTextHidden,
        label2: enforcementApplication.writFeeValidationLabelHidden,
        text2: enforcementApplication.writFeeValidationTextHidden
      });
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.warrantOfPossessionRadioOption,
        nextPage: nameAndAddressForEviction.mainHeader
      });
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.yesRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
        nextPage: confirmDefendantsDOB.mainHeader
      });
      await performAction('confirmDefendantsDOB', {
        question: confirmDefendantsDOB.defendantsDOBQuestion,
        option: confirmDefendantsDOB.noRadioOption,
        nextPage: peopleWhoWillBeEvicted.mainHeader
      });
      await performAction('selectPeopleWhoWillBeEvicted', {
        question: peopleWhoWillBeEvicted.evictEveryOneQuestion,
        option: peopleWhoWillBeEvicted.noRadioOption,
        nextPage: peopleYouWantToEvict.mainHeader
      });
      await performAction('errorValidationPeopleYouWantToEvictPage', peopleYouWantToEvict.errorValidation);
      await performAction('selectPeopleYouWantToEvict', {
        question: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
        option: defendantDetails,
        nextPage: livingInTheProperty.mainHeader
      });
      await performAction('selectEveryoneLivingAtTheProperty', {
        question: livingInTheProperty.riskToBailiffQuestion,
        option: livingInTheProperty.yesRadioOption,
        nextPage: evictionRisksPosed.mainHeader
      });
      await performAction('selectRiskPosedByEveryoneAtProperty', {
        question: evictionRisksPosed.kindOfRiskQuestion,
        option: [
          evictionRisksPosed.protestGroupCheckbox,
          evictionRisksPosed.policeOrSocialServiceCheckbox,
        ],
        nextPage: protestorGroupRisk.mainHeader
      });
      await performAction('provideRiskPosedByEveryoneAtProperty', {
        label: protestorGroupRisk.whichGroupMemberQuestion,
        input: protestorGroupRisk.whichGroupMemberTextInput,
        nextPage: policeOrSocialServicesRisk.mainHeader
      });
      await performAction('provideRiskPosedByEveryoneAtProperty', {
        label: policeOrSocialServicesRisk.whyDidThePoliceOrSSVisitThePropertyQuestion,
        input: policeOrSocialServicesRisk.whyDidThePoliceOrSSVisitThePropertyTextInput,
        nextPage: vulnerableAdultsChildren.mainHeader
      });
      await performAction('selectVulnerablePeopleInTheProperty', {
        question: vulnerableAdultsChildren.IsAnyOneLivingAtThePropertyQuestion,
        option: vulnerableAdultsChildren.noRadioOption,
        confirm: vulnerableAdultsChildren.confirmVulnerablePeopleHiddenQuestion,
        peopleOption: vulnerableAdultsChildren.vulnerableAdultsHiddenRadioOption,
        label: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextLabel,
        input: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextInput,
        nextPage: propertyAccessDetails.mainHeader
      });
      await performAction('provideDetailsBasedOnRadioOptionSelection', {
        question: propertyAccessDetails.accessToThePropertyQuestion,
        option: propertyAccessDetails.noRadioOption,
        nextPage: additionalInformation.mainHeader
      });
      await performAction('provideDetailsBasedOnRadioOptionSelection', {
        question: additionalInformation.anythingElseQuestion,
        option: additionalInformation.noRadioOption,
        nextPage: moneyOwed.mainHeader
      });
      await performAction('provideMoneyOwed', {
        label: moneyOwed.totalAmountOwedTextLabel,
        input: moneyOwed.totalAmountOwedTextInput,
        nextPage: legalCosts.mainHeader
      });
      await performAction('provideLegalCosts', {
        question: legalCosts.reclaimLegalCostsQuestion,
        option: legalCosts.noRadioOption,
        label: legalCosts.howMuchYouWantToReclaimTextLabelHidden,
        input: legalCosts.howMuchYouWantToReclaimTextInputHidden,
        nextPage: landRegistryFees.mainHeader
      });
      await performAction('provideLandRegistryFees', {
        question: landRegistryFees.landRegistryFeeQuestion,
        option: landRegistryFees.noRadioOption,
        label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabelHidden,
        input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput,
        nextPage: repayments.mainHeader,
      });
      await performAction('validateAmountToRePayTable', { headerName: repayments.mainHeader });
      await performAction('provideAmountToRePay', {
        question: repayments.rePaymentQuestion,
        option: repayments.noneRadioOptions,
        label: repayments.enterTheAmountTextLabelHidden,
        input: repayments.enterTheAmountTextInputHidden,
        nextPage: languageUsed.mainHeader
      });
      await performAction('selectLanguageUsed', {
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.englishRadioOption,
        nextPage: suspendedOrder.mainHeader
      });
      await performAction('confirmSuspendedOrder', {
        question: suspendedOrder.suspendedOrderQuestion,
        option: suspendedOrder.noRadioOption,
        nextPage: statementOfTruth.mainHeader
      });
      await performAction('validateAmountToRePayTable', { headerName: statementOfTruth.mainHeader });
      await performAction('selectStatementOfTruth', {
        selectCheckbox: statementOfTruth.iCertifyCheckboxDynamic,
        question: statementOfTruth.completedByLabel,
        option: statementOfTruth.claimantLegalRepresentativeRadioOption,
        option1: statementOfTruth.signThisStatementHiddenCheckbox,
        label: statementOfTruth.fullNameHiddenTextLabel,
        input: statementOfTruth.fullNameHiddenTextInput,
        label1: statementOfTruth.nameOfFirmHiddenTextLabel,
        input1: statementOfTruth.nameOfFirmHiddenTextInput,
        label2: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
        input2: statementOfTruth.positionOrOfficeHeldHiddenTextInput,
        nextPage: checkYourAnswers.mainHeader
      });
    });

  test('Warrant - Apply for a Warrant of Possession - risk to Bailiff [No]- only main defendants name known @onlyMain @enforcement',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', enforcementApplication.mainHeader);
      await performAction('validateWritOrWarrantFeeAmount', {
        journey: enforcementApplication.warrantOfPossessionRadioOption,
        type: enforcementApplication.summaryWritOrWarrantLink,
        label1: enforcementApplication.warrantFeeValidationLabelHidden,
        text1: enforcementApplication.warrantFeeValidationTextHidden,
        label2: enforcementApplication.writFeeValidationLabelHidden,
        text2: enforcementApplication.writFeeValidationTextHidden
      });
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.warrantOfPossessionRadioOption,
        nextPage: nameAndAddressForEviction.mainHeader,
      });
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.yesRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
        nextPage: confirmDefendantsDOB.mainHeader,
      });
      await performAction('confirmDefendantsDOB', {
        question: confirmDefendantsDOB.defendantsDOBQuestion,
        option: confirmDefendantsDOB.noRadioOption,
        nextPage: peopleWhoWillBeEvicted.mainHeader,
      });
      await performAction('selectPeopleWhoWillBeEvicted', {
        question: peopleWhoWillBeEvicted.evictEveryOneQuestion,
        option: peopleWhoWillBeEvicted.noRadioOption,
        nextPage: peopleYouWantToEvict.mainHeader,
      });     
      await performAction('selectPeopleYouWantToEvict', {
        question: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
        option: defendantDetails[0],
        nextPage: livingInTheProperty.mainHeader,
      });
      await performAction('selectEveryoneLivingAtTheProperty', {
        question: livingInTheProperty.riskToBailiffQuestion,
        option: livingInTheProperty.noRadioOption,
        nextPage: vulnerableAdultsChildren.mainHeader,
      });
      await performAction('selectVulnerablePeopleInTheProperty', {
        question: vulnerableAdultsChildren.IsAnyOneLivingAtThePropertyQuestion,
        option: vulnerableAdultsChildren.noRadioOption,
        confirm: vulnerableAdultsChildren.confirmVulnerablePeopleHiddenQuestion,
        peopleOption: vulnerableAdultsChildren.vulnerableAdultsHiddenRadioOption,
        label: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextLabel,
        input: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextInput,
        nextPage: propertyAccessDetails.mainHeader,
      });
      await performAction('provideDetailsBasedOnRadioOptionSelection', {
        question: propertyAccessDetails.accessToThePropertyQuestion,
        option: propertyAccessDetails.noRadioOption,
        nextPage: additionalInformation.mainHeader,
      });
      await performAction('provideDetailsBasedOnRadioOptionSelection', {
        question: additionalInformation.anythingElseQuestion,
        option: additionalInformation.noRadioOption,
        nextPage: moneyOwed.mainHeader,
      });
      await performAction('provideMoneyOwed', {
        label: moneyOwed.totalAmountOwedTextLabel,
        input: moneyOwed.totalAmountOwedTextInput,
        nextPage: legalCosts.mainHeader
      });
      await performAction('provideLegalCosts', {
        question: legalCosts.reclaimLegalCostsQuestion,
        option: legalCosts.noRadioOption,
        label: legalCosts.howMuchYouWantToReclaimTextLabelHidden,
        input: legalCosts.howMuchYouWantToReclaimTextInputHidden,
        nextPage: landRegistryFees.mainHeader
      });
      await performAction('provideLandRegistryFees', {
        question: landRegistryFees.landRegistryFeeQuestion,
        option: landRegistryFees.noRadioOption,
        label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabelHidden,
        input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput,
        nextPage: repayments.mainHeader,
      });
      await performAction('validateAmountToRePayTable', { headerName: repayments.mainHeader });
      await performAction('provideAmountToRePay', {
        question: repayments.rePaymentQuestion,
        option: repayments.noneRadioOptions,
        label: repayments.enterTheAmountTextLabelHidden,
        input: repayments.enterTheAmountTextInputHidden,
        nextPage: languageUsed.mainHeader
      });
      await performAction('selectLanguageUsed', {
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.englishRadioOption,
        nextPage: suspendedOrder.mainHeader,
      });
      await performAction('confirmSuspendedOrder', {
        question: suspendedOrder.suspendedOrderQuestion,
        option: suspendedOrder.noRadioOption,
        nextPage: statementOfTruth.mainHeader,
      });
      await performAction('validateAmountToRePayTable', { headerName: statementOfTruth.mainHeader });
      await performAction('selectStatementOfTruth', {
        selectCheckbox: statementOfTruth.iCertifyCheckboxDynamic,
        question: statementOfTruth.completedByLabel,
        option: statementOfTruth.claimantLegalRepresentativeRadioOption,
        option1: statementOfTruth.signThisStatementHiddenCheckbox,
        label: statementOfTruth.fullNameHiddenTextLabel,
        input: statementOfTruth.fullNameHiddenTextInput,
        label1: statementOfTruth.nameOfFirmHiddenTextLabel,
        input1: statementOfTruth.nameOfFirmHiddenTextInput,
        label2: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
        input2: statementOfTruth.positionOrOfficeHeldHiddenTextInput,
        nextPage: checkYourAnswers.mainHeader,
      });
    });
});
