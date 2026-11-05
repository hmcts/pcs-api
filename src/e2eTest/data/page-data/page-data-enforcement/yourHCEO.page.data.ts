export const yourHCEO = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Your High Court Enforcement Officer',
  nameOfYourHCEOTextLabel: 'Name of your High Court Enforcement Officer',
  nameOfYourHCEOTextInput: 'Morgan Freeman',
  continueButton: 'Continue',
  thereIsAProblemErrorMessageHeader: 'There is a problem',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorTextField: [
      { type: 'moreThanMax', input: 120, errMessage: 'Name of your High Court Enforcement Officer exceeds the maximum length' },
      { type: 'empty', input: 'EMPTY', errMessage: 'Name of your High Court Enforcement Officer is required' },
    ]
  }
}