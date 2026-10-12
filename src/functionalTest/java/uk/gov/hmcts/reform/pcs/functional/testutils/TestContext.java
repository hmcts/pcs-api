package uk.gov.hmcts.reform.pcs.functional.testutils;

import net.serenitybdd.annotations.Steps;
import uk.gov.hmcts.reform.pcs.functional.steps.ApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.CCDDataStoreApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.PCSBaseApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.PCSTestingSupportApiSteps;

public class TestContext {

    @Steps
    private ApiSteps apiSteps;

    @Steps
    private CCDDataStoreApiSteps ccdDataStoreApiSteps;

    @Steps
    private PCSBaseApiSteps pcsBaseApiSteps;

    @Steps
    private PCSTestingSupportApiSteps testingSupportApiSteps;

    private String testUser;
    private String caseReference;

    public TestContext initialize() {
        testUser = null;
        caseReference = null;
        ApiSteps.setUp();
        ccdDataStoreApiSteps.init();
        pcsBaseApiSteps.initialise();
        testingSupportApiSteps.initialise();
        return this;
    }

    public TestContext asUser(String userIdentifier) {
        setTestUser(userIdentifier);
//        apiSteps.withUser(userIdentifier);
        ccdDataStoreApiSteps.withUser(userIdentifier);
        pcsBaseApiSteps.withUser(userIdentifier);
        testingSupportApiSteps.withUser(userIdentifier);
        return this;
    }

    public String getTestUser() {
        return testUser;
    }

    private void setTestUser(String testUser) {
        this.testUser = testUser;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        ccdDataStoreApiSteps.withCaseReference(caseReference);
        this.caseReference = caseReference;
    }

    public ApiSteps withApiSteps() {
        return apiSteps;
    }

    public CCDDataStoreApiSteps withCCDDataStoreApiSteps() {
        return ccdDataStoreApiSteps;
    }

    public PCSBaseApiSteps withPCSBaseApiSteps() {
        return pcsBaseApiSteps;
    }

    public PCSTestingSupportApiSteps withPCSTestingSupportApiSteps() {
        return testingSupportApiSteps;
    }
}
