export const legalCosts = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Legal costs',
  reclaimLegalCostsQuestion: 'Do you want to reclaim any legal costs?',
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  howMuchYouWantToReclaimTextLabel: 'How much do you want to reclaim?',
  howMuchYouWantToReclaimTextInput: ['755559.7','119999','1099.95','6320.00','890.10','111.01'],
  legalCostsFee: 'Legal costs',
  continueButton: 'Continue',
  errorValidation: 'YES',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorMoneyField: [
      { type: 'negativeDecimal', input: '-0.5', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'negative', input: '-5', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'zero', input: '0', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'max', input: '100000000', errMessage: 'Should be less than or equal to £10,000,000.00' },
      { type: 'alpha', input: 'test', errMessage: 'The data entered is not valid for How much do you want to reclaim?' },
      { type: 'decimal', input: '10.234', errMessage: 'The data entered is not valid for How much do you want to reclaim?' },
      { type: 'empty', input: '', errMessage: 'The data entered is not valid for How much do you want to reclaim?' },
    ],
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Do you want to reclaim any legal costs? is required' }
    ]
  }
}
