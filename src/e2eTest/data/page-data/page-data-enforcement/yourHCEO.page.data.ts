export const yourHCEO = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Your High Court enforcement officer',
  nameOfYourHCEOLabel: 'Name of your High Court enforcement officer',
  nameOfYourHCEOInput: 'Morgan Freeman',
  continueButton: 'Continue',
  errors: 'There is a problem',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorTextField: [
      { type: 'moreThanMax', input: 120, errMessage: 'Name of your High Court enforcement officer exceeds the maximum length' },
      { type: 'empty', input: 'EMPTY', errMessage: 'Name of your High Court enforcement officer is required' },
    ]
  }
}