export const claimantType =
  {
    title : 'Create a case - HM Courts & Tribunals Service - GOV.UK',
    mainHeader : 'Claimant type',
    claimantTypeOptions:
      {
        elementType: 'radio',
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
      },
    errorMessage: {
      header: 'There is a problem',
      errorMessage: 'Who is the claimant in this case? is required'
    }
  };
