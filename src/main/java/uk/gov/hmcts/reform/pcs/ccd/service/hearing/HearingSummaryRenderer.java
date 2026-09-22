package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class HearingSummaryRenderer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER
        = DateTimeFormatter.ofPattern("d MMMM yyyy, hh:mma", Locale.UK);

    private static final String TEMPLATE = """
        <p class="body govuk-!-font-weight-bold">You’re viewing the next scheduled hearing for this case.</p>

        <h3 class="govuk-heading-s govuk-!-margin-bottom-1">Hearing location:</h3>
        <p class="body">%s</p>

        <h3 class="govuk-heading-s govuk-!-margin-bottom-1">Which type of hearing is this?</h3>
        <p class="body">%s</p>

        <h3 class="govuk-heading-s govuk-!-margin-bottom-1">When is the hearing?</h3>
        <p class="body">%s</p>

        <h3 class="govuk-heading-s govuk-!-margin-bottom-1">Hearing duration</h3>
        <p class="body">%s</p>

        <h3 class="govuk-heading-s govuk-!-margin-bottom-1">Hearing notes</h3>
        <p class="body">%s</p>

        <p class="body govuk-!-margin-bottom-2">&nbsp;</p>
        """;

    private final DurationFormatter durationFormatter;

    public String renderMarkdown(HearingEntity hearingEntity, String hearingLocation) {

        String hearingTypeLabel = getHearingTypeLabel(hearingEntity);
        String formattedDate = DATE_TIME_FORMATTER.format(hearingEntity.getHearingDate());
        String formattedDuration = durationFormatter.format(hearingEntity.getDurationDays(),
                                                            hearingEntity.getDurationHours(),
                                                            hearingEntity.getDurationMinutes());
        String hearingNotes = StringUtils.isBlank(hearingEntity.getNotes()) ? "Not provided" : hearingEntity.getNotes();

        return TEMPLATE.formatted(
            hearingLocation,
            hearingTypeLabel,
            formattedDate,
            formattedDuration,
            hearingNotes
        );
    }

    private static String getHearingTypeLabel(HearingEntity hearingEntity) {
        HearingType type = hearingEntity.getType();
        if (type != HearingType.OTHER) {
            return type.getLabel();
        } else {
            return "%s (%s)".formatted(type.getLabel(), hearingEntity.getOtherHearingType());
        }
    }

}
