/**
 * Dynamic solicitor provisioning for PCS e2e (pattern inspired by EXUI / rpx-xui-webapp).
 *
 * Enable in global setup with E2E_DYNAMIC_SOLICITOR=true. Writes `.auth/runtime-solicitor.json`;
 * `user.claimantSolicitor` resolves via getter when that flag is set.
 *
 * Typical env: TEST_SOLICITOR_ORGANISATION_ID, IDAM_PCS_USER_PASSWORD, PCS_API_IDAM_SECRET,
 * CREATE_USER_BEARER_TOKEN or IDAM_SYSTEM_USERNAME/IDAM_SYSTEM_USER_PASSWORD,
 * RD_PROFESSIONAL_API_URL (optional; defaults to AAT internal),
 * E2E_RD_S2S_MICROSERVICE (default rd_professional_api), E2E_RD_S2S_SECRET or PCS_API_S2S_SECRET,
 * E2E_RD_USER_AUTHORIZATION or PCS_PRD_ADMIN_* / PCS_ORG_ADMIN_*.
 * Optional Manage Org: E2E_ORG_ASSIGNMENT_MODE, MANAGE_ORG_API_URL, E2E_MANAGE_ORG_USER_TOKEN.
 */

export * from './runtimeUserCredentials';
export * from './provisionRoleResolution';
export * from './idamCreateUserSupport';
export * from './orgAssignmentAuth';
export * from './manageOrgInvite';
export * from './professionalOrgAssignment';
export * from './organisationPropagation';
export * from './rdServiceAuth';
export * from './dynamicSolicitorSession';
