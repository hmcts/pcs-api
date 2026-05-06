import axios from 'axios';
import { manageOrgApiData } from '@data/api-data';

export type ManageOrgInviteParams = {
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
};

function shouldAttemptManageOrgInvite(): boolean {
  const mode = process.env.E2E_ORG_ASSIGNMENT_MODE?.trim().toLowerCase() || 'rd';
  return mode === 'both' || mode === 'manage_org' || mode === 'manage_org_first';
}

function resolveManageOrgBaseUrl(): string | null {
  const explicit = process.env.MANAGE_ORG_API_URL?.trim();
  if (explicit) {
    return explicit.replace(/\/$/, '');
  }
  if (process.env.E2E_MANAGE_ORG_USE_INTERNAL_FALLBACK === 'true') {
    return manageOrgApiData.defaultInternalBaseUrlAat.replace(/\/$/, '');
  }
  return null;
}

/**
 * Optional Manage Organisation `POST /api/inviteUser` (EXUI parity). Requires a real user/session JWT.
 * Skipped unless E2E_ORG_ASSIGNMENT_MODE is `manage_org`, `manage_org_first`, or `both`.
 */
export async function inviteUserViaManageOrgIfConfigured(params: ManageOrgInviteParams): Promise<void> {
  if (!shouldAttemptManageOrgInvite()) {
    return;
  }

  const base = resolveManageOrgBaseUrl();
  const token = process.env.E2E_MANAGE_ORG_USER_TOKEN?.trim().replace(/^Bearer\s+/i, '');
  if (!base || !token) {
    console.warn(
      'Manage Org invite skipped: set MANAGE_ORG_API_URL (or E2E_MANAGE_ORG_USE_INTERNAL_FALLBACK=true) and E2E_MANAGE_ORG_USER_TOKEN.'
    );
    return;
  }

  const url = `${base}/api/inviteUser`;
  try {
    const { status, data } = await axios.post(
      url,
      {
        firstName: params.firstName,
        lastName: params.lastName,
        email: params.email.toLowerCase(),
        roles: params.roles,
      },
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        validateStatus: () => true,
      }
    );
    if (status >= 200 && status < 300) {
      return;
    }
    console.warn(`Manage Org invite returned HTTP ${status}: ${JSON.stringify(data)}`);
  } catch (err) {
    console.warn('Manage Org invite failed (non-fatal):', err);
  }
}
