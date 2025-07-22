export const pageIdentifier =
  {
    title : 'Case Options',
    mainHeader : 'some header'
  };

export const claimantType = 'Claimant type';

export const claimantTypeOptions = {
  elementType:'radio',
  wales:
    {
      privateLandlord: 'Private landlord',
      registeredCommunityLandlord: 'Registered community landlord',
      mortgageProviderOrLender: 'Mortgage Provider or lender',
      other: 'Other',
    },
  england:
    {
      privateLandlord: 'Private landlord',
      registeredProviderForSocialHousing: 'Private registered provider of social housing',
      mortgageProviderOrLender: 'Mortgage Provider or lender',
      other: 'Other',
    }
};

export const errorMessage = {
  header: 'There is a problem',
  errorMessage: 'An address is required',
}
