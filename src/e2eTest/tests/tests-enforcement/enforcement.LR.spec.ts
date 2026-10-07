import {initializeExecutor} from "@utils/controller";
import {initializeEnforcementExecutor, performAction, performValidation} from "@utils/controller-enforcement";
import {createCaseApiData, submitCaseApiData} from "@data/api-data";
import {dismissCookieBanner} from "@config/cookie-banner";
import {user} from "@data/user-data";
import {getCaseTypeId} from "@utils/common/caseType.utils";
import test, {expect} from "@playwright/test";
import {VERY_LONG_TIMEOUT} from "../../playwright.config";
import {home} from "@data/page-data";
import {caseInfo} from "@utils/actions/custom-actions";
import {PageContentValidation} from "@utils/validations/element-validations/pageContent.validation";
import {caseSummary} from "@data/page-data";
import {
    additionalInformation,
    aggressiveAnimalsRisk,
    confirmDefendantsDOB, confirmHCEOfficer,
    criminalAntisocialRisk,
    defendantInBreathingSpace,
    enforcementApplication,
    evictionRisksPosed,
    firearmsPossessionRisk,
    knownDefendantsDOBInformation, landRegistryFees, languageUsed, legalCosts,
    livingInTheProperty,
    missedPayments, moneyOwed,
    nameAndAddressForEviction,
    peopleWhoWillBeEvicted,
    policeOrSocialServicesRisk,
    propertyAccessDetails,
    protestorGroupRisk, repayments, statementOfTruth, suspendedOrder,
    verbalOrWrittenThreatsRisk,
    violentAggressiveRisk,
    vulnerableAdultsChildren
} from "@data/page-data-figma/page-data-enforcement-figma";
import {defendantDetails, fieldsMap, moneyMap} from "@utils/actions/custom-actions/custom-actions-enforcement";
import {checkYourAnswers, yourHCEO} from "@data/page-data/page-data-enforcement";

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeEnforcementExecutor(page);
  const manageCaseBaseUrl = process.env.MANAGE_CASE_BASE_URL;
  if (!manageCaseBaseUrl) {
    throw new Error('MANAGE_CASE_BASE_URL is not set.');
  }
  defendantDetails.length = 0;
  moneyMap.clear();
  fieldsMap.clear();
  await performAction('getDefendantDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayload.addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayload
  });
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  await performAction('updatePaymentAPI');
 //await performAction('getCaseAPI', 'Link Solicitor');
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.defendantSolicitor);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
  await page.waitForLoadState();
  await page.locator('.spinner-container').waitFor({ state: 'detached' });
  await performValidation('mainHeader', home.caseSummary);
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();
});

test.describe.skip('XUI - Respond to a claim - e2e Journey @nightly', () => {
  test('Warrant - Apply for a Warrant of Possession - risk to Bailiff [Yes] - BreathingSpace - Yes @regression',
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
        nextPage: defendantInBreathingSpace.mainHeader
      });
      await performAction('errorValidationIsDefendantInBreathingSpacePage', defendantInBreathingSpace.errorValidation);
      await performAction('isDefendantInBreathingSpace', {option: defendantInBreathingSpace.yesRadioOption, nextPage: missedPayments.mainHeader});
      await performAction('errorValidationMissedPaymentsPage', missedPayments.errorValidation);
      await performAction('missedPayments', {option: missedPayments.yesRadioOption, nextPage: nameAndAddressForEviction.mainHeader});
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

  test('Writ - Apply for a Writ of Possession - Have you hired HCEO [Yes] - Repayment [SOME] - BreathingSpace - No @regression',
      async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', enforcementApplication.mainHeader);
      await performAction('validateWritOrWarrantFeeAmount', {
          journey: enforcementApplication.writOfPossessionRadioOption,
          type: enforcementApplication.summaryWritOrWarrantLink,
          label1: enforcementApplication.warrantFeeValidationLabelHidden,
          text1: enforcementApplication.warrantFeeValidationTextHidden,
          label2: enforcementApplication.writFeeValidationLabelHidden,
          text2: enforcementApplication.writFeeValidationTextHidden
      });
      await performAction('expandSummary', enforcementApplication.summarySaveApplicationLink);
      await performAction('errorValidationYourApplicationPage', enforcementApplication.errorValidation);
      await performAction('selectApplicationType', {
          question: enforcementApplication.typeOfApplicationQuestion,
          option: enforcementApplication.writOfPossessionRadioOption,
          question1: enforcementApplication.claimTransferredToHighCourtQuestionHidden,
          question2: enforcementApplication.genAppSuccessfulQuestionHidden,
          option1: enforcementApplication.yesRadioOptionHidden,
          nextPage: nameAndAddressForEviction.mainHeader
      });
      await performAction('errorValidationNameAndAddressForEvictionPage', nameAndAddressForEviction.errorValidation);
      await performAction('selectNameAndAddressForEviction', {
          question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
          option: nameAndAddressForEviction.yesRadioOption,
          defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
          nextPage: defendantInBreathingSpace.mainHeader
      });
      await performAction('errorValidationIsDefendantInBreathingSpacePage', defendantInBreathingSpace.errorValidation);
      await performAction('isDefendantInBreathingSpace', {option: defendantInBreathingSpace.noRadioOption, nextPage: missedPayments.mainHeader});
      await performAction('errorValidationMissedPaymentsPage', missedPayments.errorValidation);
      await performAction('missedPayments', {option: missedPayments.yesRadioOption, nextPage: confirmHCEOfficer.mainHeader});
      await performAction('errorValidationConfirmHCEOHiredPage', confirmHCEOfficer.errorValidation);
      await performAction('selectHaveHiredHCEO', {
          question: confirmHCEOfficer.haveYouHiredHCEOQuestion,
          option: confirmHCEOfficer.yesRadioOption,
          nextPage: yourHCEO.mainHeader
      });
      await performValidation('mainHeader', yourHCEO.mainHeader);
      await performAction('errorValidationYourHCEOPage', yourHCEO.errorValidation);
      await performAction('nameYourHCEO', {
          label: yourHCEO.nameOfYourHCEOTextLabel,
          input: yourHCEO.nameOfYourHCEOTextInput,
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
      await performValidation('mainHeader', repayments.mainHeader);
      await performAction('validateAmountToRePayTable', {headerName: repayments.mainHeader});
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
          nextPage: statementOfTruth.mainHeader
      });
      await performAction('errorValidationSOTWritPage', statementOfTruth.errorValidation);
      await performAction('validateAmountToRePayTable', {headerName: statementOfTruth.mainHeader});
      await performAction('selectStatementOfTruthWrit', {
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
});
