/**
 * Thin wrappers that compose the existing enforcement actions (utils/registry/registry-enforcement) into the
 * journeys the feature files describe. The action arguments follow tests-enforcement/*.spec.ts.
 */
import { Page } from '@playwright/test';
import { expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementType, EnforcementWorld, SubmitCasePayload } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition } from '../fixtures/pending';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { performAction as performCommonAction } from '@utils/controller';
import { performAction, performValidation } from '@utils/controller-enforcement';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { defendantDetails } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import { createCaseApiData, enforceWarrantApiData, submitCaseApiData } from '@data/api-data';
import { caseSummary } from '@data/page-data';
import {
  checkYourAnswers,
  evidenceUpload,
  provideEvidence,
  theNICEWillChoose,
  warrantOfRestitutionAnyoneAtPropertyRiskIntro,
} from '@data/page-data/page-data-enforcement';
import {
  additionalInformation,
  confirmDefendantsDOB,
  confirmEvictionDetails,
  confirmHCEOfficer,
  enforcementApplication,
  landRegistryFees,
  languageUsed,
  legalCosts,
  livingInTheProperty,
  moneyOwed,
  nameAndAddressForEviction,
  peopleWhoWillBeEvicted,
  peopleYouWantToEvict,
  propertyAccessDetails,
  repayments,
  shareEvidenceWithJudge,
  statementOfTruth,
  suspendedOrder,
  vulnerableAdultsChildren,
} from '@data/page-data-figma/page-data-enforcement-figma';
import { explainHowDefendantsReturned } from '@data/page-data-figma/page-data-enforcement-figma/explainHowDefendantsReturned.page.data';
import { decideEnforcement } from './decide-enforcement';


/** XUI events the steps can drive today, keyed by the name the feature files use. */
export const xuiEvents: Record<string, { label: string; firstPageHeader: string }> = {
  'Enforce the order': { label: caseSummary.enforceTheOrderEvent, firstPageHeader: enforcementApplication.mainHeader },
  // The requirements and the case summary markup call it "Confirm the eviction date";
  // pcs-api names the event "Confirm the eviction details".
  'Confirm the eviction date': { label: caseSummary.confirmEvictionEvent, firstPageHeader: confirmEvictionDetails.mainHeader },
  // A caseworker event: sign in as a hearing centre admin first.
  [decideEnforcement.eventName]: { label: decideEnforcement.eventName, firstPageHeader: decideEnforcement.mainHeader },
};

/** Creates and submits a claim through the API, pays the issue fee if needed, and records the case on the world. */
export async function createIssuedCase(world: EnforcementWorld): Promise<void> {
  const submitPayload = submitCaseApiData.submitCasePayload();
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitPayload });
  if (caseInfo.state !== 'CASE_ISSUED') {
    await performCommonAction('updatePaymentAPI');
  }
  await performAction('getDefendantDetails', {
    defendant1NameKnown: submitPayload.defendant1.nameKnown,
    additionalDefendants: submitPayload.addAnotherDefendant,
    payLoad: submitPayload,
  });
  world.caseReference = caseInfo.id;
  world.submitPayload = submitPayload;
}

/** Submits a warrant of possession application through the API, as the warrant of restitution spec does. */
export async function seedWarrantApplication(world: EnforcementWorld): Promise<void> {
  await performAction('enforceCaseAPI', { data: enforceWarrantApiData.enforceCasePayloadNoJourney });
  world.enforcementSeeded = true;
}

export async function openCaseSummary(page: Page): Promise<void> {
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  // Login and cookie consent are handled globally via storageState in global-setup.config.ts
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
  }).toPass({ timeout: VERY_LONG_TIMEOUT });
}

/** Option labels in the case summary "Next step" drop down (same locator as the select action). */
export async function nextStepOptions(page: Page): Promise<string[]> {
  const field = caseSummary.nextStepEventList;
  const select = page.locator(`:has-text("${field}") + select, :has-text("${field}") ~ select`).first();
  return (await select.locator('option').allTextContents()).map(option => option.trim());
}

export async function startEvent(eventName: string): Promise<void> {
  const event = xuiEvents[eventName];
  await performAction('select', caseSummary.nextStepEventList, event.label);
  await performAction('clickButton', caseSummary.go);
  await performValidation('mainHeader', event.firstPageHeader);
}

