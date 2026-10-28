export const helpPayingTheFee  = (claimType: string = 'Claimant') => ({
  mainHeader: `Confirm if the defendant needs help paying fees`,
  doYouNeedHelpPayingTheFeeQuestion: `Does the defendant need help paying the fee for this application?`,
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  itCostsParagraphHidden: claimType === 'LR'
    ? `It costs £16 to apply.`
    : `It usually costs £126 to apply. The fee will increase to £321 if:`,
  youHaveAlreadyToldListHidden: `the defendant has already told the other party that they are making this application, and`,
  theOtherPartyDidNotAgreeListHidden: `the other party did not agree to it (this means that they objected to it)`,
  youSeeFinalApplicationFeeParagraphHidden:`You’ll see the final application fee before you pay.`,
  theDefendantMayBeAbleToGetHelpParagraph: `The defendant may be able to get help paying the fee if they (one or more of the following):`,
  areOnCertainBenefitsList: `are on certain benefits`,
  haveLittleOrNoSavingsList: `have little or no savings`,
  haveLowIncomeList: `have low income`,
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
});
