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
  hearingCenterAdmin: {
  email: 'hearing_center_admin_reg2@justice.gov.uk',
  password: process.env.HEARING_CENTER_ADMIN_PASSWORD
},
};
