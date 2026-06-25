export const submitCaseApiData = {
  submitCaseEventName: 'resumePossessionClaim',
  submitCasePayload: {
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
        },
        {
          code: 'MORTGAGE_LENDER',
          label: 'Mortgage lender'
        },
        {
          code: 'OTHER',
          label: 'Other'
        }
      ],
      valueCode: 'PROVIDER_OF_SOCIAL_HOUSING'
    },
    claimAgainstTrespassers: 'NO',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    claimantContactEmail: 'pcs-solicitor1@test.com',
    isCorrectClaimantContactEmail: 'YES',
    formattedClaimantContactAddress: '2 Second Avenue<br>London<br>W3 7RX',
    isCorrectClaimantContactAddress: 'YES',
    claimantProvidePhoneNumber: 'NO',
    defendant1: {
      nameKnown: 'YES',
      addressKnown: 'YES',
      addressSameAsPossession: 'YES',
      firstName: 'John',
      lastName: 'Doe',
    },
    addAnotherDefendant: 'YES',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'YES',
          firstName: 'Peter',
          lastName: 'Parker',
          addressKnown: 'YES',
          addressSameAsPossession: 'YES'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'YES',
          firstName: 'Jen',
          lastName: 'Parker',
          addressKnown: 'YES',
          addressSameAsPossession: 'YES'
        },
        id: null
      },
    ],
    tenancy_TypeOfTenancyLicence: 'DEMOTED_TENANCY',
    tenancy_TenancyLicenceDate: null,
    tenancy_TenancyLicenceDocuments: [],
    showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'NO',
    noGrounds: 'text no grounds',
    preActionProtocolCompleted: 'NO',
    preActionProtocolIncompleteExplanation: 'preAction protocol completed',
    mediationAttempted: 'NO',
    settlementAttempted: 'NO',
    noticeServed: 'No',
    claimantNamePossessiveForm: null,
    claimantCircumstancesSelect: 'NO',
    hasDefendantCircumstancesInfo: 'NO',
    suspensionOfRTB_ShowHousingActsPage: 'No',
    demotionOfTenancy_ShowHousingActsPage: 'No',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: [],
    additionalReasonsForPossession: {
      hasReasons: 'NO',
      reasons: null
    },
    hasUnderlesseeOrMortgagee: 'NO',
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'NO',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW'
  },
  submitCasePayloadNoDefendants: {
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
        },
        {
          code: 'MORTGAGE_LENDER',
          label: 'Mortgage lender'
        },
        {
          code: 'OTHER',
          label: 'Other'
        }
      ],
      valueCode: 'PROVIDER_OF_SOCIAL_HOUSING'
    },
    claimAgainstTrespassers: 'NO',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    claimantContactEmail: 'pcs-solicitor1@test.com',
    isCorrectClaimantContactEmail: 'YES',
    formattedClaimantContactAddress: '2 Second Avenue<br>London<br>W3 7RX',
    isCorrectClaimantContactAddress: 'YES',
    claimantProvidePhoneNumber: 'NO',
    defendant1: {
      nameKnown: 'NO',
      addressKnown: 'NO',
      addressSameAsPossession: 'NO',
    },
    addAnotherDefendant: 'NO',
    tenancy_TypeOfTenancyLicence: 'DEMOTED_TENANCY',
    tenancy_TenancyLicenceDate: null,
    tenancy_TenancyLicenceDocuments: [],
    showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'NO',
    noGrounds: 'text no grounds',
    preActionProtocolCompleted: 'NO',
    preActionProtocolIncompleteExplanation: 'preAction protocol completed',
    mediationAttempted: 'NO',
    settlementAttempted: 'NO',
    noticeServed: 'No',
    claimantNamePossessiveForm: null,
    claimantCircumstancesSelect: 'NO',
    hasDefendantCircumstancesInfo: 'NO',
    suspensionOfRTB_ShowHousingActsPage: 'No',
    demotionOfTenancy_ShowHousingActsPage: 'No',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: [],
    additionalReasonsForPossession: {
      hasReasons: 'NO',
      reasons: null
    },
    hasUnderlesseeOrMortgagee: 'NO',
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'NO',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW'
  },
  submitCasePayloadOnlyMain: {
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
        },
        {
          code: 'MORTGAGE_LENDER',
          label: 'Mortgage lender'
        },
        {
          code: 'OTHER',
          label: 'Other'
        }
      ],
      valueCode: 'PROVIDER_OF_SOCIAL_HOUSING'
    },
    claimAgainstTrespassers: 'NO',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    claimantContactEmail: 'pcs-solicitor1@test.com',
    isCorrectClaimantContactEmail: 'YES',
    formattedClaimantContactAddress: '2 Second Avenue<br>London<br>W3 7RX',
    isCorrectClaimantContactAddress: 'YES',
    claimantProvidePhoneNumber: 'NO',
    defendant1: {
      nameKnown: 'YES',
      addressKnown: 'YES',
      addressSameAsPossession: 'YES',
      firstName: 'John',
      lastName: 'Doe',
    },
    addAnotherDefendant: 'YES',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
    ],
    tenancy_TypeOfTenancyLicence: 'DEMOTED_TENANCY',
    tenancy_TenancyLicenceDate: null,
    tenancy_TenancyLicenceDocuments: [],
    showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'NO',
    noGrounds: 'text no grounds',
    preActionProtocolCompleted: 'NO',
    preActionProtocolIncompleteExplanation: 'preAction protocol completed',
    mediationAttempted: 'NO',
    settlementAttempted: 'NO',
    noticeServed: 'No',
    claimantNamePossessiveForm: null,
    claimantCircumstancesSelect: 'NO',
    hasDefendantCircumstancesInfo: 'NO',
    suspensionOfRTB_ShowHousingActsPage: 'No',
    demotionOfTenancy_ShowHousingActsPage: 'No',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: [],
    additionalReasonsForPossession: {
      hasReasons: 'NO',
      reasons: null
    },
    hasUnderlesseeOrMortgagee: 'NO',
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'NO',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW'
  },
  submitCasePayloadCaseTab: {
    legislativeCountry: 'England',
    orgNameFound: 'Yes',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
        },
        {
          code: 'MORTGAGE_LENDER',
          label: 'Mortgage lender'
        },
        {
          code: 'OTHER',
          label: 'Other'
        }
      ],
      valueCode: 'PROVIDER_OF_SOCIAL_HOUSING'
    },
    claimAgainstTrespassers: 'NO',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    claimantContactEmail: 'pcs-solicitor1@test.com',
    isCorrectClaimantContactEmail: 'YES',
    orgAddressFound: 'Yes',
    organisationAddress: {
      AddressLine1: 'Ministry Of Justice',
      AddressLine2: 'Seventh Floor 102 Petty France',
      PostTown: 'London',
      PostCode: 'SW1H 9AJ',
      Country: 'United Kingdom'
    },
    formattedClaimantContactAddress: 'Ministry Of Justice<br>Seventh Floor 102 Petty France<br>London<br>SW1H 9AJ',
    isCorrectClaimantContactAddress: 'YES',
    claimantProvidePhoneNumber: 'YES',
    claimantContactPhoneNumber: '04469172429',
    defendant1: {
      nameKnown: 'YES',
      addressKnown: 'YES',
      addressSameAsPossession: 'NO',
      correspondenceAddress: {
        AddressLine1: '6 Second Avenue',
        AddressLine2: 'Oxford Street',
        AddressLine3: '',
        PostTown: 'London',
        County: '',
        Country: 'United Kingdom',
        PostCode: 'W3 7RX'
      },
      firstName: 'John',
      lastName: 'Doe',
    },
    addAnotherDefendant: 'NO',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
    ],
    tenancy_TypeOfTenancyLicence: 'DEMOTED_TENANCY',
    tenancy_TenancyLicenceDate: null,
    tenancy_HasCopyOfTenancyLicence: 'NO',
    tenancy_ReasonsForNoTenancyLicenceDocuments: 'tet',
    showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'NO',
    noGrounds: 'text no grounds',
    preActionProtocolCompleted: 'NO',
    preActionProtocolIncompleteExplanation: 'preAction protocol completed',
    mediationAttempted: 'NO',
    settlementAttempted: 'NO',
    noticeServed: 'No',
    claimantNamePossessiveForm: 'Possession Claims Solicitor Org’s',
    claimantCircumstancesSelect: 'NO',
    hasDefendantCircumstancesInfo: 'NO',
    suspensionOfRTB_ShowHousingActsPage: 'No',
    demotionOfTenancy_ShowHousingActsPage: 'No',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: [],
    additionalReasonsForPossession: {
      hasReasons: 'NO',
      reasons: null
    },
    hasUnderlesseeOrMortgagee: 'NO',
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'NO',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW'
  },
  submitCasePayloadCaseSummary: {
    legislativeCountry: 'England',
    orgNameFound: 'Yes',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
        },
        {
          code: 'MORTGAGE_LENDER',
          label: 'Mortgage lender'
        },
        {
          code: 'OTHER',
          label: 'Other'
        }
      ],
      valueCode: 'PROVIDER_OF_SOCIAL_HOUSING'
    },
    claimAgainstTrespassers: 'NO',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    claimantContactEmail: 'pcs-solicitor1@test.com',
    isCorrectClaimantContactEmail: 'YES',
    orgAddressFound: 'Yes',
    organisationAddress: {
      AddressLine1: 'Ministry Of Justice',
      AddressLine2: 'Seventh Floor 102 Petty France',
      PostTown: 'London',
      PostCode: 'SW1H 9AJ',
      Country: 'United Kingdom'
    },
    formattedClaimantContactAddress: 'Ministry Of Justice<br>Seventh Floor 102 Petty France<br>London<br>SW1H 9AJ',
    isCorrectClaimantContactAddress: 'YES',
    claimantProvidePhoneNumber: 'YES',
    claimantContactPhoneNumber: '04469172429',
    defendant1: {
      nameKnown: 'YES',
      addressKnown: 'YES',
      addressSameAsPossession: 'NO',
      correspondenceAddress: {
        AddressLine1: '6 Second Avenue',
        AddressLine2: 'Oxford Street',
        AddressLine3: '',
        PostTown: 'London',
        County: '',
        Country: 'United Kingdom',
        PostCode: 'W3 7RX'
      },
      firstName: 'John',
      lastName: 'Doe',
    },
    addAnotherDefendant: 'NO',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
    ],
    tenancy_TypeOfTenancyLicence: 'DEMOTED_TENANCY',
    tenancy_TenancyLicenceDate: '2020-10-02',
    tenancy_HasCopyOfTenancyLicence: 'NO',
    tenancy_ReasonsForNoTenancyLicenceDocuments: 'tet',
    showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'YES',
    introGrounds_IntroductoryDemotedOrOtherGrounds: ['RENT_ARREARS'],
    noGrounds: 'text no grounds',
    preActionProtocolCompleted: 'NO',
    preActionProtocolIncompleteExplanation: 'preAction protocol completed',
    mediationAttempted: 'NO',
    settlementAttempted: 'NO',
    noticeServed: 'YES',
    notice_PostedDate: '2025-11-10',
    notice_ServiceMethod: 'EMAIL',
    notice_EmailAddress: 'test@gmail.com',
    notice_EmailSentDateTime: '2025-12-11T14:22:59',
    notice_AbleToUploadDocument: 'No',
    notice_UnableToUploadReason: 'no documents to upload',
    rentDetails_CurrentRent: '125000',
    rentDetails_Frequency: 'MONTHLY',
    rentDetails_CalculatedDailyCharge: '3285',
    rentArrears_Total: '23999',
    rentArrears_RecoveryAttempted: 'NO',
    arrearsJudgmentWanted: 'NO',
    claimantNamePossessiveForm: 'Possession Claims Solicitor Org’s',
    claimantCircumstancesSelect: 'NO',
    hasDefendantCircumstancesInfo: 'NO',
    suspensionOfRTB_ShowHousingActsPage: 'No',
    demotionOfTenancy_ShowHousingActsPage: 'No',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: [],
    additionalReasonsForPossession: {
      hasReasons: 'NO',
      reasons: null
    },
    hasUnderlesseeOrMortgagee: 'NO',
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'NO',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW',
    statementOfTruth: {
      completedBy: 'CLAIMANT',
      fullNameParty: 'fg',
      positionParty: 'fg',
      agreementClaimant: ['BELIEVE_TRUE'],
    },
  },
  submitCasePayloadCaseDetails: {
    legislativeCountry: 'England',
    orgNameFound: 'Yes',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
        },
        {
          code: 'MORTGAGE_LENDER',
          label: 'Mortgage lender'
        },
        {
          code: 'OTHER',
          label: 'Other'
        }
      ],
      valueCode: 'PROVIDER_OF_SOCIAL_HOUSING'
    },
    claimAgainstTrespassers: 'NO',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    claimantContactEmail: 'pcs-solicitor1@test.com',
    isCorrectClaimantContactEmail: 'YES',
    orgAddressFound: 'Yes',
    organisationAddress: {
      AddressLine1: 'Ministry Of Justice',
      AddressLine2: 'Seventh Floor 102 Petty France',
      PostTown: 'London',
      PostCode: 'SW1H 9AJ',
      Country: 'United Kingdom'
    },
    formattedClaimantContactAddress: 'Ministry Of Justice<br>Seventh Floor 102 Petty France<br>London<br>SW1H 9AJ',
    isCorrectClaimantContactAddress: 'YES',
    claimantProvidePhoneNumber: 'YES',
    claimantContactPhoneNumber: '04469172429',
    defendant1: {
      nameKnown: 'YES',
      addressKnown: 'YES',
      addressSameAsPossession: 'NO',
      correspondenceAddress: {
        AddressLine1: '6 Second Avenue',
        AddressLine2: 'Oxford Street',
        AddressLine3: '',
        PostTown: 'London',
        County: '',
        Country: 'United Kingdom',
        PostCode: 'W3 7RX'
      },
      firstName: 'John',
      lastName: 'Doe',
    },
    addAnotherDefendant: 'NO',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
    ],
    tenancy_TypeOfTenancyLicence: 'DEMOTED_TENANCY',
    tenancy_TenancyLicenceDate: '2020-10-02',
    tenancy_HasCopyOfTenancyLicence: 'NO',
    tenancy_ReasonsForNoTenancyLicenceDocuments: 'user input no tenancy documents',
    showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'YES',
    introGrounds_IntroductoryDemotedOrOtherGrounds: ['RENT_ARREARS', 'ANTI_SOCIAL', 'BREACH_OF_THE_TENANCY', 'ABSOLUTE_GROUNDS'],
    antiSocialBehaviourGround: 'Antisocial behaviour test input',
    breachOfTheTenancyGround: 'breach of tenancy test data',
    absoluteGrounds: 'absolute grounds test data',
    rentDetails_CurrentRent: '125000',
    rentDetails_Frequency: 'WEEKLY',
    rentDetails_CalculatedDailyCharge: '1429',
    rentArrears_StatementDocuments: [
      {
        id: '77d43175-cf1f-4feb-885f-0a0c454391c2',
        value: {
          document_url: `${process.env.DM_STORE}/documents/fee16b4c-b9f2-4697-8f5e-10db5c51e056`,
          document_binary_url: `${process.env.DM_STORE}/documents/fee16b4c-b9f2-4697-8f5e-10db5c51e056/binary`,
          document_filename: 'rentStatement.pdf',
        }
      }
    ],
    rentArrears_Total: '23999',
    rentArrears_RecoveryAttempted: 'NO',
    arrearsJudgmentWanted: 'YES',
    preActionProtocolCompleted: 'YES',
    mediationAttempted: 'YES',
    settlementAttempted: 'YES',
    noticeServed: 'Yes',
    notice_PostedDate: '2025-11-10',
    notice_ServiceMethod: 'FIRST_CLASS_POST',
    notice_AbleToUploadDocument: 'No',
    notice_UnableToUploadReason: 'no documents to upload',
    claimantNamePossessiveForm: 'Possession Claims Solicitor Org’s',
    claimantCircumstancesSelect: 'YES',
    claimantCircumstancesDetails: 'claimant circumstances test data',
    hasDefendantCircumstancesInfo: 'YES',
    defendantCircumstancesInfo: 'User Input Defendants circumstances',
    suspensionOfRTB_ShowHousingActsPage: 'No',
    demotionOfTenancy_ShowHousingActsPage: 'Yes',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: ['DEMOTION_OF_TENANCY', 'SUSPENSION_OF_RIGHT_TO_BUY'],
    suspensionOfRightToBuyActs: 'SECTION_6A_2',
    demotionOfTenancyActs: 'SECTION_82A_2',
    demotionOfTenancy_StatementOfExpressTermsServed: 'YES',
    demotionOfTenancy_StatementOfExpressTermsDetails: 'User input terms',
    demotionOrderReason: 'demotion reasons test data',
    suspensionOrderReason: 'suspension of order reason',
    additionalReasonsForPossession: {
      hasReasons: 'YES',
      reasons: 'User input for additional reasons'
    },
    hasUnderlesseeOrMortgagee: 'YES',
    underlesseeOrMortgagee1: {
      nameKnown: 'YES',
      name: 'peter',
      addressKnown: 'YES',
      address: {
        AddressLine1: '4 Second Avenue',
        AddressLine2: 'Whirlpool Street',
        AddressLine3: '',
        PostTown: 'London',
        County: '',
        Country: 'United Kingdom',
        PostCode: 'W3 7RX'
      }
    },
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'NO',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW',
    endButtonLabel: 'Submit claim',
    statementOfTruth: {
      completedBy: 'CLAIMANT',
      fullNameParty: 'fg',
      positionParty: 'fg',
      fullNameLegalRep: null,
      firmNameLegalRep: null,
      positionLegalRep: null,
      agreementClaimant: ['BELIEVE_TRUE'],
      agreementClaimantLegalRep: [],
      agreementDefendantLegalRep: []
    },
  },
  submitCasePayloadCaseFileView: {
    orgNameFound: 'Yes',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      valueCode: 'PROVIDER_OF_SOCIAL_HOUSING'
    },
    claimAgainstTrespassers: 'NO',
    claimantContactEmail: 'pcs-solicitor-automation@test.com',
    isCorrectClaimantContactEmail: 'YES',
    orgAddressFound: 'Yes',
    organisationAddress: {
      AddressLine1: 'Ministry Of Justice',
      AddressLine2: 'Seventh Floor 102 Petty France',
      PostTown: 'London',
      PostCode: 'SW1H 9AJ',
      Country: 'United Kingdom'
    },
    formattedClaimantContactAddress: 'Ministry Of Justice<br>Seventh Floor 102 Petty France<br>London<br>SW1H 9AJ',
    isCorrectClaimantContactAddress: 'YES',
    claimantProvidePhoneNumber: 'NO',
    defendant1: {
      nameKnown: 'YES',
      firstName: 'peter',
      lastName: 'parker',
      addressKnown: 'NO',
      addressSameAsPossession: null,
      correspondenceAddress: {
        AddressLine1: null,
        AddressLine2: null,
        AddressLine3: null,
        PostTown: null,
        County: null,
        Country: null,
        PostCode: null
      }
    },
    addAnotherDefendant: 'NO',
    tenancy_TypeOfTenancyLicence: 'ASSURED_TENANCY',
    tenancy_TenancyLicenceDate: '2013-01-01',
    tenancy_HasCopyOfTenancyLicence: 'YES',
    tenancy_TenancyLicenceDocuments: [
      {
        id: '3bbe3b5d-8a49-46cf-ac3e-8863a7aea372',
        value: {
          document_url: `${process.env.DM_STORE}/documents/b5aacf07-97b4-4455-9140-c9220725a765`,
          document_binary_url: `${process.env.DM_STORE}/documents/b5aacf07-97b4-4455-9140-c9220725a765/binary`,
          document_filename: 'tenancy.pdf',
        }
      }
    ],
    claimDueToRentArrears: 'Yes',
    rentArrears_RentArrearsGrounds: [
      'RENT_ARREARS_GROUND10'
    ],
    hasOtherAdditionalGrounds: 'No',
    preActionProtocolCompleted: 'YES',
    mediationAttempted: 'YES',
    settlementAttempted: 'YES',
    noticeServed: 'Yes',
    notice_ServiceMethod: 'FIRST_CLASS_POST',
    notice_PostedDate: '2015-12-01',
    notice_Documents: [
      {
        id: '2fbd79a1-19dc-46fb-9baf-83534187f37f',
        value: {
          document_url: `${process.env.DM_STORE}/documents/f42251d3-70ac-4a60-914d-c1af51f0a5ac`,
          document_binary_url: `${process.env.DM_STORE}/documents/f42251d3-70ac-4a60-914d-c1af51f0a5ac/binary`,
          document_filename: 'NoticeDetails.pdf',
        }
      }
    ],
    rentDetails_CurrentRent: '1200',
    rentDetails_Frequency: 'WEEKLY',
    rentDetails_CalculatedDailyCharge: '171',
    rentArrears_StatementDocuments: [
      {
        id: '468ec779-350f-4484-9694-ea6b3285d86e',
        value: {
          document_url: `${process.env.DM_STORE}/documents/c88969c7-b5c7-4f87-89b7-5ec0c74c3e52`,
          document_binary_url: `${process.env.DM_STORE}/documents/c88969c7-b5c7-4f87-89b7-5ec0c74c3e52/binary`,
          document_filename: `rentStatement.pdf`,
        }
      }
    ],
    rentArrears_Total: '123300',
    rentArrears_RecoveryAttempted: 'YES',
    rentArrears_RecoveryAttemptDetails: 'test attempts made',
    arrearsJudgmentWanted: 'YES',
    claimantNamePossessiveForm: 'Possession Claims Solicitor Org’s',
    claimantCircumstancesSelect: 'YES',
    claimantCircumstancesDetails: 'claimant circumstances',
    hasDefendantCircumstancesInfo: 'YES',
    defendantCircumstancesInfo: 'defendant circumstances',
    suspensionOfRTB_ShowHousingActsPage: 'Yes',
    demotionOfTenancy_ShowHousingActsPage: 'No',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: [
      'SUSPENSION_OF_RIGHT_TO_BUY'
    ],
    suspensionOfRTB_HousingAct: 'SECTION_6A_2',
    suspensionOfRTB_Reason: 'test reasons',
    additionalReasonsForPossession: {
      hasReasons: 'YES',
      reasons: 'test additional possessions'
    },
    hasUnderlesseeOrMortgagee: 'YES',
    underlesseeOrMortgagee1: {
      nameKnown: 'NO',
      name: null,
      addressKnown: 'NO',
      address: {
        AddressLine1: null,
        AddressLine2: null,
        AddressLine3: null,
        PostTown: null,
        County: null,
        Country: null,
        PostCode: null
      }
    },
    addAdditionalUnderlesseeOrMortgagee: 'NO',
    wantToUploadDocuments: 'YES',
    additionalDocuments: [
      {
        value: {
          documentType: {
            value: {
              code: '4b80f5c9-ed97-472e-a4c9-6dbb7bfd2863',
              label: 'Inspection or report'
            },
            valueCode: '4b80f5c9-ed97-472e-a4c9-6dbb7bfd2863',
            valueLabel: 'Inspection or report'
          },
          description: 'inspection',
          document: {
            document_url: `${process.env.DM_STORE}/documents/bdedc7cf-21e2-46db-8176-da5dc5728b3e`,
            document_binary_url: `${process.env.DM_STORE}/documents/bdedc7cf-21e2-46db-8176-da5dc5728b3e/binary`,
            document_filename: 'inspectionOrReport.pdf',
          }
        },
        id: '6cb51a17-7ffe-4d10-b979-625bba2645f5'
      },
      {
        value: {
          documentType: {
            value: {
              code: 'a8a4ddf2-5d14-4b42-a892-de2547e6202e',
              label: 'Other document'
            },

            valueLabel: 'Other document',
            valueCode: 'a8a4ddf2-5d14-4b42-a892-de2547e6202e'
          },
          description: 'test',
          document: {
            document_url: `${process.env.DM_STORE}/documents/844af629-9597-42bd-9bed-6433f3cc9c86`,
            document_binary_url: `${process.env.DM_STORE}/documents/844af629-9597-42bd-9bed-6433f3cc9c86/binary`,
            document_filename: 'otherDocument.pdf',
          }
        },
        id: '0765e04d-c8bc-4c64-afab-6f91916d19b2'
      },
      {
        value: {
          documentType: {
            value: {
              code: '8b60b091-8e09-4748-af9d-dc67dd1657cb',
              label: 'Legal aid certificate'
            },

            valueLabel: 'Legal aid certificate',
            valueCode: '8b60b091-8e09-4748-af9d-dc67dd1657cb'
          },
          description: 'legal aid',
          document: {
            document_url: `${process.env.DM_STORE}/documents/7a829740-61c3-4180-a6f3-0c24ab73cd6c`,
            document_binary_url: `${process.env.DM_STORE}/documents/7a829740-61c3-4180-a6f3-0c24ab73cd6c/binary`,
            document_filename: 'legalAidCertificate.pdf',
          }
        },
        id: 'c76d0fb7-f5cb-4ea1-826a-29059e6322a3'
      },
      {
        value: {
          documentType: {
            value: {
              code: '1fa7f01c-5376-45f4-9fa6-baaee1d6f65d',
              label: 'Notice for service out of the jurisdiction'
            },

            valueLabel: 'Notice for service out of the jurisdiction',
            valueCode: '1fa7f01c-5376-45f4-9fa6-baaee1d6f65d'
          },
          description: 'Notice for service',
          document: {
            document_url: `${process.env.DM_STORE}/documents/481ce2ed-8557-4036-be9c-3d8e63638f79`,
            document_binary_url: `${process.env.DM_STORE}/documents/481ce2ed-8557-4036-be9c-3d8e63638f79/binary`,
            document_filename: 'noticeForService.pdf',
          }
        },
        id: '7e7bcdea-7dd4-465b-b2ab-7e046ca2c57d'
      }

    ],
    applicationWithClaim: 'YES',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW',
    endButtonLabel: 'Submit claim',
    statementOfTruth: {
      completedBy: 'CLAIMANT',
      fullNameParty: 'Jen Parker',
      positionParty: 'Rep',
      fullNameLegalRep: null,
      firmNameLegalRep: null,
      positionLegalRep: null,
      agreementClaimant: [
        'BELIEVE_TRUE'
      ],
      agreementClaimantLegalRep: [],
      agreementDefendantLegalRep: []
    }

  },
  submitCasePayloadDefault: {
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing',
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord',
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing',
        },
        {
          code: 'MORTGAGE_LENDER',
          label: 'Mortgage lender',
        },
        {
          code: 'OTHER',
          label: 'Other',
        },
      ],
      valueCode: 'PROVIDER_OF_SOCIAL_HOUSING',
    },
    claimAgainstTrespassers: 'NO',
    orgNameFound: 'YES',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    claimantContactEmail: 'pcs-solicitor-automation@test.com',
    isCorrectClaimantContactEmail: 'YES',
    orgAddressFound: 'YES',
    organisationAddress: {
      AddressLine1: 'Ministry Of Justice',
      AddressLine2: 'Seventh Floor 102 Petty France',
      PostTown: 'London',
      PostCode: 'SW1H 9AJ',
      Country: 'United Kingdom',
    },
    formattedClaimantContactAddress: 'Ministry Of Justice<br>Seventh Floor 102 Petty France<br>London<br>SW1H 9AJ',
    isCorrectClaimantContactAddress: 'YES',
    claimantProvidePhoneNumber: 'NO',
    defendant1: {
      nameKnown: 'YES',
      addressKnown: 'YES',
      addressSameAsPossession: 'YES',
      firstName: 'John',
      lastName: 'Doe',
    },
    addAnotherDefendant: 'YES',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO',
        },
        id: null,
      },
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO',
        },
        id: null,
      },
    ],
    tenancy_TypeOfTenancyLicence: 'OTHER',
    tenancy_DetailsOfOtherTypeOfTenancyLicence: 'Other tenancy - short term',
    tenancy_TenancyLicenceDate: null,
    tenancy_TenancyLicenceDocuments: [],
    showIntroductoryDemotedOtherGroundReasonPage: 'YES',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'YES',
    introGrounds_IntroductoryDemotedOrOtherGrounds: ['ANTI_SOCIAL'],
    antiSocialBehaviourGround: 'Antisocial behaviour',
    preActionProtocolCompleted: 'NO',
    mediationAttempted: 'NO',
    settlementAttempted: 'NO',
    noticeServed: 'YES',
    notice_AbleToUploadDocument: 'No',
    notice_UnableToUploadReason: 'no documents to upload',
    claimantNamePossessiveForm: 'Possession Claims Solicitor Org’s',
    claimantCircumstancesSelect: 'NO',
    hasDefendantCircumstancesInfo: 'NO',
    suspensionOfRTB_ShowHousingActsPage: 'NO',
    demotionOfTenancy_ShowHousingActsPage: 'NO',
    suspensionToBuyDemotionOfTenancyPages: 'NO',
    alternativesToPossession: [],
    additionalReasonsForPossession: {
      hasReasons: 'NO',
    },
    hasUnderlesseeOrMortgagee: 'NO',
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'YES',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW',
    statementOfTruth: {
      completedBy: 'CLAIMANT',
      fullNameParty: 'fg',
      positionParty: 'fg',
      agreementClaimant: ['BELIEVE_TRUE'],
    },
  },

  submitCaseApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/events`,
};