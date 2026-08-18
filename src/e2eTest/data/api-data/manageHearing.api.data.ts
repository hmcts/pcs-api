export const manageHearingApiData = {
 manageHearingEventName: 'manageHearing',
  AddHearingPayload: {
    hearingLocation: 'Central London County Court',
    hearing_AdditionalInformation: 'hearingNoticeDetails',
    hearing_Date: '2027-08-21T00:00:00',
    hearing_DurationHours: '1',
    hearing_DurationMinutes: '30',
    hearing_IssueNotice: 'NO',
    hearing_Notes: null,
    hearing_NoticeWording: 'TPL',
    hearing_Type: 'APPLICATION'
  },
  manageHearingApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/events`,
};
