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
      mortgageLender: 'Mortgage lender',
      other: 'Other',
    },
  england:
    {
      privateLandlord: 'Private landlord',
      registeredProviderForSocialHousing: 'Registered provider of social housing',
      mortgageProviderOrLender: 'Mortgage lender',
      other: 'Other',
    }
};

export const claimType = {
  elementType:'radio',
  options:
    {
      yes: 'Yes',
      no: 'No',
    }
};

export const legislativeCountry = {
  elementType:'radio',
  options:
    {
      england: 'England',
      wales: 'Wales',
      northernIreland: 'Northern Ireland',
      scotland: 'Scotland',
    }
};

export const claimantName = {
  elementType:'radio',
  options:
    {
      yes: 'Yes',
      no: 'No',
    }
}

export const errorMessage = {
  header: 'There is a problem',
  errorMessage: 'An address is required',
}
