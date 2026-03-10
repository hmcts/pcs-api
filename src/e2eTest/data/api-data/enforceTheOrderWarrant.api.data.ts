export const enforceWarrantApiData = {
  enforceCaseEventName: 'enforceTheOrder',
  enforceCasePayload: {


    // formattedDefendantNames: "TVR HN<br>\nTest1 Testing1<br>",
    // formattedPropertyAddress: "1 Rse Way<br>London<br>SW11 1PD",
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
    //warrantOfRestitutionInfoText: "<p class=\"govuk-body govuk-!-font-weight-bold\">If you choose a warrant of restitution</p>\n<p class=\"govuk-body govuk-!-margin-bottom-1\">It is free to apply for a warrant of\nrestitution, but:</p>\n<ul class=\"govuk-list govuk-list--bullet\">\n  <li class=\"govuk-!-font-size-19\">you’ll need a warrant of possession before you can apply\n  </li>\n  <li class=\"govuk-!-font-size-19\">you can only use it if you have already tried to evict\n  someone, but they returned to the property after the eviction. For example, if they\n  unlawfully returned after the bailiffs left.</li>\n</ul>\n<p class=\"govuk-body govuk-!-margin-bottom-1\">In a warrant of restitution, the judge will:\n</p>\n<ul class=\"govuk-list govuk-list--bullet\">\n  <li class=\"govuk-!-font-size-19\">review evidence that the defendants returned to the\n  property after the eviction</li>\n  <li class=\"govuk-!-font-size-19\">(in most cases) make a decision without a hearing</li>\n</ul>",
    warrantCorrectNameAndAddress: "YES",
    warrantDefendantsDOBKnown: "YES",
    warrantDefendantsDOBDetails: "tewt",
    warrantShowPeopleWhoWillBeEvictedPage: "Yes",
    warrantEvictEveryone: "YES",
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
    warrantIsDifficultToAccessProperty: "YES",
    warrantClarificationOnAccessDifficultyText: "Explain why it’s difficult to access the property",
    warrantAdditionalInformationSelect: "YES",
    warrantAdditionalInformationDetails: "Tell us anything else that could help with the eviction",
    warrantAmountOwed: "1200",
    warrantAreLegalCostsToBeClaimed: "YES",
    warrantAmountOfLegalCosts: "1300",
    warrantHaveLandRegistryFeesBeenPaid: "YES",
    warrantAmountOfLandRegistryFees: "1400",
    //warrantRepaymentSummaryMarkdown: "<table class=\"govuk-table\">\n  <caption class=\"govuk-table__caption govuk-table__caption--m govuk-!-font-size-19\">Total amount that can be repaid</caption>\n  <thead class=\"govuk-table__head\">\n    <tr class=\"govuk-table__row\">\n     <th scope=\"col\" class=\"govuk-table__header\">Repayment for</th>\n     <th scope=\"col\" class=\"govuk-table__header\">Amount</th>\n    </tr>\n  </thead>\n  <tbody class=\"govuk-table__body\">\n    <tr class=\"govuk-table__row\">\n     <th scope=\"row\" class=\"govuk-table__header govuk-body govuk-!-font-weight-regular\">Arrears and other costs\n     </th>\n     <td class=\"govuk-table__cell\">£12</td>\n    </tr>\n    <tr class=\"govuk-table__row\">\n     <th scope=\"row\" class=\"govuk-table__header govuk-body govuk-!-font-weight-regular\">Legal costs</th>\n     <td class=\"govuk-table__cell\">£13</td>\n    </tr>\n    <tr class=\"govuk-table__row\">\n     <th scope=\"row\" class=\"govuk-table__header govuk-body govuk-!-font-weight-regular\">Land Registry fees</th>\n     <td class=\"govuk-table__cell\">£14</td>\n    </tr>\n    <tr class=\"govuk-table__row\">\n     <th scope=\"row\" class=\"govuk-table__header govuk-body govuk-!-font-weight-regular\">Warrant of possession fee</th>\n     <td class=\"govuk-table__cell\">£148</td>\n    </tr>\n    <tr class=\"govuk-table__row\">\n     <th scope=\"row\" class=\"govuk-table__header govuk-body govuk-!-font-weight-regular\">Total</th>\n     <td class=\"govuk-table__cell\">£187</td>\n    </tr>\n  </tbody>\n</table>",
    warrantRepaymentChoice: "ALL",
    warrantEnforcementLanguageUsed: "ENGLISH",
    warrantIsSuspendedOrder: "YES",
    warrantCertification: [
      "CERTIFY"
    ],
    //warrantStatementOfTruthRepaymentSummaryMarkdown: "<table class=\"govuk-table\">\n  <caption class=\"govuk-table__caption govuk-table__caption--m govuk-!-font-size-19\">The payments due</caption>\n  <thead class=\"govuk-table__head\">\n    <tr class=\"govuk-table__row\">\n     <th scope=\"col\" class=\"govuk-table__header\">Repayment for</th>\n     <th scope=\"col\" class=\"govuk-table__header\">Amount</th>\n    </tr>\n  </thead>\n  <tbody class=\"govuk-table__body\">\n    <tr class=\"govuk-table__row\">\n     <th scope=\"row\" class=\"govuk-table__header govuk-body govuk-!-font-weight-regular\">Arrears and other costs\n     </th>\n     <td class=\"govuk-table__cell\">£12</td>\n    </tr>\n    <tr class=\"govuk-table__row\">\n     <th scope=\"row\" class=\"govuk-table__header govuk-body govuk-!-font-weight-regular\">Legal costs</th>\n     <td class=\"govuk-table__cell\">£13</td>\n    </tr>\n    <tr class=\"govuk-table__row\">\n     <th scope=\"row\" class=\"govuk-table__header govuk-body govuk-!-font-weight-regular\">Land Registry fees</th>\n     <td class=\"govuk-table__cell\">£14</td>\n    </tr>\n    <tr class=\"govuk-table__row\">\n     <th scope=\"row\" class=\"govuk-table__header govuk-body govuk-!-font-weight-regular\">Warrant of possession fee</th>\n     <td class=\"govuk-table__cell\">£148</td>\n    </tr>\n    <tr class=\"govuk-table__row\">\n     <th scope=\"row\" class=\"govuk-table__header govuk-body govuk-!-font-weight-regular\">Total</th>\n     <td class=\"govuk-table__cell\">£187</td>\n    </tr>\n  </tbody>\n</table>",
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
