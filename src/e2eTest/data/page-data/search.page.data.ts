import { getCaseTypeName } from '@utils/common/caseType.utils';

export const search =
  {
    title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
    mainHeader: 'Search',
    jurisdictionLabel: 'Jurisdiction',
    caseTypeLabel: 'Case type',
    eventLabel: 'Event',
    caseNumberLabel: 'Case Number',
    possessionsJurisdiction: 'Possessions',
    caseType:
      {
        civilPossessions: getCaseTypeName()
      },
    apply: 'Apply'
  }
