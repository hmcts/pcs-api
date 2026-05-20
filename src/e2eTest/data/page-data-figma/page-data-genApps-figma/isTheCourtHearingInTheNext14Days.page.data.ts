export const isTheCourtHearingInTheNext14Days = {
  mainHeader: `Is the court hearing in the next 14 days?`,
  isTheCourtHearingInTheNext14DaysQuestion: `Is the court hearing in the next 14 days?`,
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  thisWillAffectTheFeeHintText: `This will affect the fee you will pay. You do not need to pay a fee if your court hearing is (at least) 14 days away.`,
  continueButton: `Continue`,
  cancelLink: `Cancel`,
  backLink: `Back`,
  cymraegLink: `Cymraeg`,
  thereIsAProblemErrorMessageHeader: `There is a problem`,
  // if the below format is deemed complicated it will replaced as part of https://tools.hmcts.net/jira/browse/HDPI-5815
  errorValidationType: { one: `radioOptions`, two: `textField`, three: `checkBox` },
  errorValidationField: {
    errorRadioOption: {
      type: `none`,
      input: ``,
      errMessage: `Confirm whether the court hearing is in the next 14 days`,
    },
  },
};
