package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingSummaryRendererTest {

    @Mock
    private DurationFormatter durationFormatter;

    private HearingSummaryRenderer underTest;

    @BeforeEach
    void setUp() {
        underTest = new HearingSummaryRenderer(durationFormatter);
    }

    @Test
    void shouldRenderHearingSummary() {
        // Given
        HearingEntity hearingEntity = HearingEntity.builder()
            .type(HearingType.OTHER)
            .otherHearingType("some other hearing type")
            .hearingDate(LocalDateTime.parse("2026-07-10T09:00:00"))
            .durationHours(1)
            .durationMinutes(30)
            .notes("some notes")
            .build();

        String hearingLocation = "some hearing location";

        when(durationFormatter.format(1, 30)).thenReturn("formatted duration");

        // When
        String hearingSummary = underTest.renderMarkdown(hearingEntity, hearingLocation);

        // Then
        assertThat(hearingSummary)
            .contains("<p class=\"body\">some hearing location</p>")
            .contains("<p class=\"body\">Other (some other hearing type)</p>")
            .contains("<p class=\"body\">10 July 2026, 09:00am</p>")
            .contains("<p class=\"body\">formatted duration</p>")
            .contains("<p class=\"body\">some notes</p>");

    }

}
