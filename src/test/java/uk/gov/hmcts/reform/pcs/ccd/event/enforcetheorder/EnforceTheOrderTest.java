package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.Field.FieldBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.WarrantPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution.WarrantOfRestitutionPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ.WritPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestSupportEnvironment;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GA_CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT_OF_RESTITUTION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WRIT;
import static uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil.buildEnforcementOrderWithSpecifiedType;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.BR_DELIMITER;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@ExtendWith(MockitoExtension.class)
class EnforceTheOrderTest extends BaseEventTest {

    private static final int FEE_AMOUNT = 1000;

    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private FeeApplier feeApplier;
    @Mock
    private DefendantService defendantService;
    @Mock
    private EnforcementOrderService enforcementOrderService;
    @Mock
    private EnforcementPageConfigurer enforcementPageConfigurer;
    @Mock
    private WarrantPageConfigurer warrantPageConfigurer;
    @Mock
    private WritPageConfigurer writPageConfigurer;
    @Mock
    private WarrantOfRestitutionPageConfigurer warrantOfRestitutionPageConfigurer;
    @Mock
    private SavingPageBuilderFactory savingPageBuilderFactory;
    @InjectMocks
    private EnforceTheOrder enforceTheOrder;
    @Mock
    private SavingPageBuilder savingPageBuilder;
    @Mock
    private EnforcementSubmitEvent enforcementSubmitEvent;

