import { v4 as uuidv4 } from 'uuid';
import { RuntimeUserAlias, type ProvisionedSolicitorCredentials, writeRuntimeSolicitorCredentials } from './runtimeUserCredentials';
import { getIdamRolesForAlias, getRdOrganisationInviteRolesForAlias } from './provisionRoleResolution';
import { createIdamSolicitorUser } from './idamCreateUserSupport';
import { assignSolicitorToTestOrganisation } from './professionalOrgAssignment';
import { waitForOrganisationDetailsByUserId } from './organisationPropagation';

/**
 * Full flow (EXUI-style dynamic solicitor, PCS-shaped):
 * 1) Resolve roles for alias
 * 2) Create professional user in IDAM (testing support)
 * 3) Assign user to TEST_SOLICITOR_ORGANISATION_ID (RD internal; optional Manage Org)
 * 4) Optionally wait until internal orgDetails returns
 *
 * Enable from global setup with E2E_DYNAMIC_SOLICITOR=true. Typical env:
 * TEST_SOLICITOR_ORGANISATION_ID, IDAM_PCS_USER_PASSWORD (new solicitor password),
 * CREATE_USER_BEARER_TOKEN or IDAM_SYSTEM_USERNAME/IDAM_SYSTEM_USER_PASSWORD,
 * PCS_API_IDAM_SECRET, RD + org-assignment auth (see orgAssignmentAuth / rdServiceAuth).
 * Local runs against AAT internals usually need VPN.
 */
export async function provisionDynamicSolicitorForAlias(
  alias: RuntimeUserAlias = RuntimeUserAlias.PCS_CLAIMANT_SOLICITOR
): Promise<ProvisionedSolicitorCredentials> {
  const password = process.env.IDAM_PCS_USER_PASSWORD;
  if (!password) {
    throw new Error('provisionDynamicSolicitorForAlias: IDAM_PCS_USER_PASSWORD is required (password for the new user).');
  }

  const organisationIdentifier = process.env.TEST_SOLICITOR_ORGANISATION_ID?.trim();
  if (!organisationIdentifier) {
    throw new Error('provisionDynamicSolicitorForAlias: TEST_SOLICITOR_ORGANISATION_ID is required.');
  }

  const unique = uuidv4();
  const email = `pcs-e2e.${unique}@test.test`;
  const forename = 'E2E';
  const surname = `Solicitor${unique.replace(/-/g, '').slice(0, 8)}`;

  const idamRoles = getIdamRolesForAlias(alias);
  const rdRoles = getRdOrganisationInviteRolesForAlias(alias);

  const uid = await createIdamSolicitorUser({
    email,
    password,
    forename,
    surname,
    roleNames: idamRoles,
  });

  await assignSolicitorToTestOrganisation({
    email,
    firstName: forename,
    lastName: surname,
    organisationIdentifier,
    roles: rdRoles,
  });

  const creds: ProvisionedSolicitorCredentials = {
    email,
    password,
    firstName: forename,
    lastName: surname,
    uid,
  };
  writeRuntimeSolicitorCredentials(creds);

  try {
    await waitForOrganisationDetailsByUserId(uid);
  } catch (err) {
    console.warn('Dynamic solicitor: organisation propagation check failed (non-fatal):', err);
  }

  return creds;
}
