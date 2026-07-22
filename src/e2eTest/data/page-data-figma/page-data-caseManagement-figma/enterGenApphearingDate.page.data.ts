export const enterGenAppHearingDate = {
  mainHeader: `Hearing date`,
  hearingInNext14DaysQuestion: `Is there a hearing for this case in the next 14 days?`,
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  errorValidation: `YES`,
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown`, five: `dateField` },
  errorValidationField: {
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Is there a hearing for this case in the next 14 days? is required`, errInlineMessage: `Is there a hearing for this case in the next 14 days? is required` },
    ],
  },
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
};
