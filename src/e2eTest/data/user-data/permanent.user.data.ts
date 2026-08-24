export const user = {
  claimantSolicitor:  { 
    //email: 'pcs-solicitor-user01@test.com',
    email: 'pcs.solicitor.orguser3@hmcts.net', //Org name - PCS_Solicitor_Org_Testing
    password: process.env.IDAM_PCS_USER_PASSWORD,
    uid: process.env.PCS_SOLICITOR_AUTOMATION_UID
  },
  claimantSolicitor1:  { 
    email: 'pcs.solicitor.orguser4@hmcts.net', //Org name - PCS_Solicitor_Org_Testing
    //email: 'pcs.solicitor.org1@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD,
    uid: process.env.PCS_SOLICITOR_AUTOMATION_UID
  }, 
  claimantSolicitorOrg2:  {
    email: 'pcs.solicitor.org2user3@hmcts.net', //Org name - PCS_Solicitor_Org2_Testing
    password: process.env.IDAM_PCS_USER_PASSWORD,
    uid: process.env.PCS_SOLICITOR_AUTOMATION_UID
  },
  defendantSolicitor:  {
    email: 'pcs.solicitor.orguser4@hmcts.net', //Org name - PCS_Solicitor_Org_Testing //pcs-org1-solicitor2@test.com', 
    password: process.env.IDAM_PCS_USER_PASSWORD,
  },
  caseworker:  {
    email: 'pcs-caseworker@test.com',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  staffAdmin:  {
    email: 'pcs-ctsc-admin-01@justice.gov.uk',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  hearingCenterAdmin:  {
    email: 'pcs-hearing-centre-administrator-01@justice.gov.uk',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  defendantSolicitor2:  {
    email: 'pcs-defendant-solicitor2@test.com',
    password: process.env.IDAM_PCS_USER_PASSWORD,
  },
  hearingCenterAdminWales:  {
    email: 'pcs-hearing-centre-wales1@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  localAuthorityOrg1Usr1:  {
    email: 'pcs.local.auth1user1@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  localAuthorityOrg1Usr2:  {
    email: 'pcs.local.auth1user2@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  localAuthorityOrg2Usr1:  {
    email: 'pcs.local.authuser3@hmcts.net',  
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  otherRealEstateActivitiesOrg1Usr1: {
    email: 'pcs.other.re.orguser3@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  otherRealEstateActivitiesOrg1Usr2: {
    email: 'pcs.other.re.orguser4@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  otherRealEstateActivitiesOrg2Usr1: {
    email: 'pcs.other.re.org2user1@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  otherPropertyAndConstructionOrg1Usr1: {
    email: 'pcs.other.prop.orguser3@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  otherPropertyAndConstructionOrg1Usr2: {
    email: 'pcs.other.prop.orguser4@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  otherNotForProfitOrg1Usr1: {
    email: 'pcs.other.notprofit.orguser3@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  otherNotForProfitOrg1Usr2: {
    email: 'pcs.other.notprofit.orguser4@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  otherCharityAndVoluntaryWorkOrg1Usr1: {
    email: 'pcs.other.charity.orguser3@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
  otherCharityAndVoluntaryWorkOrg1Usr2: {
    email: 'pcs.other.charity.orguser4@hmcts.net',
    password: process.env.IDAM_PCS_USER_PASSWORD
  },
};
