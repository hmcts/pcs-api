package uk.gov.hmcts.reform.pcs.enforcement.view;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.enforcement.view.TabMarkup.Row;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;
import static uk.gov.hmcts.reform.pcs.enforcement.view.TabMarkup.heading;
import static uk.gov.hmcts.reform.pcs.enforcement.view.TabMarkup.paragraph;
import static uk.gov.hmcts.reform.pcs.enforcement.view.TabMarkup.summaryList;

/**
 * Renders the "Enforcement status" tab (12-enforcement-status-tab.feature, HDPI-7351): a section for each
 * generated warrant and writ, headed with its number. A warrant has the "Eviction notice", "Scheduled
 * eviction", "Claimant eviction confirmation" and "Eviction outcome" sections; a writ goes to a High Court
 * Enforcement Officer, so it shows the date of transfer to the High Court instead.
 */
final class EnforcementStatusTabMarkdown {

    static final String EVICTION_NOTICE = "Eviction notice";
    static final String SCHEDULED_EVICTION = "Scheduled eviction";
    static final String CLAIMANT_EVICTION_CONFIRMATION = "Claimant eviction confirmation";
    static final String EVICTION_OUTCOME = "Eviction outcome";

    static final String TYPE = "Type";
    static final String STATUS = "Status";
    static final String HIGH_COURT_TRANSFER_DATE = "Date of transfer to the High Court";
    static final String SERVICE = "Service";
    static final String DATE_SERVED = "Date served";
    static final String TIME_SERVED = "Time served";
    static final String HOW_SERVED = "How it was served";
    static final String EVICTION_DATE = "Eviction date";
    static final String EVICTION_TIME = "Eviction time";
    static final String CAN_ATTEND = "Can you (or someone from your organisation) attend on %s at %s?";
    static final String RETURN = "Return";
    static final String RETURN_NAME = "Return name";
    static final String OUTCOME = "Outcome";
    static final String ADDITIONAL_DETAILS = "Additional details";

    static final String NOT_YET_SCHEDULED = "Not yet scheduled";
    static final String NOT_YET_SERVED = "Not yet served";
    static final String NOT_YET_CONFIRMED = "Not yet confirmed";
    static final String NO_RETURN = "No return recorded yet";
    static final String NOT_APPLICABLE = "Not applicable";
    static final String WITHOUT_NOTICE = "Without notice: the court has given permission to dispense with service";

    private static final Set<SelectEnforcementType> WRITS =
        EnumSet.of(SelectEnforcementType.WRIT, SelectEnforcementType.WRIT_OF_RESTITUTION);

    /**
     * One warrant or writ on the tab, with its most recent warrant return, if any.
     */
    record Entry(EnforcementOrderEntity enforcement, WarrantReturnEntity latestReturn) {
    }

    private EnforcementStatusTabMarkdown() {
    }

    /**
     * Renders the entries in the order given, for internal users and the claimant side.
     */
    static String render(List<Entry> entries) {
        return renderSections(entries, false);
    }

    /**
     * Renders the entries in the order given for a defendant: without the "Claimant eviction confirmation"
     * section, which is the claimant's own answer (12-enforcement-status-tab.feature).
     */
    static String renderForDefendants(List<Entry> entries) {
        return renderSections(entries, true);
    }

    private static String renderSections(List<Entry> entries, boolean forDefendants) {
        List<String> blocks = new ArrayList<>();
        for (Entry entry : entries) {
            if (!blocks.isEmpty()) {
                blocks.add(TabMarkup.sectionBreak());
            }
            blocks.addAll(section(entry, forDefendants));
        }
        return TabMarkup.lines(blocks);
    }

