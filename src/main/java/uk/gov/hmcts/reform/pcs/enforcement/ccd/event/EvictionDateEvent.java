package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionConfirmationFormat;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionDetailsMarkup;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionDetailsService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.BailiffStaffRoles.BAILIFF_STAFF_ROLES;

/**
 * "Schedule eviction" and "Reschedule eviction" ask the same questions: the eviction date and time, and whether
 * the court has dispensed with service. Both save the date as the enforcement's bailiff date, which the
 * claimant's Confirm eviction journey reads, and move the warrant to awaiting claimant confirmation.
 *
 * <p>Bailiff staff, the allocated bailiff included (its case role has the bailiff access profile), can use
 * them. CCD cannot tell the allocated bailiff from other bailiffs, so the event does not try to.</p>
 */
abstract class EvictionDateEvent implements CCDConfig<PCSCase, State, UserRole> {

    private final PageConfigurer pageConfigurer;
    private final EvictionDetailsService evictionDetailsService;
    private final EvictionScheduleValidator validator;
    private final CaseReferenceFormatter caseReferenceFormatter;

    EvictionDateEvent(PageConfigurer pageConfigurer, EvictionDetailsService evictionDetailsService,
                      EvictionScheduleValidator validator, CaseReferenceFormatter caseReferenceFormatter) {
        this.pageConfigurer = pageConfigurer;
        this.evictionDetailsService = evictionDetailsService;
        this.validator = validator;
        this.caseReferenceFormatter = caseReferenceFormatter;
    }

    abstract EventId eventId();

    abstract String eventName();

    /**
     * The event's enabling condition, feature flag included.
     */
    abstract String showCondition();

    /**
     * Throws when the event cannot start on the claim's enforcement.
     */
    abstract void checkCanStart(long caseReference);

    /**
     * Saves the eviction date, giving back the warrant number for the confirmation screen or the errors to
     * show instead.
     */
    abstract EnforcementChange<String> save(long caseReference, LocalDateTime evictionDateTime,
                                            boolean dispenseWithService);

    /**
     * "scheduled" or "rescheduled", for the confirmation header.
     */
    abstract String confirmationVerb();

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(eventId().name(), this::submit, this::start)
                .forStates(AddBailiffNote.STATES)
                .name(eventName())
                .grant(Permission.CRUD, BAILIFF_STAFF_ROLES)
                .showCondition(showCondition())
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary();
        pageConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        checkCanStart(caseReference);
        PCSCase pcsCase = eventPayload.caseData();
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        pcsCase.getEnforcementOrder().setFormattedCaseName(pcsCase.getCaseNameHmctsInternal());
        pcsCase.getEnforcementOrder().setEvictionSchedule(
            EvictionSchedule.builder()
                .evictionDetailsMarkup(
                    EvictionDetailsMarkup.render(evictionDetailsService.getEvictionDetails(caseReference)))
                .evictionDispenseWithService(YesOrNo.NO)
                .build());
        return pcsCase;
    }

    SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        EvictionSchedule schedule = answeredSchedule(eventPayload.caseData().getEnforcementOrder());

        List<String> errors = validator.validateEvictionDate(schedule);
        if (!errors.isEmpty()) {
            return SubmitResponse.<State>builder().errors(errors).build();
        }
        LocalDateTime evictionDateTime = Objects.requireNonNull(
                schedule.getEvictionDateTime(), "The eviction schedule was accepted with no date and time")
            .truncatedTo(ChronoUnit.MINUTES);
        EnforcementChange<String> saved =
            save(caseReference, evictionDateTime, schedule.getEvictionDispenseWithService() == YesOrNo.YES);
        if (saved.refused()) {
            return SubmitResponse.<State>builder().errors(saved.errors()).build();
        }
        return SubmitResponse.<State>builder()
            .confirmationHeader("# Eviction date %s for %s".formatted(
                confirmationVerb(), EvictionConfirmationFormat.format(evictionDateTime)))
            .confirmationBody(EvictionConfirmationFormat.scheduledBody(
                caseReferenceFormatter.formatCaseReferenceWithDashes(caseReference), caseReference,
                saved.change()))
            .build();
    }

    /**
     * The answers the event's page collected, or an empty schedule when the event data holds none: the
     * validator refuses both the same way.
     */
    private static EvictionSchedule answeredSchedule(EnforcementOrder enforcementOrder) {
        EvictionSchedule schedule = enforcementOrder == null ? null : enforcementOrder.getEvictionSchedule();
        return schedule == null ? EvictionSchedule.builder().build() : schedule;
    }
}
