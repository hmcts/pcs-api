package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PeopleYouWantToEvictPage tests")
class PeopleYouWantToEvictPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new PeopleYouWantToEvictPage());
    }

    @Nested
    @DisplayName("Mid-event callback tests")
    class MidEventCallbackTests {

        @Test
        @DisplayName("Should return error when no defendants are selected")
        void shouldReturnErrorWhenNoDefendantsSelected() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .warrantDetails(WarrantDetails.builder()
                        .selectedDefendants(DynamicMultiSelectStringList.builder()
                            .value(List.of())
                            .build())
                        .build())
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors().getFirst())
                .contains("Please select at least one person you want to evict");
        }

        @Test
        @DisplayName("Should return error when selectedDefendants is null")
        void shouldReturnErrorWhenSelectedDefendantsIsNull() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .warrantDetails(WarrantDetails.builder()
                        .selectedDefendants(null)
                        .build())
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getErrors()).isNotEmpty();
            assertThat(response.getErrors().getFirst())
                .contains("Please select at least one person you want to evict");
        }

        @Test
        @DisplayName("Should accept valid selection with one defendant")
        void shouldAcceptValidSelectionWithOneDefendant() {
            // Given
            String defendantCode = UUID.randomUUID().toString();
            DynamicStringListElement element1 = DynamicStringListElement.builder()
                .code(defendantCode)
                .label("John Doe")
                .build();
            PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .warrantDetails(WarrantDetails.builder()
                        .selectedDefendants(DynamicMultiSelectStringList.builder()
                            .value(List.of(element1))
                            .listItems(List.of(element1))
                            .build())
                        .build())
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getErrors()).isNullOrEmpty();
            DynamicMultiSelectStringList selected = 
                response.getData().getEnforcementOrder().getWarrantDetails().getSelectedDefendants();
            assertThat(selected).isNotNull();
            assertThat(selected.getValue()).hasSize(1);
            assertThat(selected.getValue().getFirst().getCode()).isEqualTo(defendantCode);
            assertThat(selected.getValue().getFirst().getLabel()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should accept valid selection with multiple defendants")
        void shouldAcceptValidSelectionWithMultipleDefendants() {
            // Given
            String defendantCode1 = UUID.randomUUID().toString();
            String defendantCode2 = UUID.randomUUID().toString();
            DynamicStringListElement element1 = DynamicStringListElement.builder()
                .code(defendantCode1)
                .label("John Doe")
                .build();
            DynamicStringListElement element2 = DynamicStringListElement.builder()
                .code(defendantCode2)
                .label("Jane Smith")
                .build();
            PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .warrantDetails(WarrantDetails.builder()
                        .selectedDefendants(DynamicMultiSelectStringList.builder()
                            .value(List.of(element1, element2))
                            .listItems(List.of(element1, element2))
                            .build())
                        .build())
                    .build())
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getErrors()).isNullOrEmpty();
            DynamicMultiSelectStringList selected = 
                response.getData().getEnforcementOrder().getWarrantDetails().getSelectedDefendants();
            assertThat(selected).isNotNull();
            assertThat(selected.getValue()).hasSize(2);
            assertThat(selected.getValue().stream()
                .map(DynamicStringListElement::getCode)
                .toList())
                .containsExactlyInAnyOrder(defendantCode1, defendantCode2);
        }
    }
}

