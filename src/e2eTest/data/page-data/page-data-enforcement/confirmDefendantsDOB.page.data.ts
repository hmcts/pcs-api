export const confirmDefendantsDOB = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Confirm if you know the defendants‘ dates of birth',
  defendantsDOBQuestion: 'Do you know the defendants‘ dates of birth?',
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  errorValidation: 'YES',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox',five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Do you know the defendants‘ dates of birth? is required' }
    ]
  },
  continueButton: 'Continue',
}