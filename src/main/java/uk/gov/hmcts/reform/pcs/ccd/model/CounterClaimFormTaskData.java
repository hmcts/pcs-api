package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Payload for the counter-claim-form-generation db-scheduler task. Keyed by the counter claim id;
 * the case reference and party are resolved from the counter claim inside the task so nothing lazy
 * is read in the entity listener that schedules it.
 */
@Data
@Builder
@AllArgsConstructor
public class CounterClaimFormTaskData {

    private final UUID counterClaimId;

}
