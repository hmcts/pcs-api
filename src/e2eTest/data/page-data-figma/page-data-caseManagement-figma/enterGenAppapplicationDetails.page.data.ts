export const enterGenappApplication = {
  mainHeader: `Application details`,
  whichPartyMadeAppQuestion: `Which party made the application?`,
  whatDateAppReceivedQuestion: `What date was the application received?`,
  typeOfAppQuestion: `Which type of application has the applicant made?`,
  adjournRadioOption: `Adjourn`,
  setAsideRadioOption: `Set aside`,
  somethingElseRadioOption: `Something else`,
  dayTextLabel: `Day`,
  monthTextLabel: `Month`,
  yearTextLabel: `Year`,
  dateTypeHiddenUserInput: 'past',
  whichCategoriesHiddenTextLabel: `Which categories apply?`,
  whichCategoriesHiddenTextInput: 400,
  errorValidation: `YES`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  thereIsProbErrorMessageHeader: `There is a problem`,
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown`, five: `dateField`, six: `dateRadioOption`, seven :`moneyField` },
  errorValidationField: {
    errorRadioOption1: [
      { type: `none`, input: ``, errMessage: `Which party made the application? is required`, errInlineMessage: `Field is required` },
    ],
    errorRadioOption2: [
      { type: `none`, input: ``, errMessage: `Which type of application has the applicant made? is required`, errInlineMessage: `Which type of application has the applicant made? is required` },
    ],
    errorDateField: [
      { type: `empty`, input: `empty`, errMessage: `What date was the application received? is required`,errInlineMessage: `What date was the application received? is required`},
      { type: `invalid`, input: `invalid`, errMessage: `What date was the application received? is not valid `,errInlineMessage: `The data entered is not valid for What date was the application received?`},
      { type: `future`, input: 'future', errMessage: `Date the application was received must be in the past`, errInlineMessage: `The data entered is not valid for What date was the application received?` },
      { type: `present`, input: 'present', errMessage: `Date the application was received must be in the past`, errInlineMessage: `The data entered is not valid for What date was the application received?` },
      { type: `past`, input: 'past', errMessage: ``, errInlineMessage: `` },
      
    ],
    errorTextField: [
      { type: `moreThanMax`, input: 505, errMessage: `In ‘Which categories apply’, you have entered more than the maximum number of characters (500)` },
      { type: `empty`, input: `EMPTY`, errMessage: `Which categories apply? is required `},
    ]
  },
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
};
