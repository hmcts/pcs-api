import axios from 'axios';
import { referenceDataApiData } from '@data/api-data';
import { getRdS2SToken } from './rdServiceAuth';
import { getOrganisationAssignmentUserBearer } from './orgAssignmentAuth';
import { inviteUserViaManageOrgIfConfigured } from './manageOrgInvite';

export type AssignSolicitorParams = {
  email: string;
  firstName: string;
  lastName: string;
  organisationIdentifier: string;
  roles: string[];
};

/**
 * Invites/assigns the user to the test organisation: optional Manage Org, then RD internal
 * `POST .../organisations/{orgId}/users/`.
 */
export async function assignSolicitorToTestOrganisation(params: AssignSolicitorParams): Promise<void> {
  const mode = process.env.E2E_ORG_ASSIGNMENT_MODE?.trim().toLowerCase() || 'rd';

  if (mode === 'manage_org_first' || mode === 'both') {
    await inviteUserViaManageOrgIfConfigured({
      email: params.email,
      firstName: params.firstName,
      lastName: params.lastName,
      roles: params.roles,
    });
  }

  const base =
    process.env.RD_PROFESSIONAL_API_URL?.trim().replace(/\/$/, '') ||
    referenceDataApiData.defaultInternalBaseUrlAat.replace(/\/$/, '');

  const fixedS2s = process.env.E2E_RD_SERVICE_AUTHORIZATION?.trim();
  const serviceAuth = fixedS2s
    ? fixedS2s.replace(/^Bearer\s+/i, '')
    : await getRdS2SToken();

  const userAuth = await getOrganisationAssignmentUserBearer();

  const url = `${base}/refdata/internal/v1/organisations/${params.organisationIdentifier}/users/`;

  const res = await axios.post(
    url,
    {
      firstName: params.firstName,
      lastName: params.lastName,
      email: params.email.toLowerCase(),
      roles: params.roles,
      resendInvite: false,
    },
    {
      headers: {
        'Content-Type': 'application/json',
        ServiceAuthorization: `Bearer ${serviceAuth}`,
        Authorization: `Bearer ${userAuth}`,
      },
      validateStatus: () => true,
    }
  );

  if (res.status !== 201 && res.status !== 200) {
    throw new Error(
      `RD add user failed: HTTP ${res.status} for POST ${url}. Body: ${JSON.stringify(res.data)}`
    );
  }

  if (mode === 'manage_org') {
    await inviteUserViaManageOrgIfConfigured({
      email: params.email,
      firstName: params.firstName,
      lastName: params.lastName,
      roles: params.roles,
    });
  }
}
