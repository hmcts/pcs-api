export const addReviewDates = {
  mainHeader: `Review dates`,
  addReviewDateHintText: `Add review date`,
  reviewDateSubHeader: `Review date`,
  dateOfReviewLabel: `Date of review`,
  addNewButton:`Add new`,
  dayTextLabel: `Day`,
  monthTextLabel: `Month`,
  yearTextLabel: `Year`,
  reasonLabel: `Reason`,
  unlessOrderRadioOption:`Unless order`,
  StayCaseRadioOption: `Stay a case`,
  liftStayRadioOption: `Lift a stay`,
  dismissCaseRadioOption: `Dismiss case`,
  generalOrderRadioOption: `General order`,
  OtherRadioOption: `Other`,
  descriptionTextLabel: `Description of review`,
  youCanEnterUpToHiddenHintText: `You can enter up to 500 characters`,
  removeButton:`Remove`,
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  dayInputText :`20`,
  monthInputText :`12`,
  yearInputText :`2030`,
  descriptionTextInput:`50`,
  errorValidation: `YES`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown`, five: `dateField`, six: `maxInputField` },
  errorValidationField: {
    errorDateField: [
      {type: 'none', input: {day: '', month: '', year: '',errMessage: 'Date of review is required' }},
      {type: 'invalid', input: {day: '30', month: '', year: '', errMessage: 'The data entered is not valid for Date of review'}},
      {type: 'invalid', input: {day: '30', month: '02', year: '', errMessage: 'The data entered is not valid for Date of review'}},
      {type: 'invalid', input: {day: '', month: '02', year: '2026', errMessage: 'The data entered is not valid for Date of review'}},
    ],
    errorRadioOption: [
      { type: 'none', input: '', errMessage: `Reason is required` }
    ],
    errorTextField: [
      { type: 'none', input: '', errMessage: `Description of review is required` },
      ],
    errorMaxInputField: [
      {type: 'Max', input: {
          day: '30',
          month: '08',
          year: '2026',
          maxLength: '510'
        },
        errMessage: 'In ‘Description of review’, you have entered more than the maximum number of characters (500)'
      }
    ]
  },
};
