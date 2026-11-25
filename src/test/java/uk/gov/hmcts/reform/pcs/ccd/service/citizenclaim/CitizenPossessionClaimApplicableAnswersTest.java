package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.CitizenClaimRentProofType;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimAdditionalClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNotice;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentArrears;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentPeriodEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimantKind;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaimFixtures;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

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
    void shouldKeepOnlyTheOccupationContractForAPropertyInWales() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getEligibility().setLegislativeCountry(LegislativeCountry.WALES);
        claim.getTenancy().setOtherTenancyTypeDetails("Company let");
        claim.getTenancy().setOccupationContractType(OccupationLicenceTypeWales.SECURE_CONTRACT);
        claim.getTenancy().setOtherOccupationContractDetails("Left from Other");

        CitizenClaimTenancy tenancy = underTest.of(claim).getTenancy();

        assertThat(tenancy.getTenancyType()).isNull();
        assertThat(tenancy.getOtherTenancyTypeDetails()).isNull();
        assertThat(tenancy.getOccupationContractType()).isEqualTo(OccupationLicenceTypeWales.SECURE_CONTRACT);
        assertThat(tenancy.getOtherOccupationContractDetails()).isNull();
        assertThat(tenancy.getTenancyStartDate()).isEqualTo(claim.getTenancy().getTenancyStartDate());
        assertThat(tenancy.getTenancyAgreementDocuments()).hasSize(1);
    }

    @Test
    void shouldKeepAdditionalClaimantsOnlyWhenThereAreOtherClaimants() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getClaimant().setMultipleClaimants(VerticalYesNo.YES);
        claim.setAdditionalClaimants(List.of(ListValue.<CitizenClaimAdditionalClaimant>builder().id("1").value(
            CitizenClaimAdditionalClaimant.builder()
                .individualOrOrganisation(CitizenClaimantKind.ORGANISATION)
                .firstName("Left").lastName("Over").organisationName("Joint Lettings Ltd")
                .contactByPhone(VerticalYesNo.NO).phoneNumber("07700900000")
                .contactByText(VerticalYesNo.NO).textMessageNumber("07700900001")
                .build()).build()));

        CitizenClaimAdditionalClaimant kept = underTest.of(claim).getAdditionalClaimants().getFirst().getValue();

        assertThat(kept.getOrganisationName()).isEqualTo("Joint Lettings Ltd");
        assertThat(kept.getFirstName()).isNull();
        assertThat(kept.getLastName()).isNull();
        assertThat(kept.getPhoneNumber()).isNull();
        assertThat(kept.getTextMessageNumber()).isNull();

        claim.getClaimant().setMultipleClaimants(VerticalYesNo.NO);
        assertThat(underTest.of(claim).getAdditionalClaimants()).isNull();
    }

    @Test
    void shouldAskOnlyTheExcludedGroundsForAnExcludedTenancy() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getTenancy().setTenancyType(CitizenClaimTenancyType.EXCLUDED);
        claim.getGrounds().setHasExcludedGrounds(VerticalYesNo.YES);
        claim.getGrounds().setExcludedGrounds(Set.of(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL));
        claim.getGrounds().setExcludedOtherGroundDetails("Left from Other");
        claim.getGrounds().setExcludedReason("Noise");

        CitizenPossessionClaim applicable = underTest.of(claim);

        assertThat(applicable.getGrounds().getRentArrearsGrounds()).isNull();
        assertThat(applicable.getGrounds().getOtherGrounds()).isNull();
        assertThat(applicable.getGrounds().getGroundReasons()).isNull();
        assertThat(applicable.getGrounds().getExcludedOtherGroundDetails()).isNull();
        assertThat(applicable.getGrounds().getExcludedReason()).isEqualTo("Noise");
        assertThat(applicable.hasRentArrearsGround()).isFalse();
        assertThat(applicable.getRentArrears()).isNull();
    }

    @Test
    void shouldKeepTheArrearsForAnExcludedRentArrearsGround() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getTenancy().setTenancyType(CitizenClaimTenancyType.EXCLUDED);
        claim.getGrounds().setHasExcludedGrounds(VerticalYesNo.YES);
        claim.getGrounds().setExcludedGrounds(Set.of(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS));

        CitizenPossessionClaim applicable = underTest.of(claim);

        assertThat(applicable.hasRentArrearsGround()).isTrue();
        assertThat(applicable.getRentArrears()).isNotNull();
    }

    @Test
    void shouldDropTheExcludedGroundsForOtherTenancyTypes() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setHasExcludedGrounds(VerticalYesNo.YES);
        claim.getGrounds().setExcludedGrounds(Set.of(IntroductoryDemotedOrOtherGrounds.OTHER));
        claim.getGrounds().setExcludedReason("Stale");

        CitizenClaimGrounds grounds = underTest.of(claim).getGrounds();

        assertThat(grounds.getHasExcludedGrounds()).isNull();
        assertThat(grounds.getExcludedGrounds()).isNull();
        assertThat(grounds.getExcludedReason()).isNull();
        assertThat(grounds.getRentArrearsGrounds()).isNotEmpty();
    }

    @Test
    void shouldKeepOnlyTheWalesGroundsForAPropertyInWales() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeWalesClaim();
        claim.getGrounds().setRentArrearsGrounds(Set.of(PrivateLandlordAssuredGround.GROUND_8));
        claim.getGrounds().setHasOtherGrounds(VerticalYesNo.YES);
        claim.getGrounds().setOtherGrounds(Set.of(PrivateLandlordAssuredGround.GROUND_1A,
                                                  PrivateLandlordAssuredGround.OTHER));

        CitizenClaimGrounds grounds = underTest.of(claim).getGrounds();

        assertThat(grounds.getRentArrearsGrounds()).isNull();
        assertThat(grounds.getHasOtherGrounds()).isNull();
        assertThat(grounds.getOtherGrounds()).containsExactly(PrivateLandlordAssuredGround.OTHER);
        assertThat(grounds.getWalesDiscretionaryGrounds()).hasSize(2);
        assertThat(grounds.getWalesGroundReasons()).hasSize(2);
        assertThat(underTest.of(claim).hasRentArrearsGround()).isTrue();
    }

    @Test
    void shouldDropTheWalesGroundsAndDocumentsForAPropertyInEngland() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setWalesDiscretionaryGrounds(Set.of(DiscretionaryGroundWales.RENT_ARREARS_S157));
        claim.getGrounds().setWalesMandatoryGrounds(Set.of(MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199));
        claim.getTenancy().setHasEnergyPerformanceCertificate(VerticalYesNo.NO);
        claim.getTenancy().setNoEnergyPerformanceCertificateReason("Not asked");

        CitizenPossessionClaim applicable = underTest.of(claim);

        assertThat(applicable.getGrounds().getWalesDiscretionaryGrounds()).isNull();
        assertThat(applicable.getGrounds().getWalesMandatoryGrounds()).isNull();
        assertThat(applicable.getTenancy().getHasEnergyPerformanceCertificate()).isNull();
        assertThat(applicable.getTenancy().getNoEnergyPerformanceCertificateReason()).isNull();
    }

    @Test
    void shouldKeepAReasonOrADocumentForEachRequiredDocumentAnswer() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeWalesClaim();
        claim.getTenancy().setNoEnergyPerformanceCertificateReason("Left from Yes");
        claim.getTenancy().setGasSafetyReportDocuments(claim.getTenancy().getEnergyPerformanceDocuments());

        CitizenClaimTenancy tenancy = underTest.of(claim).getTenancy();

        assertThat(tenancy.getNoEnergyPerformanceCertificateReason()).isNull();
        assertThat(tenancy.getEnergyPerformanceDocuments()).hasSize(1);
        assertThat(tenancy.getGasSafetyReportDocuments()).isNull();
        assertThat(tenancy.getNoGasSafetyReportReason()).isEqualTo("The property has no gas supply");
    }

    @Test
    void shouldDropAnOccupationContractForAPropertyInEngland() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getTenancy().setOccupationContractType(OccupationLicenceTypeWales.OTHER);
        claim.getTenancy().setOtherOccupationContractDetails("Not asked");

        CitizenClaimTenancy tenancy = underTest.of(claim).getTenancy();

        assertThat(tenancy.getOccupationContractType()).isNull();
        assertThat(tenancy.getOtherOccupationContractDetails()).isNull();
        assertThat(tenancy.getTenancyType()).isEqualTo(CitizenClaimTenancyType.ASSURED);
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

        rentArrears.setProofType(CitizenClaimRentProofType.TABLE);
        CitizenClaimRentArrears table = underTest.of(claim).getRentArrears();
        assertThat(table.getRentHistoryTable()).hasSize(1);
        assertThat(table.getRentStatementDocuments()).isNull();

        rentArrears.setProofType(CitizenClaimRentProofType.UPLOAD_STATEMENT);
        CitizenClaimRentArrears upload = underTest.of(claim).getRentArrears();
        assertThat(upload.getRentStatementDocuments()).hasSize(1);
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
        assertThat(applicable.getClaimant().effectiveTextNumber()).isNull();
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

    @Test
    void shouldKeepTheRentDueDayOnlyForACreatedStatementOfMonthlyRent() {
        CitizenPossessionClaim table = CitizenPossessionClaimFixtures.completeClaim();
        table.getRentArrears().setProofType(CitizenClaimRentProofType.TABLE);
        table.getRentArrears().setRentDueDay(15);
        CitizenPossessionClaim upload = table.toBuilder().build();
        upload.setRentArrears(table.getRentArrears().toBuilder()
                                  .proofType(CitizenClaimRentProofType.UPLOAD_STATEMENT).build());
        CitizenPossessionClaim weekly = table.toBuilder().build();
        weekly.setRentArrears(table.getRentArrears().toBuilder().rentFrequency(RentPaymentFrequency.WEEKLY).build());

        assertThat(underTest.of(table).getRentArrears().getRentDueDay()).isEqualTo(15);
        assertThat(underTest.of(upload).getRentArrears().getRentDueDay()).isNull();
        assertThat(underTest.of(weekly).getRentArrears().getRentDueDay()).isNull();
    }

    @Test
    void shouldIgnoreTheArrearsAtTheNoticeDateWhenNoNoticeWasServed() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getNotice().setNoticeServed(VerticalYesNo.NO);

        CitizenPossessionClaim applicable = underTest.of(claim);

        assertThat(applicable.getRentArrears().getArrearsAtNoticeDate()).isNull();
        assertThat(applicable.getRentArrears().getCurrentArrears()).isNotNull();
    }

}
