export const violentOrAggressiveBehaviour = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Their violent or aggressive behaviour',
  howHaveTheyBeenViolentAndAggressive: 'How have they been violent or aggressive?',
  howHaveTheyBeenViolentAndAggressiveInput: 'Test input for How have they been violent or aggressive?',
  continueButton: 'Continue',
  errors: 'The event could not be created',
  errorValidation: 'YES',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox',five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorTextField: [
      { type: 'moreThanMax', input: 'MAXPLUS', errMessage: 'In ‘How have they been violent or aggressive?’, you have entered more than the maximum number of characters (6,800)' },
      { type: 'empty', input: 'EMPTY', errMessage: 'How have they been violent or aggressive? is required' },
    ]
  }
}