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
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

@Slf4j
@Service
@RequiredArgsConstructor

public class CcdTestCaseOrchestrator {

    private static final String CASE_TYPE = "PCS-1237";
    private static final String CREATE_EVENT = "createPossessionClaim";
    private static final String RESUME_EVENT = "resumePossessionClaim";

    private final AuthTokenGenerator s2sAuthTokenGenerator;
    private final CoreCaseDataApi coreCaseDataApi;

    public Long createCase(String idamToken, LegislativeCountry legislativeCountry, JsonNode payloadOverride) {

        String s2sToken = s2sAuthTokenGenerator.generate();

        log.info("About to start CCD event '{}'", CREATE_EVENT);

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
        log.info("Started CCD event '{}' – token received", CREATE_EVENT);

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
        log.info("CCD case created successfully with caseId={}", caseId);

        StartEventResponse resumeEvent = coreCaseDataApi.startEvent(
            idamToken,
            s2sToken,
            caseId,
            RESUME_EVENT
        );

        JsonNode resumePossessionClaimPayload = buildResumePossessionClaimPayload(payloadOverride);

        CaseDataContent resumeContent = CaseDataContent.builder()
            .event(Event.builder().id(RESUME_EVENT).build())
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


    private JsonNode buildResumePossessionClaimPayload(JsonNode payloadOverride
    ) {
        ObjectNode base = baseResumePossessionClaimPayload();

        if (payloadOverride != null && payloadOverride.isObject()) {
            deepMerge(base, (ObjectNode) payloadOverride);
        }

        return base;
    }

    private void deepMerge(ObjectNode target, ObjectNode source) {
        source.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode sourceValue = entry.getValue();
            JsonNode targetValue = target.get(fieldName);

            if (targetValue != null
                && targetValue.isObject()
                && sourceValue.isObject()) {

                deepMerge((ObjectNode) targetValue, (ObjectNode) sourceValue);

            } else {
                target.set(fieldName, sourceValue);
            }
        });
    }

    private ObjectNode baseResumePossessionClaimPayload() {
        try {
            String json = """
        {
          "claimantType": {
            "value": {
              "code": "PROVIDER_OF_SOCIAL_HOUSING",
              "label": "Registered provider of social housing"
            },
            "list_items": [
              { "code": "PRIVATE_LANDLORD", "label": "Private landlord" },
              { "code": "PROVIDER_OF_SOCIAL_HOUSING", "label": "Registered provider of social housing" },
              { "code": "MORTGAGE_LENDER", "label": "Mortgage lender" },
              { "code": "OTHER", "label": "Other" }
            ],
            "valueCode": "PROVIDER_OF_SOCIAL_HOUSING"
          },
          "claimAgainstTrespassers": "NO",
          "claimantName": "pcs-solicitor1@test.com",
          "isClaimantNameCorrect": "YES",
          "claimantContactEmail": "pcs-solicitor1@test.com",
          "isCorrectClaimantContactEmail": "YES",
          "orgAddressFound": "No",
          "organisationAddress": null,
          "formattedClaimantContactAddress": null,
          "overriddenClaimantContactAddress": {
            "AddressLine1": "1 Rse Way",
            "AddressLine2": "",
            "AddressLine3": "",
            "PostTown": "London",
            "County": "",
            "Country": "United Kingdom",
            "PostCode": "SW11 1PD"
          },
          "claimantProvidePhoneNumber": "NO",
          "defendant1": {
            "nameKnown": "NO",
            "firstName": null,
            "lastName": null,
            "addressKnown": "NO",
            "addressSameAsPossession": null,
            "correspondenceAddress": {
              "AddressLine1": null,
              "AddressLine2": null,
              "AddressLine3": null,
              "PostTown": null,
              "County": null,
              "Country": null,
              "PostCode": null
            }
          },
          "addAnotherDefendant": "NO",
          "tenancy_TypeOfTenancyLicence": "ASSURED_TENANCY",
          "tenancy_TenancyLicenceDate": null,
          "tenancy_TenancyLicenceDocuments": [],
          "claimDueToRentArrears": "No",
          "showRentSectionPage": "No",
          "noRentArrears_ShowGroundReasonPage": "Yes",
          "noRentArrears_MandatoryGrounds": [ "OWNER_OCCUPIER" ],
          "noRentArrears_DiscretionaryGrounds": [],
          "ownerOccupierTextArea": "test",
          "preActionProtocolCompleted": "NO",
          "mediationAttempted": "NO",
          "settlementAttempted": "NO",
          "noticeServed": "No",
          "claimantNamePossessiveForm": "pcs-solicitor1@test.com’s",
          "claimantCircumstancesSelect": "NO",
          "hasDefendantCircumstancesInfo": "NO",
          "suspensionOfRTB_ShowHousingActsPage": "No",
          "demotionOfTenancy_ShowHousingActsPage": "No",
          "suspensionToBuyDemotionOfTenancyPages": "No",
          "alternativesToPossession": [],
          "claimingCostsWanted": "NO",
          "additionalReasonsForPossession": {
            "hasReasons": "NO",
            "reasons": null
          },
          "hasUnderlesseeOrMortgagee": "NO",
          "wantToUploadDocuments": "NO",
          "applicationWithClaim": "NO",
          "languageUsed": "ENGLISH",
          "completionNextStep": "SUBMIT_AND_PAY_NOW",
          "statementOfTruth": {
            "completedBy": "CLAIMANT",
            "fullNameClaimant": "ghjk",
            "positionClaimant": "bhj",
            "fullNameLegalRep": null,
            "firmNameLegalRep": null,
            "positionLegalRep": null,
            "agreementClaimant": [ "BELIEVE_TRUE" ],
            "agreementLegalRep": []
          }
        }
                """;

            ObjectMapper mapper = new ObjectMapper();
            return (ObjectNode) mapper.readTree(json);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build resumePossessionClaimPayload JSON", e);
        }
    }

    private AddressUK addressFor(LegislativeCountry legislativeCountry) {
        return switch (legislativeCountry) {
            case ENGLAND -> AddressUK.builder()
                .addressLine1("1 Second Avenue")
                .postTown("London")
                .county("Greater London")
                .postCode("W3 7RX")
                .county("United Kingdom")
                .build();

            case WALES -> AddressUK.builder()
                .addressLine1("2 Pentre Street")
                .postTown("Caerdydd")
                .postCode("CF11 6QX")
                .county("Deyrnas Unedig")
                .build();

            default -> throw new IllegalArgumentException(
                "Unsupported legislative country: " + legislativeCountry
            );
        };
    }
}
