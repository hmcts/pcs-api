export const missedPayments = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Missed payments',
  haveDefendantsMissedPaymentsQuestion: 'Have the defendants missed any payments?',
  paymentsOutlinedHintText: `These are payments outlined in the suspended order for possession. They were ordered to make these payments by the judge`,
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
  previousButton: 'Previous',
  cancelLink: `Cancel`,
  missedPaymentsQuestion: 'Have the defendants missed any payments?',
  errorMessage: 'Have the defendants missed any payments? is required',
  permissionScreenHeader: 'You need permission from a judge to apply for the warrant of possession',
  permissionExplanationText: 'You need permission from a judge to apply for the warrant of possession',
  permissionLinkText: 'Ask the judge for permission to apply for a warrant of possession (GOV.UK, opens in a new tab)',
};
