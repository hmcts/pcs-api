package uk.gov.hmcts.reform.pcs;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class CitizenCreateApplicationTest extends CftlibTest {

    @Autowired
    private CoreCaseDataApi ccdApi;

    @Autowired
    private IdamClient idamClient;

    private String idamToken;
    private String s2sToken;
    private Long caseReference;

    @BeforeAll
    void setup() {
        idamToken = idamClient.getAccessToken("citizen@pcs.com", "password");
        s2sToken = generateDummyS2SToken("ccd_gw");
    }

    @Test
    @Order(1)
    void citizenCreatesApplication() {
        // String applicantForename = "Test Forename";
        // String applicantSurname = "Test Surname";
        //
        // PCSCase caseData = PCSCase.builder()
        //     .applicantForename(applicantForename)
        //     .applicantSurname(applicantSurname)
        //     .propertyAddress(AddressUK.builder()
        //         .addressLine1("123 Baker Street")
        //         .addressLine2("Marylebone")
        //         .postTown("London")
        //         .county("Greater London")
        //         .postCode("NW1 6XE")
        //         .build())
        //     .paymentStatus(PaymentStatus.UNPAID)
        //     .build();
        //
        // CaseDetails caseDetails = startAndSubmitCreationEvent(citizenCreateApplication, caseData);
        //
        // caseReference = caseDetails.getId();
        // assertThat(caseReference).isNotNull();
        //
        // CaseDetails retrievedCase = ccdApi.getCase(idamToken, s2sToken, Long.toString(caseReference));
        // assertThat(retrievedCase.getData().get("applicantForename")).isEqualTo(applicantForename);
        // assertThat(retrievedCase.getData().get("applicantSurname")).isEqualTo(applicantSurname);
        // assertThat(retrievedCase.getState()).isEqualTo(State.AWAITING_SUBMISSION_TO_HMCTS.name());
    }

    @Test
    @Order(2)
    void citizenUpdatesApplication() {
        // String updatedForename = "Updated Forename";
        //
        // PCSCase caseData = PCSCase.builder()
        //     .applicantForename(updatedForename)
        //     .build();
        //
        // CaseResource caseResource = startAndSubmitUpdateEvent(citizenUpdateApplication, caseData);
        //
        // assertThat(caseResource.getReference()).isNotBlank();
        //
        // CaseDetails retrievedCase = ccdApi.getCase(idamToken, s2sToken, Long.toString(caseReference));
        // assertThat(retrievedCase.getData().get("applicantForename")).isEqualTo(updatedForename);
        // assertThat(retrievedCase.getState()).isEqualTo(State.AWAITING_SUBMISSION_TO_HMCTS.name());
    }

    @Test
    @Order(3)
    void citizenSubmitsApplication() {
        // PCSCase caseData = PCSCase.builder()
        //     .build();
        //
        // startAndSubmitUpdateEvent(citizenSubmitApplication, caseData);
        //
        // CaseDetails retrievedCase = ccdApi.getCase(idamToken, s2sToken, Long.toString(caseReference));
        // assertThat(retrievedCase.getState()).isEqualTo(State.CASE_ISSUED.name());
    }

    // @SuppressWarnings("SameParameterValue")
    // private CaseDetails startAndSubmitCreationEvent(EventId eventId, PCSCase caseData) {
    //     StartEventResponse startEventResponse = ccdApi.startCase(
    //         idamToken,
    //         s2sToken,
    //         CaseType.getCaseType(),
    //         eventId.name()
    //     );
    //
    //     CaseDataContent content = CaseDataContent.builder()
    //         .data(caseData)
    //         .event(Event.builder().id(eventId.name()).build())
    //         .eventToken(startEventResponse.getToken())
    //         .build();
    //
    //     return ccdApi.submitCaseCreation(idamToken, s2sToken, CaseType.getCaseType(), content);
    // }

    private CaseResource startAndSubmitUpdateEvent(EventId eventId, PCSCase caseData) {
        StartEventResponse startEventResponse = ccdApi.startEvent(
            idamToken,
            s2sToken,
            Long.toString(caseReference),
            eventId.name()
        );

        CaseDataContent content = CaseDataContent.builder()
            .data(caseData)
            .event(Event.builder().id(eventId.name()).build())
            .eventToken(startEventResponse.getToken())
            .build();

        return ccdApi.createEvent(idamToken, s2sToken, Long.toString(caseReference), content);
    }
}
