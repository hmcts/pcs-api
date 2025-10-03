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
        civilPossessions: process.env.CHANGE_ID
          ? `Civil Possessions ${process.env.CHANGE_ID}`
          : 'Civil Possessions'
      },
    apply: 'Apply'
  }
