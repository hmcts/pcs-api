export const createCase =
  {
    title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
    mainHeader: 'Create Case',
    caseOption: {
      jurisdiction:
        {
          possessions: 'Possessions'
        },
      caseType:
        {civilPossessions: 'Civil Possessions 394'},

// caseType:
      //   {
      //     civilPossessions: process.env.CHANGE_ID
      //       ? `Civil Possessions ${process.env.CHANGE_ID}`
      //       : 'Civil Possessions'
      //   },
      event:
        {
          makeAPossessionClaim: 'Make a claim'
        }
    }
  };

