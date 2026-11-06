package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimWhoIsClaiming;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaimFixtures;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;

import java.util.Set;

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

}
