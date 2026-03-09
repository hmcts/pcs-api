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
        civilPossessions: process.env.CASE_TYPE_SUFFIX
          ? `Civil Possessions ${process.env.CASE_TYPE_SUFFIX}`
          : 'Civil Possessions'
      },
    apply: 'Apply'
  }
