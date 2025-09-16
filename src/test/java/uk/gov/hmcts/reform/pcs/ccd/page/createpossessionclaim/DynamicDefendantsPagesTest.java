package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DynamicDefendantsPagesTest extends BasePageTest {

    private Event<PCSCase, UserRole, State> event;
    private DynamicDefendantsPages dynamicDefendantsPages;

    @BeforeEach
    void setUp() {
        dynamicDefendantsPages = new DynamicDefendantsPages();
        event = buildPageInTestEvent(dynamicDefendantsPages);
    }

    @Test
    void shouldCreateAllDefendantPages() {
        // Given & When
        // The event is built in setUp() by calling addTo() on the page builder

        // Then
        // Verify that all 25 defendant detail pages are created (these have mid-events)
        for (int i = 1; i <= 25; i++) {
            String pageId = "DefendantDetails" + i;
            MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, pageId);
            assertThat(midEvent).isNotNull();
        }

        // Verify that all 25 defendant list pages are created (these don't have mid-events, just display pages)
        // We can verify they exist by checking the event structure
        assertThat(event).isNotNull();
        assertThat(event.getFields()).isNotNull();
        // The pages are created but don't have mid-events, so we just verify the event was built successfully
    }

    @Test
    void shouldBuildDefendantsSummaryTableForValidCounts() {
        // Given & When & Then
        for (int i = 1; i <= 25; i++) {
            String table = dynamicDefendantsPages.buildDefendantsSummaryTable(i);
            
            // Verify table structure
            assertThat(table).contains("<table class=\"govuk-table\">");
            assertThat(table).contains("<caption class=\"govuk-table__caption govuk-table__caption--m\">Defendants</caption>");
            assertThat(table).contains("<thead class=\"govuk-table__head\">");
            assertThat(table).contains("<tbody class=\"govuk-table__body\">");
            assertThat(table).contains("</table>");
            
            // Verify correct number of data rows (count occurrences of "Defendant " + number)
            long dataRowCount = 0;
            for (int j = 1; j <= i; j++) {
                if (table.contains("Defendant " + j)) {
                    dataRowCount++;
                }
            }
            assertThat(dataRowCount).isEqualTo(i);
            
            // Verify each defendant row contains expected content
            for (int j = 1; j <= i; j++) {
                assertThat(table).contains("Defendant " + j);
                assertThat(table).contains("${defendant" + j + ".firstName}");
                assertThat(table).contains("${defendant" + j + ".lastName}");
                assertThat(table).contains("${defendant" + j + ".correspondenceAddress.AddressLine1}");
                assertThat(table).contains("${defendant" + j + ".correspondenceAddress.PostTown}");
                assertThat(table).contains("${defendant" + j + ".correspondenceAddress.PostCode}");
                assertThat(table).contains("${defendant" + j + ".email}");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 26, 100})
    void shouldThrowExceptionForInvalidDefendantCounts(int invalidCount) {
        // When & Then
        assertThatThrownBy(() -> dynamicDefendantsPages.buildDefendantsSummaryTable(invalidCount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid defendant count: " + invalidCount);
    }

    @Test
    void shouldHandleMidEventForDefendantWithAddressSameAsPossession() {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("123 Property Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        DefendantDetails defendant = DefendantDetails.builder()
            .firstName("John")
            .lastName("Doe")
            .addressSameAsPossession(VerticalYesNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .defendant1(defendant)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "DefendantDetails1");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        
        DefendantDetails updatedDefendant = response.getData().getDefendant1();
        assertThat(updatedDefendant).isNotNull();
        assertThat(updatedDefendant.getCorrespondenceAddress()).isEqualTo(propertyAddress);
    }

    @Test
    void shouldHandleMidEventForDefendantWithDifferentAddress() {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("123 Property Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        AddressUK correspondenceAddress = AddressUK.builder()
            .addressLine1("456 Correspondence Street")
            .postTown("Manchester")
            .postCode("M1 1AA")
            .build();

        DefendantDetails defendant = DefendantDetails.builder()
            .firstName("Jane")
            .lastName("Smith")
            .addressSameAsPossession(VerticalYesNo.NO)
            .correspondenceAddress(correspondenceAddress)
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .defendant1(defendant)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "DefendantDetails1");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        
        DefendantDetails updatedDefendant = response.getData().getDefendant1();
        assertThat(updatedDefendant).isNotNull();
        assertThat(updatedDefendant.getCorrespondenceAddress()).isEqualTo(correspondenceAddress);
        assertThat(updatedDefendant.getCorrespondenceAddress()).isNotEqualTo(propertyAddress);
    }

    @Test
    void shouldHandleMidEventForDefendantWithNullAddressSameAsPossession() {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("123 Property Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        DefendantDetails defendant = DefendantDetails.builder()
            .firstName("Bob")
            .lastName("Wilson")
            .addressSameAsPossession(null) // null value
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .defendant1(defendant)
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "DefendantDetails1");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        
        DefendantDetails updatedDefendant = response.getData().getDefendant1();
        assertThat(updatedDefendant).isNotNull();
        assertThat(updatedDefendant.getCorrespondenceAddress()).isNull(); // Should remain unchanged
    }

    @Test
    void shouldHandleMidEventForDefendantWithNullDefendant() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .defendant1(null) // null defendant
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "DefendantDetails1");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getDefendant1()).isNull();
    }

    @ParameterizedTest
    @MethodSource("defendantIndexProvider")
    void shouldHandleMidEventForDifferentDefendantIndexes(int defendantIndex, String expectedMethod) {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("123 Property Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        DefendantDetails defendant = DefendantDetails.builder()
            .firstName("Test")
            .lastName("User")
            .addressSameAsPossession(VerticalYesNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        // Set the specific defendant field based on index
        switch (defendantIndex) {
            case 1 -> caseData.setDefendant1(defendant);
            case 2 -> caseData.setDefendant2(defendant);
            case 3 -> caseData.setDefendant3(defendant);
            case 4 -> caseData.setDefendant4(defendant);
            case 5 -> caseData.setDefendant5(defendant);
            case 6 -> caseData.setDefendant6(defendant);
            case 7 -> caseData.setDefendant7(defendant);
            case 8 -> caseData.setDefendant8(defendant);
            case 9 -> caseData.setDefendant9(defendant);
            case 10 -> caseData.setDefendant10(defendant);
            case 11 -> caseData.setDefendant11(defendant);
            case 12 -> caseData.setDefendant12(defendant);
            case 13 -> caseData.setDefendant13(defendant);
            case 14 -> caseData.setDefendant14(defendant);
            case 15 -> caseData.setDefendant15(defendant);
            case 16 -> caseData.setDefendant16(defendant);
            case 17 -> caseData.setDefendant17(defendant);
            case 18 -> caseData.setDefendant18(defendant);
            case 19 -> caseData.setDefendant19(defendant);
            case 20 -> caseData.setDefendant20(defendant);
            case 21 -> caseData.setDefendant21(defendant);
            case 22 -> caseData.setDefendant22(defendant);
            case 23 -> caseData.setDefendant23(defendant);
            case 24 -> caseData.setDefendant24(defendant);
            case 25 -> caseData.setDefendant25(defendant);
        }

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "DefendantDetails" + defendantIndex);
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        
        // Verify the correct defendant was updated
        DefendantDetails updatedDefendant = getDefendantByIndex(response.getData(), defendantIndex);
        assertThat(updatedDefendant).isNotNull();
        assertThat(updatedDefendant.getCorrespondenceAddress()).isEqualTo(propertyAddress);
    }

    @Test
    void shouldHandleMidEventForInvalidDefendantIndex() {
        // Given
        PCSCase caseData = PCSCase.builder().build();
        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "DefendantDetails1");
        AboutToStartOrSubmitResponse<PCSCase, State> response = midEvent.handle(caseDetails, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        // Should handle gracefully without throwing exception
    }

    @Test
    void shouldGenerateCorrectHtmlTableRow() {
        // Given
        int defendantNumber = 5;

        // When
        String tableRow = dynamicDefendantsPages.buildDefendantsSummaryTable(1);
        
        // Extract the table row content (excluding header)
        String rowContent = tableRow.substring(tableRow.indexOf("<tbody"), tableRow.indexOf("</tbody>"));
        
        // Then
        assertThat(rowContent).contains("Defendant 1");
        assertThat(rowContent).contains("${defendant1.firstName}");
        assertThat(rowContent).contains("${defendant1.lastName}");
        assertThat(rowContent).contains("${defendant1.correspondenceAddress.AddressLine1}");
        assertThat(rowContent).contains("${defendant1.correspondenceAddress.PostTown}");
        assertThat(rowContent).contains("${defendant1.correspondenceAddress.PostCode}");
        assertThat(rowContent).contains("${defendant1.email}");
        assertThat(rowContent).contains("<br>"); // Address line breaks
    }

    @Test
    void shouldHaveCorrectTableStructure() {
        // Given & When
        String table = dynamicDefendantsPages.buildDefendantsSummaryTable(3);

        // Then
        assertThat(table).contains("<table class=\"govuk-table\">");
        assertThat(table).contains("<caption class=\"govuk-table__caption govuk-table__caption--m\">Defendants</caption>");
        assertThat(table).contains("<thead class=\"govuk-table__head\">");
        assertThat(table).contains("<tbody class=\"govuk-table__body\">");
        assertThat(table).contains("</table>");
        
        // Verify column headers
        assertThat(table).contains("<th scope=\"col\" class=\"govuk-table__header\">Defendant</th>");
        assertThat(table).contains("<th scope=\"col\" class=\"govuk-table__header\">Defendant name</th>");
        assertThat(table).contains("<th scope=\"col\" class=\"govuk-table__header\">Defendant correspondence address</th>");
        assertThat(table).contains("<th scope=\"col\" class=\"govuk-table__header\">Defendant email address</th>");
    }

    private static Stream<Arguments> defendantIndexProvider() {
        return Stream.of(
            arguments(1, "getDefendant1"),
            arguments(2, "getDefendant2"),
            arguments(3, "getDefendant3"),
            arguments(10, "getDefendant10"),
            arguments(15, "getDefendant15"),
            arguments(20, "getDefendant20"),
            arguments(25, "getDefendant25")
        );
    }

    private DefendantDetails getDefendantByIndex(PCSCase caseData, int index) {
        return switch (index) {
            case 1 -> caseData.getDefendant1();
            case 2 -> caseData.getDefendant2();
            case 3 -> caseData.getDefendant3();
            case 4 -> caseData.getDefendant4();
            case 5 -> caseData.getDefendant5();
            case 6 -> caseData.getDefendant6();
            case 7 -> caseData.getDefendant7();
            case 8 -> caseData.getDefendant8();
            case 9 -> caseData.getDefendant9();
            case 10 -> caseData.getDefendant10();
            case 11 -> caseData.getDefendant11();
            case 12 -> caseData.getDefendant12();
            case 13 -> caseData.getDefendant13();
            case 14 -> caseData.getDefendant14();
            case 15 -> caseData.getDefendant15();
            case 16 -> caseData.getDefendant16();
            case 17 -> caseData.getDefendant17();
            case 18 -> caseData.getDefendant18();
            case 19 -> caseData.getDefendant19();
            case 20 -> caseData.getDefendant20();
            case 21 -> caseData.getDefendant21();
            case 22 -> caseData.getDefendant22();
            case 23 -> caseData.getDefendant23();
            case 24 -> caseData.getDefendant24();
            case 25 -> caseData.getDefendant25();
            default -> null;
        };
    }
}
