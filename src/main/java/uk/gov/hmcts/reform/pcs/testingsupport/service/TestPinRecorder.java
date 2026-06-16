package uk.gov.hmcts.reform.pcs.testingsupport.service;

import java.util.UUID;

/**
 * Captures plaintext access codes for test journeys. No-op in production. Never logs the plaintext.
 */
public interface TestPinRecorder {

    void record(UUID caseId, UUID partyId, String plaintextCode);
}
