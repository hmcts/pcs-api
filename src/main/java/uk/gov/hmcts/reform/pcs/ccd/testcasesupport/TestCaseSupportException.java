package uk.gov.hmcts.reform.pcs.ccd.testcasesupport;

import uk.gov.hmcts.reform.pcs.exception.PCSRuntimeException;

import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.TEST_CASE_SUPPORT;

public class TestCaseSupportException extends PCSRuntimeException {

    public TestCaseSupportException(Throwable cause) {
        super(TEST_CASE_SUPPORT, cause);
    }

}
