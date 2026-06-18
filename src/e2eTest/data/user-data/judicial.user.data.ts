export const judicial = {
  possessionFeePaid_Judge_email: 'DDJ.Randell.Lesch@ejudiciary.net',
  //possession_Leadership_Salaried_Judge_email: 'ChiefICCJudge.Nichols@ejudiciary.net',
  //possession_Circuit_Salaried_Judge_email: 'HHJ.Steven.Harper@ejudiciary.net',
  //possession_Salaried_Salaried_Judge_email: 'DDJ.Carrie.Cruz@ejudiciary.net'
} as const;
export const judicialNightly: readonly string[] = [
  //judicial.possessionFeePaid_Judge_email
];

export const judicialFull: readonly string[] = Object.values(judicial);

export function judicialUserEmailsForScope(scope = process.env.E2E_TEST_SCOPE?.trim()): string[] {
  return scope === '@nightly' ? [...judicialNightly] : [...judicialFull];
}
