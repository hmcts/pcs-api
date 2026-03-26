export const vulnerableAdultsChildren = {
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `Vulnerable adults and children at the property`,
  theBailiffNeedsToKnowParagraph: `The bailiff needs to know if anyone at the property is vulnerable.`,
  someoneIsVulnerableParagraph: `Someone is vulnerable if they have:`,
  aHistoryOfDrugList: `a history of drug or alcohol abuse`,
  aMentalHealthList: `a mental health condition`,
  aDisabilityList: `a disability, for example a learning disability or cognitive impairment`,
  beenAVictimList: `been a victim of domestic abuse`,
  IsAnyOneLivingAtThePropertyQuestion: `Is anyone living at the property vulnerable?`,
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  notSureRadioOption: `I’m not sure`,
  confirmVulnerablePeopleHiddenQuestion: `Confirm if the vulnerable people in the property are adults, children, or both adults and children`,
  vulnerableAdultsHiddenRadioOption: `Vulnerable adults`,
  vulnerableChildrenHiddenRadioOption: `Vulnerable children`,
  vulnerableAdultsAndChildrenHiddenRadioOption: `Vulnerable adults and children`,
  howAreTheyVulnerableHiddenTextLabel: `How are they vulnerable?`,
  howAreTheyVulnerableHiddenTextInput: 1000,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
  cancelLink: `Cancel`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`, five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorRadioOption1: [
      { type: `multi`, input: ``, errMessage: `Is anyone living at the property vulnerable? is required` },
    ],
    errorRadioOption2: [
      { type: `none`, input: ``, errMessage: `Confirm if the vulnerable people in the property are adults, children, or both adults and children is required` },
    ],
    errorTextField: [
      { type: `moreThanMax`, input: `MAXPLUS`, errMessage: `In ‘How are they vulnerable?’, you have entered more than the maximum number of characters (6,800)` },
      { type: `empty`, input: `EMPTY`, errMessage: `How are they vulnerable? is required` },
    ]
  }
}