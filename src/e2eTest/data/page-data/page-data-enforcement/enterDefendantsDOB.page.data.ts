export const enterDefendantsDOB = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Enter the defendants’ dates of birth',
  defendantsDOBTextLabel: 'What are the defendants’ dates of birth?',
  errorValidation: 'YES',//set it to 'NO' before raising a PR
  errors: 'Errors',
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Do you know the defendants’ dates of birth? is required' }
    ],
    errorTextField: [
      { type: 'moreThanMax', input: 'MAXPLUS', errMessage: 'In ‘What are the defendants’ dates of birth?’, you have entered more than the maximum number of characters (6,800)' },
      { type: 'empty', input: 'EMPTY', errMessage: 'What are the defendants’ dates of birth? is required' },
    ]
  },
  continueButton: 'Continue'
}