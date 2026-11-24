import * as englishDataEnforcement from './page-data-figma/page-data-enforcement-figma';
import * as welshDataEnforcement from './page-data-cy/page-data-enforcement-cy';

const selectedData = process.env.RUN_E2E_IN_WELSH?.toLowerCase() === 'true'
  ? welshDataEnforcement
  : englishDataEnforcement;

const data = selectedData as typeof welshDataEnforcement;

export const {
  additionalInformation,
  aggressiveAnimalsRisk,
  amountDefendantOwes,
  cannotApplyForWarrantInfo,
  cannotApplyForWritInfo,
  caseSummary,
  changeNameAddress,
  confirmDefendantsDOB,
  confirmEvictionDetails,
  confirmHCEOfficer,
  criminalAntisocialRisk,
  defendantInBreathingSpace,
  enforcementApplication,
  evictionDate,
  evictionDelayWarning,
  evictionRisksPosed,
  explainHowDefendantsReturned,
  firearmsPossessionRisk,
  hCEOfficerDetails,
  knownDefendantsDOBInformation,
  languageUsed,
  landRegistryFees,
  legalCosts,
  livingInTheProperty,
  missedPayments,
  moneyOwed,
  nameAndAddressForEviction,
  peopleWhoWillBeEvicted,
  peopleYouWantToEvict,
  policeOrSocialServicesRisk,
  propertyAccessDetails,
  protestorGroupRisk,
  repayments,
  repaymentsWrit,
  shareEvidenceWithJudge,
  statementOfTruth,
  suspendedOrder,
  verbalOrWrittenThreatsRisk,
  violentAggressiveRisk,
  vulnerableAdultsChildren
} = data;
