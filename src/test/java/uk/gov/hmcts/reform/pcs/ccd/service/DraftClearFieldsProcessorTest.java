package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DraftClearFieldsProcessorTest {

    private DraftClearFieldsProcessor processor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        processor = new DraftClearFieldsProcessor(objectMapper);
    }

    @Test
    void extractClearFieldsContext_fromPossessionClaimResponse() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder()
                .clearFields(List.of("defendantResponses.disputeClaimDetails", "defendantResponses.someField"))
                .build())
            .build();

        // When
        Optional<ClearFieldsContext> result = processor.extractClearFieldsContext(pcsCase);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getRootNodeName()).isEqualTo("possessionClaimResponse");
        assertThat(result.get().getClearFields()).containsExactly(
            "defendantResponses.disputeClaimDetails",
            "defendantResponses.someField"
        );
    }

    @Test
    void extractClearFieldsContext_fromRootPCSCase() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .clearFields(List.of("claimantInformation.someField", "defendantsDetails"))
            .build();

        // When
        Optional<ClearFieldsContext> result = processor.extractClearFieldsContext(pcsCase);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getRootNodeName()).isEmpty();
        assertThat(result.get().getClearFields()).containsExactly(
            "claimantInformation.someField",
            "defendantsDetails"
        );
    }

    @Test
    void extractClearFieldsContext_prioritizesPossessionClaimResponseOverRoot() {
        // Given - both have clearFields, possessionClaimResponse should win
        PCSCase pcsCase = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder()
                .clearFields(List.of("field1"))
                .build())
            .clearFields(List.of("field2"))
            .build();

        // When
        Optional<ClearFieldsContext> result = processor.extractClearFieldsContext(pcsCase);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getRootNodeName()).isEqualTo("possessionClaimResponse");
        assertThat(result.get().getClearFields()).containsExactly("field1");
    }

    @Test
    void applyClearFields_clearsRootFieldsForResumePossessionClaim() throws Exception {
        // Given
        String json = """
            {
                "mediationAttempted": "NO",
                "mediationAttemptedDetails": "Sample mediation input data",
                "settlementAttempted": "NO",
                "clearFields": ["mediationAttemptedDetails"]
            }
            """;

        ClearFieldsContext context = new ClearFieldsContext(
            "",
            List.of("mediationAttemptedDetails")
        );

        // When
        String result = processor.applyClearFields(json, context);

        // Then
        assertThat(result).doesNotContain("mediationAttemptedDetails");
        assertThat(result).contains("mediationAttempted");
        assertThat(result).contains("settlementAttempted");
        assertThat(result).doesNotContain("clearFields");
    }

    @Test
    void extractClearFieldsContext_returnsEmpty_whenNoClearFields() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder().build())
            .build();

        // When
        Optional<ClearFieldsContext> result = processor.extractClearFieldsContext(pcsCase);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void extractClearFieldsContext_returnsEmpty_whenNotPCSCase() {
        // Given
        String notPcsCase = "some other object";

        // When
        Optional<ClearFieldsContext> result = processor.extractClearFieldsContext(notPcsCase);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void applyClearFields_clearsNestedFields() throws Exception {
        // Given
        String json = """
            {
                "possessionClaimResponse": {
                    "defendantResponses": {
                        "disputeClaimDetails": "This should be removed",
                        "someOtherField": "This should stay"
                    },
                    "clearFields": ["defendantResponses.disputeClaimDetails"]
                }
            }
            """;

        ClearFieldsContext context = new ClearFieldsContext(
            "possessionClaimResponse",
            List.of("defendantResponses.disputeClaimDetails")
        );

        // When
        String result = processor.applyClearFields(json, context);

        // Then
        assertThat(result).doesNotContain("disputeClaimDetails");
        assertThat(result).doesNotContain("This should be removed");
        assertThat(result).contains("someOtherField");
        assertThat(result).contains("This should stay");
        assertThat(result).doesNotContain("clearFields");
    }

    @Test
    void applyClearFields_clearsMultipleFields() throws Exception {
        // Given
        String json = """
            {
                "possessionClaimResponse": {
                    "defendantResponses": {
                        "field1": "value1",
                        "field2": "value2",
                        "field3": "value3"
                    },
                    "clearFields": ["defendantResponses.field1", "defendantResponses.field2"]
                }
            }
            """;

        ClearFieldsContext context = new ClearFieldsContext(
            "possessionClaimResponse",
            List.of("defendantResponses.field1", "defendantResponses.field2")
        );

        // When
        String result = processor.applyClearFields(json, context);

        // Then
        assertThat(result).doesNotContain("field1");
        assertThat(result).doesNotContain("field2");
        assertThat(result).contains("field3");
        assertThat(result).doesNotContain("clearFields");
    }

    @Test
    void applyClearFields_handlesRootLevelClearFields() throws Exception {
        // Given
        String json = """
            {
                "claimantInformation": {
                    "claimantName": "Test"
                },
                "someField": "Remove me",
                "clearFields": ["someField"]
            }
            """;

        ClearFieldsContext context = new ClearFieldsContext(
            "",
            List.of("someField")
        );

        // When
        String result = processor.applyClearFields(json, context);

        // Then
        assertThat(result).doesNotContain("someField");
        assertThat(result).contains("claimantInformation");
        assertThat(result).doesNotContain("clearFields");
    }

    @Test
    void applyClearFields_safelyHandlesMissingPaths() throws Exception {
        // Given
        String json = """
            {
                "possessionClaimResponse": {
                    "defendantResponses": {
                        "existingField": "value"
                    },
                    "clearFields": ["defendantResponses.nonExistentField", "nonExistentParent.field"]
                }
            }
            """;

        ClearFieldsContext context = new ClearFieldsContext(
            "possessionClaimResponse",
            List.of("defendantResponses.nonExistentField", "nonExistentParent.field")
        );

        // When
        String result = processor.applyClearFields(json, context);

        // Then - should not throw exception, should just continue
        assertThat(result).contains("existingField");
        assertThat(result).doesNotContain("clearFields");
    }

    @Test
    void applyClearFields_forEnforcementOrder() throws Exception {
        // Given - enforcement fields are at root level due to @JsonUnwrapped
        String json = """
            {
                "writDetails": {
                    "applicantAddress": "123 Test St"
                },
                "warrantDetails": {
                    "someField": "value"
                },
                "clearFields": ["writDetails.applicantAddress"]
            }
            """;

        ClearFieldsContext context = new ClearFieldsContext(
            "",
            List.of("writDetails.applicantAddress")
        );

        // When
        String result = processor.applyClearFields(json, context);

        // Then
        assertThat(result).doesNotContain("applicantAddress");
        assertThat(result).contains("warrantDetails");
        assertThat(result).doesNotContain("clearFields");
    }

    @Test
    void applyClearFields_forEnforcementOrder_removesStaleConditionalFields() throws Exception {
        // Given - user toggled a Yes/No, stale conditional field needs clearing
        String json = """
            {
                "selectEnforcementType": "WARRANT",
                "warrantApplicantName": "John Smith",
                "warrantApplicantAddress": "123 Test St",
                "clearFields": ["warrantApplicantAddress"]
            }
            """;

        ClearFieldsContext context = new ClearFieldsContext(
            "",
            List.of("warrantApplicantAddress")
        );

        // When
        String result = processor.applyClearFields(json, context);

        // Then
        assertThat(result).doesNotContain("warrantApplicantAddress");
        assertThat(result).contains("warrantApplicantName");
        assertThat(result).contains("selectEnforcementType");
        assertThat(result).doesNotContain("clearFields");
    }

    @Test
    void applyClearFields_handlesDeeplyNestedPaths() throws Exception {
        // Given
        String json = """
            {
                "possessionClaimResponse": {
                    "defendantResponses": {
                        "householdCircumstances": {
                            "incomeFromJobsAmount": "1000",
                            "incomeFromJobsFrequency": "MONTHLY"
                        }
                    },
                    "clearFields": [
                        "defendantResponses.householdCircumstances.incomeFromJobsAmount",
                        "defendantResponses.householdCircumstances.incomeFromJobsFrequency"
                    ]
                }
            }
            """;

        ClearFieldsContext context = new ClearFieldsContext(
            "possessionClaimResponse",
            List.of(
                "defendantResponses.householdCircumstances.incomeFromJobsAmount",
                "defendantResponses.householdCircumstances.incomeFromJobsFrequency"
            )
        );

        // When
        String result = processor.applyClearFields(json, context);

        // Then
        assertThat(result).doesNotContain("incomeFromJobsAmount");
        assertThat(result).doesNotContain("incomeFromJobsFrequency");
        assertThat(result).contains("householdCircumstances");
        assertThat(result).doesNotContain("clearFields");
    }
}
