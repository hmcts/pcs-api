export const judicial = {
  possessionFeePaid_Judge_email: 'DDJ.Randell.Lesch@ejudiciary.net',
  possession_Salaried_Judge_email1: 'Master.Mayer@ejudiciary.net', //'HHJ.Steven.Harper@ejudiciary.net', //'ChiefICCJudge.Nichols@ejudiciary.net',
  //Duplicate case flags are appearing for below users, so disabled until https://tools.hmcts.net/jira/browse/HDPI-7507
  // possession_Leadership_Salaried_Judge_email: 'ChiefICCJudge.Nichols@ejudiciary.net',
  // possession_Circuit_Salaried_Judge_email: 'HHJ.Steven.Harper@ejudiciary.net',
  possession_Circuit_Judge_FeePaid_Judge_email: 'HHJ.Test.McGinn@judicialofficeelinkssw.onmicrosoft.com',
  possession_Salaried_Judge_email: 'ICCJudge.Barry@ejudiciary.net'
} as const;

export const judicialUsers: readonly string[] = Object.values(judicial);
