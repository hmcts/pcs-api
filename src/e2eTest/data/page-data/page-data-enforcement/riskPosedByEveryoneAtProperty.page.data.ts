export const riskPosedByEveryoneAtProperty = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'The risks posed by everyone at the property',
  violentOrAggressiveBehaviourCheckbox: 'Violent or aggressive behaviour',
  historyOfFirearmPossessionCheckbox: 'History of firearm possession',
  criminalOrAntisocialBehaviourCheckbox: 'Criminal or antisocial behaviour',
  verbalOrWrittenThreatsCheckbox: 'Verbal or written threats',
  protestGroupCheckbox: 'Member of a group that protests evictions',
  policeOrSocialServiceCheckbox: 'Police or social services visits to the property',
  aggressiveAnimalsCheckbox: 'Aggressive dogs or other animals',
  kindOfRiskQuestion: 'What kind of risks do they pose to the bailiff?',
  continueButton: 'Continue',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorCheckBoxOption: [
      { type: 'none', input: '', errMessage: 'What kind of risks do they pose to the bailiff? is required' }
    ]
  }
}
