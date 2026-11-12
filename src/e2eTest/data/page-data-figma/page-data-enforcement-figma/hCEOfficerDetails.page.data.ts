export const hCEOfficerDetails = {
  mainHeader: `Your High Court Enforcement Officer`,
  whoHaveYouHiredQuestion: `Who have you hired to carry out your eviction?`,
  nameOfYourHCEOTextLabel: `Name of your High Court Enforcement Officer`,
  ifYouDoNotKnowHintText: `If you do not know their name, use the name of the organisation they work for`,
  nameOfYourHCEOTextInput: `Morgan Freeman`,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
  cancelLink: `Cancel`,
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorTextField: [
      { type: 'moreThanMax', input: 120, errMessage: 'Name of your High Court Enforcement Officer exceeds the maximum length' },
      { type: 'empty', input: 'EMPTY', errMessage: 'Name of your High Court Enforcement Officer is required' },
    ]
  }
}
