export const haveYouAlreadyAppliedForHelpWithFees = {
  mainHeader: `Have you already applied for help with your application fee?`,
  haveYouAlreadyAppliedForHelpQuestion: `Have you already applied for help with your application fee?`,
  yesRadioOption: `Yes`,
  hwfReferenceHiddenTextLabel: `Enter your Help with Fees reference number`,
  hwfReferenceTextInput: 10,
  noRadioOption: `No`,
  continueButton: `Continue`,
  cancelLink: `Cancel`,
  thereIsAProblemErrorMessageHeader: `There is a problem`,
  errorValidationType: { one: `radioOptions`, two: `textField`, three: `checkBox` },
  errorValidationField: {
    errorRadioOption: {
      type: `none`,
      input: ``,
      errMessage: `Confirm whether you have already applied for help with your application fee`,
    },
    errorTextField: [
      { type: 'empty', input: 'EMPTY', errMessage: 'You must enter a reference number for Help with Fees' },
      { type: 'moreThanMax', input: 65, errMessage: 'Must be 60 characters or fewer' },
    ],
  },
};
