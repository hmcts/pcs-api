package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;

import java.time.LocalDateTime;

public record AllocatedWarrant(String caseReference,
                               String warrantNumber,
                               EnforcementStatus status,
                               LocalDateTime evictionDate) {
}
