export const users = [{
  user: 'Creator',
  email: 'pcs-solicitor2@test.com',
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
  email: 'HHJ.Test.McGinn@judicialofficeelinkssw.onmicrosoft.com',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags']
},
{
  user: 'CTSC Team Leader',
  email: 'pcs-ctsc-team-leader-01@justice.gov.uk',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags']
},
{
  user: 'CTSC Administrator',
  email: 'pcs-ctsc-admin-01@justice.gov.uk',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags']
},
{
  user: 'WLU Team Leader',
  email: 'pcs-wlu-team-leader-01@justice.gov.uk',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags']
},
{
  user: 'WLU Administrator',
  email: 'pcs-wlu-administrator-01@justice.gov.uk',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags']
},

];