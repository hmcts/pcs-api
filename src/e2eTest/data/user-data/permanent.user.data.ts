export const user = {
  claimantSolicitor:  {
    email: 'pcs-solicitor-automation@test.com',
    password: process.env.IDAM_PCS_USER_PASSWORD,
    uid: process.env.PCS_SOLICITOR_AUTOMATION_UID
  },
  ctscAdministrator:  {
    email: 'pcs-ctsc-admin-02@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  defendantSolicitor:  {
    email: 'pcs-org1-solicitor2@test.com',
    password: process.env.IDAM_PCS_USER_PASSWORD,
  },
  caseworker:  {
    email: 'pcs-caseworker@test.com',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
};
