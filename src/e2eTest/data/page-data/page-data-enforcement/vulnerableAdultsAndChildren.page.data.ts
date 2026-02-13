export const vulnerableAdultsAndChildren = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Vulnerable adults and children at the property',
  IsAnyOneLivingAtThePropertyQuestion: 'Is anyone living at the property vulnerable?',
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  notSureRadioOption: 'I’m not sure',
  confirmVulnerablePeopleQuestion: 'Confirm if the vulnerable people in the property are adults, children, or both adults and children',
  vulnerableAdultsRadioOption: 'Vulnerable adults',
  vulnerableChildrenRadioOption: 'Vulnerable children',
  vulnerableAdultsAndChildrenRadioOption: 'Vulnerable adults and children',
  howAreTheyVulnerableTextLabel: 'How are they vulnerable?',
  howAreTheyVulnerableTextInput: 'Test Input How are they vulnerable',
  continueButton: 'Continue',
  errors: 'The event could not be created',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption1: [
      { type: 'multi', input: '', errMessage: 'Is anyone living at the property vulnerable? is required' },
    ],
    errorRadioOption2: [
      { type: 'none', input: '', errMessage: 'Confirm if the vulnerable people in the property are adults, children, or both adults and children is required' },
    ],
    errorTextField: [
      { type: 'moreThanMax', input: 'MAXPLUS', errMessage: 'In ‘How are they vulnerable?’, you have entered more than the maximum number of characters (6,800)' },
      { type: 'empty', input: 'EMPTY', errMessage: 'How are they vulnerable? is required' },
    ]
  }
}
