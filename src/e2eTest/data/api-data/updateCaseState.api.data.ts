
export const updateCaseStateApiData = {
  changeStateEventName: 'changeCaseState',
  updateCaseStatePayload:(targetState: string) => ({
    targetState
  }),
  updateCaseStateApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/events`,
}
