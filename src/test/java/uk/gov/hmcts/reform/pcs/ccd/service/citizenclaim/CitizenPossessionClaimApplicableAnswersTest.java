package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNotice;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentArrears;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentPeriodEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentProofType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaimFixtures;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CitizenPossessionClaimApplicableAnswersTest {

    private final CitizenPossessionClaimApplicableAnswers underTest =
        new CitizenPossessionClaimApplicableAnswers(CitizenPossessionClaimFixtures.draftObjectMapper());

    @Test
    void shouldKeepEverythingInTheSelfConsistentFixture() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();

        assertThat(underTest.of(claim)).isEqualTo(claim);
    }

    @Test
    void shouldNotChangeTheOriginalOrShareItsObjects() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setHasOtherGrounds(VerticalYesNo.NO);

        CitizenPossessionClaim applicable = underTest.of(claim);
        applicable.getTenancy().getTenancyAgreementDocuments().getFirst().getValue().setFilename("renamed.pdf");

        assertThat(claim.getGrounds().getOtherGrounds()).isNotEmpty();
        assertThat(claim.getTenancy().getTenancyAgreementDocuments().getFirst().getValue().getFilename())
            .isEqualTo("tenancy.pdf");
    }

    @Test
    void shouldLeaveOutTheConfirmedSectionsAsTheyAreJourneyStateNotAnAnswer() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.setConfirmedSections(Set.of(CitizenClaimSection.CLAIMANT));

        assertThat(underTest.of(claim).getConfirmedSections()).isNull();
        assertThat(claim.getConfirmedSections()).containsExactly(CitizenClaimSection.CLAIMANT);
    }

    @Test
    void shouldIgnoreRentArrearsGroundsWhenTheCitizenSaidNoToThem() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setHasRentArrearsGrounds(VerticalYesNo.NO);

        CitizenPossessionClaim applicable = underTest.of(claim);

        assertThat(applicable.getGrounds().getRentArrearsGrounds()).isNull();
        assertThat(applicable.getRentArrears()).isNull();
    }

    @Test
    void shouldIgnoreOtherGroundsAndTheirReasonsWhenTheCitizenSaidNoToThem() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setHasOtherGrounds(VerticalYesNo.NO);

        CitizenPossessionClaim applicable = underTest.of(claim);

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
        CitizenClaimRentArrears ledger = underTest.of(claim).getRentArrears();
        assertThat(ledger.getRentHistoryLedger()).hasSize(2);
        assertThat(ledger.getRentHistoryTable()).isNull();
        assertThat(ledger.getRentStatementDocuments()).isNull();

        rentArrears.setProofType(CitizenClaimRentProofType.TABLE);
        CitizenClaimRentArrears table = underTest.of(claim).getRentArrears();
        assertThat(table.getRentHistoryTable()).hasSize(1);
        assertThat(table.getRentHistoryLedger()).isNull();
        assertThat(table.getRentStatementDocuments()).isNull();

        rentArrears.setProofType(CitizenClaimRentProofType.UPLOAD_STATEMENT);
        CitizenClaimRentArrears upload = underTest.of(claim).getRentArrears();
        assertThat(upload.getRentStatementDocuments()).hasSize(1);
        assertThat(upload.getRentHistoryLedger()).isNull();
        assertThat(upload.getRentHistoryTable()).isNull();
    }

    @Test
    void shouldIgnoreOtherFrequencyDetailsUnlessTheFrequencyIsOther() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getRentArrears().setOtherFrequencyDetails("Every 3 weeks");

        assertThat(underTest.of(claim).getRentArrears().getOtherFrequencyDetails())
            .isNull();

        claim.getRentArrears().setRentFrequency(RentPaymentFrequency.OTHER);
        assertThat(underTest.of(claim).getRentArrears().getOtherFrequencyDetails())
            .isEqualTo("Every 3 weeks");
    }

    @Test
    void shouldIgnoreNoticeDetailsUnlessNoticeWasServed() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getNotice().setNoticeServed(VerticalYesNo.NO);

        assertThat(underTest.of(claim).getNotice())
            .isEqualTo(CitizenClaimNotice.builder().noticeServed(VerticalYesNo.NO).build());
    }

    @Test
    void shouldIgnoreTheDetailsOfNoticeMethodsThatWereNotChosen() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getNotice().setNoticeMethods(Set.of(CitizenClaimNoticeMethod.FIRST_CLASS_POST));

        CitizenClaimNotice notice = underTest.of(claim).getNotice();

        assertThat(notice.getPostedDate()).isNotNull();
        assertThat(notice.getEmailedTo()).isNull();
        assertThat(notice.getEmailedDate()).isNull();
    }

    @Test
    void shouldIgnoreTheUnderlesseeUnlessOneExists() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getCircumstances().setUnderlesseeExists(VerticalYesNo.NO);

        CitizenPossessionClaim applicable = underTest.of(claim);

        assertThat(applicable.getCircumstances().getUnderlesseeName()).isNull();
        assertThat(applicable.getCircumstances().getUnderlesseeAddress()).isNull();
    }

    @Test
    void shouldKeepOnlyTheFirstDefendantUnlessAdditionalDefendantsIsYes() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.setAdditionalDefendants(VerticalYesNo.NO);

        CitizenPossessionClaim applicable = underTest.of(claim);

        assertThat(applicable.getDefendants()).singleElement()
            .satisfies(defendant -> assertThat(defendant.getValue().getFirstName()).isEqualTo("Tom"));
    }

    @Test
    void shouldKeepReasonsOnlyForGroundsThatAreAskedForOne() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        List<ListValue<CitizenClaimGroundReason>> reasons = new ArrayList<>(claim.getGrounds().getGroundReasons());
        reasons.add(reason(PrivateLandlordAssuredGround.GROUND_8, "x".repeat(600)));
        reasons.add(reason(PrivateLandlordAssuredGround.OTHER, "not asked"));
        reasons.add(null);
        claim.getGrounds().setGroundReasons(reasons);
        claim.getGrounds().setOtherGrounds(Set.of(PrivateLandlordAssuredGround.GROUND_1A,
                                                  PrivateLandlordAssuredGround.GROUND_14,
                                                  PrivateLandlordAssuredGround.OTHER));

        assertThat(underTest.of(claim).getGrounds().getGroundReasons())
            .extracting(item -> item.getValue().getGround())
            .containsExactly(PrivateLandlordAssuredGround.GROUND_1A, PrivateLandlordAssuredGround.GROUND_14);
    }

    @Test
    void shouldDropALeftoverPhoneNumberWhenTheClaimantSaidNoToCalls() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getClaimant().setContactByPhone(VerticalYesNo.NO);
        claim.getClaimant().setContactByText(VerticalYesNo.YES);
        claim.getClaimant().setTextMessageNumber(null);

        CitizenPossessionClaim applicable = underTest.of(claim);

        assertThat(applicable.getClaimant().getPhoneNumber()).isNull();
        assertThat(CitizenPossessionClaimApplicableAnswers.effectiveTextNumber(applicable.getClaimant())).isNull();
    }

    @Test
    void shouldTextThePhoneNumberOnlyWhenTheClaimantAgreedToCalls() {
        CitizenClaimClaimant claimant = CitizenClaimClaimant.builder()
            .contactByPhone(VerticalYesNo.YES)
            .phoneNumber("07700900000")
            .contactByText(VerticalYesNo.YES)
            .build();
        assertThat(CitizenPossessionClaimApplicableAnswers.effectiveTextNumber(claimant)).isEqualTo("07700900000");

        claimant.setTextMessageNumber("07700900111");
        assertThat(CitizenPossessionClaimApplicableAnswers.effectiveTextNumber(claimant)).isEqualTo("07700900111");

        claimant.setTextMessageNumber(" ");
        claimant.setContactByPhone(VerticalYesNo.NO);
        assertThat(CitizenPossessionClaimApplicableAnswers.effectiveTextNumber(claimant)).isNull();
    }

    @Test
    void shouldDropThePhoneNumberWhenNeitherCallsNorTextsUseIt() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getClaimant().setContactByPhone(VerticalYesNo.NO);
        claim.getClaimant().setContactByText(VerticalYesNo.YES);
        claim.getClaimant().setTextMessageNumber("07700900111");

        CitizenPossessionClaim applicable = underTest.of(claim);

        assertThat(applicable.getClaimant().getPhoneNumber()).isNull();
        assertThat(applicable.getClaimant().getTextMessageNumber()).isEqualTo("07700900111");
    }

    private static ListValue<CitizenClaimGroundReason> reason(PrivateLandlordAssuredGround ground, String reason) {
        return ListValue.<CitizenClaimGroundReason>builder()
            .value(CitizenClaimGroundReason.builder().ground(ground).reason(reason).build())
            .build();
    }

}
