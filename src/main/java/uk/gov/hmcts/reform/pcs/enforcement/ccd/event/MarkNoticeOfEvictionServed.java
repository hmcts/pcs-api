package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.MarkNoticeOfEvictionServedConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.markNoticeOfEvictionServed;
import static uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.BailiffStaffRoles.BAILIFF_STAFF_ROLES;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

/**
 * The bailiff records when and how they served the notice of eviction, with an optional bailiff note
 * (HDPI-7346, HDPI-7657, HDPI-7658). Available once the eviction is scheduled, when the court has not dispensed
 * with service and the notice is not yet marked as served.
 */
@Component
@AllArgsConstructor
public class MarkNoticeOfEvictionServed implements CCDConfig<PCSCase, State, UserRole> {

    static final String SHOW_CONDITION = and(featureFlagsEnabled(ENFORCEMENT),
                                             "canMarkNoticeOfEvictionServed=\"Yes\"");
    static final String CONFIRMATION_HEADER = "# 'Notice of eviction' marked as served";

    private final MarkNoticeOfEvictionServedConfigurer markNoticeOfEvictionServedConfigurer;
    private final EvictionScheduleService evictionScheduleService;
    private final EvictionScheduleValidator evictionScheduleValidator;
    private final CaseReferenceFormatter caseReferenceFormatter;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(markNoticeOfEvictionServed.name(), this::submit, this::start)
                .forStates(AddBailiffNote.STATES)
                // CCD rejects event names longer than 30 characters
                .name("Mark eviction notice served")
                .description("Mark notice of eviction as served")
                .grant(Permission.CRUD, BAILIFF_STAFF_ROLES)
                .showCondition(SHOW_CONDITION)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary();
        markNoticeOfEvictionServedConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        evictionScheduleService.checkCanMarkNoticeServed(eventPayload.caseReference());
        PCSCase pcsCase = eventPayload.caseData();
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        pcsCase.getEnforcementOrder().setEvictionSchedule(new EvictionSchedule());
        pcsCase.getEnforcementOrder().setBailiffNoteText(null);
        return pcsCase;
    }

    SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        EnforcementOrder enforcementOrder = eventPayload.caseData().getEnforcementOrder();
        EvictionSchedule schedule = enforcementOrder == null ? null : enforcementOrder.getEvictionSchedule();
        String bailiffNoteText = enforcementOrder == null ? null : enforcementOrder.getBailiffNoteText();

        List<String> errors = evictionScheduleValidator.validateNoticeServed(schedule, bailiffNoteText);
        if (!errors.isEmpty()) {
            return SubmitResponse.<State>builder().errors(errors).build();
        }
        LocalDateTime servedAt = schedule.getNoticeOfEvictionServedAt().truncatedTo(ChronoUnit.MINUTES);
        String bailiffNote = schedule.getNoticeOfEvictionAddBailiffNote() == YesOrNo.YES ? bailiffNoteText : null;
        try {
            evictionScheduleService.markNoticeServed(
                caseReference, servedAt, schedule.getNoticeOfEvictionServedHow(), bailiffNote);
        } catch (NoEnforcementException | EnforcementEventNotAvailableException ex) {
            return SubmitResponse.<State>builder().errors(List.of(ex.getMessage())).build();
        }
        // TODO HDPI-7658: show the warrant number once warrants are numbered.
        return SubmitResponse.<State>builder()
            .confirmationHeader(CONFIRMATION_HEADER)
            .confirmationBody("Case number: " + caseReferenceFormatter.formatCaseReferenceWithDashes(caseReference))
            .build();
    }
}
