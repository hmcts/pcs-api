export const peopleYouWantToEvict = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'The people you want to evict',
  whoDoYouWantToEvictQuestion: 'Who do you want to evict?',
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  continueButton: 'Continue',
  thereIsAProblemErrorMessageHeader: 'There is a problem',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption', six: 'checkBoxPageLevel' },
  errorValidationField: {
    errorCheckBoxOption: [
      { type: 'none', input: '', errMessage: 'Who do you want to evict? is required' }
    ]
  }
}