export const moneyOwed = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'The amount the defendants owe you',
  totalAmountOwedTextLabel: 'What is the total amount that the defendants owe you?',
  totalAmountOwedTextInput: '9999.99',
  continueButton: 'Continue',
  arrearsAndOtherCosts: 'Arrears and other costs',
  errorValidation: 'YES',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox',five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorMoneyField: [
      { type: 'negativeDecimal', input: '-0.5', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'negative', input: '-5', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'zero', input: '0', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'max', input: '100000000', errMessage: 'Should be less than or equal to £10,000,000.00' },
      { type: 'alpha', input: 'test', errMessage: 'The data entered is not valid for What is the total amount that the defendants owe you?' },
      { type: 'decimal', input: '10.234', errMessage: 'The data entered is not valid for What is the total amount that the defendants owe you?' },
      { type: 'empty', input: '', errMessage: 'The data entered is not valid for What is the total amount that the defendants owe you?' },
    ]
  }
}