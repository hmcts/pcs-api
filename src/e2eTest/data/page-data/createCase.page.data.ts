export const createCase =
  {
    title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
    mainHeader: 'Create Case',
    caseOption: {
      jurisdiction:
        {
          possessions: 'Possessions'
        },
     /* caseType:
        {
          civilPossessions: process.env.CHANGE_ID
            ? `Civil Possessions ${process.env.CHANGE_ID}`
            : 'Civil Possessions'
        },*/
      caseType:
        {
          civilPossessions: 'Civil Possessions 424'
        },
      event:
        {
          makeAPossessionClaim: 'Make a claim'
        }
    }
  };
