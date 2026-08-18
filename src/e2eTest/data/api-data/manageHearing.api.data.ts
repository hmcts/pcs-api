import {
  CaseManagementCommonUtils
} from "@utils/actions/custom-actions/custom-actions-caseManagement/caseManagementUtils.action";

let date = CaseManagementCommonUtils.getRandomDate('future', 'dateTime');
const [day, month, year] = date.split('/');
const hearingDate = `${year}-${month}-${day}T00:00:00`;

export const manageHearingApiData = {
  manageHearingEventName: 'manageHearing',
  manageHearingApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/events`,
  AddHearingPayload: {
    hearingLocation: 'Central London County Court',
    hearing_AdditionalInformation: 'hearingNoticeDetails',
    hearing_Date: hearingDate,
    hearing_DurationDays: '3',
    hearing_DurationHours: '1',
    hearing_DurationMinutes: '30',
    hearing_IssueNotice: 'NO',
  }
};
