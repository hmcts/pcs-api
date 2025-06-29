package uk.gov.hmcts.reform.pcs;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.PCSCaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestWithCCD extends CftlibTest {

    @Autowired
    private CoreCaseDataApi ccdApi;

    @Autowired
    private IdamClient idamClient;

    CaseDetails caseDetails;
    String idamToken;
    String s2sToken;
    String userId;

    @BeforeAll
    public void setup() {
        idamToken = idamClient.getAccessToken("caseworker@pcs.com", "password");
        s2sToken = generateDummyS2SToken("ccd_gw");
        userId = idamClient.getUserInfo(idamToken).getUid();
    }

    @Order(1)
    @Test
    public void createsTestCase() {
        var r = ccdApi.startCase(idamToken, s2sToken, PCSCaseType.getCaseType(), "createTestApplication");

        GACase generalApp = GACase.builder()
            .caseReference(1L)
            .adjustment("Wheelchair access")
            .additionalInformation("Requires support dog")
            .status(State.Draft) // or whatever state is valid
            .build();

        PCSCase caseData = PCSCase.builder()
            .applicantForename("Foo")
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .addressLine2("Marylebone")
                                 .postTown("London")
                                 .county("Greater London")
                                 .postCode("NW1 6XE")
                                 .build())
            .generalApplications(List.of(
                ListValue.<GACase>builder()
                    .id(UUID.randomUUID().toString())
                    .value(GACase.builder()
                               .caseReference(1L)
                               .adjustment("Wheelchair access")
                               .additionalInformation("Requires interpreter")
                               .status(State.Draft)
                               .build()).build())).build();
        // .generalApplications(wrapListItems(List.of(generalApp))).build();
        var content = CaseDataContent.builder()
            .data(caseData)
            .event(Event.builder().id("createTestApplication").build())
            .eventToken(r.getToken())
            .build();
        caseDetails = ccdApi.submitForCaseworker(idamToken, s2sToken, userId,
                "PCS", PCSCaseType.getCaseType(), false, content
        );
        assertThat(caseDetails.getId()).isNotNull();
    }

}
