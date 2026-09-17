package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoPreferNotToSay;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RTCStatementOfTruth;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;

import uk.gov.hmcts.reform.pcs.client.CcdClient;

import java.math.BigDecimal;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import static uk.gov.hmcts.reform.pcs.auth.ServiceAuthorizationGenerator.generateTestS2SToken;

import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResponseCreationService {

    private static final String CITIZEN_EMAIL_ADDRESS = "test@test.com";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${core_case_data.api.url}")
    private String dataStoreUrl;

    @Autowired
    private CcdClient ccdClient;
    @Autowired
    private IdamClient idamClient;
    @Autowired
    private CoreCaseDataApi ccdApi;

    private String solicitorToken;
    private String citizenToken;

    @BeforeAll
    void setup() {
        solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
        citizenToken = idamClient.getAccessToken("citizen@pcs.com", "password");
    }

    private void validateEventData(long caseReference, String eventId, String pageId,
                                   PCSCase caseData, String authorisation) {

        ccdApi.startEvent(authorisation, generateTestS2SToken("ccd_gw"), Long.toString(caseReference), eventId);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("id", eventId);
        event.put("summary", "string");
        event.put("description", "string");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orgNameFound", "Yes");
        data.put("claimantName", "TreeTops Housing");
        data.put("isClaimantNameCorrect", "YES");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", event);
        body.put("data", data);
        body.put("ignore_warning", false);
        body.put("case_reference", String.valueOf(caseReference));
        body.put("event_data", caseData);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorisation);
        headers.set("ServiceAuthorization", generateTestS2SToken("ccd_gw"));
        headers.set("Experimental", "True");
        headers.set("Accept",
                    "application/vnd.uk.gov.hmcts.ccd-data-store-api.case-data-validate.v2+json;charset=UTF-8");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String url = dataStoreUrl + "/case-types/PCS/validate?pageId=" + eventId + pageId;

        restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    public long createDefendantResponse(long caseReference, String userToken) {

        String bearerToken = userToken.startsWith("Bearer ") ? userToken : "Bearer " + userToken;

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .freeLegalAdvice(YesNoPreferNotToSay.NO)
            .hasSolicitor(VerticalYesNo.NO)

            .defendantNameConfirmation(VerticalYesNo.YES)
            .correspondenceAddressConfirmation(VerticalYesNo.YES)
            .contactByEmail(VerticalYesNo.YES)
            .contactByPhone(VerticalYesNo.YES)
            .contactByPost(VerticalYesNo.YES)
            .contactByText(VerticalYesNo.YES)

            .exemptLandlord(YesNoNotSure.NOT_SURE)
            .writtenTerms(YesNoNotSure.NOT_SURE)
            .tenancyTypeConfirmation(YesNoNotSure.YES)
            .tenancyStartDate(LocalDate.of(2026, 1, 1))
            .disputeClaim(VerticalYesNo.NO)
            .makeCounterClaim(VerticalYesNo.NO)

            .householdCircumstances(HouseholdCircumstances.builder()
                                        .dependantChildren(YesOrNo.NO)
                                        .otherDependants(YesOrNo.NO)
                                        .otherTenants(YesOrNo.NO)
                                        .alternativeAccommodation(YesNoNotSure.NOT_SURE)
                                        .shareAdditionalCircumstances(YesOrNo.NO)
                                        .exceptionalHardship(YesOrNo.YES)
                                        .exceptionalHardshipDetails("I cannot find alternative housing at short notice")
                                        .shareIncomeExpenseDetails(YesOrNo.YES)
                                        .universalCredit(YesOrNo.YES)
                                        .universalCreditAmount(BigDecimal.valueOf(1000))
                                        .universalCreditFrequency(RecurrenceFrequency.MONTHLY)
                                        .priorityDebts(YesOrNo.NO)
                                        .build())

            .otherConsiderations(VerticalYesNo.NO)
            .languageUsed(LanguageUsed.ENGLISH)

            .statementOfTruth(RTCStatementOfTruth.builder()
                                  .accepted(VerticalYesNo.YES)
                                  .fullName("Dominic Defendant")
                                  .build())
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                                         .party(Party.builder()
                                                    .firstName("Dominic")
                                                    .lastName("Defendant")
                                                    .phoneNumberProvided(VerticalYesNo.NO)
                                                    .build())
                                         .build())
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        validateEventData(caseReference, "respondPossessionClaim",
                          "respondToPossessionDraftSavePage", caseData, bearerToken);

        ccdClient.updateCase(respondPossessionClaim, caseReference, caseData, bearerToken);

        return caseReference;
    }
}
