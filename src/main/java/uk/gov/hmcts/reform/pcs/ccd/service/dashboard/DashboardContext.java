package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;


public record DashboardContext(
    long caseReference,
    PCSCase submittedCaseData
) {}
