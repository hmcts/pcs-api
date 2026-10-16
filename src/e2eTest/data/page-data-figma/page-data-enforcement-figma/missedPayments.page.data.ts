export const missedPayments = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Missed payments',
  haveDefendantsMissedPaymentsQuestion: 'Have the defendants missed any payments?',
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  iWantToSaveThisApplicationAndReturnToItLaterLink: `I want to save this application and return to it later`,
  paymentsHintText: `These are payments outlined in the suspended order for possession. They were ordered to make these payments by the judge.`,
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Have the defendants missed any payments? is required' }
    ]
  },
  continueButton: 'Continue',
  previousButton: 'Previous'
}
