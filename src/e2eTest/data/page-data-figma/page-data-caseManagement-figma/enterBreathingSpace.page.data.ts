export const enterBreathingSpace = {
  mainHeader: 'Enter breathing space',
  referenceNumberTextLabel: 'Reference Number (Optional)',
  whenDidBreathingSpaceStartQuestion: 'When did breathing space start? (Optional)',
  youShouldEnterStartDateHintText:
    'You should enter the date it started, not the date you received notification of it. For example, 15 11 2020',
  whichTypeOfBreathingSpace: 'Which type of breathing space is it? (Optional)',
  standardBreathingSpaceHintText:
    'Standard breathing space can only be used on a possession claim on the grounds of rent arrears',
  mentalHealthRadioOption: 'Mental Health Crisis Moratorium',
  standardBreathingSpaceRadioOption: 'Standard Breathing Space',
  whenIsExpectedEndDateHiddenTextLabel: 'When is the expected end date? (Optional)',
  whenIsExpectedEndDateHiddenHintText: 'For example, 17 11 2020',
  dateTypeHiddenUserInput: 'validFuture',
  dayTextLabel: 'Day',
  monthTextLabel: 'Month',
  yearTextLabel: 'Year',
  errorValidation: 'YES',
  eventCouldNotBeCreatedHeader: 'The event could not be created',
  eventCouldNotBeCreatedErrorMessageHeader: 'The event could not be created',
  thereIsProbErrorMessageHeader: 'There is a problem',
  continueButton: 'Continue',
  previousButton: 'Previous',
  cancelLink: 'Cancel',
  cancelConfig: {
    referenceNumber: 'CANCEL_REF_777',
    startDate: '01/01/2024',
    targetType: 'standard',
  },
  cancelReturnHeader: 'Summary',
  invalidFutureDate: {
    day: '18',
    month: '06',
    year: '2032',
  },
  happyPathConfig: {
    referenceNumber: 'REF123',
    targetType: 'standard',
  },
  
  errorValidationType: {
    one: 'textField',
    two: 'radioOptions',
    three: 'checkBox',
    four: 'dropDown',
    five: 'dateField',
    six: 'dateRadioOption',
    seven: 'moneyField',
  },
  errorValidationField: {
  errorTextField: [
    {
      type: 'moreThanMax',
      input: 15,
      errMessage: 'Case data validation failed',
      errDetailMessage: '(15 characters) exceeds the maximum length of 14',
    },
    { type: 'valid', input: 14 },
  ],

   errorDateField: [
  { type: 'future', day: '18', month: '06', year: '2050', errMessage: 'Date Breathing Space started must be today or in the past' },
  { type: 'past', errMessage: '' },
],
  },
};
