package uk.gov.hmcts.reform.pcs.ccd.event.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeesAndPayService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto.builder;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderEventTest extends BaseEventTest {

    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private FeesAndPayService feesAndPayService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new EnforcementOrderEvent(addressFormatter, feesAndPayService));
    }

    @Test
    void shouldReturnCaseDataInStartCallback() {
        // Given
        AddressUK propertyAddress = mock(AddressUK.class);
        String expectedFormattedPropertyAddress = "expected formatted property address";

        String firstName = "Test";
        String lastName = "Testing";

        DefendantDetails defendantDetails = DefendantDetails.builder().firstName(firstName).lastName(lastName).build();
        PCSCase caseData = PCSCase.builder()
                .defendants(List.of(ListValue.<DefendantDetails>builder().value(defendantDetails).build()))
            .propertyAddress(propertyAddress)
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();

        when(addressFormatter.formatAddressWithHtmlLineBreaks(propertyAddress))
            .thenReturn(expectedFormattedPropertyAddress);

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFormattedPropertyAddress()).isEqualTo(expectedFormattedPropertyAddress);
        assertThat(result.getDefendants()).hasSize(1);
        assertThat(result.getDefendant1().getFirstName()).isEqualTo(firstName);
        assertThat(result.getDefendant1().getLastName()).isEqualTo(lastName);
    }

    @Test
    void shouldSetWarrantFeeAmountOnStart() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build()).build();
        when(feesAndPayService.getFee("enforcementWarrantFee")).thenReturn(
            builder()
                .feeAmount(new BigDecimal("404.00"))
                .build()
        );

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getEnforcementOrder().getWarrantFeeAmount()).isEqualTo("£404");
        verify(feesAndPayService).getFee("enforcementWarrantFee");
    }

    @Test
    void shouldSetWritFeeAmountOnStart() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build()).build();
        when(feesAndPayService.getFee("enforcementWritFee")).thenReturn(
            builder()
                .feeAmount(new BigDecimal("100.00"))
                .build()
        );

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getEnforcementOrder().getWritFeeAmount()).isEqualTo("£100");
        verify(feesAndPayService).getFee("enforcementWritFee");
    }

    @Test
    void shouldHandleNullWarrantFeeAmount() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build()).build();
        when(feesAndPayService.getFee("enforcementWarrantFee")).thenReturn(
            builder()
                .feeAmount(null)
                .build()
        );

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getEnforcementOrder().getWarrantFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feesAndPayService).getFee("enforcementWarrantFee");
    }

    @Test
    void shouldHandleNullWritFeeAmount() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build()).build();
        when(feesAndPayService.getFee("enforcementWritFee")).thenReturn(
            builder()
                .feeAmount(null)
                .build()
        );

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getEnforcementOrder().getWritFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feesAndPayService).getFee("enforcementWritFee");
    }

    @Test
    void shouldSetDefaultWarrantFeeWhenFeeServiceFails() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build()).build();

        when(feesAndPayService.getFee("enforcementWarrantFee"))
            .thenThrow(new RuntimeException("Fee not found"));

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getEnforcementOrder().getWarrantFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feesAndPayService).getFee("enforcementWarrantFee");
    }

    @Test
    void shouldSetDefaultWritFeeWhenFeeServiceFails() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build()).build();

        when(feesAndPayService.getFee("enforcementWritFee"))
            .thenThrow(new RuntimeException("Fee not found"));

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getEnforcementOrder().getWritFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feesAndPayService).getFee("enforcementWritFee");
    }
}
