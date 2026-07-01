package uk.gov.hmcts.reform.pcs;


import com.fasterxml.jackson.core.type.TypeReference;
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
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class CreatePossessionClaimTest extends CftlibTest {

    @Autowired
    private CcdClient ccdClient;

    @Autowired
    private IdamClient idamClient;

    @Autowired
    private ObjectMapper objectMapper;

    private String solicitorToken;
    private Long caseReference;

    @BeforeAll
    void setup() {
        solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
    }

    @Test
    @Order(1)
    void createPossessionClaim() {

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .addressLine2("Marylebone")
                                 .postTown("London")
                                 .county("Greater London")
                                 .postCode("NW1 6XE")
                                 .build()
            )
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        CaseDetails caseDetails = ccdClient.createCase(caseData, solicitorToken);

        caseReference = caseDetails.getId();
        assertThat(caseReference).isNotNull();

        CaseDetails retrievedCase = ccdClient.getCaseDetails(caseReference, solicitorToken);
        assertThat(retrievedCase.getState()).isEqualTo(State.AWAITING_SUBMISSION_TO_HMCTS.name());
    }

    @Test
    @Order(2)
    void resumePossessionClaim() {
        PCSCase caseData = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .defendant1(DefendantDetails.builder()
                            .nameKnown(VerticalYesNo.YES)
                            .firstName("Danny")
                            .lastName("Defendant")
                            .build())
            .noticeServed(YesOrNo.NO)
            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
            .build();

        CaseResource caseResource
            = ccdClient.updateCase(resumePossessionClaim, caseReference, caseData, solicitorToken);

        assertThat(caseResource.getReference()).isNotBlank();

        CaseDetails retrievedCase = ccdClient.getCaseDetails(caseReference, solicitorToken);

        TypeReference<List<ListValue<Party>>> partyList = new TypeReference<>() {};

        List<ListValue<Party>> allDefendants = objectMapper.convertValue(
            retrievedCase.getData().get("allDefendants"),
            partyList
        );
        assertThat(allDefendants).hasSize(1);
        assertThat(allDefendants)
            .extracting(ListValue::getValue)
            .extracting(Party::getFirstName)
                .containsExactly("Danny");

        assertThat(retrievedCase.getState()).isEqualTo(State.PENDING_CASE_ISSUED.name());
    }

}
