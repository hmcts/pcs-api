package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

class DocumentViewUtilTest {

    @Test
    void shouldReturnNullWhenNoSubmittedDate() {
        // Given
        DocumentEntity documentEntity = DocumentEntity.builder().submittedDate(null).build();

        // When
        LocalDateTime uploadTimestamp = DocumentViewUtil.uploadTimestamp(documentEntity);

        // Then
        assertThat(uploadTimestamp).isNull();
    }

    @ParameterizedTest
    @MethodSource("submittedDateScenarios")
    void shouldConvertSubmittedDateToUtcLocalDateTime(Instant submittedDate, LocalDateTime expectedResult) {
        // Given
        DocumentEntity documentEntity = DocumentEntity.builder().submittedDate(submittedDate).build();

        // When
        LocalDateTime uploadTimestamp = DocumentViewUtil.uploadTimestamp(documentEntity);

        // Then
        assertThat(uploadTimestamp).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> submittedDateScenarios() {
        return Stream.of(
            // Submitted date, Expected result
            argumentSet(
                "Mid-morning UTC",
                Instant.parse("2026-05-14T09:30:00Z"),
                LocalDateTime.of(2026, 5, 14, 9, 30, 0)
            ),
            argumentSet(
                "During British Summer Time, no local offset applied",
                Instant.parse("2026-07-01T23:45:00Z"),
                LocalDateTime.of(2026, 7, 1, 23, 45, 0)
            ),
            argumentSet(
                "Sub-second precision is retained",
                Instant.parse("2026-01-31T00:00:00.123456Z"),
                LocalDateTime.of(2026, 1, 31, 0, 0, 0, 123_456_000)
            )
        );
    }

}
