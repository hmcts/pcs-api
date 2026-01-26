export const landRegistryFees = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Land Registry fees',
  mainHeaderPlaceholder: 'Land Registry fees (placeholder)',
  landRegistryFeeQuestion: 'Have you paid any Land Registry fees?',
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  howMuchYouSpendOnLandRegistryFeeTextLabel: 'How much did you spend on Land Registry fees?',
  howMuchYouSpendOnLandRegistryFeeTextInput: '10009.19',
  continueButton: 'Continue',
  landRegistryFee: 'Land Registry fees',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorMoneyField: [
      { type: 'negativeDecimal', input: '-0.5', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'negative', input: '-5', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'zero', input: '0', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'max', input: '100000000000000000', errMessage: '10000000000000000000 is not a valid value. Money GBP needs to be expressed in pence' },
      { type: 'alpha', input: 'test', errMessage: 'The data entered is not valid for How much did you spend on Land Registry fees?' },
      { type: 'decimal', input: '10.234', errMessage: 'The data entered is not valid for How much did you spend on Land Registry fees?' },
      { type: 'empty', input: '', errMessage: 'The data entered is not valid for How much did you spend on Land Registry fees?' },
    ],
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Have you paid any Land Registry fees? is required' }
    ]
  }
}
