package uk.gov.hmcts.reform.pcs.testingsupport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

@Slf4j
@Service
@RequiredArgsConstructor

public class CcdTestCaseOrchestrator {

    private static final String CASE_TYPE = "PCS";
    private static final String CREATE_EVENT = "createPossessionClaim";
    private static final String RESUME_EVENT = "resumePossessionClaim";

    private final IdamClient idamClient;
    private final AuthTokenGenerator s2sAuthTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;

//    @BeforeAll
//    public void setup() {
//        idamToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
//        s2sToken = s2sAuthTokenGenerator.generate();
//    }

    public Long createCase(String idamToken, String s2sToken) {

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

        StartEventResponse createEvent = coreCaseDataApi.startCase(
            idamToken,
            s2sToken,
            CASE_TYPE,
            CREATE_EVENT
        );

        CaseDataContent createContent = CaseDataContent.builder()
            .eventToken(createEvent.getToken())
            .data(caseData)
            .build();

        CaseDetails caseDetails = coreCaseDataApi.submitCaseCreation(
            idamToken,
            s2sToken,
            CASE_TYPE,
            createContent
        );

        String caseId = caseDetails.getId().toString();

        StartEventResponse resumeEvent = coreCaseDataApi.startEvent(
            idamToken,
            s2sToken,
            caseId,
            RESUME_EVENT
        );

        JsonNode resumePossessionClaimPayload = buildResumePossessionClaimPayload();

        CaseDataContent resumeContent = CaseDataContent.builder()
            .eventToken(resumeEvent.getToken())
            .data(resumePossessionClaimPayload)
            .build();

        coreCaseDataApi.createEvent(
            idamToken,
            s2sToken,
            caseId,
            resumeContent
        );

        log.info("Created CCD test case {}", caseId);
        return Long.valueOf(caseId);
    }

    private JsonNode buildResumePossessionClaimPayload() {
        try {
            String json = """
            {
              "claimReference": "PCS-123456",
              "claimType": "Possession",
              "claimDate": "2026-01-16",
              "claimAmount": 1234.56,
              "propertyAddress": {
                "line1": "10 Downing Street",
                "line2": "",
                "town": "London",
                "postcode": "SW1A 2AA"
              },
              "defendant": {
                "firstName": "John",
                "lastName": "Doe",
                "dateOfBirth": "1980-01-01",
                "idamUserId": "1234567890"
              },
              "legalDetails": {
                "legislation": "Housing Act 1988",
                "courtFee": 455
              },
              "additionalData": {
                "someFlag": true,
                "notes": "Test case for resumePossessionClaim"
              }
            }
            """;

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build resumePossessionClaimPayload JSON", e);
        }
    }
}
