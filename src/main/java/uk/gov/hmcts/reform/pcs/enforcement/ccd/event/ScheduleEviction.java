package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.ScheduleEvictionConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionDetailsService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;

import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

/**
 * The bailiff schedules the eviction on a warrant awaiting scheduling (HDPI-7344, HDPI-7546, HDPI-7547).
 */
@Component
public class ScheduleEviction extends EvictionDateEvent {

    static final String SHOW_CONDITION = and(featureFlagsEnabled(ENFORCEMENT), "canScheduleEviction=\"Yes\"");

    private final EvictionScheduleService evictionScheduleService;

    public ScheduleEviction(ScheduleEvictionConfigurer scheduleEvictionConfigurer,
                            EvictionDetailsService evictionDetailsService,
                            EvictionScheduleValidator evictionScheduleValidator,
                            CaseReferenceFormatter caseReferenceFormatter,
                            EvictionScheduleService evictionScheduleService) {
        super(scheduleEvictionConfigurer, evictionDetailsService, evictionScheduleValidator, caseReferenceFormatter);
        this.evictionScheduleService = evictionScheduleService;
    }

    @Override
    EventId eventId() {
        return EventId.scheduleEviction;
    }

    @Override
    String eventName() {
        return "Schedule eviction";
    }

    @Override
    String showCondition() {
        return SHOW_CONDITION;
    }

    @Override
    void checkCanStart(long caseReference) {
        evictionScheduleService.checkCanSchedule(caseReference);
    }

    @Override
    EnforcementChange<String> save(long caseReference, LocalDateTime evictionDateTime,
                                   boolean dispenseWithService) {
        return evictionScheduleService.schedule(caseReference, evictionDateTime, dispenseWithService);
    }

    @Override
    String confirmationVerb() {
        return "scheduled";
    }
}
