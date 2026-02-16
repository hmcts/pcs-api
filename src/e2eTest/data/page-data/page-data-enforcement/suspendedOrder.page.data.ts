export const suspendedOrder = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Suspended order',
  suspendedOrderQuestion: 'Is your order a suspended order?',
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  errorValidation: 'YES',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox',five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Is your order a suspended order? is required' }
    ]
  },
  continueButton: 'Continue'
};