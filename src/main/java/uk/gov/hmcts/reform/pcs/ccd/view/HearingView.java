package uk.gov.hmcts.reform.pcs.ccd.view;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingListingStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class HearingView {

    private static final DateTimeFormatter DATE_TIME_FORMAT
        = DateTimeFormatter.ofPattern("d MMMM yyyy 'at' h:mma", Locale.UK);

    private final Clock ukClock;

    public HearingView(@Qualifier("ukClock") Clock ukClock) {
        this.ukClock = ukClock;
    }

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        List<HearingEntity> hearings = pcsCaseEntity.getHearings();
        setHearingFields(pcsCase, hearings);
        pcsCase.setHearingListMarkdown(pcsCase.makeOrderEnabled() ? renderHearingList(hearings) : null);
    }

    private String renderHearingList(List<HearingEntity> hearings) {
        if (hearings.isEmpty()) {
            return "There are no hearings recorded on this case.";
        }

        LocalDateTime now = LocalDateTime.now(ukClock);
        Comparator<LocalDateTime> byDate = Comparator.nullsLast(Comparator.naturalOrder());
        String rows = hearings.stream()
            .sorted(Comparator.comparing(HearingEntity::getHearingDate, byDate))
            .map(hearing -> renderRow(hearing, now))
            .collect(Collectors.joining());

        return """
            <table class="govuk-table">
            <thead class="govuk-table__head"><tr class="govuk-table__row">
            <th scope="col" class="govuk-table__header">Date and time</th>
            <th scope="col" class="govuk-table__header">Hearing type</th>
            <th scope="col" class="govuk-table__header">Status</th>
            <th scope="col" class="govuk-table__header">Date recorded</th>
            </tr></thead>
            <tbody class="govuk-table__body">%s</tbody>
            </table>
            """.formatted(rows);
    }

    private static String renderRow(HearingEntity hearing, LocalDateTime now) {
        String type = hearing.getType() == null ? "Not recorded" : hearing.getType().getLabel();
        if (hearing.getType() == HearingType.OTHER && StringUtils.isNotBlank(hearing.getOtherHearingType())) {
            type = StringUtils.normalizeSpace(hearing.getOtherHearingType());
        }

        return """
            <tr class="govuk-table__row">
            <td class="govuk-table__cell">%s</td>
            <td class="govuk-table__cell">%s</td>
            <td class="govuk-table__cell">%s</td>
            <td class="govuk-table__cell">%s</td>
            </tr>
            """.formatted(formatDate(hearing.getHearingDate()), HtmlUtils.htmlEscape(type),
            hearingStatus(hearing, now), formatDate(hearing.getCreatedDate()));
    }

    private static String hearingStatus(HearingEntity hearing, LocalDateTime now) {
        if (Boolean.TRUE.equals(hearing.getCancelled()) || hearing.getListingStatus() == HearingListingStatus.CNCL) {
            return "Cancelled";
        }
        LocalDateTime hearingDate = hearing.getHearingDate();
        if (hearingDate == null) {
            return "Date not recorded";
        }
        return hearingDate.isBefore(now) ? "Past" : "Scheduled";
    }

    private static String formatDate(LocalDateTime date) {
        return date == null ? "Not recorded" : DATE_TIME_FORMAT.format(date);
    }

    private void setHearingFields(PCSCase pcsCase, List<HearingEntity> hearingEntities) {
        List<ListValue<Hearing>> hearings = hearingEntities.stream().map(hearingEntity -> {
            Hearing hearing = Hearing.builder()
                .type(hearingEntity.getType())
                .otherHearingType(hearingEntity.getOtherHearingType())
                .noticeWording(hearingEntity.getNoticeWording())
                .date(hearingEntity.getHearingDate())
                .durationDays(hearingEntity.getDurationDays())
                .durationHours(asFloat(hearingEntity.getDurationHours()))
                .durationMinutes(asFloat(hearingEntity.getDurationMinutes()))
                .notes(hearingEntity.getNotes())
                .issueNotice(hearingEntity.getIssueNotice())
                .isWithoutNotice(hearingEntity.getIsWithoutNotice())
                .additionalInformation(hearingEntity.getAdditionalInformation())
                .build();

            ListValue<Hearing> listValue = new ListValue<>();
            listValue.setValue(hearing);

            return listValue;
        }).toList();

        pcsCase.setHearingList(hearings);
    }

    private static Float asFloat(Integer duration) {
        return duration == null ? null : duration.floatValue();
    }
}