export async function chooseApplicationType(type: EnforcementType, world: EnforcementWorld, page: Page): Promise<void> {
  world.applicationType = type;
  switch (type) {
    case 'warrant of possession':
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.warrantOfPossessionRadioOption,
        nextPage: nameAndAddressForEviction.mainHeader,
      });
      return;
    case 'writ of possession':
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.writOfPossessionRadioOption,
        question1: enforcementApplication.claimTransferredToHighCourtQuestionHidden,
        question2: enforcementApplication.genAppSuccessfulQuestionHidden,
        option1: enforcementApplication.yesRadioOptionHidden,
        nextPage: nameAndAddressForEviction.mainHeader,
      });
      return;
    case 'warrant of restitution':
      // pcs-api only offers restitution once a warrant has been submitted on the case (EnforceTheOrder.start).
      if (!world.enforcementSeeded) {
        approximatePrecondition('HDPI-7419', 'warrant of restitution needs an earlier warrant; seeded a warrant application through the API');
        await seedWarrantApplication(world);
        await openCaseSummary(page);
        await startEvent('Enforce the order');
      }
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.warrantOfRestitutionRadioOptionDynamic,
        nextPage: peopleYouWantToEvict.mainHeaderWarrantOfRestitutionDynamic,
      });
      return;
    default:
      throw new Error(`No "Enforce the order" journey for ${type}`);
  }
}

export async function answerApplicationQuestions(type: EnforcementType, world: EnforcementWorld): Promise<void> {
  switch (type) {
    case 'warrant of possession':
      return answerWarrantQuestions(world);
    case 'writ of possession':
      return answerWritQuestions(world);
    case 'warrant of restitution':
      return answerWarrantOfRestitutionQuestions();
    default:
      throw new Error(`No "Enforce the order" journey for ${type}`);
  }
}

