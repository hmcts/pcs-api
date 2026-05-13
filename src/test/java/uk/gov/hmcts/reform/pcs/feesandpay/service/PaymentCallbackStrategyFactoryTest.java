package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.feesandpay.model.JourneyId;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackStrategyFactoryTest {

    @Mock
    private MakeAClaimPaymentCallbackStrategy makeAClaimPaymentCallbackStrategy;

    @InjectMocks
    private PaymentCallbackStrategyFactory underTest;

    @Test
    void shouldReturnMakeAClaimStrategyForResumePossessionClaim() {
        // When
        PaymentCallbackStrategy result = underTest.getStrategy(JourneyId.RESUME_POSSESSION_CLAIM);

        // Then
        assertThat(result).isSameAs(makeAClaimPaymentCallbackStrategy);
    }

    @Test
    void shouldReturnNullForUnmappedJourneyId() {
        for (JourneyId journeyId : JourneyId.values()) {
            PaymentCallbackStrategy result = underTest.getStrategy(journeyId);
            assertThat(result)
                .as("Expected a non-null strategy for JourneyId %s", journeyId)
                .isNotNull();
        }
    }

    @Test
    void shouldHaveARegisteredStrategyForEveryJourneyId() {
        for (JourneyId journeyId : JourneyId.values()) {
            PaymentCallbackStrategy result = underTest.getStrategy(journeyId);
            assertThat(result)
                .as("Missing strategy mapping for JourneyId: %s", journeyId)
                .isNotNull();
        }
    }

}
