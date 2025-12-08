package uk.gov.hmcts.reform.pcs.feesandpay.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fees.client.FeesApi;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PCSFeesClientTest {

    private PCSFeesClient underTest;

    @Mock
    private FeesClientContext strategy;

    @Mock
    private FeesApi feesApi;

    @Mock
    private Jurisdictions jurisdictions;

    @Mock
    private ServiceName serviceName;

    @Mock
    private FeeTypes feeTypes;

    @BeforeEach
    void setUp() {
        underTest = new PCSFeesClient(List.of(strategy));
    }

    @Test
    void shouldLookupFeeWhenStrategySupportsFeeType() {
        // Given
        String feeCode = "testCode";
        String channel = "default";
        String event = "issue";
        BigDecimal amount = BigDecimal.TEN;
        String keyword = "keyword";
        String service = "service";
        String jurisdiction1 = "j1";
        String jurisdiction2 = "j2";

        when(feeTypes.getCode()).thenReturn(feeCode);
        when(strategy.supports(feeCode)).thenReturn(true);
        when(strategy.getApi()).thenReturn(feesApi);
        when(strategy.getServiceName()).thenReturn(serviceName);
        when(strategy.getJurisdictions()).thenReturn(jurisdictions);

        when(serviceName.service()).thenReturn(service);
        when(jurisdictions.jurisdiction1()).thenReturn(jurisdiction1);
        when(jurisdictions.jurisdiction2()).thenReturn(jurisdiction2);

        FeeLookupResponseDto expectedResponse = new FeeLookupResponseDto();
        when(feesApi.lookupFee(
            eq(service),
            eq(jurisdiction1),
            eq(jurisdiction2),
            eq(channel),
            eq(event),
            isNull(),
            eq(amount),
            eq(keyword)
        )).thenReturn(expectedResponse);

        // When
        FeeLookupResponseDto result = underTest.lookupFee(feeTypes, channel, event, amount, keyword);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(feesApi).lookupFee(
            eq(service),
            eq(jurisdiction1),
            eq(jurisdiction2),
            eq(channel),
            eq(event),
            isNull(),
            eq(amount),
            eq(keyword)
        );
    }

    @Test
    void shouldThrowExceptionWhenNoStrategyFound() {
        // Given
        String feeCode = "unsupportedCode";
        when(feeTypes.getCode()).thenReturn(feeCode);
        when(strategy.supports(feeCode)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> underTest.lookupFee(feeTypes, "channel",
                                                         "event", BigDecimal.TEN, "keyword"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No strategy found for fee type: " + feeTypes);
    }

    @Test
    void shouldHandleEmptyStrategyList() {
        // Given
        underTest = new PCSFeesClient(List.of());

        // When/Then
        assertThatThrownBy(() -> underTest.lookupFee(feeTypes, "channel",
                                                       "event", BigDecimal.TEN, "keyword"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No strategy found for fee type: " + feeTypes);
    }

    @Test
    void shouldSelectCorrectStrategyWhenMultipleStrategiesExist() {
        FeesClientContext unsupportedStrategy = mock(FeesClientContext.class);
        FeesClientContext supportedStrategy = mock(FeesClientContext.class);
        underTest = new PCSFeesClient(List.of(unsupportedStrategy, supportedStrategy));

        String feeCode = "testCode";
        when(feeTypes.getCode()).thenReturn(feeCode);
        when(unsupportedStrategy.supports(feeCode)).thenReturn(false);
        when(supportedStrategy.supports(feeCode)).thenReturn(true);
        when(supportedStrategy.getApi()).thenReturn(feesApi);
        when(supportedStrategy.getServiceName()).thenReturn(serviceName);
        when(supportedStrategy.getJurisdictions()).thenReturn(jurisdictions);

        when(serviceName.service()).thenReturn("service");
        when(jurisdictions.jurisdiction1()).thenReturn("j1");
        when(jurisdictions.jurisdiction2()).thenReturn("j2");

        FeeLookupResponseDto expectedResponse = new FeeLookupResponseDto();
        when(feesApi.lookupFee(
            eq("service"),
            eq("j1"),
            eq("j2"),
            eq("channel"),
            eq("event"),
            isNull(),
            eq(BigDecimal.TEN),
            eq("keyword")
        )).thenReturn(expectedResponse);

        // When
        FeeLookupResponseDto result = underTest.lookupFee(feeTypes, "channel",
                                                                    "event", BigDecimal.TEN, "keyword");

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(unsupportedStrategy).supports(feeCode);
        verify(unsupportedStrategy, never()).getApi();

        verify(supportedStrategy).supports(feeCode);
        verify(supportedStrategy).getApi();
        verify(supportedStrategy).getServiceName();
        verify(supportedStrategy, times(2)).getJurisdictions();
    }

}
