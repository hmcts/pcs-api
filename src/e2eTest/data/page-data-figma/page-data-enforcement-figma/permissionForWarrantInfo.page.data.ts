export const permissionForWarrantInfo = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'You need permission from a judge to apply for the warrant of possession',
  youNeedPermissionParagraph: `You need permission from a judge before you can apply for the warrant of possession. This is because the defendants have not failed to make payments which are a condition of the suspension of the order for possession. You can do this by making a general application.`,
  breachedDifferentTermHeader: `If the defendants have breached a different term in the suspended order`,
  breachedDifferentTermParagraph: `If the defendants have breached a different term in the order, you still need permission from a judge. For example, if they have breached an order to reduce their antisocial behaviour.`,
  askJudgeForPermissionLink: `Ask the judge for permission to apply for a warrant of possession (opens in a new tab, GOV.UK)`,
  eventCouldNotBeCreatedHeader: 'The event could not be created',
  errorValidation: 'YES',
  errorValidationType: {
    one: 'moneyField',
    two: 'textField',
    three: 'radioOptions',
    four: 'checkBox',
    five: 'moneyFieldAndRadioOption',
    six: 'pageLevel'
  },
  errorValidationField: {
    errorPageLevel: [
      {
        type: 'none',
        input: '',
        errMessage: 'You cannot continue with this application until you ask the judge for permission to apply for a warrant of possession'
      }
    ]
  },
  continueButton: 'Continue',
  previousButton: 'Previous',
  cancelLink: `Cancel`,
};
