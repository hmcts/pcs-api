export const judicial = {
  //Possession Fee Paid User
  mrs_justice_chavez_email: 'DDJ.Randell.Lesch@ejudiciary.net',
  //chief_CCJ_Judge_email: 'ChiefICCJudge.Nichols@ejudiciary.net'
} as const;
export const judicialNightly: readonly string[] = [
  judicial.mrs_justice_chavez_email
];

export const judicialFull: readonly string[] = Object.values(judicial);

export function judicialUserEmailsForScope(scope = process.env.E2E_TEST_SCOPE?.trim()): string[] {
  return scope === '@nightly' ? [...judicialNightly] : [...judicialFull];
}
