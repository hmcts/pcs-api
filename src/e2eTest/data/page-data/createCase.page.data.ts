export const createCase =
  {
    title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
    mainHeader: 'Create Case',
    possessionsJurisdiction: 'Possessions',
    /*caseType:
        {
          civilPossessions: process.env.CHANGE_ID
            ? `Civil Possessions ${process.env.CHANGE_ID}`
            : 'Civil Possessions'
        },*/
    caseType:
      {
        civilPossessions: 'Civil Possessions 443'
      },
    makeAPossessionClaimEvent: 'Make a claim'
  };