    private static List<String> section(Entry entry, boolean forDefendants) {
        EnforcementOrderEntity enforcement = entry.enforcement();
        SelectEnforcementType type = SelectEnforcementType.from(enforcement.getEnforcementOrder());
        List<String> blocks = new ArrayList<>();
        blocks.add(heading(2, title(enforcement, type)));

        List<Row> summary = new ArrayList<>();
        if (type != null) {
            summary.add(new Row(TYPE, type.getLabel()));
        }
        summary.add(new Row(STATUS, enforcement.getStatus().getLabel()));
        if (WRITS.contains(type)) {
            if (enforcement.getApprovedAt() != null) {
                summary.add(new Row(HIGH_COURT_TRANSFER_DATE, TabMarkup.SHORT_DATE.format(
                    enforcement.getApprovedAt().atZone(UK_ZONE_ID).toLocalDate())));
            }
            blocks.add(summaryList(summary));
            return blocks;
        }
        blocks.add(summaryList(summary));

        blocks.add(heading(3, EVICTION_NOTICE));
        blocks.add(evictionNotice(enforcement));
        blocks.add(heading(3, SCHEDULED_EVICTION));
        blocks.add(scheduledEviction(enforcement));
        if (!forDefendants) {
            blocks.add(heading(3, CLAIMANT_EVICTION_CONFIRMATION));
            blocks.add(claimantConfirmation(enforcement));
        }
        blocks.add(heading(3, EVICTION_OUTCOME));
        blocks.add(evictionOutcome(entry.latestReturn()));
        return blocks;
    }

    private static String title(EnforcementOrderEntity enforcement, SelectEnforcementType type) {
        if (enforcement.getEnforcementNumber() != null) {
            return enforcement.getEnforcementNumber();
        }
        return type != null ? type.getLabel() : "Enforcement";
    }

    private static String evictionNotice(EnforcementOrderEntity enforcement) {
        if (Boolean.TRUE.equals(enforcement.getDispenseWithService())) {
            return summaryList(List.of(
                new Row(SERVICE, WITHOUT_NOTICE),
                new Row(DATE_SERVED, NOT_APPLICABLE),
                new Row(TIME_SERVED, NOT_APPLICABLE),
                new Row(HOW_SERVED, NOT_APPLICABLE)
            ));
        }
        LocalDateTime servedAt = enforcement.getNoticeServedAt();
        if (servedAt == null) {
            return paragraph(NOT_YET_SERVED);
        }
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(DATE_SERVED, TabMarkup.date(servedAt)));
        rows.add(new Row(TIME_SERVED, TabMarkup.time(servedAt)));
        if (enforcement.getNoticeServedHow() != null) {
            rows.add(new Row(HOW_SERVED, enforcement.getNoticeServedHow()));
        }
        return summaryList(rows);
    }

    private static String scheduledEviction(EnforcementOrderEntity enforcement) {
        LocalDateTime evictionAt = enforcement.getBailiffDate();
        if (evictionAt == null) {
            return paragraph(NOT_YET_SCHEDULED);
        }
        return summaryList(List.of(
            new Row(EVICTION_DATE, TabMarkup.date(evictionAt)),
            new Row(EVICTION_TIME, TabMarkup.time(evictionAt))
        ));
    }

    /**
     * "Confirm the eviction details" only asks whether the claimant can attend (OPEN-QUESTIONS 25), so that is
     * the only answer shown.
     */
    private static String claimantConfirmation(EnforcementOrderEntity enforcement) {
        LocalDateTime evictionAt = enforcement.getBailiffDate();
        Boolean canAttend = enforcement.getClaimantCanAttend();
        if (evictionAt == null || canAttend == null) {
            return paragraph(NOT_YET_CONFIRMED);
        }
        String question = CAN_ATTEND.formatted(TabMarkup.date(evictionAt), TabMarkup.time(evictionAt));
        return summaryList(List.of(new Row(question, canAttend ? "Yes" : "No")));
    }

    private static String evictionOutcome(WarrantReturnEntity latestReturn) {
        if (latestReturn == null) {
            return paragraph(NO_RETURN);
        }
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(RETURN, label(latestReturn.getReturnType())));
        rows.add(new Row(RETURN_NAME, label(latestReturn.getReturnName())));
        if (latestReturn.getOutcome() != null) {
            rows.add(new Row(OUTCOME, latestReturn.getOutcome().getLabel()));
        }
        if (latestReturn.getEvictionAt() != null) {
            LocalDateTime evictionAt = latestReturn.getEvictionAt().atZone(UK_ZONE_ID).toLocalDateTime();
            rows.add(new Row(EVICTION_DATE, TabMarkup.date(evictionAt)));
            rows.add(new Row(EVICTION_TIME, TabMarkup.time(evictionAt)));
        }
        if (latestReturn.getAdditionalDetails() != null) {
            rows.add(new Row(ADDITIONAL_DETAILS, latestReturn.getAdditionalDetails()));
        }
        return summaryList(rows);
    }

    private static String label(HasLabel value) {
        return value == null ? "" : value.getLabel();
    }
}
