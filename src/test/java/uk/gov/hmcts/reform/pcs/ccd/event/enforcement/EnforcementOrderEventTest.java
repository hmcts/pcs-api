package uk.gov.hmcts.reform.pcs.ccd.event.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcement.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.AdditionalInformationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.AggressiveAnimalsRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.CriminalAntisocialRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.FirearmsPossessionRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.PoliceOrSocialServicesRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.PropertyAccessDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.ProtestorGroupRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.VerbalOrWrittenThreatsRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.ViolentAggressiveRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.VulnerableAdultsChildrenPage;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.CurrencyFormatter;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeesAndPayService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderEventTest extends BaseEventTest {

    private static final String ENFORCEMENT_WARRANT_FEE = "FEE0380";
    public static final String ENFORCEMENT_WRIT_FEE = "FEE0397";
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private CurrencyFormatter currencyFormatter;
    @Mock
    private FeesAndPayService feesAndPayService;
    @Mock
    private ViolentAggressiveRiskPage violentAggressiveRiskPage;
    @Mock
    private VerbalOrWrittenThreatsRiskPage verbalOrWrittenThreatsRiskPage;
    @Mock
    private ProtestorGroupRiskPage protestorGroupRiskPage;
    @Mock
    private PoliceOrSocialServicesRiskPage policeOrSocialServicesRiskPage;
    @Mock
    private FirearmsPossessionRiskPage firearmsPossessionRiskPage;
    @Mock
    private CriminalAntisocialRiskPage criminalAntisocialRiskPage;
    @Mock
    private AggressiveAnimalsRiskPage aggressiveAnimalsRiskPage;
    @Mock
    private PropertyAccessDetailsPage propertyAccessDetailsPage;
    @Mock
    private VulnerableAdultsChildrenPage vulnerableAdultsChildrenPage;
    @Mock
    private AdditionalInformationPage additionalInformationPage;
    @Mock
    private SavingPageBuilderFactory savingPageBuilderFactory;
    @Mock
    private EnforcementOrderService enforcementOrderService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        SavingPageBuilder savingPageBuilder = mock(SavingPageBuilder.class);
        when(savingPageBuilder.add(any())).thenReturn(savingPageBuilder);
        when(savingPageBuilderFactory.create(any(Event.EventBuilder.class), eq(enforceTheOrder)))
            .thenReturn(savingPageBuilder);
        setEventUnderTest(new EnforcementOrderEvent(enforcementOrderService, addressFormatter, feesAndPayService,
                                violentAggressiveRiskPage, verbalOrWrittenThreatsRiskPage, protestorGroupRiskPage,
                                policeOrSocialServicesRiskPage, firearmsPossessionRiskPage,
                                criminalAntisocialRiskPage, aggressiveAnimalsRiskPage, propertyAccessDetailsPage,
                                vulnerableAdultsChildrenPage, additionalInformationPage, savingPageBuilderFactory,
                                                    currencyFormatter));
    }

    @Test
    void shouldReturnCaseDataInStartCallback() {
        // Given
        AddressUK propertyAddress = mock(AddressUK.class);
        String expectedFormattedPropertyAddress = "expected formatted property address";
        when(addressFormatter.formatAddressWithHtmlLineBreaks(propertyAddress))
            .thenReturn(expectedFormattedPropertyAddress);

        String firstName = "Test";
        String lastName = "Testing";

        DefendantDetails defendantDetails = DefendantDetails.builder().firstName(firstName).lastName(lastName).build();
        PCSCase caseData = PCSCase.builder()
            .allDefendants(List.of(ListValue.<DefendantDetails>builder().value(defendantDetails).build()))
            .propertyAddress(propertyAddress)
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFormattedPropertyAddress()).isEqualTo(expectedFormattedPropertyAddress);
        assertThat(result.getAllDefendants()).hasSize(1);
        assertThat(result.getDefendant1().getFirstName()).isEqualTo(firstName);
        assertThat(result.getDefendant1().getLastName()).isEqualTo(lastName);
    }

    @Test
    void shouldCreateEnforcementDataInSubmitCallback() {
        // Given
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();
        PCSCase pcsCase = PCSCase.builder().enforcementOrder(enforcementOrder).build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(enforcementOrderService).createEnforcementOrder(TEST_CASE_REFERENCE, enforcementOrder);
    }

    @Test
    void shouldSetWarrantFeeAmountOnStart() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build()).build();

        // When
        when(feesAndPayService.getFee(ENFORCEMENT_WRIT_FEE)).thenReturn(
            FeeDetails
                .builder()
                .feeAmount(new BigDecimal("100.00"))
                .build());
        when(feesAndPayService.getFee(ENFORCEMENT_WARRANT_FEE)).thenReturn(
            FeeDetails
                .builder()
                .feeAmount(new BigDecimal("404.00"))
                .build()
        );
        when(currencyFormatter.formatAsCurrency(new BigDecimal("100.00")))
            .thenReturn("£100");
        //Stubbing Warrant fee
        when(currencyFormatter.formatAsCurrency(new BigDecimal("404.00")))
            .thenReturn("£404");

        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getEnforcementOrder().getWarrantFeeAmount()).isEqualTo("£404");
        verify(feesAndPayService).getFee(ENFORCEMENT_WARRANT_FEE);
    }

    @Test
    void shouldSetWritFeeAmountOnStart() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();

        // When
        when(feesAndPayService.getFee(ENFORCEMENT_WRIT_FEE))
            .thenReturn(FeeDetails.builder()
                            .feeAmount(new BigDecimal("100.00"))
                            .build());
        when(feesAndPayService.getFee(ENFORCEMENT_WARRANT_FEE))
            .thenReturn(FeeDetails.builder()
                            .feeAmount(new BigDecimal("120.00"))
                            .build());
        //Stubbing Writ fee
        when(currencyFormatter.formatAsCurrency(new BigDecimal("100.00")))
            .thenReturn("£100");
        //Stubbing Warrant fee
        when(currencyFormatter.formatAsCurrency(new BigDecimal("120.00")))
            .thenReturn("£120");

        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getEnforcementOrder().getWritFeeAmount()).isEqualTo("£100");
        verify(feesAndPayService).getFee(ENFORCEMENT_WRIT_FEE);
    }


    @Test
    void shouldHandleNullWarrantFeeAmount() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build()).build();

        // When
        when(feesAndPayService.getFee(ENFORCEMENT_WARRANT_FEE)).thenReturn(
            FeeDetails
                .builder()
                .feeAmount(null)
                .build()
        );
        when(feesAndPayService.getFee(ENFORCEMENT_WRIT_FEE)).thenReturn(
            FeeDetails
                .builder()
                .feeAmount(null)
                .build()
        );
        when(currencyFormatter.formatAsCurrency(null)).thenReturn("Unable to retrieve");

        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getEnforcementOrder().getWarrantFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feesAndPayService).getFee(ENFORCEMENT_WARRANT_FEE);
    }

    @Test
    void shouldHandleNullWritFeeAmount() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build()).build();

        // When
        when(feesAndPayService.getFee(ENFORCEMENT_WARRANT_FEE)).thenReturn(
            FeeDetails
                .builder()
                .feeAmount(null)
                .build()
        );
        when(feesAndPayService.getFee(ENFORCEMENT_WRIT_FEE)).thenReturn(
            FeeDetails
                .builder()
                .feeAmount(null)
                .build()
        );
        when(currencyFormatter.formatAsCurrency(null)).thenReturn("Unable to retrieve");

        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getEnforcementOrder().getWritFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feesAndPayService).getFee(ENFORCEMENT_WRIT_FEE);
    }

    @Test
    void shouldSetDefaultWarrantFeeWhenFeeServiceFails() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build()).build();

        // When
        when(feesAndPayService.getFee(ENFORCEMENT_WRIT_FEE))
            .thenThrow(new RuntimeException("Fee not found"));
        when(feesAndPayService.getFee(ENFORCEMENT_WARRANT_FEE))
            .thenThrow(new RuntimeException("Fee not found"));
        when(currencyFormatter.formatAsCurrency(null)).thenReturn("Unable to retrieve");

        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getEnforcementOrder().getWarrantFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feesAndPayService).getFee(ENFORCEMENT_WARRANT_FEE);
    }

    @Test
    void shouldSetDefaultWritFeeWhenFeeServiceFails() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build()).build();

        // When
        when(feesAndPayService.getFee(ENFORCEMENT_WRIT_FEE))
            .thenThrow(new RuntimeException("Fee not found"));
        when(feesAndPayService.getFee(ENFORCEMENT_WARRANT_FEE))
            .thenThrow(new RuntimeException("Fee not found"));
        when(currencyFormatter.formatAsCurrency(null)).thenReturn("Unable to retrieve");

        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getEnforcementOrder().getWritFeeAmount()).isEqualTo("Unable to retrieve");
        verify(feesAndPayService).getFee(ENFORCEMENT_WRIT_FEE);
    }
}
