package uk.gov.hmcts.reform.pcs.enforcement.service;

/**
 * A user with the bailiff organisational role, as found in IDAM.
 */
public record BailiffUser(String userId, String name, String email) {
}
