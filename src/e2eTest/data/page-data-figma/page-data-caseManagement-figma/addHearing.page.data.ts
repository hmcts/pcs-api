export const addHearing = {
  mainHeader: `Add a hearing`,
  typeOfHearingQuestion: `Which type of hearing is this?`,
  typeOfHearingOption: ['Possession first hearing', 'Application', 'Adjourned first hearing', 'Other'],
  wordingForHearingNoticeTextLabel: `Wording for hearing notice`,
  wordingForHearingHiddenOption: `TPL - will take place on`,
  whenIsTheHearingQuestion: `When is the hearing?`,
  dayTextLabel: `Day`,
  monthTextLabel: `Month`,
  yearTextLabel: `Year`,
  hourTextLabel: `Hour`,
  minutesTextLabel: `Minute`,
  secondsTextLabel: `Second`,
  dateTypeHiddenUserInput: `future`,
  hearingNotesTextLabel: `Hearing notes (Optional)`,
  hearingNotesTextInput: 10,
  hearingNoticeQuestion: `Does a hearing notice need to be issued?`,
  hearingNoticeYesRadioOption: `Yes`,
  hearingNoticeNoRadioOption: `No`,
  hearingWithOutNoticeHiddenQuestion: `Is the hearing without notice?`,
  whoShouldReceiveHiddenQuestion: `Who should receive the hearing notice?`,
  enterAdditionalInfoTextLabel: `Enter any additional information (Optional)`,
  enterAdditionalInfoTextInput: 100,
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  errorValidation: `YES`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  thereIsProbErrorMessageHeader: `There is a problem`,
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown`, five: `dateField`, six: `dateRadioOption`, seven :`moneyField` },
  errorValidationField: {
    errorRadioOption1: [
      { type: `none`, input: ``, errMessage: `Which type of hearing is this? is required`, errInlineMessage: `Which type of hearing is this? is required` },
    ],
    errorDropDown: [
      { type: 'none', input: '', errMessage: `Wording for hearing notice is required` },
    ],
    errorRadioOption2: [
      { type: `none`, input: ``, errMessage: `Which type of application has the applicant made? is required`, errInlineMessage: `Which type of application has the applicant made? is required` },
    ],
    errorDateField: [
      { type: `empty`, input: `empty`, errMessage: `When is the hearing? is required`,errInlineMessage: `When is the hearing? is required`},
      { type: `invalid`, input: `invalid`, errMessage: `The data entered is not valid for When is the hearing?` },
     
      { type: `future`, input: 'future', errMessage: ``, errInlineMessage: `` },
      
    ],
    errorTextField: [
      { type: `moreThanMax`, input: 505, errMessage: `In ‘Which categories apply’, you have entered more than the maximum number of characters (500)` },
      { type: `empty`, input: `EMPTY`, errMessage: `Which categories apply? is required `},
    ]
  },
}