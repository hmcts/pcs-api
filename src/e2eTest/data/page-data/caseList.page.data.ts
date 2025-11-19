export const caseList = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Case list',
  jurisdictionLabel: 'Jurisdiction',
  caseTypeLabel: 'Case type',
  eventLabel: 'Event',
  caseNumberLabel: 'Case Number',
  possessionsJurisdiction: 'Possessions',
  caseType:
    {
      civilPossessions: process.env.CHANGE_ID
        ? `Civil Possessions ${process.env.CHANGE_ID}`
        : 'Civil Possessions'
    },
  stateLabel: 'State',
  stateAwaitingSubmission: 'Awaiting Submission to HMCTS',
  stateAny: 'Any',
  apply: 'Apply'
}
