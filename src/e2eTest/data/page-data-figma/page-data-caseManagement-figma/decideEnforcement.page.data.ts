export const decideEnforcement = {
  mainHeader: `Decide enforcement`,
  acceptOrRejectQuestion: `Would you like to accept or reject the warrant/writ?`,
  acceptRadioOption: `Accept`,
  rejectRadioOption: `Reject`,
  rejectHiddenOptions : [`Change in party circumstances`,`Applying to enforce out of time`,`Order not enforceable`,`Possession order set aside or suspended`,`Applying to enforce before the possession date`,`Case not transferred to high court`,`Another possession enforcement journey ongoing`,`Defendant(s) in breathing space`,`Other`],
  selectReasonHiddenLabel: `Select your reason for rejection`,
  explainOtherHiddenTextLabel: `Explain why you are rejecting the warrant or writ`,
  rejectHiddenNotes: 50,
  errorValidation: `YES`,
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown` },
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: `Would you like to accept or reject the warrant/writ? is required`, errInlineMessage: `Would you like to accept or reject the warrant/writ? is required` }
    ],
    errorRadioOption1: [
      { type: 'none', input: '', errMessage: `Select your reason for rejection is required`, errInlineMessage: `Select your reason for rejection is required` }
    ],
    errorTextField: [
      { type: `moreThanMax`, input: 6805, errMessage: `In ‘Explain why you are rejecting the warrant or writ’, you have entered more than the maximum number of characters (6,800)` },
      { type: `empty`, input: `EMPTY`, errMessage: `Explain why you are rejecting the warrant or writ is required`},
    ]
  },
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
};
