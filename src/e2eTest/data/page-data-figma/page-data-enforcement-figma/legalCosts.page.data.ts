export const legalCosts = {
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `Legal costs`,
  reclaimLegalCostsQuestion: `Do you want to reclaim any legal costs?`,
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  howMuchYouWantToReclaimTextLabelHidden: `How much do you want to reclaim?`,
  howMuchYouWantToReclaimTextInputHidden: [`755559.7`,`119999`,`1099.95`,`6320.00`,`890.10`,`111.01`],
  legalCostsFeeDynamic: `Legal costs`,
  iDoNotKnowLink: `I do not know if I need to reclaim any legal costs`,
  legalCostsAreTheCostsParagraphHidden: `Legal costs are the costs you incur when a lawyer, legal representative, or someone working in a legal department applies for a writ or a warrant on your behalf.`,
  theyWillInvoiceParagraphHidden: `They will invoice these costs to you, and you can reclaim them from the defendant.`,
  ifYouAreNotSureParagraphHidden: `If you are not sure how much you can reclaim`,
  theamountYouParagraphHidden: `The amount you can reclaim from the defendant is usually fixed.`,
  youCanEitherParagraphHidden: `You can either:`,
  askYourLawyerListHidden: `ask your lawyer or legal representative how much you can reclaim, or`,
  checkTheCivilLinkHidden: `check the Civil Procedure Rules (Justice.gov website, opens in a new tab)`,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
  cancelLink: `Cancel`,
  errorValidation: `YES`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`, five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorMoneyField: [
      { type: `negativeDecimal`, input: `-0.5`, errMessage: `Should be more than or equal to £0.01` },
      { type: `negative`, input: `-5`, errMessage: `Should be more than or equal to £0.01` },
      { type: `zero`, input: `0`, errMessage: `Should be more than or equal to £0.01` },
      { type: `max`, input: `100000000`, errMessage: `Should be less than or equal to £10,000,000.00` },
      { type: `alpha`, input: `test`, errMessage: `The data entered is not valid for How much do you want to reclaim?` },
      { type: `decimal`, input: `10.234`, errMessage: `The data entered is not valid for How much do you want to reclaim?` },
      { type: `empty`, input: ``, errMessage: `The data entered is not valid for How much do you want to reclaim?` },
    ],
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Do you want to reclaim any legal costs? is required` }
    ]
  }
}