import { getCaseTypeName } from '@utils/common/caseType.utils';

export const createCase =
  {
    title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
    mainHeader: 'Create Case',
    jurisdictionLabel: 'Jurisdiction',
    caseTypeLabel: 'Case type',
    eventLabel: 'Event',
    //HDPI-5381 updated this from Possessions-> Civil Possession, however due to ccd config issue this change not reflected in AAT yet. As temp fix we have added this condition to keep the build green until ccd issue is resolved.
    possessionsJurisdiction: process.env.CHANGE_ID ? 'Civil Possession' : 'Possessions',
    //possessionsJurisdiction: 'Civil Possession',
    caseType:
      {
        civilPossessions: getCaseTypeName()
      },
    makeAPossessionClaimEvent: 'Make a claim',
    start: 'Start'
  };
