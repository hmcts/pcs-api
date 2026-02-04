export const nameAndAddressForEviction = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'The name and address for the eviction',
  nameAndAddressPageForEvictionQuestion: 'Is this the correct name and address for the eviction?',
  subHeaderAddress: 'Address',
  subHeaderDefendants: 'Defendants',
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  continueButton: 'Continue',
  errors: 'Errors',
  errorValidation: 'YES',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Is this the correct name and address for the eviction? is required' },
    ],
  }
}