export const addReviewDates = {
  mainHeader: `Review dates`,
  addReviewDateHintText: `Add review date`,
  reviewDateSubHeader: `Review date`,
  addNewButton:`Add new`,
  dateOfReviewHiddenLabel: `Date of review`,
  dayHiddenTextLabel: `Day`,
  monthHiddenTextLabel: `Month`,
  yearHiddenTextLabel: `Year`,
  reasonHiddenLabel: `Reason`,
  unlessOrderHiddenRadioOption:`Unless order`,
  StayCaseHiddenRadioOption: `Stay a case`,
  liftStayHiddenRadioOption: `Lift a stay`,
  dismissCaseHiddenRadioOption: `Dismiss case`,
  generalOrderHiddenRadioOption: `General order`,
  OtherHiddenRadioOption: `Other`,
  descriptionHiddenTextLabel: `Description of review`,
  youCanEnterUpToHiddenHintText: `You can enter up to 500 characters`,
  removeHiddenButton:`Remove`,
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  dayInputText :`20`,
  monthInputText :`12`,
  yearInputText :`2030`,
  descriptionTextInput:`50`,
  errorValidation: `YES`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  thereIsProbErrorMessageHeader: `There is a problem`,
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown`, five: `dateField`, six: `dateRadioOption` },
  errorValidationField: {
    errorDateField: [
      { type: `empty`, input: `empty`, errMessage: `Date of review is required`,errInlineMessage: `Date of review is required`},
      { type: `invalid`, input: `invalid`, errMessage: `Date of review is not valid`, errInlineMessage: `The data entered is not valid for Date of review`},
    ],
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Reason is required`,errInlineMessage: `Reason is required` }
    ],
    errorTextField: [
      { type: `none`, input: ``, errMessage: `Description of review is required` },
      ],
    errorDateRadioOption: [
      {type: `Max`, input: `510`, errMessage: `In ‘Description of review’, you have entered more than the maximum number of characters (500)`}
    ]
  },
};
