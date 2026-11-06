package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimDefendant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeType;
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
    void shouldTreatANullRentArrearsGroundAsAMissingAnswer() throws Exception {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.draftObjectMapper().readValue(
            "{\"grounds\":{\"rentArrearsGrounds\":[null,\"GROUND_8\"]}}", CitizenPossessionClaim.class);
        CitizenPossessionClaim complete = CitizenPossessionClaimFixtures.completeClaim();
        complete.getGrounds().setRentArrearsGrounds(claim.getGrounds().getRentArrearsGrounds());

        assertThat(complete.getGrounds().getRentArrearsGrounds()).containsNull();
        assertThat(underTest.validateForSubmit(complete)).containsExactly("REQUIRED:grounds.rentArrearsGrounds");
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

    @Test
    void shouldTreatANullOtherGroundAsAMissingAnswer() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setOtherGrounds(setOf(PrivateLandlordAssuredGround.GROUND_1A, null));

        assertThat(underTest.validateForSubmit(claim)).containsExactly("REQUIRED:grounds.otherGrounds");
    }

    @Test
    void shouldRefuseRentArrearsGroundsInTheOtherGroundsList() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getGrounds().setOtherGrounds(Set.of(PrivateLandlordAssuredGround.GROUND_1A,
                                                  PrivateLandlordAssuredGround.GROUND_11));

        assertThat(underTest.validateForSubmit(claim))
            .containsExactly(CitizenPossessionClaimValidator.OTHER_GROUND_NOT_ALLOWED);
    }

    @Test
    void shouldNeedTheDescriptionOfAnOtherNoticeType() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getNotice().setNoticeType(CitizenClaimNoticeType.OTHER);

        assertThat(underTest.validateForSubmit(claim)).containsExactly("REQUIRED:notice.noticeTypeOther");

        claim.getNotice().setNoticeTypeOther("Section 8 notice (old form)");
        assertThat(underTest.validateForSubmit(claim)).isEmpty();
    }

    @Test
    void shouldRefuseRentArrearsTextLongerThanItsColumn() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getRentArrears().setRecoverySteps("x".repeat(501));
        claim.getRentArrears().setOtherFrequencyDetails("x".repeat(61));
        claim.getRentArrears().getRentHistoryLedger().get(1).getValue().setDescription("x".repeat(501));

        assertThat(underTest.validateForSubmit(claim)).containsExactly(
            "TOO_LONG:rentArrears.otherFrequencyDetails",
            "TOO_LONG:rentArrears.recoverySteps",
            "TOO_LONG:rentArrears.rentHistoryLedger[1].description");
    }

    @Test
    void shouldRefuseAddressesLongerThanTheirColumns() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getClaimant().getAddress().setAddressLine1("x".repeat(101));
        claim.getDefendants().get(1).getValue().getAddress().setPostCode("x".repeat(15));
        claim.getCircumstances().getUnderlesseeAddress().setPostTown("x".repeat(101));

        assertThat(underTest.validateForSubmit(claim)).containsExactly(
            "TOO_LONG:claimant.address.AddressLine1",
            "TOO_LONG:defendants[1].address.PostCode",
            "TOO_LONG:circumstances.underlesseeAddress.PostTown");
    }

    @Test
    void shouldAcceptEmailAddressesUpToTheRfcLimit() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getClaimant().setEmailAddress(emailOfLength(254));
        claim.getNotice().setEmailedTo(emailOfLength(254));

        assertThat(underTest.validateForSubmit(claim)).isEmpty();
    }

    @Test
    void shouldRefuseEmailAddressesOverTheRfcLimit() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getClaimant().setEmailAddress(emailOfLength(255));
        claim.getNotice().setEmailedTo(emailOfLength(255));

        assertThat(underTest.validateForSubmit(claim))
            .containsExactly("TOO_LONG:claimant.emailAddress", "TOO_LONG:notice.emailedTo");
    }

    @Test
    void shouldNeedEachDefendantsNameAndAddressAnswers() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        CitizenClaimDefendant first = claim.getDefendants().get(0).getValue();
        first.setNameKnown(null);
        first.setAddressKnown(null);
        CitizenClaimDefendant second = claim.getDefendants().get(1).getValue();
        second.setNameKnown(VerticalYesNo.YES);
        second.setAddressSameAsProperty(null);

        assertThat(underTest.validateForSubmit(claim)).containsExactly(
            "REQUIRED:defendants[0].nameKnown",
            "REQUIRED:defendants[0].addressKnown",
            "REQUIRED:defendants[1].firstName",
            "REQUIRED:defendants[1].lastName",
            "REQUIRED:defendants[1].addressSameAsProperty");
    }

    @Test
    void shouldNeedADefendantsAddressWhenItIsNotTheProperty() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getDefendants().get(1).getValue().setAddress(null);

        assertThat(underTest.validateForSubmit(claim)).containsExactly("REQUIRED:defendants[1].address");
    }

    @Test
    void shouldNeedTheDetailsOfAServedNotice() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getNotice().setNoticeType(null);
        claim.getNotice().setNoticeDate(null);
        claim.getNotice().setNoticeMethods(Set.of(CitizenClaimNoticeMethod.PERSONALLY_HANDED,
                                                  CitizenClaimNoticeMethod.OTHER));

        assertThat(underTest.validateForSubmit(claim)).containsExactly(
            "REQUIRED:notice.noticeType",
            "REQUIRED:notice.noticeDate",
            "REQUIRED:notice.handedToName",
            "REQUIRED:notice.handedDate",
            "REQUIRED:notice.otherMethodDetails",
            "REQUIRED:notice.otherMethodDate");
    }

    @Test
    void shouldNeedAtLeastOneNoticeMethodAndTheDatesOfEachChosenOne() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getNotice().setNoticeMethods(null);
        assertThat(underTest.validateForSubmit(claim)).containsExactly("REQUIRED:notice.noticeMethods");

        claim.getNotice().setNoticeMethods(Set.of(CitizenClaimNoticeMethod.EMAIL));
        claim.getNotice().setEmailedDate(null);
        assertThat(underTest.validateForSubmit(claim)).containsExactly("REQUIRED:notice.emailedDate");
    }

    @Test
    void shouldNotNeedNoticeDetailsWhenNoticeWasNotServed() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getNotice().setNoticeServed(VerticalYesNo.NO);
        claim.getNotice().setNoticeType(null);
        claim.getNotice().setNoticeMethods(null);

        assertThat(underTest.validateForSubmit(claim)).isEmpty();
    }

    @Test
    void shouldNeedTheStatementOfTruthName() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getSubmission().setStatementOfTruthFullName(" ");

        assertThat(underTest.validateForSubmit(claim)).containsExactly("REQUIRED:submission.statementOfTruthFullName");

        claim.getSubmission().setStatementOfTruthFullName("x".repeat(101));
        assertThat(underTest.validateForSubmit(claim)).containsExactly("TOO_LONG:submission.statementOfTruthFullName");
    }

    @Test
    void shouldRefuseAnIndividualNameThatWouldNotFitTheClaimantNameColumn() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.getClaimant().setFirstName("x".repeat(30));
        claim.getClaimant().setLastName("y".repeat(29));
        assertThat(underTest.validateForSubmit(claim)).isEmpty();

        claim.getClaimant().setLastName("y".repeat(30));
        assertThat(underTest.validateForSubmit(claim)).containsExactly("TOO_LONG:claimant.name");
    }

    private static String emailOfLength(int length) {
        String domain = "@test.test";
        return "x".repeat(length - domain.length()) + domain;
    }

    private static Set<PrivateLandlordAssuredGround> setOf(PrivateLandlordAssuredGround... grounds) {
        return new LinkedHashSet<>(Arrays.asList(grounds));
    }

}
