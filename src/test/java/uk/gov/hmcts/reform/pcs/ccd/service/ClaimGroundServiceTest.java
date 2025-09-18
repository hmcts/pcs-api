package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.OTHER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds.RENT_ARREARS;

@ExtendWith(MockitoExtension.class)
class ClaimGroundServiceTest {

    @InjectMocks
    private ClaimGroundService claimGroundService;

    @ParameterizedTest
    @MethodSource("testGroundsOtherThanRentArrearsScenarios")
    void shouldHaveReasonIfGroundOtherThanRentArrears(
        Set<IntroductoryDemotedOrOtherGrounds> grounds) {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();

        PCSCase caseData =
            PCSCase.builder()
              .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
              .typeOfTenancyLicence(TenancyLicenceType.INTRODUCTORY_TENANCY)
              .introductoryDemotedOrOtherGrounds(grounds)
              .introductoryDemotedOtherGroundReason(getReasonForGround(grounds))
              .build();

        caseDetails.setData(caseData);

        // When
        List<ClaimGroundEntity> entities = claimGroundService.getGroundsWithReason(caseData);

        // Then
        assertThat(entities.get(0).getGroundReason()).isNotBlank();
    }

    @Test
    void shouldNotHaveReasonIfRentArrearsGround() {
        // Given
        Set<IntroductoryDemotedOrOtherGrounds> grounds = Set.of(RENT_ARREARS);

        PCSCase caseData =
                PCSCase.builder()
                        .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                        .typeOfTenancyLicence(TenancyLicenceType.INTRODUCTORY_TENANCY)
                        .introductoryDemotedOrOtherGrounds(grounds)
                        .introductoryDemotedOtherGroundReason(getReasonForGround(grounds))
                        .build();

        // When
        List<ClaimGroundEntity> entities = claimGroundService.getGroundsWithReason(caseData);

        // Then
        assertThat(entities.get(0).getGroundReason()).isBlank();
    }

    private static Stream<Arguments> testGroundsOtherThanRentArrearsScenarios() {
        return Stream.of(
            arguments(Set.of(ABSOLUTE_GROUNDS)),
            arguments(Set.of(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL)),
            arguments(Set.of(IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY)),
            arguments(Set.of(IntroductoryDemotedOrOtherGrounds.OTHER)));
    }

    private static IntroductoryDemotedOtherGroundReason getReasonForGround(
        Set<IntroductoryDemotedOrOtherGrounds> grounds) {
        IntroductoryDemotedOtherGroundReason reasonForGround = null;

        for (IntroductoryDemotedOrOtherGrounds ground : grounds) {
            if (ground.equals(ABSOLUTE_GROUNDS)) {
                reasonForGround =
                    IntroductoryDemotedOtherGroundReason.builder()
                        .absoluteGrounds("Absolute reason")
                        .build();
            } else if (ground.equals(ANTI_SOCIAL)) {
                reasonForGround =
                    IntroductoryDemotedOtherGroundReason.builder()
                        .antiSocialBehaviourGround("Antisocial behaviour reason")
                        .build();
            } else if (ground.equals(BREACH_OF_THE_TENANCY)) {
                reasonForGround =
                    IntroductoryDemotedOtherGroundReason.builder()
                        .breachOfTenancyGround("Breach of the tenancy reason")
                        .build();
            } else if (ground.equals(OTHER)) {
                reasonForGround =
                    IntroductoryDemotedOtherGroundReason.builder()
                        .otherGround("Other grounds reason")
                        .build();
            }
        }
        return reasonForGround;
    }
}
