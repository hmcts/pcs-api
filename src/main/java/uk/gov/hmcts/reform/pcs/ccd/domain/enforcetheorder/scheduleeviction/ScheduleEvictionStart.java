package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.scheduleeviction;

import java.util.List;
import java.util.UUID;

/**
 * What the schedule eviction pages show: the case header, the eviction details and the warrant application the
 * eviction is scheduled for, which the request sends back.
 */
public record ScheduleEvictionStart(UUID enforcementApplicationId,
                                    String caseNumber,
                                    String propertyAddress,
                                    String caseName,
                                    String claimantType,
                                    List<String> claimantNames,
                                    List<String> defendantNames) {
}
