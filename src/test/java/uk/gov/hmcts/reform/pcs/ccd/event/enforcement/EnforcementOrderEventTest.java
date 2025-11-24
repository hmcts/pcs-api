package uk.gov.hmcts.reform.pcs.ccd.event.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcement.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
@ExtendWith(MockitoExtension.class)
class EnforcementOrderEventTest extends BaseEventTest {

    @Mock
    private final AddressFormatter addressFormatter = new AddressFormatter();
    @Mock
    private DefendantService defendantService;
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
        setEventUnderTest(new EnforcementOrderEvent(
            addressFormatter,
            defendantService,
            violentAggressiveRiskPage,
            verbalOrWrittenThreatsRiskPage,
            protestorGroupRiskPage,
            policeOrSocialServicesRiskPage,
            firearmsPossessionRiskPage,
            criminalAntisocialRiskPage,
            aggressiveAnimalsRiskPage,
            propertyAccessDetailsPage,
            vulnerableAdultsChildrenPage,
            additionalInformationPage
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
        List<ListValue<DefendantDetails>> allDefendants = List.of(
            ListValue.<DefendantDetails>builder().value(defendantDetails).build()
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
        assertThat(result.getFormattedPropertyAddress()).isEqualTo(expectedFormattedPropertyAddress);
        assertThat(result.getAllDefendants()).hasSize(1);
        assertThat(result.getDefendant1().getFirstName()).isEqualTo(firstName);
        assertThat(result.getDefendant1().getLastName()).isEqualTo(lastName);
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
            DefendantDetails defendantDetails = DefendantDetails.builder()
                .firstName(firstName)
                .lastName(lastName)
                .build();
            List<ListValue<DefendantDetails>> allDefendants = List.of(
                ListValue.<DefendantDetails>builder().value(defendantDetails).build()
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

            EnforcementOrderEvent event = new EnforcementOrderEvent(
                addressFormatter,
                defendantService,
                violentAggressiveRiskPage,
                verbalOrWrittenThreatsRiskPage,
                protestorGroupRiskPage,
                policeOrSocialServicesRiskPage,
                firearmsPossessionRiskPage,
                criminalAntisocialRiskPage,
                aggressiveAnimalsRiskPage,
                propertyAccessDetailsPage,
                vulnerableAdultsChildrenPage,
                additionalInformationPage
            );

            // When
            event.populateDefendantSelectionList(caseData);

            // Then
            verify(defendantService).buildDefendantListItems(allDefendants);
            DynamicMultiSelectStringList selectedDefendants = enforcementOrder.getSelectedDefendants();
            assertThat(selectedDefendants).isNotNull();
            assertThat(selectedDefendants.getValue()).isEmpty();
            assertThat(selectedDefendants.getListItems()).isEqualTo(expectedListItems);
        }

        @ParameterizedTest(name = "Should handle {0} defendants list")
        @MethodSource("emptyDefendantsScenarios")
        @DisplayName("Should handle empty or null defendants list")
        void shouldHandleEmptyOrNullDefendantsList(
            String scenarioName,
            List<ListValue<DefendantDetails>> allDefendants) {
            // Given
            List<DynamicStringListElement> expectedListItems = new ArrayList<>();
            when(defendantService.buildDefendantListItems(allDefendants)).thenReturn(expectedListItems);

            EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();
            PCSCase caseData = PCSCase.builder()
                .allDefendants(allDefendants)
                .enforcementOrder(enforcementOrder)
                .build();

            EnforcementOrderEvent event = new EnforcementOrderEvent(
                addressFormatter,
                defendantService,
                violentAggressiveRiskPage,
                verbalOrWrittenThreatsRiskPage,
                protestorGroupRiskPage,
                policeOrSocialServicesRiskPage,
                firearmsPossessionRiskPage,
                criminalAntisocialRiskPage,
                aggressiveAnimalsRiskPage,
                propertyAccessDetailsPage,
                vulnerableAdultsChildrenPage,
                additionalInformationPage
            );

            // When
            event.populateDefendantSelectionList(caseData);

            // Then
            verify(defendantService).buildDefendantListItems(allDefendants);
            DynamicMultiSelectStringList selectedDefendants = enforcementOrder.getSelectedDefendants();
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
            DefendantDetails defendant1 = DefendantDetails.builder()
                .firstName("John")
                .lastName("Doe")
                .build();
            DefendantDetails defendant2 = DefendantDetails.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();
            List<ListValue<DefendantDetails>> allDefendants = List.of(
                ListValue.<DefendantDetails>builder().value(defendant1).build(),
                ListValue.<DefendantDetails>builder().value(defendant2).build()
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

            EnforcementOrderEvent event = new EnforcementOrderEvent(
                addressFormatter,
                defendantService,
                violentAggressiveRiskPage,
                verbalOrWrittenThreatsRiskPage,
                protestorGroupRiskPage,
                policeOrSocialServicesRiskPage,
                firearmsPossessionRiskPage,
                criminalAntisocialRiskPage,
                aggressiveAnimalsRiskPage,
                propertyAccessDetailsPage,
                vulnerableAdultsChildrenPage,
                additionalInformationPage
            );

            // When
            event.populateDefendantSelectionList(caseData);

            // Then
            verify(defendantService).buildDefendantListItems(allDefendants);
            DynamicMultiSelectStringList selectedDefendants = enforcementOrder.getSelectedDefendants();
            assertThat(selectedDefendants).isNotNull();
            assertThat(selectedDefendants.getValue()).isEmpty();
            assertThat(selectedDefendants.getListItems()).hasSize(2);
            assertThat(selectedDefendants.getListItems()).isEqualTo(expectedListItems);
        }
    }

}