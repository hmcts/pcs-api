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
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

@Slf4j
@Service
@RequiredArgsConstructor

public class CcdTestCaseOrchestrator {

    private static final String CASE_TYPE = CaseType.getCaseType();
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
                        "legislativeCountry": "England",
                        "claimantType": {
                            "value": {
                                "code": "PROVIDER_OF_SOCIAL_HOUSING",
                                "label": "Registered provider of social housing"
                            },
                            "list_items": [
                                {
                                    "code": "PRIVATE_LANDLORD",
                                    "label": "Private landlord"
                                },
                                {
                                    "code": "PROVIDER_OF_SOCIAL_HOUSING",
                                    "label": "Registered provider of social housing"
                                },
                                {
                                    "code": "MORTGAGE_LENDER",
                                    "label": "Mortgage lender"
                                },
                                {
                                    "code": "OTHER",
                                    "label": "Other"
                                }
                            ],
                            "valueCode": "PROVIDER_OF_SOCIAL_HOUSING"
                        },
                        "claimAgainstTrespassers": "NO",
                        "claimantName": "Possession Claims Solicitor Org",
                        "isClaimantNameCorrect": "YES",
                        "claimantContactEmail": "pcs-solicitor1@test.com",
                        "isCorrectClaimantContactEmail": "YES",
                        "orgAddressFound": "Yes",
                        "organisationAddress": {
                            "AddressLine1": "Ministry Of Justice",
                            "AddressLine2": "Seventh Floor 102 Petty France",
                            "PostTown": "London",
                            "PostCode": "SW1H 9AJ",
                            "Country": "United Kingdom"
                        },
                        "formattedClaimantContactAddress": "Ministry Of Justice<br>Seventh Floor 102 Petty\s
                        France<br>London<br>SW1H 9AJ",
                        "isCorrectClaimantContactAddress": "YES",
                        "claimantProvidePhoneNumber": "NO",
                        "defendant1": {
                            "nameKnown": "YES",
                            "firstName": "John",
                            "lastName": "doe",
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
                        "noRentArrears_MandatoryGrounds": [
                            "REPOSSESSION_GROUND2"
                        ],
                        "noRentArrears_DiscretionaryGrounds": [],
                        "repossessionByLenderTextArea": "test",
                        "preActionProtocolCompleted": "NO",
                        "mediationAttempted": "NO",
                        "settlementAttempted": "NO",
                        "noticeServed": "No",
                        "claimantNamePossessiveForm": "Possession Claims Solicitor Org’s",
                        "claimantCircumstancesSelect": "NO",
                        "hasDefendantCircumstancesInfo": "NO",
                        "suspensionOfRTB_ShowHousingActsPage": "Yes",
                        "demotionOfTenancy_ShowHousingActsPage": "No",
                        "suspensionToBuyDemotionOfTenancyPages": "No",
                        "alternativesToPossession": [
                            "SUSPENSION_OF_RIGHT_TO_BUY"
                        ],
                        "suspensionOfRTB_HousingAct": "SECTION_82A_2",
                        "suspensionOfRTB_Reason": "test",
                        "claimingCostsWanted": "NO",
                        "additionalReasonsForPossession": {
                            "hasReasons": "NO",
                            "reasons": null
                        },
                        "hasUnderlesseeOrMortgagee": "NO",
                        "wantToUploadDocuments": "NO",
                        "applicationWithClaim": "NO",
                        "languageUsed": "ENGLISH",
                        "completionNextStep": "SAVE_IT_FOR_LATER"
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
