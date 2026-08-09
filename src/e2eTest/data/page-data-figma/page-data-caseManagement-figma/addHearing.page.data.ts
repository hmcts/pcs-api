export const addHearing= {
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
  hearingNotesTextLabel : `Hearing notes (Optional)`,
  hearingNotesTextInput: 10,
  hearingNoticeQuestion: `Does a hearing notice need to be issued?`,
  hearingNoticeYesRadioOption: `Yes`,
  hearingNoticeNoRadioOption: `No`,
  hearingWithOutNoticeHiddenQuestion: `Is the hearing without notice?`,
  whoShouldReceiveHiddenQuestion : `Who should receive the hearing notice?`,
  enterAdditionalInfoTextLabel: `Enter any additional information (Optional)`,
  enterAdditionalInfoTextInput: 100,
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  errorValidation: `YES`,
  thereIsProbErrorMessageHeader: `There is a problem`,
  errorValidationType: {
    one: `textField`,
    two: `radioOptions`,
    three: `checkBox`,
    four: `dropDown`,
    five: `dateField`,
    six: `dateRadioOption`,
    seven: `moneyField`
  },
  errorValidationField: {
    errorRadioOption: [
      {
        type: `none`,
        input: ``,
        errMessage: `Do you want to add, edit or cancel a hearing? is required`,
        errInlineMessage: `Do you want to add, edit or cancel a hearing? is required`
      },
    ],
  }
}