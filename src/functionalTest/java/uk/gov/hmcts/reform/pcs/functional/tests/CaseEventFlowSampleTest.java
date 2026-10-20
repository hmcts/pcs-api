package uk.gov.hmcts.reform.pcs.functional.tests;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;
import uk.gov.hmcts.reform.pcs.functional.config.CaseTypeEvents;
import uk.gov.hmcts.reform.pcs.functional.testutils.EventDataUtil;
import uk.gov.hmcts.reform.pcs.functional.testutils.TestContext;
import uk.gov.hmcts.reform.pcs.functional.testutils.eventdata.ResumePossessionClaimEventData;

import java.util.Map;

@Tag("Functional")
@EnabledIfEnvironmentVariable(named = "CCD_ENABLED", matches = "true")
@DisabledIfEnvironmentVariable(named = "SHUTTER_SERVICE", matches = "true")
@ExtendWith(SerenityJUnit5Extension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CaseEventFlowSampleTest extends BaseApi {

    @Steps
    TestContext testContext;

    private static String caseId = null;

    @Title("Create case with testing support endpoint")
    @Order(1)
    @Test
    void createCaseWithTestSupport() {
        testContext.initialize().asUser("ORG_LOCALAUTHORITY.solicitor1");
        ObjectNode createCaseTestSupportPayload = EventDataUtil.forEvent(CaseTypeEvents.CREATE_CASE_TEST_SUPPORT)
                                                                .getData();
        testContext.withPCSTestingSupportApiSteps()
            .createCaseForLegislativeCountry("england", createCaseTestSupportPayload, true);
        testContext.setCaseReference(
            testContext.withPCSTestingSupportApiSteps().getTestingSupportCaseReference().toString()
        );
        caseId = testContext.getCaseReference();
    }

    @Title("Event flow - create case with multiple defendants")
    @Order(1)
    //@Test
    void createCaseWithMultipleDefendants() {
        testContext.initialize().asUser("ORG_LOCALAUTHORITY.solicitor1");

        testContext.withCCDDataStoreApiSteps()
            .createPossessionClaimCase("england");
        testContext.setCaseReference(testContext.withCCDDataStoreApiSteps().getCaseReference());
        caseId = testContext.getCaseReference();

        ObjectNode resumePossessionClaimPayload = ((ResumePossessionClaimEventData) EventDataUtil
            .forEvent(CaseTypeEvents.RESUME_POSSESSION_CLAIM))
            .addDefendant("defendant1",
                          Map.of("nameKnown", "NO"))
            .addDefendant("defendant2",
                          Map.of("nameKnown", "YES",
                                 "firstName", "John",
                                 "lastName", "Doe",
                                 "addressKnown", "NO"
                          ))
            .getData();

        testContext.withCCDDataStoreApiSteps()
            .triggerEvent(CaseTypeEvents.RESUME_POSSESSION_CLAIM.getEventId())
            .submitEvent(CaseTypeEvents.RESUME_POSSESSION_CLAIM.getEventId(), resumePossessionClaimPayload);

    }

    @Title("Event flow - respond to possession claim")
    @Order(2)
    @Test
    void respondPossessionClaim() {
        testContext.initialize().setCaseReference(caseId);
        testContext.asUser("ORG_LOCALAUTHORITY.solicitor1");
        Map<String, String> claimantParty = testContext.withCCDDataStoreApiSteps()
            .fetchCaseDetails()
            .getClaimantParty(0);

        Map<String, String> defenantParty = testContext.withCCDDataStoreApiSteps()
            .fetchCaseDetails()
            .getDefendantParty(1);

        String serviceRequestRef = testContext.withPCSTestingSupportApiSteps()
            .getFeePaymentInfo()
            .getPaymentReferenceForPartyId(claimantParty.get("id"));

        System.out.println("serviceRequestRef reference : " + serviceRequestRef);
        testContext.withPCSBaseApiSteps()
            .updatePayment(serviceRequestRef);

        testContext.asUser("DEFENDANT.CITIZEN_1")
            .withPCSTestingSupportApiSteps()
            .accessCodeIsFetchedForUser(defenantParty.get("firstName"),defenantParty.get("lastName"))
            .validateAccessCodeForParty(
                testContext.getCaseReference(),
                defenantParty.get("id"),
                defenantParty.get("firstName"),
                defenantParty.get("lastName")
            );

        ObjectNode respondPossessionClaimEventPayload =
            EventDataUtil.forEvent(CaseTypeEvents.RESPOND_POSSESSION_CLAIM).getData();
        testContext.withCCDDataStoreApiSteps()
            .triggerEvent(CaseTypeEvents.RESPOND_POSSESSION_CLAIM.getEventId())
            .validateEvent("respondPossessionClaimrespondToPossessionDraftSavePage", respondPossessionClaimEventPayload)
            .submitEvent(CaseTypeEvents.RESPOND_POSSESSION_CLAIM.getEventId(), respondPossessionClaimEventPayload);
    }
}
