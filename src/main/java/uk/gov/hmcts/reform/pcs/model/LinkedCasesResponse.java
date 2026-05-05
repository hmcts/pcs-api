package uk.gov.hmcts.reform.pcs.model;

import java.util.List;

public record LinkedCasesResponse(List<Long> caseReferences) {
}
