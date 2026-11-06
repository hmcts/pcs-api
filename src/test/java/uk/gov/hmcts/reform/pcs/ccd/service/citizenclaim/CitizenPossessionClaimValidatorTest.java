package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimWhoIsClaiming;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaimFixtures;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CitizenPossessionClaimValidatorTest {

    private final CitizenPossessionClaimValidator underTest = new CitizenPossessionClaimValidator();

    @Test
    void shouldAcceptACompleteClaim() {
        assertThat(underTest.validateForSubmit(CitizenPossessionClaimFixtures.completeClaim())).isEmpty();
    }

    @Test
    void shouldReportEveryMissingRequiredAnswer() {
        assertThat(underTest.validateForSubmit(CitizenPossessionClaim.builder().build())).containsExactly(
            CitizenPossessionClaimValidator.CLAIMANT_TYPE_NOT_ELIGIBLE,
            "REQUIRED:claimant",
            "REQUIRED:defendants",
            "REQUIRED:tenancy.tenancyType",
            "REQUIRED:grounds",
            "REQUIRED:notice.noticeServed",
            "REQUIRED:otherInformation.languageUsed",
            "REQUIRED:submission.statementOfTruthAgreed"
        );
    }

    @Test
    void shouldRefuseIneligibleAnswers() {
        CitizenClaimEligibility eligibility = CitizenClaimEligibility.builder()
            .claimantType(ClaimantType.PROVIDER_OF_SOCIAL_HOUSING)
            .isTrespass(VerticalYesNo.YES)
            .whoIsClaiming(CitizenClaimWhoIsClaiming.SOLICITOR)
            .build();

        assertThat(underTest.validateEligibility(eligibility)).containsExactly(
            CitizenPossessionClaimValidator.CLAIMANT_TYPE_NOT_ELIGIBLE,
            CitizenPossessionClaimValidator.CLAIM_AGAINST_TRESPASSERS_NOT_ELIGIBLE,
            CitizenPossessionClaimValidator.WHO_IS_CLAIMING_NOT_ELIGIBLE
        );
    }

    @Test
    void shouldRefuseMultipleClaimants() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getClaimant().setMultipleClaimants(VerticalYesNo.YES);

        assertThat(underTest.validateForSubmit(claim))
            .containsExactly(CitizenPossessionClaimValidator.MULTIPLE_CLAIMANTS_NOT_ELIGIBLE);
    }

    @Test
    void shouldNeedTheArrearsForARentArrearsGround() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getRentArrears().setCurrentArrears(null);

        assertThat(underTest.validateForSubmit(claim)).containsExactly("REQUIRED:rentArrears.currentArrears");
    }

    @Test
    void shouldOnlyAllowArrearsGroundsInTheRentArrearsList() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setRentArrearsGrounds(Set.of(PrivateLandlordAssuredGround.GROUND_1A));

        assertThat(underTest.validateForSubmit(claim))
            .containsExactly(CitizenPossessionClaimValidator.RENT_ARREARS_GROUND_NOT_ALLOWED);
    }

    @Test
    void shouldNeedDetailsOfAnOtherTenancyType() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getTenancy().setTenancyType(CitizenClaimTenancyType.OTHER);

        assertThat(underTest.validateForSubmit(claim)).containsExactly("REQUIRED:tenancy.otherTenancyTypeDetails");
    }

    @Test
    void shouldRefuseTextLongerThanItsColumn() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getClaimant().setFirstName("x".repeat(61));

        assertThat(underTest.validateForSubmit(claim)).containsExactly("TOO_LONG:claimant.firstName");
    }

    @Test
    void shouldTreatGroundsWithoutAnySelectionAsMissing() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.setGrounds(null);

        assertThat(underTest.validateForSubmit(claim)).containsExactly("REQUIRED:grounds");
    }

    @Test
    void shouldTreatGroundSetsHoldingOnlyNullsAsMissing() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setRentArrearsGrounds(setOf((PrivateLandlordAssuredGround) null));
        claim.getGrounds().setOtherGrounds(setOf((PrivateLandlordAssuredGround) null));

        assertThat(underTest.validateForSubmit(claim)).containsExactly("REQUIRED:grounds");
    }

    @Test
    void shouldRefuseANullRentArrearsGroundAlongsideRealOnes() throws Exception {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.draftObjectMapper().readValue(
            "{\"grounds\":{\"rentArrearsGrounds\":[null,\"GROUND_8\"]}}", CitizenPossessionClaim.class);
        CitizenPossessionClaim complete = CitizenPossessionClaimFixtures.completeClaim();
        complete.getGrounds().setRentArrearsGrounds(claim.getGrounds().getRentArrearsGrounds());

        assertThat(complete.getGrounds().getRentArrearsGrounds()).containsNull();
        assertThat(underTest.validateForSubmit(complete))
            .containsExactly(CitizenPossessionClaimValidator.RENT_ARREARS_GROUND_NOT_ALLOWED);
    }

    @Test
    void shouldDropNullGroundsWhenCollectingThem() {
        CitizenClaimGrounds grounds = CitizenClaimGrounds.builder()
            .rentArrearsGrounds(setOf(null, PrivateLandlordAssuredGround.GROUND_8))
            .otherGrounds(setOf(PrivateLandlordAssuredGround.GROUND_1, null))
            .build();

        assertThat(CitizenPossessionClaimValidator.allGrounds(grounds))
            .containsExactly(PrivateLandlordAssuredGround.GROUND_8, PrivateLandlordAssuredGround.GROUND_1);
        assertThat(CitizenPossessionClaimValidator.allGrounds(null)).isEmpty();
    }

    @Test
    void shouldAllowExactlyTheRentArrearsGroundsInTheRentArrearsList() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setRentArrearsGrounds(Arrays.stream(PrivateLandlordAssuredGround.values())
                                                     .filter(PrivateLandlordAssuredGround::isRentArrears)
                                                     .collect(Collectors.toSet()));

        assertThat(claim.getGrounds().getRentArrearsGrounds()).containsExactlyInAnyOrder(
            PrivateLandlordAssuredGround.GROUND_8, PrivateLandlordAssuredGround.GROUND_10,
            PrivateLandlordAssuredGround.GROUND_11);
        assertThat(underTest.validateForSubmit(claim)).isEmpty();
    }

    @Test
    void shouldNeedDetailsForTheOtherGround() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setOtherGrounds(Set.of(PrivateLandlordAssuredGround.OTHER));

        assertThat(underTest.validateForSubmit(claim)).containsExactly("REQUIRED:grounds.otherGroundsDetails");

        claim.getGrounds().setOtherGroundsDetails("The tenant sublet without consent.");
        assertThat(underTest.validateForSubmit(claim)).isEmpty();
    }

    @Test
    void shouldNeedAReasonForEveryGroundThatIsNotARentArrearsGround() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setGroundReasons(null);

        assertThat(underTest.validateForSubmit(claim)).containsExactlyInAnyOrder(
            "REQUIRED:grounds.groundReasons.GROUND_1A",
            "REQUIRED:grounds.groundReasons.GROUND_14");
    }

    private static Set<PrivateLandlordAssuredGround> setOf(PrivateLandlordAssuredGround... grounds) {
        return new LinkedHashSet<>(Arrays.asList(grounds));
    }

}
