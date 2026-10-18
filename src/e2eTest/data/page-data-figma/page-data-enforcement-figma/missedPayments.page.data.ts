export const missedPayments = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Missed payments',
  haveDefendantsMissedPaymentsQuestion: 'Have the defendants missed any payments?',
  paymentsOutlinedHintText: `These are payments outlined in the suspended order for possession. They were ordered to make these payments by the judge`,
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
      { type: 'none', input: '', errMessage: 'Have the defendants missed any payments? is required' }
    ]
  },
  continueButton: 'Continue',
  previousButton: 'Previous',
  cancelLink: `Cancel`,
  missedPaymentsQuestion: 'Have the defendants missed any payments?',
  saveAndReturnLabel: 'I want to save this application and return to it later',
  saveAndReturnHideLabel: 'I want to save this application and return to it later',
  errorMessage: 'Have the defendants missed any payments? is required',
  saveAndReturnText: 'If you want to save your application and return to it later',
  permissionScreenHeader: 'You need permission from a judge to apply for the warrant of possession',
  permissionExplanationText: 'You need permission from a judge to apply for the warrant of possession',
  permissionLinkText: 'Ask the judge for permission to apply for a warrant of possession (GOV.UK, opens in a new tab)',
};
