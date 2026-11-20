export const statementOfTruth = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Datganiad gwirionedd`,
  iCertifyCheckboxDynamic: `Rwy’n tystio bod:`,
  //Will create dynamic elements while page replacement with HDPI-3644 for below 4 lines as its diff in Warrant/Writ
  //iCertifyCheckbox: `I certify that:`,
  //defendantHasNotVacatedParagraph: `the defendant has not vacated the land as ordered (*and that the whole or part of any instalments due under the judgment or order have not been paid) (†and the balance now due is as shown)`,
  //commented above line as this is expected to be changeds as per discussion with Nafees and is reported in HDPI-5199 scenario3
 // noticeHasBeenGivenParagraph: `notice has been given in accordance with The Dwelling Houses (Execution of Possession Orders by Mortgagees) Regulations 2010. `,
  //aStatementOfThePaymentParagraph: `a statement of the payments due and made under the judgment or order is attached to this request.††`,  // not in all cases only for suspended order
  //warrantOfPossessionFeeTableHeader: `Warrant of possession fee`, //Will create Dynamic element for this during page replacement
  //writOfPossessionFeeTableHeader: `Writ of possession fee`,
  completedByParagraph: `Cwblhawyd gan`,
  claimantRadioOption: `Hawlydd`,
  claimantLegalRepresentativeRadioOption: `Cynrychiolydd cyfreithiol yr hawlydd (fel y’i diffinnir gan CPR 2.3(1))`,
  iBelieveTheFactsHiddenCheckbox: `Credaf bod y ffeithiau a nodir yn y ffurflen hawlio hon yn wir.`,
  fullNameHiddenTextLabel: `Enw llawn`,
  nameOfFirmHiddenTextLabel: `Enw’r cwmni`,
  positionOrOfficeHeldHiddenTextLabel: `Safle neu swydd a ddelir`,
  fullNameHiddenTextInput: `John Doe`,
  nameOfFirmHiddenTextInput: `Doe & Co Solicitors`,
  positionOrOfficeHeldHiddenTextInput: `Solicitor`,
  signThisStatementHiddenCheckbox: `Creda’r hawlydd fod y ffeithiau a nodir yn y ffurflen hawlio hon yn wir. Fe’m hawdurdodir gan yr hawlydd i lofnodi’r datganiad hwn.`,
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato yn ddiweddarach`,
  previousButton: `Blaenorol`,
  continueButton: `Parhau`,
  theClaimantBelievesHiddenCheckBox: `Creda’r hawlydd fod y ffeithiau a nodir yn y ffurflen hawlio hon yn wir. Fe’m hawdurdodir gan yr hawlydd i lofnodi’r datganiad hwn.`,
  thereIsAProblemErrorMessageHeader: `There is a problem`,
  checkBoxGenericErrorMessageHeader: `Field is required`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`, five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorCheckBoxOption: [
      { type: `none`, input: ``, errMessage: `Field is required` }
    ],
    errorRadioOption: [
      { type: `none`, input: ``, isSOT: true, errInlineMessage: `Field is required`, errMessage: `Field is required` }
    ],
    errorTextField1: [
      { type: `moreThanMax`, input: 100, errMessage: `Full name exceeds the maximum length` },
      { type: `empty`, input: `EMPTY`, errMessage: `Full name is required` },
    ],
    errorTextField2: [
      { type: `moreThanMax`, input: 100, errMessage: `Name of firm exceeds the maximum length` },
      { type: `empty`, input: `EMPTY`, errMessage: `Name of firm is required` },
    ],
    errorTextField3: [
      { type: `moreThanMax`, input: 100, errMessage: `Position or office held exceeds the maximum length` },
      { type: `empty`, input: `EMPTY`, errMessage: `Position or office held is required` },
    ]
  },
  cancelLink: `Canslo`
}
