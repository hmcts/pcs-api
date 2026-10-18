export const enforceWarrantApiData = {
  enforceCaseEventName: 'enforceTheOrder',
  enforceCasePayloadYesJourney: {
    chooseEnforcementType: {
      value: {
        code: "WARRANT",
        label: "Warrant of possession"
      },
      list_items: [
        {
          code: "WARRANT",
          label: "Warrant of possession"
        },
        {
          code: "WRIT",
          label: "Writ of possession"
        },
        {
          code: "WARRANT_OF_RESTITUTION",
          label: "Warrant of restitution"
        }
      ],
      valueCode: "WARRANT"
    },
    warrantFeeAmount: "£152",
    writFeeAmount: "£82",
    warrantDefendantInBreathingSpace: "NO",
    warrantDefendantMissedPayments: "YES",
    warrantCorrectNameAndAddress: "YES",
    warrantDefendantsDOBKnown: "YES",
    warrantDefendantsDOBDetails: "tewt",
    warrantAnyRiskToBailiff: "YES",
    warrantEnforcementRiskCategories: [
      "VIOLENT_OR_AGGRESSIVE",
      "FIREARMS_POSSESSION",
      "CRIMINAL_OR_ANTISOCIAL",
      "VERBAL_OR_WRITTEN_THREATS",
      "PROTEST_GROUP_MEMBER",
      "AGENCY_VISITS",
      "AGGRESSIVE_ANIMALS"
    ],
    warrantEnforcementProtestGroupMemberDetails: "group",
    vulnerablePeoplePresent: "YES",
    vulnerableAdultsChildren: {
      vulnerableCategory: "VULNERABLE_ADULTS",
      vulnerableReasonText: "User input on vulnerable adults"
    },
    warrantIsDifficultToAccessProperty: "YES",
    warrantClarificationOnAccessDifficultyText: "User input informing the difficulties accessing the property",
    warrantAdditionalInformationSelect: "YES",
    warrantAdditionalInformationDetails: "Tell us anything else that could help with the eviction",
    warrantEnforcementLanguageUsed: "ENGLISH",
    warrantCertification: [
      "CERTIFY"
    ],
    warrantCompletedBy: "CLAIMANT",
    warrantAgreementClaimant: [
      "BELIEVE_TRUE"
    ],
    warrantFullNameParty: "Full name",
    warrantPositionParty: "Position or office held"

  },
  enforceCasePayloadNoJourney: {
    chooseEnforcementType: {
      value: {
        code: "WARRANT",
        label: "Warrant of possession"
      },
      list_items: [
        {
          code: "WARRANT",
          label: "Warrant of possession"
        },
        {
          code: "WRIT",
          label: "Writ of possession"
        },
        {
          code: "WARRANT_OF_RESTITUTION",
          label: "Warrant of restitution"
        }
      ],
      valueCode: "WARRANT"
    },
    warrantFeeAmount: "£152",
    writFeeAmount: "£82",
    warrantDefendantInBreathingSpace: "NO",
    warrantDefendantMissedPayments: "YES",
    warrantCorrectNameAndAddress: "YES",
    warrantDefendantsDOBKnown: "NO",
    warrantDefendantsDOBDetails: "tewt",
    warrantAnyRiskToBailiff: "NO",
    warrantEnforcementRiskCategories: [
      "PROTEST_GROUP_MEMBER"
    ],
    warrantEnforcementProtestGroupMemberDetails: "group",
    vulnerablePeoplePresent: "NO",
    warrantIsDifficultToAccessProperty: "NO",
    warrantClarificationOnAccessDifficultyText: "Explain why it’s difficult to access the property",
    warrantAdditionalInformationSelect: "NO",
    warrantAdditionalInformationDetails: "Tell us anything else that could help with the eviction",
    warrantEnforcementLanguageUsed: "ENGLISH",
    warrantCertification: [
      "CERTIFY"
    ],
    warrantCompletedBy: "CLAIMANT",
    warrantAgreementClaimant: [
      "BELIEVE_TRUE"
    ],
    warrantFullNameParty: "Full name",
    warrantPositionParty: "Position or office held"

  },
  enforceCaseApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/events`,
}
