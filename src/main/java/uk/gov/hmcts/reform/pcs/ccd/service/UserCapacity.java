package uk.gov.hmcts.reform.pcs.ccd.service;

/**
 * The three populations that reach a PCS case: an unrepresented party, an external professional
 * acting through an organisation, and HMCTS staff.
 */
public enum UserCapacity {
    CITIZEN,
    PROFESSIONAL,
    INTERNAL
}
