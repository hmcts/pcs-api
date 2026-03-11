export const enforceWarrantApiData = {
  enforceCaseEventName: 'enforceTheOrder',
  enforceCasePayloadYesJourney: {
    selectEnforcementType: {
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
    warrantFeeAmount: "£148",
    writFeeAmount: "£80",
    warrantCorrectNameAndAddress: "YES",
    warrantDefendantsDOBKnown: "YES",
    warrantDefendantsDOBDetails: "tewt",
    warrantShowPeopleWhoWillBeEvictedPage: "Yes",
    warrantEvictEveryone: "YES",
    warrantAnyRiskToBailiff: "YES",
    warrantEnforcementRiskCategories: [
      "PROTEST_GROUP_MEMBER"
    ],
    warrantEnforcementProtestGroupMemberDetails: "group",
    vulnerablePeoplePresent: "YES",
    vulnerableAdultsChildren: {
      vulnerableCategory: "VULNERABLE_ADULTS",
      vulnerableReasonText: "adults"
    },
    warrantIsDifficultToAccessProperty: "YES",
    warrantClarificationOnAccessDifficultyText: "Explain why it’s difficult to access the property",
    warrantAdditionalInformationSelect: "YES",
    warrantAdditionalInformationDetails: "Tell us anything else that could help with the eviction",
    warrantAmountOwed: "1200",
    warrantAreLegalCostsToBeClaimed: "YES",
    warrantAmountOfLegalCosts: "1300",
    warrantHaveLandRegistryFeesBeenPaid: "YES",
    warrantAmountOfLandRegistryFees: "1400",
    warrantRepaymentChoice: "ALL",
    warrantEnforcementLanguageUsed: "ENGLISH",
    warrantIsSuspendedOrder: "YES",
    warrantCertification: [
      "CERTIFY"
    ],
    warrantCompletedBy: "CLAIMANT",
    warrantAgreementClaimant: [
      "BELIEVE_TRUE"
    ],
    warrantFullNameClaimant: "Full name",
    warrantPositionClaimant: "Position or office held"

  },
  enforceCasePayloadNoJourney: {
    selectEnforcementType: {
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
    warrantFeeAmount: "£148",
    writFeeAmount: "£80",
    warrantCorrectNameAndAddress: "YES",
    warrantDefendantsDOBKnown: "NO",
    warrantDefendantsDOBDetails: "tewt",
    warrantShowPeopleWhoWillBeEvictedPage: "NO",
    warrantEvictEveryone: "NO",
    warrantAnyRiskToBailiff: "NO",
    warrantEnforcementRiskCategories: [
      "PROTEST_GROUP_MEMBER"
    ],
    warrantEnforcementProtestGroupMemberDetails: "group",
    vulnerablePeoplePresent: "YES",
    vulnerableAdultsChildren: {
      vulnerableCategory: "VULNERABLE_ADULTS",
      vulnerableReasonText: "adults"
    },
    warrantIsDifficultToAccessProperty: "NO",
    warrantClarificationOnAccessDifficultyText: "Explain why it’s difficult to access the property",
    warrantAdditionalInformationSelect: "NO",
    warrantAdditionalInformationDetails: "Tell us anything else that could help with the eviction",
    warrantAmountOwed: "1200",
    warrantAreLegalCostsToBeClaimed: "YES",
    warrantAmountOfLegalCosts: "1300",
    warrantHaveLandRegistryFeesBeenPaid: "YES",
    warrantAmountOfLandRegistryFees: "1400",
    warrantRepaymentChoice: "ALL",
    warrantEnforcementLanguageUsed: "ENGLISH",
    warrantIsSuspendedOrder: "YES",
    warrantCertification: [
      "CERTIFY"
    ],
    warrantCompletedBy: "CLAIMANT",
    warrantAgreementClaimant: [
      "BELIEVE_TRUE"
    ],
    warrantFullNameClaimant: "Full name",
    warrantPositionClaimant: "Position or office held"

  },
    enforceCaseApiEndPoint: () =>
      `/cases/${process.env.CASE_NUMBER}/events`,
  
}
