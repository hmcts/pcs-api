const CASE_TYPE_BASE = 'PCS';
const CASE_TYPE_NAME_BASE = 'Civil Possessions';

/**
 * Case type ID for API paths and URLs (e.g. PCS, PCS-123, PCS-staging).
 * Aligns with Java CaseType.getCaseType() using CASE_TYPE_SUFFIX.
 */
export function getCaseTypeId(): string {
  const suffix = process.env.CASE_TYPE_SUFFIX;
  return suffix ? `${CASE_TYPE_BASE}-${suffix}` : CASE_TYPE_BASE;
}

/**
 * Case type display name for UI (e.g. Civil Possessions, Civil Possessions 123).
 * Aligns with Java CaseType.getCaseTypeName() using CASE_TYPE_SUFFIX.
 */
export function getCaseTypeName(): string {
  const suffix = process.env.CASE_TYPE_SUFFIX;
  return suffix ? `${CASE_TYPE_NAME_BASE} ${suffix}` : CASE_TYPE_NAME_BASE;
}
