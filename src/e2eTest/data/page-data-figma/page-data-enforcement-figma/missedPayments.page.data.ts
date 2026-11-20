export const missedPayments = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: `Missed payments`,
  haveDefendantsMissedPaymentsQuestion: `Are you alleging that the defendant has failed to make payments which are a condition of the suspension of the order for possession?`,
  thisWillSaveHiddenParagraph: `This will save your progress and take you to the case overview.`,
  chooseHiddenListItem: `Choose ‘Continue’.`,
  cancelHiddenListItem: `On the next page choose ‘Cancel’.`,
  ifYouWantToSaveHiddenText: `If you want to save your application and return to it later:`,
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  iWantToSaveThisApplicationAndReturnToItLaterLink: `I want to save this application and return to it later`,
  paymentsHintText: `These are payments outlined in the suspended order for possession. They were ordered to make these payments by the judge.`,
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Are you alleging that the defendant has failed to make payments which are a condition of the suspension of the order for possession?? is required' }
    ]
  },
  continueButton: 'Continue',
  previousButton: 'Previous',
  cancelLink: `Cancel`,
}
