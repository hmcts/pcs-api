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
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmationScreenFactoryTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private MoneyFormatter moneyFormatter;

    private ConfirmationScreenFactory underTest;

    @BeforeEach
    void setUp() {
        underTest = new ConfirmationScreenFactory(moneyFormatter);
    }

    @Test
    void shouldBuildFeeConfirmationScreen() {
        // Given
        String expectedFormattedFee = "expected formatted fee";

        GenAppRequest genAppRequest = XuiGenAppRequest.builder().build();
        FeeDetails feeDetails = mock(FeeDetails.class);
        BigDecimal applicationFee = mock(BigDecimal.class);
        when(feeDetails.getFeeAmount()).thenReturn(applicationFee);
        when(moneyFormatter.formatFee(applicationFee)).thenReturn(expectedFormattedFee);

        // When
        SubmitResponse<State> response
            = underTest.buildConfirmationScreenResponse(genAppRequest, CASE_REFERENCE, feeDetails);

        // Then
        assertThat(response.getConfirmationBody())
            .contains("Pay " + expectedFormattedFee);
        assertThat(response.getConfirmationBody())
            .contains("You must pay the application fee of " + expectedFormattedFee);
    }

    @ParameterizedTest
    @MethodSource("noFeeScenarios")
    @SuppressWarnings("ConstantValue")
    void shouldBuildNoFeeConfirmationScreen(GenAppType genAppType, String expectedMessage) {
        // Given
        GenAppRequest genAppRequest = XuiGenAppRequest.builder()
            .applicationType(genAppType)
            .build();

        FeeDetails feeDetails = null;

        // When
        SubmitResponse<State> response
            = underTest.buildConfirmationScreenResponse(genAppRequest, CASE_REFERENCE, feeDetails);

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
