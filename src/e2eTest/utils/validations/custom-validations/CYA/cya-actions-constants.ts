export const CYA_EXCLUDED_ACTIONS = new Set([
  'createCaseAPI',
  'submitCaseAPI',
  'getAddressInfo',
  'updatePaymentAPI',
  'getAllPartyDetails',
  'navigateToUrl',
  'inputErrorValidation',
  'login',
  'reTryOnCallBackError',
  'selectAnEvent',
  'errorValidationEnterGeneralAppPage'
]);

export const CYA_FLOW_EXCLUDED_ACTIONS = new Set([
  'inputErrorValidation',
  'selectAnEvent',
  'errorValidationEnterGeneralAppPage'
]);

export const CYA_CAPTURE_ACTIONS = new Set([
  'clickRadioButton',
  'inputText',
  'check',
  'select',
  'uploadFile',
  'uploadADocument',
  'inputDate',
]);