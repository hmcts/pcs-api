package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static uk.gov.hmcts.reform.pcs.exception.RedactionGate.setShowFullExceptionsForTesting;

public class ResetExceptionRedactionExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        setShowFullExceptionsForTesting(null);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        setShowFullExceptionsForTesting(null);
    }

}
