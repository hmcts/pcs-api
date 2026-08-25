package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.setShowFullMessagesForTesting;

public class ResetExceptionRedactionExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        setShowFullMessagesForTesting(null);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        setShowFullMessagesForTesting(null);
    }

}
