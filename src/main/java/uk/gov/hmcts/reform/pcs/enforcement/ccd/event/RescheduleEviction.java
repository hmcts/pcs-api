package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionScheduleValidator;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.RescheduleEvictionConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionDetailsService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;

import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

/**
 * The bailiff gives a new eviction date once the claimant has said whether they can attend (HDPI-7549,
 * HDPI-7550, HDPI-7551). Available when the claimant cannot attend and, from the event availability rules, when
 * they have confirmed (open question 13).
 */
@Component
public class RescheduleEviction extends EvictionDateEvent {

    static final String SHOW_CONDITION = and(featureFlagsEnabled(ENFORCEMENT), "canRescheduleEviction=\"Yes\"");

    private final EvictionScheduleService evictionScheduleService;

    public RescheduleEviction(RescheduleEvictionConfigurer rescheduleEvictionConfigurer,
                              EvictionDetailsService evictionDetailsService,
                              EvictionScheduleValidator evictionScheduleValidator,
                              CaseReferenceFormatter caseReferenceFormatter,
                              EvictionScheduleService evictionScheduleService) {
        super(rescheduleEvictionConfigurer, evictionDetailsService, evictionScheduleValidator,
              caseReferenceFormatter);
        this.evictionScheduleService = evictionScheduleService;
    }

    @Override
    EventId eventId() {
        return EventId.rescheduleEviction;
    }

    @Override
    String eventName() {
        return "Reschedule eviction";
    }

    @Override
    String showCondition() {
        return SHOW_CONDITION;
    }

    @Override
    void checkCanStart(long caseReference) {
        evictionScheduleService.checkCanReschedule(caseReference);
    }

    @Override
    void save(long caseReference, LocalDateTime evictionDateTime, boolean dispenseWithService) {
        evictionScheduleService.reschedule(caseReference, evictionDateTime, dispenseWithService);
    }

    @Override
    String confirmationVerb() {
        return "rescheduled";
    }
}
