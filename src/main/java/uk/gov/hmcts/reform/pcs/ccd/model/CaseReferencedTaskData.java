package uk.gov.hmcts.reform.pcs.ccd.model;

/**
 * Common contract for db-scheduler task payloads that are keyed by a case reference, so the shared
 * generation-task scaffolding can read it uniformly.
 */
public interface CaseReferencedTaskData {

    String getCaseReference();
}
