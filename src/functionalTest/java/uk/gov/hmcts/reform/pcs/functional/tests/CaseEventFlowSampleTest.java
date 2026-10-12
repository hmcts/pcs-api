package uk.gov.hmcts.reform.pcs.functional.tests;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import uk.gov.hmcts.reform.pcs.functional.steps.BaseApi;
import uk.gov.hmcts.reform.pcs.functional.config.CaseTypeEvents;
import uk.gov.hmcts.reform.pcs.functional.testutils.EventDataUtil;
import uk.gov.hmcts.reform.pcs.functional.testutils.TestContext;
import uk.gov.hmcts.reform.pcs.functional.testutils.eventData.ResumePossessionClaimEventData;

import java.util.Map;

@Tag("Functional1")
@EnabledIfEnvironmentVariable(named = "CCD_ENABLED", matches = "true")
@DisabledIfEnvironmentVariable(named = "SHUTTER_SERVICE", matches = "true")
@ExtendWith(SerenityJUnit5Extension.class)
class CaseEventFlowSampleTest extends BaseApi {

    @Steps
    TestContext testContext;

    @Title("Event flow - create case with multiple defendants")
    @Test
    void createCaseWithMultipleDefendants() {
        testContext.initialize().asUser("ORG_LOCALAUTHORITY.solicitor1");

        testContext.withCCDDataStoreApiSteps()
            .createPossessionClaimCase("england");
        testContext.setCaseReference(testContext.withCCDDataStoreApiSteps().getCaseReference());

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
    @Test
    void respondPossessionClaim() {
        testContext.initialize().asUser("ORG_LOCALAUTHORITY.solicitor1");
        testContext.withPCSTestingSupportApiSteps()
            .createCaseForLegislativeCountry("england", null, true);
        testContext.setCaseReference(
            testContext.withPCSTestingSupportApiSteps().getTestingSupportCaseReference().toString()
        );
        Map<String, String> defendantParty = testContext.withCCDDataStoreApiSteps()
            .getDefendantParty(testContext.getCaseReference(), 0);

        testContext.asUser("DEFENDANT.CITIZEN_1")
            .withPCSTestingSupportApiSteps()
            .validateAccessCodeForParty(
                testContext.getCaseReference(),
                defendantParty.get("id"),
                defendantParty.get("firstName"),
                defendantParty.get("lastName")
            );

        ObjectNode respondPossessionClaimEventPayload =
            EventDataUtil.forEvent(CaseTypeEvents.RESPOND_POSSESSION_CLAIM).getData();
        testContext.withCCDDataStoreApiSteps()
            .triggerEvent(CaseTypeEvents.RESPOND_POSSESSION_CLAIM.getEventId())
            .validateEvent("respondPossessionClaimrespondToPossessionDraftSavePage", respondPossessionClaimEventPayload)
            .submitEvent(CaseTypeEvents.RESPOND_POSSESSION_CLAIM.getEventId(), respondPossessionClaimEventPayload);
    }
}
