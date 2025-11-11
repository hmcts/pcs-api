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
        civilPossessions: process.env.CHANGE_ID
          ? `Civil Possessions ${process.env.CHANGE_ID}`
          : 'Civil Possessions'
      },
    makeAPossessionClaimEvent: 'Make a claim',
    start: 'Start'
  };
