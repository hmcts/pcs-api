package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentEntityTest {

    @Test
    void shouldSetSubmittedDateWhenMissingBeforePersist() {
        DocumentEntity entity = DocumentEntity.builder().build();

        entity.prePersist();

        assertThat(entity.getSubmittedDate()).isNotNull();
    }

    @Test
    void shouldNotOverwriteSubmittedDateWhenAlreadySet() {
        Instant existingDate = Instant.parse("2026-05-14T10:15:30Z");
        DocumentEntity entity = DocumentEntity.builder().submittedDate(existingDate).build();

        entity.prePersist();

        assertThat(entity.getSubmittedDate()).isEqualTo(existingDate);
    }
}
