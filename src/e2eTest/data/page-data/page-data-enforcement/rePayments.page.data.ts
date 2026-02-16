export const rePayments = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Repayments',
  rePaymentQuestion: 'How much do you want the defendants to repay?',
  rePaymentRadioOptions: {
    all: 'All of it',
    some: 'Some of it',
    none: 'None of it',
  },
  enterTheAmountTextLabel: 'Enter the amount that you want the defendants to repay',
  enterTheAmountTextInput: '100.50',
  continueButton: 'Continue',
  totalAmt: 'Total',
  errorValidation: 'YES',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorMoneyField: [
      { type: 'negativeDecimal', input: '-0.5', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'negative', input: '-5', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'zero', input: '0', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'max', input: '100000000000000000', errMessage: '10000000000000000000 is not a valid value. Money GBP needs to be expressed in pence' },
      { type: 'alpha', input: 'test', errMessage: 'The data entered is not valid for Enter the amount that you want the defendants to repay' },
      { type: 'decimal', input: '10.234', errMessage: 'The data entered is not valid for Enter the amount that you want the defendants to repay' },
      { type: 'empty', input: '', errMessage: 'The data entered is not valid for Enter the amount that you want the defendants to repay' },
    ],
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'How much do you want the defendants to repay? is required' }
    ]
  }
}
