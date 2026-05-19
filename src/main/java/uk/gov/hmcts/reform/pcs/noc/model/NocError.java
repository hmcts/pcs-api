package uk.gov.hmcts.reform.pcs.noc.model;

public record NocError(
    String code,
    String message
) {
}
