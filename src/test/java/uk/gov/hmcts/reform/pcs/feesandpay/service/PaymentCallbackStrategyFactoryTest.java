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
    @Mock
    private GenAppPaymentCallbackHandler genAppPaymentCallbackHandler;

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
    void shouldReturnPaymentCallbackHandlerForGenApp() {
        // When
        PaymentCallbackStrategy result = underTest.getStrategy(PaymentCallbackHandlerType.GEN_APP_ISSUE);

        // Then
        assertThat(result).isSameAs(genAppPaymentCallbackHandler);
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
