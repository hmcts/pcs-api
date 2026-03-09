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
      civilPossessions: process.env.CASE_TYPE_SUFFIX
        ? `Civil Possessions ${process.env.CASE_TYPE_SUFFIX}`
        : 'Civil Possessions'
    },
  stateLabel: 'State',
  stateAwaitingSubmission: 'Awaiting Submission to HMCTS',
  stateAny: 'Any',
  apply: 'Apply'
}
