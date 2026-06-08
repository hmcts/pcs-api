package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppFeeCalculator;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmationScreenFactoryTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private GenAppFeeCalculator genAppFeeCalculator;
    @Mock
    private MoneyFormatter moneyFormatter;

    private ConfirmationScreenFactory underTest;

    @BeforeEach
    void setUp() {
        underTest = new ConfirmationScreenFactory(genAppFeeCalculator, moneyFormatter);
    }

    @Test
    void shouldHaveNoConfirmationScreenForCuiJourney() {
        // Given
        GenAppRequest genAppRequest = CitizenGenAppRequest.builder().build();

        // When
        SubmitResponse<State> response = underTest.buildConfirmationScreenResponse(genAppRequest, CASE_REFERENCE);

        // Then
        assertThat(response.getConfirmationBody()).isNull();
    }

    @Test
    void shouldBuildFeeConfirmationScreenForXuiJourney() {
        // Given
        String expectedFormattedFee = "expected formatted fee";

        GenAppRequest genAppRequest = XuiGenAppRequest.builder().build();
        BigDecimal feeAmount = mock(BigDecimal.class);
        when(genAppFeeCalculator.getApplicationFee(genAppRequest)).thenReturn(Optional.of(feeAmount));
        when(moneyFormatter.formatFee(feeAmount)).thenReturn(expectedFormattedFee);

        // When
        SubmitResponse<State> response = underTest.buildConfirmationScreenResponse(genAppRequest, CASE_REFERENCE);

        // Then
        assertThat(response.getConfirmationBody())
            .contains("Pay " + expectedFormattedFee);
        assertThat(response.getConfirmationBody())
            .contains("You must pay the application fee of " + expectedFormattedFee);
    }

    @ParameterizedTest
    @MethodSource("noFeeScenarios")
    void shouldBuildNoFeeConfirmationScreenForXuiJourney(GenAppType genAppType, String expectedMessage) {
        // Given
        GenAppRequest genAppRequest = XuiGenAppRequest.builder()
            .applicationType(genAppType)
            .build();

        when(genAppFeeCalculator.getApplicationFee(genAppRequest)).thenReturn(Optional.empty());

        // When
        SubmitResponse<State> response = underTest.buildConfirmationScreenResponse(genAppRequest, CASE_REFERENCE);

        // Then
        assertThat(response.getConfirmationBody())
            .contains(expectedMessage);
    }

    private static Stream<Arguments> noFeeScenarios() {
        return Stream.of(
            arguments(GenAppType.ADJOURN, "We have received your request to adjourn (delay) the court hearing"),
            arguments(GenAppType.SET_ASIDE, "We have received your request to set aside (cancel) the order"),
            arguments(GenAppType.SOMETHING_ELSE, "We have received your request to ask the court to make an order")
        );
    }


}
