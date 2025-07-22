export const pageIdentifier =
{
  title : 'Case Options',
  mainHeader : 'some header'
};

export const caseOption = {
  jurisdiction: 'Possessions',
  caseType: process.env.CHANGE_ID
    ? `Civil Possessions ${process.env.CHANGE_ID}`
    : 'Civil Possessions',
  event: 'Make a claim',
}

export const errorMessages = {
  header: 'There is a problem',
  errorMessage: 'An address is required',
}
