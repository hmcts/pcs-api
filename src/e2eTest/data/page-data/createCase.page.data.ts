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
        civilPossessions: process.env.CASE_TYPE_SUFFIX
          ? `Civil Possessions ${process.env.CASE_TYPE_SUFFIX}`
          : 'Civil Possessions'
      },
    makeAPossessionClaimEvent: 'Make a claim',
    start: 'Start'
  };
