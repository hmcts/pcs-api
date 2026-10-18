package uk.gov.hmcts.reform.pcs.enforcement.repository;

import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;

import java.time.LocalDateTime;

/**
 * A warrant allocated to a bailiff, as {@link BailiffAllocationRepository#findAllocatedWarrants} loads it.
 */
public record AllocatedWarrantRow(Long caseReference,
                                  String enforcementNumber,
                                  EnforcementStatus status,
                                  LocalDateTime bailiffDate) {
}
