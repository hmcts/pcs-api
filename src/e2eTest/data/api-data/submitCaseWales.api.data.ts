import { testDocument, testDocumentListValue } from "@utils/common/uploadDocument.utils";
import { generateRandomFirstAndLastNames } from "@utils/common/userData.utils";

const names = generateRandomFirstAndLastNames({
countOfFirstNamesToGenerate: 3,
countOfLastNamesToGenerate: 3,
});

export const submitCaseApiDataWales = {
  submitCaseEventName: 'resumePossessionClaim',

  submitCasePayloadCaseSummary:()=>({
    orgNameFound: 'Yes',
    claimantName: 'PCS_Local_Auth_Org_Testing',
    isClaimantNameCorrect: 'YES',
    legislativeCountry: 'Wales',
    claimantType: {
      value: {
        code: 'COMMUNITY_LANDLORD',
        label: 'Community landlord'
      },
      valueCode: 'COMMUNITY_LANDLORD'
    },
    claimAgainstTrespassers: 'NO',
    isExemptLandlord: 'YES',
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
      addressSameAsPossession: 'YES',
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
    occupationLicenceTypeWales: 'SECURE_CONTRACT',
    licenceStartDate: '2012-01-01',
    licenceDocuments: [],
    secureGroundsWales_DiscretionaryGrounds: [
      'RENT_ARREARS_S157',
      'ANTISOCIAL_BEHAVIOUR_S157',
      'ESTATE_MANAGEMENT_GROUNDS_S160'
    ],
    secureGroundsWales_EstateManagementGrounds: [
      'BUILDING_WORKS'
    ],
    secureGroundsWales_MandatoryGrounds: [
      'LANDLORD_NOTICE_S186'
    ],
    showReasonsForGroundsPageWales: 'Yes',
    walesSecureLandlordNoticeSection186Reason: 'Test user input reason under section 186',
    walesSecureBuildingWorksReason: 'building ground works test data',
    showASBQuestionsPageWales: 'Yes',
    walesAntisocialBehaviour: 'YES',
    walesAntisocialBehaviourDetails: 'Test data anti social',
    walesIllegalPurposesUse: 'YES',
    walesIllegalPurposesUseDetails: 'Test data illegal purposes',
    walesOtherProhibitedConduct: 'YES',
    walesOtherProhibitedConductDetails: 'Test data prohibited conduct',
    preActionProtocolCompleted: 'YES',
    mediationAttempted: 'YES',
    settlementAttempted: 'YES',
    walesNoticeServed: 'Yes',
    walesTypeOfNoticeServed: 'document',
    notice_ServiceMethod: 'FIRST_CLASS_POST',
    notice_PostedDate: '2015-12-01',
    notice_AbleToUploadDocument: 'No',
    notice_UnableToUploadReason: 'no documents to upload',
    rentDetails_CurrentRent: '10000',
    rentDetails_Frequency: 'WEEKLY',
    rentDetails_CalculatedDailyCharge: '1429',
    rentArrears_StatementDocuments: [
     testDocumentListValue('rentStatement.pdf')
    ],
    rentArrears_Total: '10000',
    rentArrears_RecoveryAttempted: 'YES',
    rentArrears_RecoveryAttemptDetails: 'recovery attempts made',
    arrearsJudgmentWanted: 'YES',
    claimantNamePossessiveForm: 'PCS_Local_Auth_Org_Testing’s',
    claimantCircumstancesSelect: 'YES',
    claimantCircumstancesDetails: 'test data claimant circumstances',
    hasDefendantCircumstancesInfo: 'YES',
    defendantCircumstancesInfo: 'test data defendant circumstances',
    prohibitedConductWalesClaim: 'YES',
    periodicContractTermsWales: {
      agreedTermsOfPeriodicContract: 'YES',
      detailsOfTerms: 'test data contract holder agreement'
    },
    prohibitedConductWalesClaimDetails: 'test data why making this claim',
    additionalReasonsForPossession: {
      hasReasons: 'YES',
      reasons: 'test data additional reasons'
    },
    hasUnderlesseeOrMortgagee: 'YES',
    underlesseeOrMortgagee1: {
      nameKnown: 'YES',
      name: 'Jen parker',
      addressKnown: 'YES',
      address: {
        AddressLine1: '6 Pentre Street',
        AddressLine2: 'Cymed avenue',
        AddressLine3: '',
        PostTown: 'Caerdydd',
        County: '',
        Country: 'Deyrnas Unedig',
        PostCode: 'CF11 6QX'
      }
    },
    addAdditionalUnderlesseeOrMortgagee: 'NO',
    walesDocs_HasEnergyPerformanceCertificate: 'YES',
    walesDocs_EnergyPerformance: [
      testDocumentListValue('noticeForService.pdf')
    ],
    walesDocs_HasGasSafetyReport: 'NO',
    walesDocs_NoGasReportReason: 'no gas safety report',
    walesDocs_HasElectricalInstallationConditionReport: 'NO',
    walesDocs_NoEicrReason: 'no copy of EICR',
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'YES',
    languageUsed: 'WELSH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW',
    endButtonLabel: 'Submit claim',
    statementOfTruth: {
      completedBy: 'CLAIMANT',
      fullNameParty: 'test',
      positionParty: 'Head',
      fullNameLegalRep: null,
      firmNameLegalRep: null,
      positionLegalRep: null,
      agreementClaimant: [
        'BELIEVE_TRUE'
      ],
      agreementClaimantLegalRep: []
    }
  }),
  submitCasePayloadCaseFileView:()=> ({
    regionId: '7',
    caseManagementLocationNumber: '366572',
    orgNameFound: 'Yes',
    claimantName: 'PCS_Local_Auth_Org_Testing',
    isClaimantNameCorrect: 'YES',
    legislativeCountry: 'Wales',
    claimantType: {
      value: {
        code: 'COMMUNITY_LANDLORD',
        label: 'Community landlord',
      },
      valueCode: 'COMMUNITY_LANDLORD',
    },
    claimAgainstTrespassers: 'NO',
    isExemptLandlord: 'YES',
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
    addAnotherDefendant: 'YES',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'YES',
          firstName: names.firstNames[1],
          lastName: names.lastNames[1],
          addressKnown: 'YES',
          addressSameAsPossession: 'YES',
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
        id: '61cd2875-03af-45c7-a1e4-db5583a1eb11'
      },
      {
        value: {
          nameKnown: 'YES',
          firstName: names.firstNames[2],
          lastName: names.lastNames[2],
          addressKnown: 'YES',
          addressSameAsPossession: 'YES',
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
        id: '52834083-6f14-4fa9-8f2c-ebb628380530'
      },
    ],
    occupationLicenceTypeWales: 'SECURE_CONTRACT',
    licenceStartDate: '2015-11-01',
    licenceDocuments: [
      testDocument(`licence.pdf`)
    ],
    secureGroundsWales_DiscretionaryGrounds: [
      'RENT_ARREARS_S157'
    ],
    secureGroundsWales_MandatoryGrounds: [],
    preActionProtocolCompleted: 'YES',
    mediationAttempted: 'YES',
    settlementAttempted: 'YES',
    walesNoticeServed: 'Yes',
    walesTypeOfNoticeServed: 'notice',
    notice_ServiceMethod: 'FIRST_CLASS_POST',
    notice_PostedDate: '2020-01-01',
    notice_AbleToUploadDocument: 'Yes',
    notice_Documents: [
      testDocumentListValue(`NoticeDetails.pdf`)
    ],
    rentDetails_CurrentRent: '1200',
    rentDetails_Frequency: 'WEEKLY',
    rentDetails_CalculatedDailyCharge: '171',
    rentArrears_StatementDocuments: [
      testDocumentListValue('rentStatement.pdf')
    ],
    rentArrears_Total: '123300',
    rentArrears_RecoveryAttempted: 'YES',
    rentArrears_RecoveryAttemptDetails: 'test attempts made',
    arrearsJudgmentWanted: 'YES',
    claimantNamePossessiveForm: 'PCS_Local_Auth_Org_Testing’s',
    claimantCircumstancesSelect: 'YES',
    claimantCircumstancesDetails: 'claimant circumstances',
    hasDefendantCircumstancesInfo: 'YES',
    defendantCircumstancesInfo: 'defendant circumstances',
    prohibitedConductWalesClaim: 'YES',
    periodicContractTermsWales: {
      agreedTermsOfPeriodicContract: 'NO',
      detailsOfTerms: null
    },
    prohibitedConductWalesClaimDetails: 'eds',
    additionalReasonsForPossession: {
      hasReasons: 'NO',
      reasons: null
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
    walesDocs_HasEnergyPerformanceCertificate: 'YES',
    walesDocs_EnergyPerformance: [
      testDocument('energyPerformance.pdf')
    ],
    walesDocs_HasGasSafetyReport: 'YES',
    walesDocs_GasSafetyReport: [
      testDocument('gasSafety.pdf')
    ],
    walesDocs_HasElectricalInstallationConditionReport: 'YES',
    walesDocs_ElectricalInstallation: [
      testDocument('electricalReport.pdf')
    ],
    wantToUploadDocuments: 'YES',
    additionalDocuments: [
      {
        value: {
          documentType: {
            value: {
              code: '4b80f5c9-ed97-472e-a4c9-6dbb7bfd2863',
              label: 'Inspection or report'
            },
            valueCode: '9438901a-0c43-4e19-829d-5c0e0118b093',
            valueLabel: 'Inspection or report'
          },
          description: 'inspection',
           document: testDocument('inspectionOrReport.pdf')
        },
      },
      {
        value: {
          documentType: {
            value: {
              code: 'a8a4ddf2-5d14-4b42-a892-de2547e6202e',
              label: 'Other document'
            },

            valueLabel: 'Other document',
            valueCode: '6c947e0e-5f25-4296-802a-26c9aaee4d36'
          },
          description: 'test',
          document: testDocument(`otherDocument.pdf`)
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
            valueCode: '8b60b091-8e09-4748-af9d-0d034af13a88'
          },
          description: 'legal aid',
          document:  testDocument(`legalAidCertificate.pdf`)
        },
        
      },
      {
        value: {
          documentType: {
            value: {
              code: '1fa7f01c-5376-45f4-9fa6-baaee1d6f65d',
              label: 'Notice for service out of the jurisdiction'
            },
            valueLabel: 'Notice for service out of the jurisdiction',
            valueCode: '52fc07d0-4dd2-4202-ac22-775cb8caaef0'
          },
          description: 'Notice for service',
          document: testDocument(`noticeForService.pdf`)
        },
      }

    ],
    documentsYouveUploaded: [
      'CURRENT_GAS_SAFETY_REPORT',
      'ENERGY_PERFORMANCE_CERTIFICATE',
      'CURRENT_EICR_REPORT'
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

  }),
  submitCaseApiEndPoint: (): string => `/cases/${process.env.CASE_NUMBER}/events`,
};