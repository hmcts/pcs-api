package uk.gov.hmcts.reform.pcs.exception;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ResetExceptionRedactionExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        ExceptionRedaction.setShowFullExceptionsForTesting(null);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ExceptionRedaction.setShowFullExceptionsForTesting(null);
    }

}
