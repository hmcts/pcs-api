/**
 * Who the browser is signed in as, within one scenario.
 *
 * Each test starts from `storageState`, i.e. signed in as the claimant's legal representative. A scenario
 * that swaps to another role and later needs the claimant back — to check that a tab really is internal
 * only, for instance — cannot rely on the storage state any more, so the sign-in helpers record the
 * current role here and sign in again when it has changed.
 *
 * Reset by the `enforcementExecutor` fixture before each scenario, because the module outlives a test
 * within a worker process.
 */
export const CLAIMANT_LEGAL_REPRESENTATIVE = 'claimant legal representative';

let signedInRole: string = CLAIMANT_LEGAL_REPRESENTATIVE;

export const currentSignedInRole = (): string => signedInRole;

export const recordSignedInRole = (role: string): void => {
  signedInRole = role;
};

export const resetSignedInRole = (): void => {
  signedInRole = CLAIMANT_LEGAL_REPRESENTATIVE;
};
