export const cancelHearing= {
  mainHeader: `Cancel a hearing`,
  enterReasonForCancellationLabel: `Enter reason for cancellation`,
  reasonForCancellationTextInput: 14,
  thisOneIsRequiredHintText: `This will be included in the notice sent to parties, if one is required`,
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  errorValidation: `YES`,
  thereIsProbErrorMessageHeader: `There is a problem`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  errorValidationType: {
    one: `textField`,
    two: `radioOptions`,
    three: `checkBox`,
    four: `dropDown`,
    five: `dateField`,
    six: `dateRadioOption`,
    seven: `moneyField`
  },
  errorValidationField: {
    errorTextField: [
      { type: `moreThanMax`, input: 505, errMessage: `In ‘Enter reason for cancellation’, you have entered more than the maximum number of characters (500)` },
      { type: `empty`, input: `EMPTY`, errMessage: `Enter reason for cancellation is required`},
    ]
  },
};
