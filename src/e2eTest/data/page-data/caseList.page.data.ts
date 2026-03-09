import { getCaseTypeName } from '@utils/common/caseType.utils';

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
      civilPossessions: getCaseTypeName()
    },
  stateLabel: 'State',
  stateAwaitingSubmission: 'Awaiting Submission to HMCTS',
  stateAny: 'Any',
  apply: 'Apply'
}
