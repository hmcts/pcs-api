export const peopleWillBeEvicted = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'The people who will be evicted',
  mainHeaderWarrantOfRestitution: 'The people who will be evicted (placeholder)',
  evictEveryOneQuestion: 'Do you want to evict everyone at the property?',
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  continueButton: 'Continue',
  thereIsAProblemErrorMessageHeader: 'There is a problem',
  errorValidation: 'YES',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Do you want to evict everyone at the property? is required' },
    ],
  },
}