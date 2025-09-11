package uk.gov.hmcts.reform.pcs;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd3.CaseType;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd3.domain.State;
import uk.gov.hmcts.reform.pcs.ccd3.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd3.event.EventId;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd3.event.EventId.citizenCreateApplication;
import static uk.gov.hmcts.reform.pcs.ccd3.event.EventId.citizenSubmitApplication;
import static uk.gov.hmcts.reform.pcs.ccd3.event.EventId.citizenUpdateApplication;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class CitizenCreateApplicationTest extends CftlibTest {

    @Autowired
    private CoreCaseDataApi ccdApi;

    @Autowired
    private IdamClient idamClient;

    @Autowired
    private ObjectMapper objectMapper;

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

        PCSCase caseData = PCSCase.builder()
            .claimantName("Wrong Name")
            .isClaimantNameCorrect(VerticalYesNo.NO)
            .overriddenClaimantName("New Name")
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .addressLine2("Marylebone")
                                 .postTown("London")
                                 .county("Greater London")
                                 .postCode("NW1 6XE")
                                 .build())
            .paymentStatus(PaymentStatus.UNPAID)
            .build();

        CaseDetails caseDetails = startAndSubmitCreationEvent(citizenCreateApplication, caseData);

        caseReference = caseDetails.getId();
        assertThat(caseReference).isNotNull();

        CaseDetails retrievedCase = ccdApi.getCase(idamToken, s2sToken, Long.toString(caseReference));
        assertThat(retrievedCase.getState()).isEqualTo(State.AWAITING_SUBMISSION_TO_HMCTS.name());
    }

    @Test
    @Order(2)
    void citizenUpdatesApplication() {
        AddressUK updatedAddress = AddressUK.builder()
            .addressLine1("89 Lower Street")
            .addressLine2("WestMinister")
            .postTown("London")
            .county("Greater London")
            .postCode("W3 4FD")
            .build();
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(updatedAddress)
            .build();

        CaseResource caseResource = startAndSubmitUpdateEvent(citizenUpdateApplication, caseData);

        assertThat(caseResource.getReference()).isNotBlank();

        CaseDetails retrievedCase = ccdApi.getCase(idamToken, s2sToken, Long.toString(caseReference));
        AddressUK actualAddress = objectMapper.convertValue(
            retrievedCase.getData().get("propertyAddress"),
            AddressUK.class
        );
        assertThat(actualAddress).isEqualTo(updatedAddress);
        assertThat(retrievedCase.getState()).isEqualTo(State.AWAITING_SUBMISSION_TO_HMCTS.name());
    }

    @Test
    @Order(3)
    void citizenSubmitsApplication() {
        PCSCase caseData = PCSCase.builder()
            .build();

        startAndSubmitUpdateEvent(citizenSubmitApplication, caseData);

        CaseDetails retrievedCase = ccdApi.getCase(idamToken, s2sToken, Long.toString(caseReference));
        assertThat(retrievedCase.getState()).isEqualTo(State.CASE_ISSUED.name());
    }

    @SuppressWarnings("SameParameterValue")
    private CaseDetails startAndSubmitCreationEvent(EventId eventId, PCSCase caseData) {
        StartEventResponse startEventResponse = ccdApi.startCase(
            idamToken,
            s2sToken,
            CaseType.getCaseType(),
            eventId.name()
        );

        CaseDataContent content = CaseDataContent.builder()
            .data(caseData)
            .event(Event.builder().id(eventId.name()).build())
            .eventToken(startEventResponse.getToken())
            .build();

        return ccdApi.submitCaseCreation(idamToken, s2sToken, CaseType.getCaseType(), content);
    }

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