    private MockedStatic<TestSupportEnvironment> mockedEnv;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        mockedEnv = Mockito.mockStatic(TestSupportEnvironment.class);
        mockedEnv.when(TestSupportEnvironment::isDev).thenReturn(true);
        when(savingPageBuilderFactory.create(any(Event.EventBuilder.class), eq(EventId.enforceTheOrder)))
                .thenReturn(savingPageBuilder);
        setEventUnderTest(enforceTheOrder);
    }

    @AfterEach
    void tearDown() {
        mockedEnv.close();
    }

    @Test
    void shouldConfigurePages() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();

        // When
        callStartHandler(caseData);

        //Then
        verify(enforcementPageConfigurer).configurePages(savingPageBuilder);
        verify(warrantPageConfigurer).configurePages(savingPageBuilder);
        verify(writPageConfigurer).configurePages(savingPageBuilder);
        verify(warrantOfRestitutionPageConfigurer).configurePages(savingPageBuilder);
    }

    @Test
    void shouldConfigureEventForCaseIssuedWithEnforcementFeatureFlagAndCorrectGrants() {
        assertConfiguredForStates(State.CASE_ISSUED);
        assertConfiguredShowConditions(and(featureFlagsEnabled(ENFORCEMENT), "hasUnsubmittedEnforcementData=\"No\""));
        assertGrants(PCS_SOLICITOR, Permission.CRUD);
        assertGrants(GA_CLAIMANT_SOLICITOR, Permission.CRUD);
        assertGrants(CLAIMANT, Permission.CRUD);
    }

    @Test
    @SuppressWarnings({"rawtypes"})
    void shouldExcludeNeverShownFieldsFromSummaryIncludingNestedFields() {
        Field<?, ?, ?, ?> hiddenField = mock(Field.class);
        Field<?, ?, ?, ?> visibleField = mock(Field.class);
        Field<?, ?, ?, ?> nestedHiddenField = mock(Field.class);
        when(hiddenField.getShowCondition()).thenReturn(NEVER_SHOW);
        when(visibleField.getShowCondition()).thenReturn("someCondition");
        when(nestedHiddenField.getShowCondition()).thenReturn(NEVER_SHOW);

        FieldBuilder hiddenFieldBuilder = mock(FieldBuilder.class);
        FieldBuilder visibleFieldBuilder = mock(FieldBuilder.class);
        FieldBuilder nestedFieldBuilder = mock(FieldBuilder.class);
        when(hiddenFieldBuilder.build()).thenReturn(hiddenField);
        when(visibleFieldBuilder.build()).thenReturn(visibleField);
        when(nestedFieldBuilder.build()).thenReturn(nestedHiddenField);

        FieldCollection nestedCollection = mock(FieldCollection.class);
        when(nestedCollection.getFields()).thenReturn(List.of(nestedFieldBuilder));
        FieldCollectionBuilder nestedCollectionBuilder = mock(FieldCollectionBuilder.class);
        when(nestedCollectionBuilder.build()).thenReturn(nestedCollection);

        FieldCollection fieldCollection = mock(FieldCollection.class);
        when(fieldCollection.getFields()).thenReturn(List.of(hiddenFieldBuilder, visibleFieldBuilder));
        when(fieldCollection.getComplexFields()).thenReturn(List.of(nestedCollectionBuilder));
        when(nestedCollection.getComplexFields()).thenReturn(List.of());

        EnforceTheOrder.excludeNeverShownFieldsFromSummary(fieldCollection);

        verify(hiddenFieldBuilder).showSummary(false);
        verify(visibleFieldBuilder, never()).showSummary(false);
        verify(nestedFieldBuilder).showSummary(false);
    }

    @Test
    void shouldReturnCaseDataInStartCallback() {
        // Given
        AddressUK propertyAddress = mock(AddressUK.class);
        String expectedFormattedPropertyAddress = "expected formatted property address";
        when(addressFormatter.formatMediumAddress(propertyAddress, BR_DELIMITER))
            .thenReturn(expectedFormattedPropertyAddress);

        String firstName = "Test";
        String lastName = "Testing";

        Party defendantDetails = Party.builder().firstName(firstName).lastName(lastName).build();
        List<ListValue<Party>> allDefendants = List.of(
            ListValue.<Party>builder().value(defendantDetails).build()
        );
        when(defendantService.buildDefendantListItems(allDefendants)).thenReturn(new ArrayList<>());

        PCSCase caseData = PCSCase.builder()
            .allDefendants(allDefendants)
            .propertyAddress(propertyAddress)
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFormattedPropertyAddress())
                .isEqualTo(expectedFormattedPropertyAddress);
        assertThat(result.getAllDefendants()).hasSize(1);
    }

    @Test
    void shouldCreateEnforcementDataInSubmitCallback() {
        // Given
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();
        PCSCase pcsCase = PCSCase.builder().enforcementOrder(enforcementOrder).build();
        SubmitResponse<State> expectedResponse = SubmitResponse.<State>builder().build();
        when(enforcementSubmitEvent.submit(any())).thenReturn(expectedResponse);

        // When
        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        // Then
        assertThat(response).isSameAs(expectedResponse);
        verify(enforcementSubmitEvent).submit(any());
    }

    @Nested
    @DisplayName("populateDefendantSelectionList tests")
    class PopulateDefendantSelectionListTests {

        @Test
        @DisplayName("Should populate selectedDefendants with empty value and list items from service")
        void shouldPopulateSelectedDefendantsWithEmptyValueAndListItems() {
            // Given
            String firstName = "John";
            String lastName = "Doe";
            Party defendantDetails = Party.builder()
                .firstName(firstName)
                .lastName(lastName)
                .build();
            List<ListValue<Party>> allDefendants = List.of(
                ListValue.<Party>builder().value(defendantDetails).build()
            );

            DynamicStringListElement listItem = DynamicStringListElement.builder()
                .code("code-1")
                .label("John Doe")
                .build();
            List<DynamicStringListElement> expectedListItems = List.of(listItem);
            when(defendantService.buildDefendantListItems(allDefendants)).thenReturn(expectedListItems);

            EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();
            PCSCase caseData = PCSCase.builder()
                .allDefendants(allDefendants)
                .enforcementOrder(enforcementOrder)
                .build();

            // When
            callStartHandler(caseData);

            // Then
            verify(defendantService).buildDefendantListItems(allDefendants);
            DynamicMultiSelectStringList selectedDefendants =
                    enforcementOrder.getRawWarrantDetails().getSelectedDefendants();
            assertThat(selectedDefendants).isNotNull();
            assertThat(selectedDefendants.getValue()).isEmpty();
            assertThat(selectedDefendants.getListItems()).isEqualTo(expectedListItems);
        }

        @ParameterizedTest(name = "Should handle {0} defendants list")
        @MethodSource("emptyDefendantsScenarios")
        @DisplayName("Should handle empty or null defendants list")
        void shouldHandleEmptyOrNullDefendantsList(
            String scenario,
            List<ListValue<Party>> allDefendants) {
            // Given
            List<DynamicStringListElement> expectedListItems = new ArrayList<>();
            when(defendantService.buildDefendantListItems(allDefendants)).thenReturn(expectedListItems);

            EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();
            PCSCase caseData = PCSCase.builder()
                .allDefendants(allDefendants)
                .enforcementOrder(enforcementOrder)
                .build();

            // When
            callStartHandler(caseData);

            // Then
            verify(defendantService).buildDefendantListItems(allDefendants);
            DynamicMultiSelectStringList selectedDefendants =
                    enforcementOrder.getRawWarrantDetails().getSelectedDefendants();
            assertThat(selectedDefendants).isNotNull();
            assertThat(selectedDefendants.getValue()).isEmpty();
            assertThat(selectedDefendants.getListItems()).isEmpty();
        }

        static Stream<Arguments> emptyDefendantsScenarios() {
            return Stream.of(
                arguments("empty", new ArrayList<>()),
                arguments("null", null)
            );
        }

        @Test
        @DisplayName("Should handle multiple defendants")
        void shouldHandleMultipleDefendants() {
            // Given
            Party defendant1 = Party.builder()
                .firstName("John")
                .lastName("Doe")
                .build();
            Party defendant2 = Party.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();
            List<ListValue<Party>> allDefendants = List.of(
                ListValue.<Party>builder().value(defendant1).build(),
                ListValue.<Party>builder().value(defendant2).build()
            );

            DynamicStringListElement listItem1 = DynamicStringListElement.builder()
                .code("code-1")
                .label("John Doe")
                .build();
            DynamicStringListElement listItem2 = DynamicStringListElement.builder()
                .code("code-2")
                .label("Jane Smith")
                .build();
            List<DynamicStringListElement> expectedListItems = List.of(listItem1, listItem2);
            when(defendantService.buildDefendantListItems(allDefendants)).thenReturn(expectedListItems);

            EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();
            PCSCase caseData = PCSCase.builder()
                .allDefendants(allDefendants)
                .enforcementOrder(enforcementOrder)
                .build();

            // When
            callStartHandler(caseData);

            // Then
            verify(defendantService).buildDefendantListItems(allDefendants);
            DynamicMultiSelectStringList selectedDefendants =
                    enforcementOrder.getRawWarrantDetails().getSelectedDefendants();
            assertThat(selectedDefendants).isNotNull();
            assertThat(selectedDefendants.getValue()).isEmpty();
            assertThat(selectedDefendants.getListItems()).hasSize(2);
            assertThat(selectedDefendants.getListItems()).isEqualTo(expectedListItems);
        }
    }

    @ParameterizedTest
    @MethodSource("enforcementFeeScenarios")
    void shouldSetFeeAmountOnStart(FeeType fee, Function<EnforcementOrder, String> feeGetter) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();

        String expectedFormattedFee = "£" + (1 + FEE_AMOUNT);

        doAnswer(invocation -> {
            PCSCase pcs = invocation.getArgument(0);
            BiConsumer<PCSCase, String> setter = invocation.getArgument(2);
            setter.accept(pcs, expectedFormattedFee);
            return null;
        }).when(feeApplier).applyFeeAmount(
            any(PCSCase.class),
            any(FeeType.class),
            any()
        );

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(feeGetter.apply(result.getEnforcementOrder())).isEqualTo(expectedFormattedFee);
        verify(feeApplier).applyFeeAmount(eq(caseData), eq(fee), any());
    }

    @ParameterizedTest
    @MethodSource("enforcementFeeScenarios")
    void shouldSetDefaultFeeWhenFeeServiceFails(FeeType fee, Function<EnforcementOrder, String> feeGetter) {
        // Given
        PCSCase caseData = PCSCase.builder().enforcementOrder(EnforcementOrder.builder().build()).build();
        String expectedFeesMessage = FeeApplier.UNABLE_TO_RETRIEVE;

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
        assertThat(feeGetter.apply(result.getEnforcementOrder())).isEqualTo(expectedFeesMessage);
        verify(feeApplier).applyFeeAmount(eq(caseData), eq(fee), any());
    }

    @Test
    void shouldAlsoSetWarrantRestTypeIfWarrantExists() {
        // Given
        EnforcementOrder warrantEnforcementOrder = buildEnforcementOrderWithSpecifiedType(WARRANT);
        when(enforcementOrderService.retrieveEnforcementOrder(TEST_CASE_REFERENCE, WARRANT))
                .thenReturn(warrantEnforcementOrder);

        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder().build())
                .build();

        // When
        callStartHandler(caseData);

        // Then
        DynamicStringList enforcementTypes = caseData.getEnforcementOrder().getChooseEnforcementType();
        assertThat(enforcementTypes.getListItems())
                .extracting(DynamicStringListElement::getCode)
                .containsExactly(WARRANT.name(), WRIT.name(), WARRANT_OF_RESTITUTION.name());
        assertThat(caseData.getEnforcementOrder().getWarrantOfRestitutionInfoText())
                .isEqualTo(CommonPageContent.WARRANT_OF_RESTITUTION_INFO_TEXT);
    }

    @Test
    void shouldNotSetWarrantRestTypeIfWarrantDoesntExist() {
        // Given
        when(enforcementOrderService.retrieveEnforcementOrder(TEST_CASE_REFERENCE, WARRANT))
                .thenReturn(null);

        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder().build())
                .build();

        // When
        callStartHandler(caseData);

        // Then
        DynamicStringList enforcementTypes = caseData.getEnforcementOrder().getChooseEnforcementType();
        assertThat(enforcementTypes.getListItems())
                .extracting(DynamicStringListElement::getCode)
                .containsExactly(WARRANT.name(), WRIT.name());
        assertThat(caseData.getEnforcementOrder().getWarrantOfRestitutionInfoText()).isEmpty();
    }

    private static Stream<Arguments> enforcementFeeScenarios() {
        return Stream.of(
            argumentSet(
                "Writ fee",
                FeeType.ENFORCEMENT_WRIT_FEE,
                (Function<EnforcementOrder, String>) EnforcementOrder::getWritFeeAmount
        ),
        argumentSet(
                "Warrant fee",
                FeeType.ENFORCEMENT_WARRANT_FEE,
                (Function<EnforcementOrder, String>) EnforcementOrder::getWarrantFeeAmount
            )
        );
    }
}
