package uk.gov.hmcts.reform.pcs.functional.tests;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;
import uk.gov.hmcts.reform.pcs.functional.steps.CCDDataStoreApiSteps;
import uk.gov.hmcts.reform.pcs.functional.steps.PCSApiSteps;
import uk.gov.hmcts.reform.pcs.functional.config.CaseTypeEvents;
import uk.gov.hmcts.reform.pcs.functional.testutils.TestUsers.UserType;
import uk.gov.hmcts.reform.pcs.functional.testutils.PayloadLoader;

import java.util.Map;

@Tag("Functional")
@EnabledIfEnvironmentVariable(named = "CCD_ENABLED", matches = "true")
@DisabledIfEnvironmentVariable(named = "SHUTTER_SERVICE", matches = "true")
@ExtendWith(SerenityJUnit5Extension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CaseEventFlowToRespondCase extends BaseApi {

    @Steps
    PCSApiSteps pcsApiSteps;

    @Steps
    CCDDataStoreApiSteps ccdDataStoreApiSteps;

    private static String caseId = null;

    @BeforeEach
    void initialiseApiSteps() {
        pcsApiSteps.initialise();
        ccdDataStoreApiSteps.init();
    }

    @Title("Create case with testing support endpoint")
    @Order(1)
    @Test
    void createCaseWithTestSupport() {
        pcsApiSteps.withUser(UserType.localAuthoritySolicitor1);
        String createCaseTestSupportPayload = PayloadLoader.load("/payloads/ccdEvent/createCaseTestSupport.json");
        pcsApiSteps
            .createCaseForLegislativeCountry("england", createCaseTestSupportPayload, false);
        caseId = pcsApiSteps.getCaseReference();
    }

    @Title("Claimant makes payment and case state updated to CASE_ISSUED")
    @Order(2)
    @Test
    void claimantMakesPayment() {
        Map<String, String> claimantParty = ccdDataStoreApiSteps
            .withUser(UserType.localAuthoritySolicitor1)
            .withCaseReference(caseId)
            .fetchCaseDetails()
            .getClaimantParty(0);

        String serviceRequestRef = pcsApiSteps.withUser(UserType.localAuthoritySolicitor1)
                                                .setCaseReference(caseId)
                                                .getFeePaymentInfo()
                                                .getPaymentReferenceForPartyId(claimantParty.get("id"));

        System.out.println("serviceRequestRef reference : " + serviceRequestRef);
        pcsApiSteps.updatePayment(serviceRequestRef);

        ccdDataStoreApiSteps.fetchCaseDetails().assertCaseState("CASE_ISSUED");
    }

    @Title("Defendant cannot access case without access code validation")
    @Order(3)
    @Test
    void defendantAccessCase() {
        ccdDataStoreApiSteps.withUser(UserType.defendantCitizen1)
            .withCaseReference(caseId)
            .assertCannotAccessCaseDetails();
    }

    @Title("Defendant validates access code and can access case details")
    @Order(4)
    @Test
    void defendantValidatesAccessCode() {
        Map<String, String> defendantParty = ccdDataStoreApiSteps
            .withUser(UserType.localAuthoritySolicitor1)
            .withCaseReference(caseId)
            .fetchCaseDetails()
            .getDefendantParty(1);

        pcsApiSteps.withUser(UserType.defendantCitizen1)
            .setCaseReference(caseId)
            .accessCodeIsFetchedForUser(defendantParty.get("firstName"), defendantParty.get("lastName"))
            .validateAccessCodeForParty(
                caseId,
                defendantParty.get("id"),
                defendantParty.get("firstName"),
                defendantParty.get("lastName")
            );

        ccdDataStoreApiSteps.withUser(UserType.defendantCitizen1)
            .withCaseReference(caseId)
            .assertCanAccessCaseDetails();
    }

    @Title("Defendant responds to possession claim")
    @Order(5)
    @Test
    void respondPossessionClaim() {
        String validateRequestBody = PayloadLoader.load(
            "/payloads/ccdEvent/respondPossessionClaim-withoutCounterClaim.json",
            Map.of("DRAFT_VERSION", "0")
        );
        String submitRequestBody = PayloadLoader.load(
            "/payloads/ccdEvent/respondPossessionClaim-withoutCounterClaim.json",
            Map.of("DRAFT_VERSION", "1")
        );
        ccdDataStoreApiSteps
            .withUser(UserType.defendantCitizen1)
            .withCaseReference(caseId)
            .triggerEvent(CaseTypeEvents.RESPOND_POSSESSION_CLAIM.getEventId())
            .validateTriggerEvent(200)
            .postValidateEvent("respondPossessionClaimrespondToPossessionDraftSavePage",validateRequestBody)
            .verifyValidateEvent(200)
            .submitEvent(CaseTypeEvents.RESPOND_POSSESSION_CLAIM.getEventId(), submitRequestBody)
            .validateSubmitEvent(201);
    }

    
}
