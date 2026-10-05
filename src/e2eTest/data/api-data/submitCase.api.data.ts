import { generateRandomFirstAndLastNames } from "@utils/common/userData.utils";

const names = generateRandomFirstAndLastNames({
countOfFirstNamesToGenerate: 3,
countOfLastNamesToGenerate: 3,
});

export const submitCaseApiData = {
  submitCaseEventName: 'resumePossessionClaim',
  submitCasePayload: {
    regionId: '1',
    caseManagementLocationNumber: '20262',
    orgNameFound: 'Yes',
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
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
      firstName: names.firstNames[0],
      lastName: names.lastNames[0],
    },
    addAnotherDefendant: 'YES',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'YES',
          firstName: names.firstNames[1],
          lastName: names.lastNames[1],
          addressKnown: 'YES',
          addressSameAsPossession: 'YES'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'YES',
          firstName: names.firstNames[2],
          lastName: names.lastNames[2],
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
    regionId: '1',
    caseManagementLocationNumber: '20262',
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
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
    regionId: '1',
    caseManagementLocationNumber: '20262',
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
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
      firstName: names.firstNames[0],
      lastName: names.lastNames[0],
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
    regionId: '1',
    caseManagementLocationNumber: '20262',
    legislativeCountry: 'England',
    orgNameFound: 'Yes',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
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
      firstName: names.firstNames[0],
      lastName: names.lastNames[0],
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
    regionId: '1',
    caseManagementLocationNumber: '20262',
    legislativeCountry: 'England',
    orgNameFound: 'Yes',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
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
      firstName: names.firstNames[0],
      lastName: names.lastNames[0],
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
    regionId: '1',
    caseManagementLocationNumber: '20262',
    legislativeCountry: 'England',
    orgNameFound: 'Yes',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
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
      firstName: names.firstNames[0],
      lastName: names.lastNames[0],
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
    regionId: '1',
    caseManagementLocationNumber: '20262',
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
      firstName: names.firstNames[0],
      lastName: names.lastNames[0],
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
    },
    addAnotherDefendant: 'YES',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'YES',
          firstName: names.firstNames[1],
          lastName: names.lastNames[1],
          addressKnown: 'YES',
          addressSameAsPossession: 'YES',
         },
        id: '61cd2875-03af-45c7-a1e4-db5583a1eb11'
      },
      {
        value: {
          nameKnown: 'YES',
          firstName: names.firstNames[2],
          lastName: names.lastNames[2],
          addressKnown: 'YES',
          addressSameAsPossession: 'YES',
        },
        id: '52834083-6f14-4fa9-8f2c-ebb628380530'
      },
    ],
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
    notice_AbleToUploadDocument: 'Yes',    
    notice_Documents: [
      {
        id: '83193877-14e5-4955-b589-c80fbe0edb53',
        value: {
          document_url: `${process.env.DM_STORE}/documents/341d1564-ee21-4394-af5d-de45aeb0951e`,
          document_binary_url: `${process.env.DM_STORE}/documents/341d1564-ee21-4394-af5d-de45aeb0951e/binary`,
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
              code: '69972f35-1f66-45be-9ea8-c57e0ca03501',
              label: 'Inspection or report'
            },
            valueCode: '69972f35-1f66-45be-9ea8-c57e0ca03501',
            valueLabel: 'Inspection or report'
          },
          description: 'inspection',
          document: {
            document_url: `${process.env.DM_STORE}/documents/9b3b7d09-188c-4d6b-9b11-8dcc78978c80`,
            document_binary_url: `${process.env.DM_STORE}/documents/9b3b7d09-188c-4d6b-9b11-8dcc78978c80/binary`,
            document_filename: 'inspectionOrReport.pdf',
          }
        },
        id: '87b02265-f876-43da-bcd4-2cbb998cb948'
      },
      {
        value: {
          documentType: {
            value: {
              code: '64af55f3-bb64-403b-832d-01df63bb47b9',
              label: 'Other document'
            },
            valueCode: '64af55f3-bb64-403b-832d-01df63bb47b9',
            valueLabel: 'Other document'
          },
          description: 'other doc',
          document: {
            document_url: `${process.env.DM_STORE}/documents/85ce1a13-4606-4519-aafd-b6ce3a0c2d3c`,
            document_binary_url: `${process.env.DM_STORE}/documents/85ce1a13-4606-4519-aafd-b6ce3a0c2d3c/binary`,
            document_filename: 'otherDocument.pdf',
          }
        },
        id: '66246982-90b5-407d-8b5b-59565a0cb091'
      },
      {
        value: {
          documentType: {
            value: {
              code: '61a87c03-9159-4c13-af31-1c3b90de42cb',
              label: 'Legal aid certificate'
            },
            valueCode: '61a87c03-9159-4c13-af31-1c3b90de42cb',
            valueLabel: 'Legal aid certificate'
          },
          description: 'legal aid',
          document: {
            document_url: `${process.env.DM_STORE}/documents/b4dea258-4a5c-4a00-b217-3ccacbdb1d8f`,
            document_binary_url: `${process.env.DM_STORE}/documents/b4dea258-4a5c-4a00-b217-3ccacbdb1d8f/binary`,
            document_filename: 'legalAidCertificate.pdf',
          }
        },
        id: '2beff9b1-44d2-4e1a-9ea5-4c9d29e9cc39'
      },
      {
        value: {
          documentType: {
            value: {
              code: 'a9012afc-83de-4678-912b-6a523c496073',
              label: 'Notice for service out of the jurisdiction'
            },
            valueCode: 'a9012afc-83de-4678-912b-6a523c496073',
            valueLabel: 'Notice for service out of the jurisdiction'
          },
          description: 'notice of service',
          document: {
            document_url: `${process.env.DM_STORE}/documents/90c34915-95a9-492e-a796-12fa2314f7b6`,
            document_binary_url: `${process.env.DM_STORE}/documents/90c34915-95a9-492e-a796-12fa2314f7b6/binary`,
            document_filename: 'noticeForService.pdf',
          }
        },
        id: '3b0a1f05-18c5-42ba-9e67-e81347125398'
      }
    ],
    applicationWithClaim: 'NO',
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
    regionId: '1',
    caseManagementLocationNumber: '20262',
    legislativeCountry: 'England',
    orgNameFound: 'Yes',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
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
      firstName: names.firstNames[0],
      lastName: names.lastNames[0],
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
      completedBy: 'LEGAL_REPRESENTATIVE',
      fullNameParty: null,
      positionParty: null,
      fullNameLegalRep: 'Solicitor Peter',
      firmNameLegalRep: 'Test Tech',
      positionLegalRep: 'Head Solicitor',
      agreementClaimant: [],
      agreementClaimantLegalRep: ['AGREED'],
      agreementDefendantLegalRep: []
    },
  },

  submitCaseApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/events`,
};
