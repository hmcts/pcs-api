export const vulnerableAdultsChildren = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Oedolion a phlant bregus yn yr eiddo`,
  theBailiffNeedsToKnowParagraph: `Mae'r beili angen gwybod os yw unrhyw un yn yr eiddo yn fregus.`,
  someoneIsVulnerableParagraph: `Mae rhywun yn fregus os oes ganddynt:`,
  aHistoryOfDrugList: `hanes o gamddefnyddio alcohol neu gyffuriau`,
  aMentalHealthList: `cyflwr iechyd meddwl`,
  aDisabilityList: `anabledd, er enghraifft anabledd dysgu neu nam gwybyddol`,
  beenAVictimList: `wedi bod yn ddioddefwr cam-drin domestig`,
  IsAnyOneLivingAtThePropertyQuestion: `A yw unrhyw un sy'n byw yn yr eiddo yn fregus?`,
  yesRadioOption: `Ydy`,
  noRadioOption: `Nac ydy`,
  notSureRadioOption: `Nid wyf yn siŵr`,
  confirmVulnerablePeopleHiddenQuestion: `Confirm if the vulnerable people in the property are adults, children, or both adults and children`,
  vulnerableAdultsHiddenRadioOption: `Oedolion bregus`,
  vulnerableChildrenHiddenRadioOption: `Plant bregus`,
  vulnerableAdultsAndChildrenHiddenRadioOption: `Oedolion a phlant bregus`,
  howAreTheyVulnerableHiddenTextLabel: `How are they vulnerable?`,
  howAreTheyVulnerableHiddenTextInput: 1000,
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato yn ddiweddarach`,
  previousButton: `Blaenorol`,
  continueButton: `Parhau`,
  cancelLink: `Canslo`,
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
