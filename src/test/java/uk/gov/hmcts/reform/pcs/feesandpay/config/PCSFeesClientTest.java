package uk.gov.hmcts.reform.pcs.feesandpay.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fees.client.FeesApi;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.pcs.feesandpay.client.PCSFeesClient;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PCSFeesClientTest {

    private static final String SERVICE = "possession claim";
    private static final String JURISDICTION_1 = "civil";
    private static final String JURISDICTION_2 = "county court";
    private static final String CHANNEL = "default";
    private static final String EVENT = "issue";
    private static final BigDecimal AMOUNT_OR_VOLUME = new BigDecimal("1.00");
    private static final String KEYWORD = "PossessionCC";

    @Mock
    private FeesConfiguration feesConfiguration;

    @Mock
    private FeesApi feesApi;

    @InjectMocks
    private PCSFeesClient underTest;

    private FeesConfiguration.LookUpReferenceData lookUpReferenceData;
    private FeeLookupResponseDto feeLookupResponseDto;

    @BeforeEach
    void setUp() {
        lookUpReferenceData = new FeesConfiguration.LookUpReferenceData();
        lookUpReferenceData.setService(SERVICE);
        lookUpReferenceData.setJurisdiction1(JURISDICTION_1);
        lookUpReferenceData.setJurisdiction2(JURISDICTION_2);
        lookUpReferenceData.setChannel(CHANNEL);
        lookUpReferenceData.setEvent(EVENT);
        lookUpReferenceData.setAmountOrVolume(AMOUNT_OR_VOLUME);
        lookUpReferenceData.setKeyword(KEYWORD);

        feeLookupResponseDto = FeeLookupResponseDto.builder()
            .code("FEE0001")
            .description("Issue fee")
            .version(1)
            .feeAmount(new BigDecimal("455.00"))
            .build();
    }

    @Test
    void shouldSuccessfullyLookupFeeWhenConfigurationExists() {
        // Given
        when(feesConfiguration.getLookup(FeeType.CASE_ISSUE_FEE)).thenReturn(lookUpReferenceData);
        when(feesApi.lookupFee(
            eq(SERVICE),
            eq(JURISDICTION_1),
            eq(JURISDICTION_2),
            eq(CHANNEL),
            eq(EVENT),
            isNull(),
            eq(AMOUNT_OR_VOLUME),
            eq(KEYWORD)
        )).thenReturn(feeLookupResponseDto);

        // When
        FeeLookupResponseDto result = underTest.lookupFee(FeeType.CASE_ISSUE_FEE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("FEE0001");
        assertThat(result.getDescription()).isEqualTo("Issue fee");
        assertThat(result.getFeeAmount()).isEqualTo(new BigDecimal("455.00"));

        verify(feesConfiguration).getLookup(FeeType.CASE_ISSUE_FEE);
        verify(feesApi).lookupFee(
            eq(SERVICE),
            eq(JURISDICTION_1),
            eq(JURISDICTION_2),
            eq(CHANNEL),
            eq(EVENT),
            isNull(),
            eq(AMOUNT_OR_VOLUME),
            eq(KEYWORD)
        );
    }

    @Test
    void shouldThrowFeeNotFoundExceptionWhenConfigurationIsNull() {
        // Given
        when(feesConfiguration.getLookup(FeeType.CASE_ISSUE_FEE)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> underTest.lookupFee(FeeType.CASE_ISSUE_FEE))
            .isInstanceOf(FeeNotFoundException.class)
            .hasMessageContaining("Fee not found for feeType: CASE_ISSUE_FEE");

        verify(feesConfiguration).getLookup(FeeType.CASE_ISSUE_FEE);
        verify(feesApi, never()).lookupFee(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldLookupFeeWithAllReferenceDataFields() {
        // Given
        when(feesConfiguration.getLookup(FeeType.CASE_ISSUE_FEE)).thenReturn(lookUpReferenceData);
        when(feesApi.lookupFee(
            eq(SERVICE),
            eq(JURISDICTION_1),
            eq(JURISDICTION_2),
            eq(CHANNEL),
            eq(EVENT),
            isNull(),
            eq(AMOUNT_OR_VOLUME),
            eq(KEYWORD)
        )).thenReturn(feeLookupResponseDto);

        // When
        underTest.lookupFee(FeeType.CASE_ISSUE_FEE);

        // Then
        verify(feesApi).lookupFee(
            eq(SERVICE),
            eq(JURISDICTION_1),
            eq(JURISDICTION_2),
            eq(CHANNEL),
            eq(EVENT),
            isNull(),
            eq(AMOUNT_OR_VOLUME),
            eq(KEYWORD)
        );
    }

    @Test
    void shouldHandleDifferentFeeTypes() {
        // Given
        FeeType differentFeeType = FeeType.CASE_ISSUE_FEE;
        when(feesConfiguration.getLookup(differentFeeType)).thenReturn(lookUpReferenceData);
        when(feesApi.lookupFee(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(feeLookupResponseDto);

        // When
        FeeLookupResponseDto result = underTest.lookupFee(differentFeeType);

        // Then
        assertThat(result).isNotNull();
        verify(feesConfiguration).getLookup(differentFeeType);
    }

    @Test
    void shouldReturnFeeLookupResponseDtoWithCorrectStructure() {
        // Given
        when(feesConfiguration.getLookup(FeeType.CASE_ISSUE_FEE)).thenReturn(lookUpReferenceData);

        FeeLookupResponseDto expectedResponse = FeeLookupResponseDto.builder()
            .code("FEE0002")
            .description("Hearing fee")
            .version(2)
            .feeAmount(new BigDecimal("275.00"))
            .build();

        when(feesApi.lookupFee(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(expectedResponse);

        // When
        FeeLookupResponseDto result = underTest.lookupFee(FeeType.CASE_ISSUE_FEE);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        assertThat(result.getCode()).isEqualTo("FEE0002");
        assertThat(result.getDescription()).isEqualTo("Hearing fee");
        assertThat(result.getVersion()).isEqualTo(2);
        assertThat(result.getFeeAmount()).isEqualTo(new BigDecimal("275.00"));
    }
}
