export const user = {
  claimantSolicitor:  {
    email: 'pcs-solicitor-automation@test.com',
    password: process.env.IDAM_PCS_USER_PASSWORD,
    uid: process.env.PCS_SOLICITOR_AUTOMATION_UID
  },
  caseworker:  {
    email: 'pcs-caseworker@test.com',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  //caseworker or judge role is required to access the global search page, so we can use either of them for the test
  caseworkerOrJudge:  {
    email: 'test',
    password: process.env.IDAM_PCS_USER_PASSWORD
  }

};
