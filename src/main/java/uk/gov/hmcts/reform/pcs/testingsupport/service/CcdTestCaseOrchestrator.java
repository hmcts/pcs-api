package uk.gov.hmcts.reform.pcs.testingsupport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.io.InputStream;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor

public class CcdTestCaseOrchestrator {

    private static final String CASE_TYPE = CaseType.getCaseType();
    private static final String CREATE_EVENT = "createPossessionClaim";
    private static final String RESUME_EVENT = "resumePossessionClaim";

    private final AuthTokenGenerator s2sAuthTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;
    private final ObjectMapper objectMapper;

    public Map<String, Object> createCase(String idamToken, LegislativeCountry legislativeCountry,
                                          JsonNode payloadMerge) {

        String s2sToken = s2sAuthTokenGenerator.generate();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(addressFor(legislativeCountry))
            .legislativeCountry(legislativeCountry)
            .build();

        StartEventResponse createEvent = coreCaseDataApi.startCase(
            idamToken,
            s2sToken,
            CASE_TYPE,
            CREATE_EVENT
        );

        CaseDataContent createContent = CaseDataContent.builder()
            .event(Event.builder().id(CREATE_EVENT).build())
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

        JsonNode resumePossessionClaimPayload = buildResumePossessionClaimPayload(legislativeCountry, payloadMerge);

        CaseDataContent resumeContent = CaseDataContent.builder()
            .event(Event.builder().id(RESUME_EVENT).build())
            .eventToken(resumeEvent.getToken())
            .data(resumePossessionClaimPayload)
            .build();

        CaseResource caseResource = coreCaseDataApi.createEvent(
            idamToken,
            s2sToken,
            caseId,
            resumeContent
        );

        log.info("Created CCD test case {}", caseId);

        return Map.of(
            "caseId", Long.valueOf(caseId),
            "caseDetails", caseResource.getData()
        );
    }

    private JsonNode buildResumePossessionClaimPayload(LegislativeCountry legislativeCountry, JsonNode payloadMerge
    ) {
        ObjectNode base = getBasePayload(legislativeCountry);

        if (payloadMerge != null && payloadMerge.isObject()) {
            mergePayload(base, (ObjectNode) payloadMerge);
        }

        return base;
    }

    private void mergePayload(ObjectNode target, ObjectNode source) {
        source.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode sourceValue = entry.getValue();
            JsonNode targetValue = target.get(fieldName);

            if (targetValue != null
                && targetValue.isObject()
                && sourceValue.isObject()) {

                mergePayload((ObjectNode) targetValue, (ObjectNode) sourceValue);

            } else {
                target.set(fieldName, sourceValue);
            }
        });
    }

    private ObjectNode getBasePayload(LegislativeCountry legislativeCountry) {
        String path = "testing-support/Create-Case-" + legislativeCountry + "-Base.json";

        try (InputStream basePayload = getClass()
            .getClassLoader()
            .getResourceAsStream(path)) {

            return (ObjectNode) objectMapper.readTree(basePayload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to read base payload JSON", e);
        }
    }

    private AddressUK addressFor(LegislativeCountry legislativeCountry) {
        return switch (legislativeCountry) {
            case ENGLAND -> AddressUK.builder()
                .addressLine1("1 Second Avenue")
                .postTown("London")
                .county("Greater London")
                .postCode("W3 7RX")
                .country("United Kingdom")
                .build();

            case WALES -> AddressUK.builder()
                .addressLine1("2 Pentre Street")
                .postTown("Caerdydd")
                .postCode("CF11 6QX")
                .country("Deyrnas Unedig")
                .build();

            default -> throw new IllegalArgumentException(
                "Unsupported legislative country: " + legislativeCountry
            );
        };
    }
}
