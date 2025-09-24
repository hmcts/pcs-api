package uk.gov.hmcts.reform.pcs;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestWithCCD extends CftlibTest {

    @Autowired
    private CoreCaseDataApi ccdApi;

    @Autowired
    private IdamClient idamClient;

    private String idamToken;
    private String s2sToken;

    @BeforeAll
    public void setup() {
        idamToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
        s2sToken = generateDummyS2SToken("ccd_gw");
    }

    @Test
    public void createsShellPossessionCase() {
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .addressLine2("Marylebone")
                                 .postTown("London")
                                 .county("Greater London")
                                 .postCode("NW1 6XE")
                                 .build())
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        CaseDetails caseDetails = startAndSubmitCreationEvent(EventId.createPossessionClaim, caseData);

        assertThat(caseDetails.getId()).isNotNull();
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

}
