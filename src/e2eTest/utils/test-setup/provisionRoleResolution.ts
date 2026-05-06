import { RuntimeUserAlias } from './runtimeUserCredentials';

/**
 * IDAM role names for professional EXUI / Manage Case users (AAT-style).
 * Adjust via env E2E_DYNAMIC_SOLICITOR_IDAM_ROLES as comma-separated list if defaults are wrong.
 */
const DEFAULT_IDAM_ROLES: string[] = [
  'pui-case-manager',
  'pui-user-manager',
  'pui-organisation-manager',
];

/** RD `NewUserCreationRequest.roles` — typically same PUI role codes. */
const DEFAULT_RD_ROLES: string[] = [...DEFAULT_IDAM_ROLES];

function parseRoleEnv(raw: string | undefined, fallback: string[]): string[] {
  if (!raw?.trim()) {
    return fallback;
  }
  return raw
    .split(',')
    .map(r => r.trim())
    .filter(Boolean);
}

export function getIdamRolesForAlias(alias: RuntimeUserAlias): string[] {
  switch (alias) {
    case RuntimeUserAlias.PCS_CLAIMANT_SOLICITOR:
      return parseRoleEnv(process.env.E2E_DYNAMIC_SOLICITOR_IDAM_ROLES, DEFAULT_IDAM_ROLES);
    default:
      return parseRoleEnv(process.env.E2E_DYNAMIC_SOLICITOR_IDAM_ROLES, DEFAULT_IDAM_ROLES);
  }
}

export function getRdOrganisationInviteRolesForAlias(alias: RuntimeUserAlias): string[] {
  switch (alias) {
    case RuntimeUserAlias.PCS_CLAIMANT_SOLICITOR:
      return parseRoleEnv(process.env.E2E_DYNAMIC_SOLICITOR_RD_ROLES, DEFAULT_RD_ROLES);
    default:
      return parseRoleEnv(process.env.E2E_DYNAMIC_SOLICITOR_RD_ROLES, DEFAULT_RD_ROLES);
  }
}
