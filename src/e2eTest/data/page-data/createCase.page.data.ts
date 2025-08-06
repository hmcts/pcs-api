export const caseOptions = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Create Case',
  jurisdiction:
    {
      posessions: 'Possessions'
    },
  /*caseType:
      {
          civilPosessions: process.env.CHANGE_ID
              ? `Civil Possessions ${process.env.CHANGE_ID}`
              : 'Civil Possessions'
      },*/
  caseType: {
    civilPosessions: 'Civil Possessions 412'
  },
  event:
    {
      makeAPosessionClaim: 'Make a claim'
    },

}

export const errorMessages = {
  header: 'There is a problem',
  errorMessage: 'An address is required',
}
