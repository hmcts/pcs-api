package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.dto.CreateClaimData;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.lang.reflect.Field;

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

    @BeforeEach
    void setUp() {
        CreatePossessionClaim underTest = new CreatePossessionClaim(
            pcsCaseService,
            new FeeApplier(feeService, new MoneyFormatter()),
            enterPropertyAddress,
            crossBorderPostcodeSelection,
            propertyNotEligible
        );
        setEventUnderTest(underTest);
    }

    @Test
    void shouldUpdateCaseOnSubmit() {
        AddressUK propertyAddress = mock(AddressUK.class);
        LegislativeCountry legislativeCountry = LegislativeCountry.ENGLAND;

        CreateClaimData caseData = CreateClaimData.builder()
            .propertyAddress(propertyAddress)
            .legislativeCountry(legislativeCountry)
            .build();

        callDtoSubmitHandler(caseData);

        verify(pcsCaseService).createCase(TEST_CASE_REFERENCE, propertyAddress, legislativeCountry);
    }

    @Test
    void shouldConfigureEventFieldPrefix() {
        assertThat(configuredEvent.getFieldPrefix()).isEqualTo("cpc");
        assertThat(configuredEvent.getEventFieldPrefix()).isEqualTo("cpc");
    }

    @Test
    void shouldKeepGeneratedFieldIdsWithinCcdLimit() {
        assertThat(configuredEvent.getFields().getFields())
            .allSatisfy(field -> assertThat(getFieldBuilderId(field)).hasSizeLessThanOrEqualTo(70));
    }

    @Test
    void shouldSetFeeAmountOnStart() {
        CreateClaimData caseData = CreateClaimData.builder().build();
        when(feeService.getFee(FeeType.CASE_ISSUE_FEE)).thenReturn(
            FeeDetails.builder()
                .feeAmount(new BigDecimal("404.00"))
                .build()
        );

        CreateClaimData result = callDtoStartHandler(caseData);

        assertThat(result.getFeeAmount()).isEqualTo("£404");
        verify(feeService).getFee(FeeType.CASE_ISSUE_FEE);
    }

    @Test
    void shouldHandleFeeWithDecimalPlaces() {
        CreateClaimData caseData = CreateClaimData.builder().build();
        when(feeService.getFee(FeeType.CASE_ISSUE_FEE)).thenReturn(
            FeeDetails.builder()
                .feeAmount(new BigDecimal("123.45"))
                .build()
        );

        CreateClaimData result = callDtoStartHandler(caseData);

        assertThat(result.getFeeAmount()).isEqualTo("£123.45");
        verify(feeService).getFee(FeeType.CASE_ISSUE_FEE);
    }

    @Test
    void shouldHandleZeroFeeAmount() {
        CreateClaimData caseData = CreateClaimData.builder().build();
        when(feeService.getFee(FeeType.CASE_ISSUE_FEE)).thenReturn(
            FeeDetails.builder()
                .feeAmount(BigDecimal.ZERO)
                .build()
        );

        CreateClaimData result = callDtoStartHandler(caseData);

        assertThat(result.getFeeAmount()).isEqualTo("£0");
        verify(feeService).getFee(FeeType.CASE_ISSUE_FEE);
    }

    @Test
    void shouldHandleNullFeeAmount() {
        CreateClaimData caseData = CreateClaimData.builder().build();
        when(feeService.getFee(FeeType.CASE_ISSUE_FEE)).thenReturn(
            FeeDetails.builder()
                .feeAmount(null)
                .build()
        );

        CreateClaimData result = callDtoStartHandler(caseData);

        assertThat(result.getFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feeService).getFee(FeeType.CASE_ISSUE_FEE);
    }

    @Test
    void shouldSetDefaultFeeWhenFeeServiceFails() {
        CreateClaimData caseData = CreateClaimData.builder().build();

        when(feeService.getFee(FeeType.CASE_ISSUE_FEE))
            .thenThrow(new RuntimeException("Fee not found"));

        CreateClaimData result = callDtoStartHandler(caseData);

        assertThat(result.getFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feeService).getFee(FeeType.CASE_ISSUE_FEE);
    }

    @Test
    void shouldSetDefaultFeeWhenFeeServiceThrowsRuntimeException() {
        CreateClaimData caseData = CreateClaimData.builder().build();

        when(feeService.getFee(FeeType.CASE_ISSUE_FEE))
            .thenThrow(new RuntimeException("API unavailable"));

        CreateClaimData result = callDtoStartHandler(caseData);

        assertThat(result.getFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feeService).getFee(FeeType.CASE_ISSUE_FEE);
    }

    private String getFieldBuilderId(Object fieldBuilder) {
        try {
            Field idField = fieldBuilder.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            return (String) idField.get(fieldBuilder);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read field builder id", e);
        }
    }
}
