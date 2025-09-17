package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

@ExtendWith(MockitoExtension.class)
class IntroductoryDemotedOrOtherGroundsForPossessionTest extends BasePageTest {
    private Event<PCSCase, UserRole, State> event;

    private static Stream<Arguments> testGroundsOtherThanRentArrearsScenarios() {
        return Stream.of(
          arguments(Set.of(IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS)),
          arguments(Set.of(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL)),
          arguments(Set.of(IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY)),
          arguments(Set.of(IntroductoryDemotedOrOtherGrounds.OTHER)),
          arguments(
              Set.of(
                  IntroductoryDemotedOrOtherGrounds.RENT_ARREARS,
                  IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS)),
          arguments(
              Set.of(
                  IntroductoryDemotedOrOtherGrounds.RENT_ARREARS,
                  IntroductoryDemotedOrOtherGrounds.OTHER)));
    }

    private static Set<IntroductoryDemotedOrOtherGrounds> buildIntroductoryDemotedOrOtherGrounds() {
        return Set.of(
            IntroductoryDemotedOrOtherGrounds.RENT_ARREARS,
            IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS,
            IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL,
            IntroductoryDemotedOrOtherGrounds.BREACH_OF_THE_TENANCY,
            IntroductoryDemotedOrOtherGrounds.OTHER);
    }

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new IntroductoryDemotedOrOtherGroundsForPossession());
    }

    @Test
    void shouldNotShowReasonsPageIfNoGrounds() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();

        PCSCase caseData =
            PCSCase.builder().hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.NO).build();

        caseDetails.setData(caseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response =
            getMidEventForPage(event, "introductoryDemotedOrOtherGroundsForPossession")
              .handle(caseDetails, null);

        // Then
        assertThat(
            response.getData().getShowIntroductoryDemotedOtherGroundReasonPage()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldNotShowReasonsPageIfRentArrearsGround() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();

        PCSCase caseData =
            PCSCase.builder()
              .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
              .introductoryDemotedOrOtherGrounds(
                  Set.of(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS))
              .build();

        caseDetails.setData(caseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response =
            getMidEventForPage(event, "introductoryDemotedOrOtherGroundsForPossession")
              .handle(caseDetails, null);

        // Then
        assertThat(
            response.getData().getShowIntroductoryDemotedOtherGroundReasonPage()).isEqualTo(YesOrNo.NO);
    }

    @ParameterizedTest
    @MethodSource("testGroundsOtherThanRentArrearsScenarios")
    void shouldShowReasonsPageIfOtherGroundThanRentArrearsSelected(
        Set<IntroductoryDemotedOrOtherGrounds> grounds) {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();

        PCSCase caseData =
            PCSCase.builder()
              .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
              .introductoryDemotedOrOtherGrounds(grounds)
              .build();

        caseDetails.setData(caseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response =
            getMidEventForPage(event, "introductoryDemotedOrOtherGroundsForPossession")
              .handle(caseDetails, null);

        // Then
        assertThat(
            response.getData().getShowIntroductoryDemotedOtherGroundReasonPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldShowGroundsOptionsWhenGroundsForPossessionIsYes() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();

        PCSCase caseData =
            PCSCase.builder()
                .hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.YES)
                .introductoryDemotedOrOtherGrounds(
                    IntroductoryDemotedOrOtherGroundsForPossessionTest
                        .buildIntroductoryDemotedOrOtherGrounds())
                .build();

        caseDetails.setData(caseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response =
            getMidEventForPage(event, "introductoryDemotedOrOtherGroundsForPossession")
                .handle(caseDetails, null);

        // Then
        assertThat(response.getData().getIntroductoryDemotedOrOtherGrounds()).isEmpty();
    }

    @Test
    void shouldNotShowGroundsOptionsWhenGroundsForPossessionIsNo() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();

        PCSCase caseData =
            PCSCase.builder().hasIntroductoryDemotedOtherGroundsForPossession(VerticalYesNo.NO).build();

        caseDetails.setData(caseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response =
            getMidEventForPage(event, "introductoryDemotedOrOtherGroundsForPossession")
                .handle(caseDetails, null);

        // Then
        assertThat(response.getData().getIntroductoryDemotedOrOtherGrounds()).isEmpty();
    }
}
