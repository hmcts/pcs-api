export const staff = {
  //pcs_ctsc_admin_email: 'pcs-ctsc-admin-01@justice.gov.uk', It is already used in e2e tests
  pcs_ctsc_admin_ca_email: 'pcs-ctsc-admin-ca-01@justice.gov.uk',
  pcs_ctsc_admin_ts_email: 'pcs-ctsc-admin-ts-01@justice.gov.uk',
  pcs_ctsc_admin_ts_ca_email: 'pcs-ctsc-admin-ts-ca-01@justice.gov.uk',

  pcs_ctsc_team_leader_email: 'pcs-ctsc-team-leader-01@justice.gov.uk',
  pcs_ctsc_team_leader_ca_email: 'pcs-ctsc-team-leader-ca-01@justice.gov.uk',
  pcs_ctsc_team_leader_ts_email: 'pcs-ctsc-team-leader-ts-01@justice.gov.uk',
  pcs_ctsc_team_leader_ts_ca_email: 'pcs-ctsc-team-leader-ts-ca-01@justice.gov.uk',

  //These users are not able to access case flags currently, bug is in place to fix this issue HDPI-6843
  // pcs_hearing_centre_team_leader_email: 'pcs-hearing-centre-team-leader-01@justice.gov.uk',
  // pcs_hearing_centre_team_leader_ca_email: 'pcs-hearing-centre-team-leader-ca-01@justice.gov.uk',
  // pcs_hearing_centre_team_leader_ts_email: 'pcs-hearing-centre-team-leader-ts-01@justice.gov.uk',
  // pcs_hearing_centre_team_leader_ts_ca_email: 'pcs-hearing-centre-team-leader-ts-ca-01@justice.gov.uk',
  //
  // pcs_hearing_centre_administrator_email: 'pcs-hearing-centre-administrator-01@justice.gov.uk',
  // pcs_hearing_centre_administrator_ca_email: 'pcs-hearing-centre-administrator-ca-01@justice.gov.uk',
  // pcs_hearing_centre_administrator_ts_email: 'pcs-hearing-centre-administrator-ts-01@justice.gov.uk',
  // pcs_hearing_centre_administrator_ts_ca_email: 'pcs-hearing-centre-administrator-ts-ca-01@justice.gov.uk',

  pcs_wlu_administrator_email: 'pcs-wlu-administrator-01@justice.gov.uk',
  pcs_wlu_administrator_ca_email: 'pcs-wlu-administrator-ca-01@justice.gov.uk',
  pcs_wlu_administrator_ts_email: 'pcs-wlu-administrator-ts-01@justice.gov.uk',
  pcs_wlu_administrator_ts_ca_email: 'pcs-wlu-administrator-ts-ca-01@justice.gov.uk',

  pcs_wlu_team_leader_email: 'pcs-wlu-team-leader-01@justice.gov.uk',
  pcs_wlu_team_leader_ca_email: 'pcs-wlu-team-leader-ca-01@justice.gov.uk',
  pcs_wlu_team_leader_ts_email: 'pcs-wlu-team-leader-ts-01@justice.gov.uk',
  pcs_wlu_team_leader_ts_ca_email: 'pcs-wlu-team-leader-ts-ca-01@justice.gov.uk',
};

export const caseWorker = {
  refundRequester:  {
    email: 'pcs-ctsc-admin-02@justice.gov.uk',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  refundApprover:  {
    email: 'pcs-ctsc-admin-02@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  }
}
