package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackStrategyFactoryTest {

    @Mock
    private MakeAClaimPaymentCallbackHandler makeAClaimPaymentCallbackHandler;

    @InjectMocks
    private PaymentCallbackStrategyFactory underTest;

    @Test
    void shouldReturnMakeAClaimCallbackHandlerForResumePossessionClaim() {
        // When
        PaymentCallbackStrategy result = underTest.getStrategy(PaymentCallbackHandlerType.CLAIM);

        // Then
        assertThat(result).isSameAs(makeAClaimPaymentCallbackHandler);
    }

    @Test
    void shouldHaveARegisteredStrategyForEveryJourneyId() {
        for (PaymentCallbackHandlerType paymentCallbackHandlerType : PaymentCallbackHandlerType.values()) {
            PaymentCallbackStrategy result = underTest.getStrategy(paymentCallbackHandlerType);
            assertThat(result)
                .as("Missing strategy mapping for PaymentCallbackHandlerType: %s", paymentCallbackHandlerType)
                .isNotNull();
        }
    }

}
