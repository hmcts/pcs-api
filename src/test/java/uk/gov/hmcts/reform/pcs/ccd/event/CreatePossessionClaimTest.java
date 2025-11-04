package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.feesandpay.entity.Fee;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeesAndPayService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePossessionClaimTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private FeesAndPayService feesAndPayService;
    @Mock
    private EnterPropertyAddress enterPropertyAddress;
    @Mock
    private CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    @Mock
    private PropertyNotEligible propertyNotEligible;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private PersistenceStrategyResolver persistenceStrategyResolver;

    @BeforeEach
    void setUp() {
        CreatePossessionClaim underTest = new CreatePossessionClaim(
            pcsCaseService,
            feesAndPayService,
            enterPropertyAddress,
            crossBorderPostcodeSelection,
            propertyNotEligible,
            persistenceStrategyResolver
        );

        when(persistenceStrategyResolver.getTestUrl()).thenReturn(mock(URI.class));

        setEventUnderTest(underTest);
    }

    @Test
    void shouldUpdateCaseOnSubmit() {
        // Given
        PCSCase caseData = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);
        LegislativeCountry legislativeCountry = mock(LegislativeCountry.class);

        when(caseData.getPropertyAddress()).thenReturn(propertyAddress);
        when(caseData.getLegislativeCountry()).thenReturn(legislativeCountry);

        // When
        callSubmitHandler(caseData);

        // Then
        verify(pcsCaseService).createCase(TEST_CASE_REFERENCE, propertyAddress, legislativeCountry);
    }

    @Test
    void shouldSetFeeAmountOnStart() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        Fee fee = Fee.builder()
            .code("FEE0412")
            .calculatedAmount(new BigDecimal("404.00"))
            .build();

        when(feesAndPayService.getFee("caseIssueFee")).thenReturn(fee);

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getFeeAmount()).isEqualTo("£404");
    }

    @Test
    void shouldHandleFeeWithDecimalPlaces() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        Fee fee = Fee.builder()
            .calculatedAmount(new BigDecimal("123.45"))
            .build();

        when(feesAndPayService.getFee("caseIssueFee")).thenReturn(fee);

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getFeeAmount()).isEqualTo("£123.45");
    }

    @Test
    void shouldHandleZeroFeeAmount() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        Fee fee = Fee.builder()
            .calculatedAmount(BigDecimal.ZERO)
            .build();

        when(feesAndPayService.getFee("caseIssueFee")).thenReturn(fee);

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getFeeAmount()).isEqualTo("£0");
    }

    @Test
    void shouldHandleNullFeeAmount() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        Fee fee = Fee.builder()
            .calculatedAmount(null)
            .build();

        when(feesAndPayService.getFee("caseIssueFee")).thenReturn(fee);

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getFeeAmount()).isEqualTo("Unable to retrieve");
    }

    @Test
    void shouldSetDefaultFeeWhenFeeServiceFails() {
        // Given
        PCSCase caseData = PCSCase.builder().build();

        when(feesAndPayService.getFee("caseIssueFee"))
            .thenThrow(new FeeNotFoundException("Fee not found"));

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feesAndPayService).getFee("caseIssueFee");
    }

    @Test
    void shouldSetDefaultFeeWhenFeeServiceThrowsRuntimeException() {
        // Given
        PCSCase caseData = PCSCase.builder().build();

        when(feesAndPayService.getFee("caseIssueFee"))
            .thenThrow(new RuntimeException("API unavailable"));

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feesAndPayService).getFee("caseIssueFee");
    }
}
