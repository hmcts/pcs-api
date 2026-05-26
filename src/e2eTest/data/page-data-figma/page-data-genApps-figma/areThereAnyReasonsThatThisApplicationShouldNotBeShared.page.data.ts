export const areThereAnyReasonsThatThisApplicationShouldNotBeShared = {
  mainHeader: `Are there any reasons that this application should not be shared with the other parties?`,
  weUsuallySendParagraph: `We usually send a copy of your application to the other parties (your landlord, housing association or mortgage lender). This gives them the opportunity to respond to it.`,
  inSomeExceptionalParagraph: `In some exceptional circumstances, the judge will consider your application without telling the other party first.`,
  forExampleParagraph: `For example, if:`,
  itIsSoUrgentList: `it is so urgent that there is not enough time to give notice`,
  givingSomeoneList: `giving someone notice could undermine the order you want the court to grant.`,
  youBelieveList: `you believe you are at risk from the other party`,
  warningText: `We will ask you to provide the reason. The court will consider your reason, and you may not be successful.`,
  areThereAnyReasonQuestion: `Are there any reasons that this application should not be shared with the other parties?`,
  cymraegLink: `Cymraeg`,
  backLink: `Back`,
  feedbackLink: `feedback (opens in new tab)`,
  continueButton: `Continue`,
  cancelLink: `Cancel`,
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  provideReasonHiddenTextLabel: `Provide a reason this application should not be shared with the other party`,
  provideReasonTextInput: 6000,
  thereIsAProblemErrorMessageHeader: `There is a problem`,
  errorValidationType: { one: `radioOptions`, two: `textField`, three: `checkBox` },
  errorValidationField: {
    errorRadioOption: {
      type: `none`,
      input: ``,
      errMessage: `Confirm whether there is a reason why this application should not be shared with the other parties`,
    },

    errorTextField: [
      { type: 'empty', input: 'EMPTY', errMessage: 'Confirm why the other parties should not be informed' },
      { type: 'moreThanMax', input: 6900, errMessage: 'Must be 6800 characters or fewer' },
    ],
  },
};
