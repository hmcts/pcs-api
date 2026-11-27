package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SupportingEvidenceTest {

    private static final Instant FORM_SAVED = Instant.parse("2026-11-20T10:00:00Z");

    @Test
    @DisplayName("Includes an upload saved before the form")
    void shouldIncludeUploadSavedBeforeForm() {
        assertThat(SupportingEvidence.savedWithSubmission(
            document(FORM_SAVED.minusSeconds(1)), document(FORM_SAVED))).isTrue();
    }

    @Test
    @DisplayName("Includes an upload saved at the same instant as the form")
    void shouldIncludeUploadSavedWithForm() {
        assertThat(SupportingEvidence.savedWithSubmission(document(FORM_SAVED), document(FORM_SAVED))).isTrue();
    }

    @Test
    @DisplayName("Leaves out an upload saved after the form")
    void shouldLeaveOutUploadSavedAfterForm() {
        assertThat(SupportingEvidence.savedWithSubmission(
            document(FORM_SAVED.plusMillis(1)), document(FORM_SAVED))).isFalse();
    }

    @Test
    @DisplayName("Leaves out an upload with no saved date")
    void shouldLeaveOutUploadWithNoDate() {
        assertThat(SupportingEvidence.savedWithSubmission(document(null), document(FORM_SAVED))).isFalse();
    }

    @Test
    @DisplayName("Leaves out an upload when the form has no saved date")
    void shouldLeaveOutUploadWhenFormHasNoDate() {
        assertThat(SupportingEvidence.savedWithSubmission(document(FORM_SAVED), document(null))).isFalse();
    }

    private static DocumentEntity document(Instant submittedDate) {
        return DocumentEntity.builder().submittedDate(submittedDate).build();
    }
}
