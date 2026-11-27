package uk.gov.hmcts.reform.pcs.ccd.event.breathingspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceType;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.breathingspace.LiftBreathingSpacePageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.breathingspace.BreathingSpaceService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@ExtendWith(MockitoExtension.class)
class LiftBreathingSpaceTest extends BaseEventTest {

    @Mock
    private LiftBreathingSpacePageConfigurer liftBreathingSpacePageConfigurer;
    @Mock
    private BreathingSpaceService breathingSpaceService;
    @Mock
    private AddressFormatter addressFormatter;

    @InjectMocks
    private LiftBreathingSpace liftBreathingSpace;

    @BeforeEach
    void setUp() {
        setEventUnderTest(liftBreathingSpace);
    }

    @Test
    void shouldConfigurePages() {
        verify(liftBreathingSpacePageConfigurer).configurePages(any(PageBuilder.class));
    }

    @Test
    void shouldOnlyShowWhenFeatureFlagEnabledAndBreathingSpaceActive() {
        assertConfiguredShowConditions(ShowConditions.and(
            ShowConditions.featureFlagsEnabled(ENFORCEMENT),
            "hasActiveBreathingSpace=\"Yes\""
        ));
    }

    @Test
    void shouldPopulateActiveBreathingSpaceWithTypeLabelOnStart() {
        // Given
        BreathingSpaceDetails activeBreathingSpace = BreathingSpaceDetails.builder()
            .referenceNumber("BS123456")
            .startDate(LocalDate.of(2026, 10, 1))
            .type(BreathingSpaceType.STANDARD)
            .build();
        PCSCase caseData = PCSCase.builder().breathingSpaceDetails(activeBreathingSpace).build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getBreathingSpaceDetails().getReferenceNumber()).isEqualTo("BS123456");
        assertThat(result.getBreathingSpaceDetails().getTypeDisplay())
            .isEqualTo(BreathingSpaceType.STANDARD.getLabel());
    }

    @Test
    void shouldShowNotProvidedWhenActiveBreathingSpaceHasNoType() {
        // Given
        PCSCase caseData = PCSCase.builder().breathingSpaceDetails(BreathingSpaceDetails.builder().build()).build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getBreathingSpaceDetails().getTypeDisplay()).isEqualTo("Not provided");
    }

    @Test
    void shouldThrowOnStartWhenNoActiveBreathingSpace() {
        // Given
        PCSCase caseData = PCSCase.builder().build();

        // When / Then
        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(String.valueOf(TEST_CASE_REFERENCE));
    }

    @Test
    void shouldLiftBreathingSpaceAndReturnConfirmationBodyOnSubmit() {
        // Given
        AddressUK address = AddressUK.builder().build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(address)
            .caseNameHmctsInternal("Claimant v Defendant")
            .build();

        when(addressFormatter.formatMediumAddress(address, COMMA_DELIMITER)).thenReturn("1 Test Street, London");

        // When
        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        // Then
        verify(breathingSpaceService).liftBreathingSpace(TEST_CASE_REFERENCE);
        assertThat(response.getConfirmationBody())
            .contains("Breathing space will be lifted")
            .contains("Case number #" + TEST_CASE_REFERENCE)
            .contains("1 Test Street, London")
            .contains("Claimant v Defendant");
    }

}
