export const confirmHCEOHired = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Confirm if you have already hired a High Court enforcement officer',
  haveYouHiredHCEOQuestion: 'Have you hired a High Court enforcement officer?',
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  continueButton: 'Continue',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Have you hired a High Court enforcement officer? is required' }
    ]
  }
}