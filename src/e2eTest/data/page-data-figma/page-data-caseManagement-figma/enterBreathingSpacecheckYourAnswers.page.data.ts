export const enterBreathingSpacecheckYourAnswers = {
  mainHeader: 'Enter breathing space', // the h1 is the same on this screen
  subHeader: 'Check your answers', // tells this screen apart from the form
  checkInfoText: 'Check the information below carefully.',
  confirmationHeader: 'Breathing space has been entered', // from AC05, confirm the exact text on screen

  submitButton: 'Submit', // the screen shows Submit, the AC says Continue
  previousButton: 'Previous',
  cancelLink: 'Cancel',
  changeLink: 'Change',

  // AC01 - case info at the top (read only)
  caseInfoLabels: {
    caseNumber: 'Case number:',
    propertyAddress: 'Property address:',
  },

  // AC02 - same labels as the form, without "(Optional)"
  answerLabels: {
    referenceNumber: 'Reference Number',
    startDate: 'When did breathing space start?',
    type: 'Which type of breathing space is it?',
    expectedEndDate: 'When is the expected end date?',
  },

  happyPathConfig: {
    referenceNumber: 'REF123',
    targetType: 'standard',
    startDate: '22/03/2018', // entered on the form
    expectedEndDate: '24/02/2034', // entered on the form
    typeDisplayText: 'Standard Breathing Space',
    startDateDisplay: '22 Mar 2018', // shown on Check Your Answers
    expectedEndDateDisplay: '24 Feb 2034', // shown on Check Your Answers
  },

  // AC03
  changeConfig: {
    updatedReferenceNumber: 'REF999',
  },

  // AC06
  cancelConfig: {
    referenceNumber: 'CYA_CANCEL_555',
    targetType: 'standard',
    startDate: '22/03/2018',
    expectedEndDate: '24/02/2034',
  },
};