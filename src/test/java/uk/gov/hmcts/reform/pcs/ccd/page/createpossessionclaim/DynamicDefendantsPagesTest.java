package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicDefendantsPagesTest extends BasePageTest {

    private Event<PCSCase, UserRole, State> event;
    private DynamicDefendantsPages dynamicDefendantsPages;

    @Mock
    private AddressValidator addressValidator;

    @BeforeEach
    void setUp() {
        dynamicDefendantsPages = new DynamicDefendantsPages(addressValidator);
        setPageUnderTest(dynamicDefendantsPages);
    }

    @Test
    void shouldCreateAllDefendantPages() {
        // Given & When
        // The DynamicDefendantsPages is created in setUp()

        // Then
        // Verify that the page configuration was created successfully
        assertThat(dynamicDefendantsPages).isNotNull();
        // Test that the table generation works for all defendant counts
        for (int i = 1; i <= 25; i++) {
            String table = dynamicDefendantsPages.buildDefendantsSummaryTable(i);
            assertThat(table).isNotNull();
            assertThat(table).isNotEmpty();
        }
    }

    @Test
    void shouldBuildDefendantsSummaryTableForValidCounts() {
        // Given & When & Then
        for (int i = 1; i <= 25; i++) {
            String table = dynamicDefendantsPages.buildDefendantsSummaryTable(i);
            
            // Verify table structure
            assertThat(table).contains("<table class=\"govuk-table\">");
            assertThat(table).contains("<caption class=\"govuk-table__caption govuk-table__caption--m\">"
                + "Defendants</caption>");
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

        // When - Test the mid-event directly
        AboutToStartOrSubmitResponse<PCSCase, State> response = dynamicDefendantsPages
            .midEventForDefendant(caseDetails, null, 1);

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
        AboutToStartOrSubmitResponse<PCSCase, State> response = dynamicDefendantsPages
            .midEventForDefendant(caseDetails, null, 1);

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
        AboutToStartOrSubmitResponse<PCSCase, State> response = dynamicDefendantsPages
            .midEventForDefendant(caseDetails, null, 1);

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
        AboutToStartOrSubmitResponse<PCSCase, State> response = dynamicDefendantsPages
            .midEventForDefendant(caseDetails, null, 1);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getDefendant1()).isNull();
    }

    @Test
    void shouldReturnValidationErrorsWhenAddressInvalid() {
        // Given
        AddressUK correspondenceAddress = mock(AddressUK.class);

        DefendantDetails defendantsDetails = DefendantDetails.builder()
            .addressSameAsPossession(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.YES)
            .correspondenceAddress(correspondenceAddress)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendantsDetails)
            .build();

        List<String> expectedValidationErrors = List.of("error 1", "error 2");
        when(addressValidator.validateAddressFields(correspondenceAddress)).thenReturn(expectedValidationErrors);

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = dynamicDefendantsPages
            .midEventForDefendant(caseDetails, null, 1);

        // Then
        assertThat(response.getErrors()).isEqualTo(expectedValidationErrors);
    }


    @Test
    void shouldGenerateCorrectHtmlTableRow() {
        // Given & When
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
        assertThat(table).contains("<caption class=\"govuk-table__caption govuk-table__caption--m\">"
            + "Defendants</caption>");
        assertThat(table).contains("<thead class=\"govuk-table__head\">");
        assertThat(table).contains("<tbody class=\"govuk-table__body\">");
        assertThat(table).contains("</table>");
        
        // Verify column headers
        assertThat(table).contains("<th scope=\"col\" class=\"govuk-table__header\">Defendant</th>");
        assertThat(table).contains("<th scope=\"col\" class=\"govuk-table__header\">Defendant name</th>");
        assertThat(table).contains("<th scope=\"col\" class=\"govuk-table__header\">"
            + "Defendant correspondence address</th>");
        assertThat(table).contains("<th scope=\"col\" class=\"govuk-table__header\">Defendant email address</th>");
    }

    @Test
    void shouldGetDefendantByIndexForValidIndexes() {
        // Test all valid indexes (1-25)
        for (int i = 1; i <= 25; i++) {
            // Given
            DefendantDetails defendant = DefendantDetails.builder()
                .firstName("Test" + i)
                .lastName("User" + i)
                .build();

            PCSCase caseData = PCSCase.builder().build();
            
            // Set the specific defendant field based on index
            switch (i) {
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

            // When
            DefendantDetails result = dynamicDefendantsPages.getDefendantByIndex(caseData, i);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo("Test" + i);
            assertThat(result.getLastName()).isEqualTo("User" + i);
        }
    }

    @Test
    void shouldReturnNullForInvalidIndexes() {
        // Given
        PCSCase caseData = PCSCase.builder().build();

        // Test invalid indexes
        int[] invalidIndexes = {0, -1, 26, 100, Integer.MIN_VALUE, Integer.MAX_VALUE};

        for (int invalidIndex : invalidIndexes) {
            // When
            DefendantDetails result = dynamicDefendantsPages.getDefendantByIndex(caseData, invalidIndex);

            // Then
            assertThat(result).isNull();
        }
    }

    @Test
    void shouldReturnNullForNullCaseData() {
        // When
        DefendantDetails result = dynamicDefendantsPages.getDefendantByIndex(null, 1);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForDefendantNotSet() {
        // Given
        PCSCase caseData = PCSCase.builder().build(); // No defendants set

        // When
        DefendantDetails result = dynamicDefendantsPages.getDefendantByIndex(caseData, 1);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldHandleEdgeCasesForDefendantIndexes() {
        // Given
        DefendantDetails defendant1 = DefendantDetails.builder()
            .firstName("First")
            .lastName("Defendant")
            .build();
        DefendantDetails defendant25 = DefendantDetails.builder()
            .firstName("Last")
            .lastName("Defendant")
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendant1)
            .defendant25(defendant25)
            .build();

        // Test first defendant
        DefendantDetails result1 = dynamicDefendantsPages.getDefendantByIndex(caseData, 1);
        assertThat(result1).isNotNull();
        assertThat(result1.getFirstName()).isEqualTo("First");

        // Test last defendant
        DefendantDetails result25 = dynamicDefendantsPages.getDefendantByIndex(caseData, 25);
        assertThat(result25).isNotNull();
        assertThat(result25.getFirstName()).isEqualTo("Last");

        // Test middle defendant (not set)
        DefendantDetails result13 = dynamicDefendantsPages.getDefendantByIndex(caseData, 13);
        assertThat(result13).isNull();
    }





}
