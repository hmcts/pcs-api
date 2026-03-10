import { getCaseTypeName } from '@utils/common/caseType.utils';

export const createCase =
  {
    title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
    mainHeader: 'Create Case',
    jurisdictionLabel: 'Jurisdiction',
    caseTypeLabel: 'Case type',
    eventLabel: 'Event',
    possessionsJurisdiction: 'Possessions',
    caseType:
      {
        civilPossessions: getCaseTypeName()
      },
    makeAPossessionClaimEvent: 'Make a claim',
    start: 'Start'
  };
