package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.scheduleeviction;

import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The eviction a bailiff has scheduled: the warrant application the start gave, whether the court has given
 * permission to dispense with service, and the date and time of the eviction in UK time.
 */
public record ScheduleEvictionRequest(UUID enforcementApplicationId,
                                      VerticalYesNo courtGivenPermissionToDispenseWithService,
                                      LocalDateTime scheduledEvictionDate) {
}
