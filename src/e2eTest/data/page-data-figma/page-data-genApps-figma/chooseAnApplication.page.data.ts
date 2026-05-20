export const chooseAnApplication = {
  mainHeader: `Choose an application`,

  youCannotApplyToSuspendParagraph: `You cannot apply to suspend (stop or delay) the eviction online.`,
  youMustApplyByPostParagraph: `You must apply by post:`,
  readTheGuidanceLink: `read the guidance explaining how to suspend the eviction (GOV.UK, opens in a new tab)`,
  fillN244aFormLink: `fill in the N244a form`,
  findYourLocalCourtLink: `find your local court`,
  sendTheCompletedFormList: `send the completed form to the court, or deliver it by hand`,
  whatDoYouWantToApplyForQuestion: `What do you want to apply for?`,
  adjournTheHearingRadioOption: `Adjourn (delay) the hearing - You can apply to change the defendant’s court hearing until a later time or date`,
  setAsideRadioOption: `Ask the court to set aside (cancel) a decision the court has made - You can ask the court to set aside its order if the defendant has a good reason. For example, if they were unable to attend the court hearing because they were ill`,
  somethingElseRadioOption: `Something else - Make an application for something that is not listed above`,
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  thereIsAProblemErrorMessageHeader: `There is a problem`,
  // if the below format is deemed complicated it will replaced as part of https://tools.hmcts.net/jira/browse/HDPI-5815
  errorValidationType: { one: `radioOptions`, two: `textField`, three: `checkBox` },
  errorValidationField: {
    errorRadioOption: { type: `none`, input: ``, errMessage: `Choose what you want to apply for` },
  },
};
