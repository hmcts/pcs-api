export const cannotApplyForWarrantInfo = {
  mainHeader: 'You need permission from a judge to apply for the warrant of possession',
  permissionExplanationParagraph:
    'You need permission from a judge before you can apply for the warrant of possession. This is because the defendants have not missed any of the payments outlined in the suspended order.',
  differentTermHeader: 'If the defendants have breached a different term in the suspended order',
  differentTermParagraph:
    'If the defendants have breached a different term in the order, you still need permission from a judge. For example, if they have breached an order to reduce their antisocial behaviour.',
  askTheJudgeLink: 'Ask the judge for permission to apply for a warrant of possession (opens in a new tab, GOV.UK).',
  previousButton: 'Previous',
  continueButton: 'Continue',
  cancelLink: 'Cancel',
  errorValidation: 'No', // set it to 'NO' before raising a PR
  eventCouldNotBeCreatedHeader: 'The event could not be created',
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption', six: 'pageLevel' },
  errorValidationField: {
    errorPageLevel: [
      { type: 'none', input: '', errMessage: 'You cannot continue with this application until you ask the judge for permission to apply for a warrant of possession' },
    ],
},
};