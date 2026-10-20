package uk.gov.hmcts.reform.pcs.enforcement.repository;

import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;

import java.time.LocalDateTime;

public record AllocatedWarrantRow(Long caseReference,
                                  String enforcementNumber,
                                  EnforcementStatus status,
                                  LocalDateTime bailiffDate) {
}
