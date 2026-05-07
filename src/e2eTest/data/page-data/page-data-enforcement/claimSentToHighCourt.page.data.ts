export const claimSentToHighCourt = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Placeholder to simulate if claim has been sent to High Court',
  claimTransferredToHighCourtQuestion: 'Has the claim been transferred to the High Court?',
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Has the claim been transferred to the High Court? is required' }
    ]
  },
  continueButton: 'Continue'
}