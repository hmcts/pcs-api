export const users = [{
  user: 'creator',
  email: 'pcs-solicitor2@test.com',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'Service Request']
},
{
  user: 'claimantSolicitor',
  email: 'pcs-solicitor-user01@test.com',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'Service Request']
},
{
  user: 'defendantSolicitor',
  email: 'pcs-org1-solicitor2@test.com',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'Service Request']
},
{
  user: 'county court judge',
  email: 'DDJ.Randell.Lesch@ejudiciary.net',
  password: process.env.IDAM_PCS_USER_PASSWORD,
  tabAccess: ['Case Parties', 'Case Details', 'Case File View', 'Summary', 'History', 'Service Request', 'Notes', 'Linked Cases', 'Case flags']
},
{
  user: 'CTSC Team leader',
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