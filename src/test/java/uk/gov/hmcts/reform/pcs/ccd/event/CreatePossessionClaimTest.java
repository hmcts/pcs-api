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
import uk.gov.hmcts.reform.pcs.ccd.util.FeeFormatter;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePossessionClaimTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private FeeService feeService;
    @Mock
    private EnterPropertyAddress enterPropertyAddress;
    @Mock
    private CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    @Mock
    private PropertyNotEligible propertyNotEligible;
    @Mock
    private FeeFormatter feeFormatter;

    @BeforeEach
    void setUp() {
        CreatePossessionClaim underTest = new CreatePossessionClaim(
            pcsCaseService,
            feeService,
            enterPropertyAddress,
            crossBorderPostcodeSelection,
            propertyNotEligible,
            feeFormatter
        );
        setEventUnderTest(underTest);
    }

    @Test
    void shouldUpdateCaseOnSubmit() {
        PCSCase caseData = mock(PCSCase.class);
        AddressUK propertyAddress = mock(AddressUK.class);
        LegislativeCountry legislativeCountry = mock(LegislativeCountry.class);

        when(caseData.getPropertyAddress()).thenReturn(propertyAddress);
        when(caseData.getLegislativeCountry()).thenReturn(legislativeCountry);

        callSubmitHandler(caseData);

        verify(pcsCaseService).createCase(TEST_CASE_REFERENCE, propertyAddress, legislativeCountry);
    }

    @Test
    void shouldSetFeeAmountOnStart() {
        PCSCase caseData = PCSCase.builder().build();

        BigDecimal feeAmount = new BigDecimal("404.00");
        String expectedFormattedFee = "formatted fee";

        when(feeService.getFee(FeeTypes.CASE_ISSUE_FEE)).thenReturn(
            FeeDetails.builder()
                .feeAmount(feeAmount)
                .build()
        );
        when(feeFormatter.formatFee(feeAmount)).thenReturn(expectedFormattedFee);

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getFeeAmount()).isEqualTo(expectedFormattedFee);
    }

    @Test
    void shouldHandleNullFeeAmount() {
        PCSCase caseData = PCSCase.builder().build();
        when(feeService.getFee(FeeTypes.CASE_ISSUE_FEE)).thenReturn(
            FeeDetails.builder()
                .feeAmount(null)
                .build()
        );

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feeService).getFee(FeeTypes.CASE_ISSUE_FEE);
    }

    @Test
    void shouldSetDefaultFeeWhenFeeServiceFails() {
        PCSCase caseData = PCSCase.builder().build();

        when(feeService.getFee(FeeTypes.CASE_ISSUE_FEE))
            .thenThrow(new RuntimeException("Fee not found"));

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feeService).getFee(FeeTypes.CASE_ISSUE_FEE);
    }

    @Test
    void shouldSetDefaultFeeWhenFeeServiceThrowsRuntimeException() {
        PCSCase caseData = PCSCase.builder().build();

        when(feeService.getFee(FeeTypes.CASE_ISSUE_FEE))
            .thenThrow(new RuntimeException("API unavailable"));

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feeService).getFee(FeeTypes.CASE_ISSUE_FEE);
    }
}
