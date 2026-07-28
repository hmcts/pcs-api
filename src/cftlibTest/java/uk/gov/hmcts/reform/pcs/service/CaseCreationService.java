package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.*;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.LocalDate;
import java.util.Set;
import java.math.BigDecimal;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@Service
@RequiredArgsConstructor
public class CaseCreationService {

    private final CcdClient ccdClient;

    public long createMinimalCase(String authorisation) {
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .postTown("London")
                                 .postCode("NW1 6XE")
                                 .build()
            )
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        CaseDetails caseDetails = ccdClient.createCase(caseData, authorisation);

        long caseReference = caseDetails.getId();

        caseData = PCSCase.builder()
            .caseManagementLocationNumber(20262)
            .regionId(1)
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .claimantInformation(ClaimantInformation.builder().claimantName("TreeTops Housing").build())
            .defendant1(DefendantDetails.builder()
                            .nameKnown(VerticalYesNo.YES)
                            .firstName("Danny")
                            .lastName("Defendant")
                            .build())
            .noticeServed(YesOrNo.NO)
            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
            .build();

        ccdClient.updateCase(resumePossessionClaim, caseReference, caseData, authorisation);

        return caseReference;
    }

    public long createMaximalCase(String authorisation) {

        PCSCase createData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .postTown("London")
                                 .postCode("NW1 6XE")
                                 .build()
            )
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .caseManagementLocationNumber(20262)
            .regionId(1)

            .claimAgainstTrespassers(VerticalYesNo.NO)

            .claimantContactPreferences(ClaimantContactPreferences.builder()
                                            .isCorrectClaimantContactEmail(VerticalYesNo.YES)
                                            .isCorrectClaimantContactAddress(VerticalYesNo.YES)
                                            .claimantProvidePhoneNumber(VerticalYesNo.YES)
                                            .claimantContactPhoneNumber("00000000000")
                                            .build())

            .defendant1(DefendantDetails.builder()
                            .nameKnown(VerticalYesNo.YES)
                            .firstName("Dominic")
                            .lastName("Defendant")
                            .addressKnown(VerticalYesNo.YES)
                            .addressSameAsPossession(VerticalYesNo.YES)
                            .build())

            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .tenancyLicenceDate(LocalDate.of(2025, 1, 1))
                                       .hasCopyOfTenancyLicence(VerticalYesNo.NO)
                                       .reasonsForNoTenancyLicenceDocuments("Copy of agreement was lost")
                                       .build())

            .claimDueToRentArrears(YesOrNo.YES)
            .assuredRentArrearsPossessionGrounds(AssuredRentArrearsPossessionGrounds.builder()
                                                     .rentArrearsGrounds(Set.of(
                                                         AssuredRentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8
                                                     ))
                                                     .build())
            .hasOtherAdditionalGrounds(YesOrNo.NO)

            .preActionProtocolCompleted(VerticalYesNo.YES)
            .mediationAttempted(VerticalYesNo.YES)
            .settlementAttempted(VerticalYesNo.YES)

            .noticeServed(YesOrNo.YES)
            .noticeServedDetails(NoticeServedDetails.builder()
                                     .serviceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                                     .personName("Dominic Defendant")
                                     .ableToUploadDocument(CanUploadNoticeServedDocument.No)
                                     .unableToUploadReason("Notice was served by hand, no digital copy available")
                                     .build())

            .rentDetails(RentDetails.builder()
                             .currentRent(BigDecimal.valueOf(2000))
                             .frequency(RentPaymentFrequency.MONTHLY)
                             .build())

            .rentArrears(RentArrearsSection.builder()
                             .total(BigDecimal.valueOf(2000))
                             .recoveryAttempted(VerticalYesNo.NO)
                             .build())

            .arrearsJudgmentWanted(VerticalYesNo.YES)

            .claimantCircumstances(ClaimantCircumstances.builder()
                                       .claimantCircumstancesSelect(VerticalYesNo.NO)
                                       .build())

            .defendantCircumstances(DefendantCircumstances.builder()
                                        .hasDefendantCircumstancesInfo(VerticalYesNo.NO)
                                        .build())

            .additionalReasonsForPossession(null) // journey answered "No" — left unset

            .hasUnderlesseeOrMortgagee(VerticalYesNo.NO)
            .wantToUploadDocuments(VerticalYesNo.NO)
            .applicationWithClaim(VerticalYesNo.NO)

            .languageUsed(LanguageUsed.ENGLISH)

            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)

            .statementOfTruth(StatementOfTruthDetails.builder()
                                  .completedBy(StatementOfTruthCompletedBy.CLAIMANT)
                                  .fullNameParty("Possession Claims Solicitor Org")
                                  .positionParty("Office position")
                                  .build())

            .build();

        CaseDetails caseDetails = ccdClient.createCase(createData, authorisation);
        return caseDetails.getId();
    }

}
