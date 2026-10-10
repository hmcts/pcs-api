export const users = [{
  user: 'Creator',
  email: 'pcs.local.auth1user1@test.com',
  //email: 'pcs-solicitor2@test.com',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'Service Request']
},
{
  user: 'Claimant Solicitor',
  email: 'pcs-solicitor-user01@test.com',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'Service Request']
},
{
  user: 'Defendant Solicitor',
  email: 'pcs-org1-solicitor2@test.com',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'Service Request']
},
{
  user: 'County Court Judge',
  email: 'TribunalMember.Brandon.Kshlerin-Hartmann@ejudiciary.net',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags', 'Tasks', 'Roles and access', 'Payment History']
},
{
  user: 'CTSC Team Leader',
  email: 'pcs-ctsc-team-leader-01@justice.gov.uk',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags', 'Tasks', 'Roles and access', 'Payment History']
},
{
  user: 'CTSC Administrator',
  email: 'pcs-ctsc-admin-01@justice.gov.uk',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags', 'Tasks', 'Roles and access', 'Payment History']
},
{
  user: 'WLU Team Leader',
  email: 'pcs-wlu-team-leader-01@justice.gov.uk',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags', 'Tasks', 'Roles and access', 'Payment History']
},
{
  user: 'WLU Administrator',
  email: 'pcs-wlu-administrator-01@justice.gov.uk',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags', 'Tasks', 'Roles and access', 'Payment History']
},
  {
    user: 'High Court Judge',
    email: 'ChiefICCJudge.Nichols@ejudiciary.net',
    password: process.env.IDAM_PCS_USER_PASSWORD,
    tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags', 'Tasks', 'Roles and access', 'Payment History']
  },
  {
    user: 'Hearing Centre Team Leader',
    email: 'pcs-hearing-centre-team-leader-01@justice.gov.uk',
    password: process.env.IDAM_PCS_USER_PASSWORD,
    tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags', 'Tasks', 'Roles and access', 'Payment History']
  },
  {
    user: 'Hearing Centre Administrator',
    email: 'pcs-hearing-centre-administrator-01@justice.gov.uk',
    password: process.env.IDAM_PCS_USER_PASSWORD,
    tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags', 'Tasks', 'Roles and access', 'Payment History']
  },
];
