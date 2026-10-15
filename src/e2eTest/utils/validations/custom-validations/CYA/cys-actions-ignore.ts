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
]);

export const CYA_FLOW_EXCLUDED_ACTIONS = new Set([
  'inputErrorValidation',
  'selectAnEvent',
]);