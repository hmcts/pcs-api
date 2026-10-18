package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;

import java.time.LocalDateTime;

/**
 * A live warrant allocated to the bailiff, for choosing what to print.
 *
 * @param evictionDate the scheduled eviction, as UK wall-clock time; null before it is scheduled
 */
public record AllocatedWarrant(String caseReference,
                               String warrantNumber,
                               EnforcementStatus status,
                               LocalDateTime evictionDate) {
}
