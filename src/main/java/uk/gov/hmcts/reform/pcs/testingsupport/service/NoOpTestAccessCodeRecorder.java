package uk.gov.hmcts.reform.pcs.testingsupport.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Production recorder: no-op, so no plaintext pin is ever captured.
 */
@Service
@ConditionalOnProperty(name = "access-code-test-table.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpTestAccessCodeRecorder implements TestAccessCodeRecorder {

    @Override
    public void record(UUID caseId, UUID partyId, String plaintextCode) {
    }
}
