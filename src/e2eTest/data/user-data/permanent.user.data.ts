export const user = {
  claimantSolicitor:  {
    email: 'pcs-solicitor-user01@test.com',
    password: process.env.IDAM_PCS_USER_PASSWORD,
    uid: process.env.PCS_SOLICITOR_AUTOMATION_UID
  },
  ctscAdministrator:  {
    email: 'pcs-ctsc-admin-02@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  defendantSolicitor:  {
    email: 'pcs-org1-solicitor2@test.com',
    password: process.env.IDAM_PCS_USER_PASSWORD,
  },
  caseworker:  {
    email: 'pcs-caseworker@test.com',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  hearingCenterAdmin: {
  email: 'hearing_center_admin_reg2@justice.gov.uk',
  password: process.env.HEARING_CENTER_ADMIN_PASSWORD ?? 'Password12!'
},
};
