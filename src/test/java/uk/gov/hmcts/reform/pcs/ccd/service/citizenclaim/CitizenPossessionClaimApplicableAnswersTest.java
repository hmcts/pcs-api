package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNotice;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentArrears;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentPeriodEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentProofType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaimFixtures;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CitizenPossessionClaimApplicableAnswersTest {

    @Test
    void shouldKeepEverythingInTheSelfConsistentFixture() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();

        assertThat(CitizenPossessionClaimApplicableAnswers.of(claim)).isEqualTo(claim);
    }

    @Test
    void shouldNotChangeTheOriginalOrShareItsObjects() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setHasOtherGrounds(VerticalYesNo.NO);

        CitizenPossessionClaim applicable = CitizenPossessionClaimApplicableAnswers.of(claim);
        applicable.getTenancy().getTenancyAgreementDocuments().getFirst().getValue().setFilename("renamed.pdf");

        assertThat(claim.getGrounds().getOtherGrounds()).isNotEmpty();
        assertThat(claim.getTenancy().getTenancyAgreementDocuments().getFirst().getValue().getFilename())
            .isEqualTo("tenancy.pdf");
    }

    @Test
    void shouldIgnoreRentArrearsGroundsWhenTheCitizenSaidNoToThem() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setHasRentArrearsGrounds(VerticalYesNo.NO);

        CitizenPossessionClaim applicable = CitizenPossessionClaimApplicableAnswers.of(claim);

        assertThat(applicable.getGrounds().getRentArrearsGrounds()).isNull();
        assertThat(applicable.getRentArrears()).isNull();
    }

    @Test
    void shouldIgnoreOtherGroundsAndTheirReasonsWhenTheCitizenSaidNoToThem() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setHasOtherGrounds(VerticalYesNo.NO);

        CitizenPossessionClaim applicable = CitizenPossessionClaimApplicableAnswers.of(claim);

        assertThat(applicable.getGrounds().getOtherGrounds()).isNull();
        assertThat(applicable.getGrounds().getGroundReasons()).isEmpty();
        assertThat(applicable.getGrounds().getRentArrearsGrounds())
            .containsExactlyInAnyOrder(PrivateLandlordAssuredGround.GROUND_8, PrivateLandlordAssuredGround.GROUND_10);
    }

    @Test
    void shouldKeepOnlyTheRentEvidenceTheProofTypeNames() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        CitizenClaimRentArrears rentArrears = claim.getRentArrears();
        rentArrears.setRentStatementDocuments(claim.getTenancy().getTenancyAgreementDocuments());
        rentArrears.setRentHistoryTable(List.of(ListValue.<CitizenClaimRentPeriodEntry>builder()
                                                    .value(CitizenClaimRentPeriodEntry.builder().build())
                                                    .build()));

        rentArrears.setProofType(CitizenClaimRentProofType.LEDGER);
        CitizenClaimRentArrears ledger = CitizenPossessionClaimApplicableAnswers.of(claim).getRentArrears();
        assertThat(ledger.getRentHistoryLedger()).hasSize(2);
        assertThat(ledger.getRentHistoryTable()).isNull();
        assertThat(ledger.getRentStatementDocuments()).isNull();

        rentArrears.setProofType(CitizenClaimRentProofType.TABLE);
        CitizenClaimRentArrears table = CitizenPossessionClaimApplicableAnswers.of(claim).getRentArrears();
        assertThat(table.getRentHistoryTable()).hasSize(1);
        assertThat(table.getRentHistoryLedger()).isNull();
        assertThat(table.getRentStatementDocuments()).isNull();

        rentArrears.setProofType(CitizenClaimRentProofType.UPLOAD_STATEMENT);
        CitizenClaimRentArrears upload = CitizenPossessionClaimApplicableAnswers.of(claim).getRentArrears();
        assertThat(upload.getRentStatementDocuments()).hasSize(1);
        assertThat(upload.getRentHistoryLedger()).isNull();
        assertThat(upload.getRentHistoryTable()).isNull();
    }

    @Test
    void shouldIgnoreOtherFrequencyDetailsUnlessTheFrequencyIsOther() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getRentArrears().setOtherFrequencyDetails("Every 3 weeks");

        assertThat(CitizenPossessionClaimApplicableAnswers.of(claim).getRentArrears().getOtherFrequencyDetails())
            .isNull();

        claim.getRentArrears().setRentFrequency(RentPaymentFrequency.OTHER);
        assertThat(CitizenPossessionClaimApplicableAnswers.of(claim).getRentArrears().getOtherFrequencyDetails())
            .isEqualTo("Every 3 weeks");
    }

    @Test
    void shouldIgnoreNoticeDetailsUnlessNoticeWasServed() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getNotice().setNoticeServed(VerticalYesNo.NO);

        assertThat(CitizenPossessionClaimApplicableAnswers.of(claim).getNotice())
            .isEqualTo(CitizenClaimNotice.builder().noticeServed(VerticalYesNo.NO).build());
    }

    @Test
    void shouldIgnoreTheDetailsOfNoticeMethodsThatWereNotChosen() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getNotice().setNoticeMethods(Set.of(CitizenClaimNoticeMethod.FIRST_CLASS_POST));

        CitizenClaimNotice notice = CitizenPossessionClaimApplicableAnswers.of(claim).getNotice();

        assertThat(notice.getPostedDate()).isNotNull();
        assertThat(notice.getEmailedTo()).isNull();
        assertThat(notice.getEmailedDate()).isNull();
    }

    @Test
    void shouldIgnoreTheUnderlesseeUnlessOneExists() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getCircumstances().setUnderlesseeExists(VerticalYesNo.NO);

        CitizenPossessionClaim applicable = CitizenPossessionClaimApplicableAnswers.of(claim);

        assertThat(applicable.getCircumstances().getUnderlesseeName()).isNull();
        assertThat(applicable.getCircumstances().getUnderlesseeAddress()).isNull();
    }

    @Test
    void shouldKeepOnlyTheFirstDefendantUnlessAdditionalDefendantsIsYes() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.setAdditionalDefendants(VerticalYesNo.NO);

        CitizenPossessionClaim applicable = CitizenPossessionClaimApplicableAnswers.of(claim);

        assertThat(applicable.getDefendants()).singleElement()
            .satisfies(defendant -> assertThat(defendant.getValue().getFirstName()).isEqualTo("Tom"));
    }

}
