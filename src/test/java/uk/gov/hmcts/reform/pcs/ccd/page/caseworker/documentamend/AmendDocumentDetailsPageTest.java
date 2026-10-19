package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.documentamend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AmendDocumentDetailsPageTest extends BasePageTest {

    private static final Clock UK_CLOCK = Clock.fixed(
        Instant.parse("2026-07-16T10:00:00Z"),
        ZoneId.of("Europe/London")
    );

    @BeforeEach
    void setUp() {
        setPageUnderTest(new AmendDocumentDetailsPage(UK_CLOCK));
    }

    @Test
    void shouldReturnErrorWhenIssueDateIsInFuture() {
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .issueDate(LocalDate.of(2026, 7, 17))
                .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride()).isEqualTo("Issue date must be today or in the past");
    }

    @Test
    void shouldAllowIssueDateOfTodayOrPast() {
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .issueDate(LocalDate.of(2026, 7, 16))
                .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride()).isNull();
    }

    @Test
    void shouldPersistSelectedCodesForLaterPageRebuilds() {
        UUID partyId = UUID.randomUUID();
        PCSCase caseData = PCSCase.builder()
            .documentAmendDetails(DocumentAmendDetails.builder()
                .relatedParty(DynamicList.builder()
                    .value(DynamicListElement.builder().code(partyId).build())
                    .build())
                .relatedSubmission(DynamicStringList.builder()
                    .value(DynamicStringListElement.builder().code("GENAPP:123").build())
                    .build())
                .relatedSubmissionsDocumentType(DynamicStringList.builder()
                    .value(DynamicStringListElement.builder().code("WITNESS_STATEMENT").build())
                    .build())
                .standaloneDocumentType(DynamicStringList.builder()
                    .value(DynamicStringListElement.builder().code("RENT_STATEMENT").build())
                    .build())
                .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        DocumentAmendDetails responseDetails = response.getData().getDocumentAmendDetails();
        assertThat(responseDetails.getRelatedPartyCode()).isEqualTo(partyId.toString());
        assertThat(responseDetails.getRelatedSubmissionCode()).isEqualTo("GENAPP:123");
        assertThat(responseDetails.getRelatedSubmissionsDocumentTypeCode()).isEqualTo("WITNESS_STATEMENT");
        assertThat(responseDetails.getStandaloneDocumentTypeCode()).isEqualTo("RENT_STATEMENT");
    }

    @Test
    void shouldRetainOnlyFieldsWithShowConditions() {
        Map<String, Field<?, ?, ?, ?>> fields = event.getFields().getFields().stream()
            .map(fieldBuilder -> (Field<?, ?, ?, ?>) fieldBuilder.build())
            .collect(Collectors.toMap(Field::getId, Function.identity(), (first, second) -> first));

        assertRetainsHiddenValue(fields, "documentAmend_RelatedSubmission");
        assertRetainsHiddenValue(fields, "documentAmend_RelatedSubmissionsDocumentType");
        assertRetainsHiddenValue(fields, "documentAmend_StandaloneDocumentType");
        assertRetainsHiddenValue(fields, "documentAmend_RelatedPartyCode");
        assertRetainsHiddenValue(fields, "documentAmend_RelatedSubmissionCode");
        assertRetainsHiddenValue(fields, "documentAmend_RelatedSubmissionsDocumentTypeCode");
        assertRetainsHiddenValue(fields, "documentAmend_StandaloneDocumentTypeCode");
        assertDoesNotRetainHiddenValue(fields, "documentAmend_AmendedFileName");
        assertDoesNotRetainHiddenValue(fields, "documentAmend_IssueDate");
        assertDoesNotRetainHiddenValue(fields, "documentAmend_RelatedParty");
    }

    private void assertRetainsHiddenValue(Map<String, Field<?, ?, ?, ?>> fields, String fieldId) {
        assertThat(fields.get(fieldId))
            .withFailMessage("Expected %s to be configured", fieldId)
            .isNotNull();
        assertThat(fields.get(fieldId).isRetainHiddenValue()).isTrue();
        assertThat(fields.get(fieldId).getShowCondition()).isNotBlank();
    }

    private void assertDoesNotRetainHiddenValue(Map<String, Field<?, ?, ?, ?>> fields, String fieldId) {
        assertThat(fields.get(fieldId))
            .withFailMessage("Expected %s to be configured", fieldId)
            .isNotNull();
        assertThat(fields.get(fieldId).isRetainHiddenValue()).isFalse();
    }
}
