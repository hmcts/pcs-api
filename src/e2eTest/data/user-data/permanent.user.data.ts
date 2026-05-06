import { readRuntimeSolicitorCredentials } from '@utils/test-setup/runtimeUserCredentials';

export type ClaimantSolicitorUser = {
  email: string;
  password: string | undefined;
  uid?: string;
};

function resolveStaticClaimantSolicitor(): ClaimantSolicitorUser {
  return {
    email: 'pcs-solicitor-automation@test.com',
    password: process.env.IDAM_PCS_USER_PASSWORD,
    uid: process.env.PCS_SOLICITOR_AUTOMATION_UID,
  };
}

/**
 * When E2E_DYNAMIC_SOLICITOR=true, reads `.auth/runtime-solicitor.json` written by global setup.
 * Uses a getter so imports after global setup see the provisioned user.
 */
export function getClaimantSolicitorForSession(): ClaimantSolicitorUser {
  if (process.env.E2E_DYNAMIC_SOLICITOR === 'true') {
    const runtime = readRuntimeSolicitorCredentials();
    if (runtime) {
      return {
        email: runtime.email,
        password: runtime.password,
        uid: runtime.uid,
      };
    }
    throw new Error(
      'E2E_DYNAMIC_SOLICITOR is true but runtime solicitor credentials are missing. Global setup should provision the user first.'
    );
  }
  return resolveStaticClaimantSolicitor();
}

export const user = {
  get claimantSolicitor(): ClaimantSolicitorUser {
    return getClaimantSolicitorForSession();
  },
  caseworker: {
    email: 'pcs-caseworker@test.com',
    password: process.env.IDAM_PCS_USER_PASSWORD,
  },
};
