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
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

import java.util.List;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType.ENFORCEMENT_WARRANT_FEE;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType.ENFORCEMENT_WRIT_FEE;


@ExtendWith(MockitoExtension.class)
class EnforcementOrderEventTest extends BaseEventTest {

    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private FeeApplier feeApplier;
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
        setEventUnderTest(new EnforcementOrderEvent(enforcementOrderService, addressFormatter, feeApplier,
                                                    violentAggressiveRiskPage, verbalOrWrittenThreatsRiskPage,
                                                    protestorGroupRiskPage, policeOrSocialServicesRiskPage,
                                                    firearmsPossessionRiskPage, criminalAntisocialRiskPage,
                                                    aggressiveAnimalsRiskPage, propertyAccessDetailsPage,
                                                    vulnerableAdultsChildrenPage, additionalInformationPage,
                                                    savingPageBuilderFactory
        ));
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
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();
        String expectedFormattedWarrantFee = "£404";

        doAnswer(invocation -> {
            PCSCase pcs = invocation.getArgument(0);
            BiConsumer<PCSCase, String> setter = invocation.getArgument(2);
            setter.accept(pcs, expectedFormattedWarrantFee);
            return null;
        }).when(feeApplier).applyFeeAmount(
            eq(caseData),
            eq(FeeType.ENFORCEMENT_WARRANT_FEE),
            any()
        );

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getEnforcementOrder().getWarrantFeeAmount()).isEqualTo(expectedFormattedWarrantFee);
        verify(feeApplier).applyFeeAmount(eq(caseData), eq(FeeType.ENFORCEMENT_WARRANT_FEE), any());
    }

    @Test
    void shouldSetWritFeeAmountOnStart() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();

        String expectedFormattedWritFee = "£100";

        doAnswer(invocation -> {
            PCSCase pcs = invocation.getArgument(0);
            BiConsumer<PCSCase, String> setter = invocation.getArgument(2);
            setter.accept(pcs, expectedFormattedWritFee);
            return null;
        }).when(feeApplier).applyFeeAmount(
            any(PCSCase.class),
            any(FeeType.class),
            any()
        );

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getEnforcementOrder().getWritFeeAmount()).isEqualTo(expectedFormattedWritFee);
        verify(feeApplier).applyFeeAmount(eq(caseData), eq(ENFORCEMENT_WRIT_FEE), any());
    }

    @Test
    void shouldSetDefaultWarrantFeeWhenFeeServiceFails() {
        // Given
        PCSCase caseData = PCSCase.builder().enforcementOrder(EnforcementOrder.builder().build()).build();
        String expectedFeesMessage = "Unable to retrieve";

        doAnswer(invocation -> {
            PCSCase pcs = invocation.getArgument(0);
            BiConsumer<PCSCase, String> setter = invocation.getArgument(2);
            try {
                throw new RuntimeException("Fee not found");
            } catch (RuntimeException e) {
                setter.accept(pcs, expectedFeesMessage);
            }
            return null;
        }).when(feeApplier).applyFeeAmount(
            eq(caseData),
            any(FeeType.class),
            any()
        );
        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getEnforcementOrder().getWarrantFeeAmount()).isEqualTo(expectedFeesMessage);
        verify(feeApplier).applyFeeAmount(eq(caseData), eq(ENFORCEMENT_WARRANT_FEE), any());

    }

    @Test
    void shouldSetDefaultWritFeeWhenFeeServiceFails() {
        // Given
        PCSCase caseData = PCSCase.builder().enforcementOrder(EnforcementOrder.builder().build()).build();
        String expectedFeesMessage = "Unable to retrieve";

        doAnswer(invocation -> {
            PCSCase pcs = invocation.getArgument(0);
            BiConsumer<PCSCase, String> setter = invocation.getArgument(2);
            try {
                throw new RuntimeException("Fee not found");
            } catch (RuntimeException e) {
                setter.accept(pcs, expectedFeesMessage);
            }
            return null;
        }).when(feeApplier).applyFeeAmount(
            eq(caseData),
            any(FeeType.class),
            any());
        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getEnforcementOrder().getWritFeeAmount()).isEqualTo(expectedFeesMessage);
        verify(feeApplier).applyFeeAmount(eq(caseData), eq(ENFORCEMENT_WRIT_FEE), any());
    }
}
