export const chooseAnApplication = {
  mainHeader: `Choose an application`,
  //youCannotApplyToSuspendParagraph: `You cannot apply to suspend (stop or delay) an eviction online.`,
  //youMustApplyByPostParagraph: `You must apply by post:`,
  //readTheGuidanceLink: `read the guidance explaining how to suspend an eviction (GOV.UK, opens in a new tab)`,
  //fillN244FormLink: `fill in the N244 form`,
  //findYourDefendantCourtLink: `find the defendant’s local court`,
  //sendTheCompletedFormList: `send the completed form to the court, or deliver it by hand`,
  whatDoYouWantToApplyForQuestion: `What do you want to apply for?`,
  //whichTypeOfApplicationQuestion: `Which type of application has the applicant made?`,
  suspendTheEvictionRadioOption: `Ask the court to suspend (stop or delay) the eviction - You can ask the court to ‘suspend the eviction’. This means stopping or delaying the eviction`,
  adjournTheHearingRadioOption: `Adjourn (delay) the hearing - You can apply to change the defendant’s court hearing until a later time or date`,
  setAsideRadioOption: `Ask the court to set aside (cancel) a decision the court has made - You can ask the court to set aside its order if the defendant has a good reason. For example, if they were unable to attend the court hearing because they were ill`,
  somethingElseRadioOption: `Something else - Make an application for something that is not listed above`,
  uploadDocHiddenOption: [`General Application - Defendant 1.pdf`,`genApps.docx`],
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  errorValidation: `YES`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`, five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorRadioOption: [
      { type: `none`, input: ``, errInlineMessage: `Field is required`,errMessage:`What do you want to apply for? is required` },
    ],
  },
};