/** Warrant of possession, no risk to the bailiff (enforcement.spec.ts "risk to Bailiff [No]"). */
async function answerWarrantQuestions(world: EnforcementWorld): Promise<void> {
  await performAction('selectNameAndAddressForEviction', {
    question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
    option: nameAndAddressForEviction.yesRadioOption,
    defendant1NameKnown: requireSubmitPayload(world).defendant1.nameKnown,
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
    option: defendantDetails,
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
  await answerMoneyQuestions();
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
}

/** Writ of possession, no HCEO hired, nothing repaid (enforcement.writJourney.spec.ts). */
async function answerWritQuestions(world: EnforcementWorld): Promise<void> {
  await performAction('selectNameAndAddressForEviction', {
    question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
    option: nameAndAddressForEviction.yesRadioOption,
    defendant1NameKnown: requireSubmitPayload(world).defendant1.nameKnown,
    nextPage: confirmHCEOfficer.mainHeader,
  });
  await performAction('selectHaveHiredHCEO', {
    question: confirmHCEOfficer.haveYouHiredHCEOQuestion,
    option: confirmHCEOfficer.noRadioOption,
    nextPage: theNICEWillChoose.mainHeader,
  });
  await performAction('clickButton', theNICEWillChoose.continueButton);
  await performValidation('mainHeader', moneyOwed.mainHeader);
  await answerMoneyQuestions();
  await performAction('selectLanguageUsed', {
    question: languageUsed.whichLanguageUsedQuestion,
    option: languageUsed.englishRadioOption,
    nextPage: statementOfTruth.mainHeader,
  });
}

/** Money owed, legal costs, Land Registry fees and repayments: the same pages for warrants and writs. */
async function answerMoneyQuestions(): Promise<void> {
  await performAction('provideMoneyOwed', {
    label: moneyOwed.totalAmountOwedTextLabel,
    input: moneyOwed.totalAmountOwedTextInput,
    nextPage: legalCosts.mainHeader,
  });
  await performAction('provideLegalCosts', {
    question: legalCosts.reclaimLegalCostsQuestion,
    option: legalCosts.noRadioOption,
    label: legalCosts.howMuchYouWantToReclaimTextLabelHidden,
    input: legalCosts.howMuchYouWantToReclaimTextInputHidden,
    nextPage: landRegistryFees.mainHeader,
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
    nextPage: languageUsed.mainHeader,
  });
}

/**
 * Warrant of restitution with the seeded warrant's answers left as they are
 * (enforcement.warrantOfRestitution.spec.ts "all NO selection"). The journey ends on placeholder pages today.
 */
async function answerWarrantOfRestitutionQuestions(): Promise<void> {
  const seeded = enforceWarrantApiData.enforceCasePayloadNoJourney;
  await performAction('reTryOnCallBackError', peopleYouWantToEvict.continueButton, shareEvidenceWithJudge.mainHeader);
  await performAction('reTryOnCallBackError', shareEvidenceWithJudge.continueButton, explainHowDefendantsReturned.mainHeader);
  await performAction('provideHowDefendantReturnToProperty', {
    label: explainHowDefendantsReturned.howDidTheDefendantsReturnToThePropertyTextLabel,
    input: explainHowDefendantsReturned.howDidTheDefendantsReturnToThePropertyTextInput,
    nextPage: provideEvidence.mainHeader,
  });
  await performAction('uploadEvidenceThatDefendantsAreAtProperty', {
    documents: [
      {
        type: evidenceUpload.witnessStatementDropDownInput,
        fileName: 'witnessStatement.pdf',
        description: evidenceUpload.shortDescriptionHiddenTextInput,
        docType: evidenceUpload.typeOfDocumentHiddenTextLabel,
        label: evidenceUpload.shortDescriptionHiddenTextLabel,
      },
    ],
    nextPage: warrantOfRestitutionAnyoneAtPropertyRiskIntro.mainHeader,
  });
  await performAction('reTryOnCallBackError', warrantOfRestitutionAnyoneAtPropertyRiskIntro.continueButton, livingInTheProperty.mainHeader);
  await performAction('validatePrePopulatedData', {
    testPage: livingInTheProperty.mainHeader,
    inputData: [{ type: 'radio', inputRadioQuestion: livingInTheProperty.riskToBailiffQuestion, expectedAnswer: seeded.warrantAnyRiskToBailiff }],
  });
  await performAction('reTryOnCallBackError', livingInTheProperty.continueButton, vulnerableAdultsChildren.mainHeader);
  await performAction('validatePrePopulatedData', {
    testPage: vulnerableAdultsChildren.mainHeader,
    inputData: [{ type: 'radio', inputRadioQuestion: vulnerableAdultsChildren.IsAnyOneLivingAtThePropertyQuestion, expectedAnswer: seeded.vulnerablePeoplePresent }],
  });
  await performAction('reTryOnCallBackError', vulnerableAdultsChildren.continueButton, propertyAccessDetails.mainHeader);
  await performAction('validatePrePopulatedData', {
    testPage: propertyAccessDetails.mainHeader,
    inputData: [{ type: 'radio', inputRadioQuestion: propertyAccessDetails.accessToThePropertyQuestion, expectedAnswer: seeded.warrantIsDifficultToAccessProperty }],
  });
  await performAction('reTryOnCallBackError', propertyAccessDetails.continueButton, additionalInformation.mainHeader);
}

/** Statement of truth signed by the claimant's legal representative; lands on check your answers. */
export async function signStatementOfTruth(type: 'warrant of possession' | 'writ of possession'): Promise<void> {
  await performAction('validateAmountToRePayTable', { headerName: statementOfTruth.mainHeader });
  const signedByLegalRepresentative = {
    question: statementOfTruth.completedByParagraph,
    option: statementOfTruth.claimantLegalRepresentativeRadioOption,
    option1: statementOfTruth.signThisStatementHiddenCheckbox,
    label: statementOfTruth.fullNameHiddenTextLabel,
    input: statementOfTruth.fullNameHiddenTextInput,
    label1: statementOfTruth.nameOfFirmHiddenTextLabel,
    input1: statementOfTruth.nameOfFirmHiddenTextInput,
    label2: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
    input2: statementOfTruth.positionOrOfficeHeldHiddenTextInput,
    nextPage: checkYourAnswers.mainHeader,
  };
  if (type === 'warrant of possession') {
    await performAction('selectStatementOfTruth', { selectCheckbox: statementOfTruth.iCertifyCheckboxDynamic, ...signedByLegalRepresentative });
  } else {
    await performAction('selectStatementOfTruthWrit', signedByLegalRepresentative);
  }
}

export async function submitApplication(): Promise<void> {
  await performAction('clickButton', checkYourAnswers.saveAndContinueButton);
  await performValidation('bannerAlert', 'Case #.* has been updated with event: Enforce the order');
}

function requireSubmitPayload(world: EnforcementWorld): SubmitCasePayload {
  if (!world.submitPayload) {
    throw new Error('No case has been created in this scenario: start with "Given a PCS case ..."');
  }
  return world.submitPayload;
}
