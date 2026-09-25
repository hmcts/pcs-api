export const courtPermission = {
  mainHeader: `Court permission`,
  hasTheCOurtGivenPermissionQuestion: `Has the court given the party permission to enter a counterclaim?`,
  warningTextHidden: `You should check whether permission is required. If it is, you must advise the party to get permission from the court to make their counterclaim.`,
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  dateTypeGrantPermissionHiddenUserInput: `past`,
  grantPermissionHiddenLabel: `Enter the date the order was made granting permission`,
  partySubmittedCCHiddenQuestion: `Which party submitted the counterclaim?`,
  dateTypeCCReceivedHiddenUserInput: `present`,
  ccReceivedDateHiddenLabel: `When was the counterclaim received?`,
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  dayHiddenTextLabel: `Day`,
  monthHiddenTextLabel: `Month`,
  yearHiddenTextLabel: `Year`,
  errorValidation: `YES`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  thereIsProbErrorMessageHeader: `There is a problem`,
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown`, five: `dateField`, six: `dateRadioOption` },
  errorValidationField: {
    errorDateField: [
      { type: `empty`, input: `empty`, multiField: 0, errMessage: `Enter the date the order was made granting permission is required`, errInlineMessage: `Enter the date the order was made granting permission is required` },
      { type: `invalid`, input: `invalid`, multiField: 0, errMessage: `Enter the date the order was made granting permission is not valid`, errInlineMessage: `The data entered is not valid for Enter the date the order was made granting permission` },
      { type: `past`, input: `past`, multiField: 0, errMessage: ``, errInlineMessage: `` },
    ],
    errorDateField1: [
      { type: `empty`, input: `empty`, multiField: 1, errMessage: `When was the counterclaim received? is required`, errInlineMessage: `When was the counterclaim received? is required` },
      { type: `invalid`, input: `invalid`, multiField: 1, errMessage: `When was the counterclaim received? is not valid`, errInlineMessage: `The data entered is not valid for When was the counterclaim received?` },
      { type: `future`, input: `future`, multiField: 1, errMessage: `Date the counterclaim was received must not be in the future`, errInlineMessage: `Date the counterclaim was received must not be in the future` },
    ],
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Has the court given the party permission to enter a counterclaim? is required`, errInlineMessage: `Has the court given the party permission to enter a counterclaim? is required` }
    ],
    errorRadioOption1: [
      { type: `none`, input: ``, errMessage: `Which party submitted the counterclaim? is required`, errInlineMessage: `Field is required` }
    ],
    errorDateRadioOption: [
      { type: `Max`, input: `510`, errMessage: `In ‘Description of review’, you have entered more than the maximum number of characters (500)` }
    ]
  },
};
