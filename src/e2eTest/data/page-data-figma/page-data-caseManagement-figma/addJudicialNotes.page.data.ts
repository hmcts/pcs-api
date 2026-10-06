export const addJudicialNotes = {
  mainHeader: `Write a note`,
  addAJudicialNotesHintText: `Add a judicial note`,
  notesAboutThisCaseTextLabel: `Write a note about this case`,
  youCanEnterUptoText: `You can enter up to 30,000 characters`,
  notesAboutThisCaseInputText1: 30,
  notesAboutThisCaseInputText2: 100,
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  errorValidation: `YES`,
  thereIsProbErrorMessageHeader: `There is a problem`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
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
    errorTextField: [
      { type: `moreThanMax`, input: 30001, errMessage: `In ‘Write a note about this case’, you have entered more than the maximum number of characters (30,000)` },
      { type: `empty`, input: `EMPTY`, errMessage: `Write a note about this case is required`},
    ]
  },

};
