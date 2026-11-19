package uk.gov.hmcts.reform.pcs.bulkclaim.model;

public record BulkValidationError(String path, String code, String message) {
}
