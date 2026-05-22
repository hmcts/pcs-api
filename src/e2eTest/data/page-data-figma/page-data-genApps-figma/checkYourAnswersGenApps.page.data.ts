export const checkYourAnswersGenApps = {
  mainHeader: `Check your answers`,
  sotHeader: `Statement of truth`,
  informationParagraph: `The information on this page forms your application.`,
  whenYourAreSatisfiedParagraph: `When you’re satisfied that your answers are correct, you should tick the box below and write your name to “sign” this statement of truth.`,
  iBelieveTheFactsHiddenCheckbox: `I believe that the facts stated in this form are true.`,
  iUnderstandParagraph: `I understand that proceedings for contempt of court may be brought against anyone who makes, or causes to be made, a false statement in a document verified by a statement of truth without an honest belief in its truth.`,
  continueToPaymentHiddenButton: `Continue to payment`,
  submitHiddenButton: `Submit`,
  yourFullNameTextLabel: `Your full name`,
  yourFullNameTextInput: `James Anderson`,
  statementOfTruthQuestion: `Statement of truth`,
  thereIsAProblemErrorMessageHeader: `There is a problem`,
  errorValidationType: { one: `radioOptions`, two: `textField`, three: `checkBox` },
  errorValidationField: {
    errorCheckBoxOption: [{ type: `none`, input: ``, errMessage: `Agree to the statement of truth` }],
    errorTextField: [
      { type: `empty`, input: `EMPTY`, errMessage: `You must sign your name` },
      { type: `moreThanMax`, input: 110, errMessage: `Your full name must be 100 characters or fewer` },
    ],
  },
};
