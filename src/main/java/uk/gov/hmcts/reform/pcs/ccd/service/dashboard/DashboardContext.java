package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

public record DashboardContext(
    long caseReference,
    boolean hasDraftResponse,
    boolean hasSubmittedResponse
) {}
