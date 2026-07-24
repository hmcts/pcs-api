package uk.gov.hmcts.reform.pcs.ccd.testcasesupport;

import uk.gov.hmcts.reform.pcs.exception.RedactedRuntimeException;

import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.TEST_CASE_SUPPORT;

public class TestCaseSupportException extends RedactedRuntimeException {

    public TestCaseSupportException(Throwable cause) {
        super(TEST_CASE_SUPPORT, cause);
    }

}
