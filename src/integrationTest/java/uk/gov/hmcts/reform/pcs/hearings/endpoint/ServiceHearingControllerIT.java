package uk.gov.hmcts.reform.pcs.hearings.endpoint;

import feign.FeignException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkReasonEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.hearings.service.ServiceHearingValuesMapper;
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;
import uk.gov.hmcts.reform.pcs.util.IdamHelper;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Transactional
@DisplayName("ServiceHearingController Integration Tests")
class ServiceHearingControllerIT extends AbstractPostgresContainerIT {

    private static final String HEARING_VALUES_PATH = "/serviceHearingValues";
    private static final String LINKED_CASES_PATH = "/serviceLinkedCases";
    private static final String AUTH_HEADER = "Bearer token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String SYSTEM_USER_ID_TOKEN = "system-user-id-token";
    private static final String USER_ID = "123e4567-e89b-12d3-a456-426614174001";

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final long LINKED_CASE_REFERENCE = 2234567890123456L;
    private static final long CASE_NOT_IN_PCS = 3234567890123456L;
    private static final String CASE_NAME = "Test Claimant vs John Doe";
    private static final int CASE_MANAGEMENT_LOCATION = 20262;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private IdamHelper idamHelper;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private FlagRefDataRepository flagRefDataRepository;
    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @MockitoBean
    private IdamUserInfoApi idamUserInfoApi;
    @MockitoSpyBean
    private FeatureToggleService featureToggleService;
    @MockitoSpyBean
    private ServiceHearingValuesMapper serviceHearingValuesMapper;

    @BeforeEach
    void setUp() {
        idamHelper.stubIdamSystemUser(authorizedClientManager, SYSTEM_USER_ID_TOKEN);
        // Both endpoints sit behind IdamAuthenticationFilter, which validates the user token against IDAM
        when(idamUserInfoApi.getUserInfo(anyString())).thenReturn(UserInfo.builder().uid(USER_ID).build());
        givenHmcIntegrationEnabled(true);
    }

    @Nested
    @DisplayName("POST /serviceHearingValues")
    class ServiceHearingValuesEndpoint {

        @Test
        @DisplayName("builds hearing values from the stored case and the configured defaults")
        void shouldReturnHearingValuesForStoredCase() throws Exception {
            // 23:30 UTC in British Summer Time is 00:30 the next day in the UK
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, LocalDateTime.of(2026, 6, 30, 23, 30));
            pcsCase.setCaseManagementLocation(CASE_MANAGEMENT_LOCATION);
            persist(pcsCase);

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(String.valueOf(CASE_REFERENCE)))
                .andExpect(jsonPath("$.hmctsServiceID").value("AAA3"))
                .andExpect(jsonPath("$.hmctsInternalCaseName").value(CASE_NAME))
                .andExpect(jsonPath("$.publicCaseName").value(CASE_NAME))
                .andExpect(jsonPath("$.caseDeepLink").value(
                    "http://localhost:3000/cases/case-details/PCS/PCS/" + CASE_REFERENCE + "#Case%20File%20View"))
                .andExpect(jsonPath("$.caseSLAStartDate").value("2026-07-01"))
                .andExpect(jsonPath("$.caseManagementLocationCode").value(String.valueOf(CASE_MANAGEMENT_LOCATION)))
                .andExpect(jsonPath("$.hearingLocations[0].locationType").value("court"))
                .andExpect(jsonPath("$.hearingLocations[0].locationId").value(String.valueOf(CASE_MANAGEMENT_LOCATION)))
                .andExpect(jsonPath("$.hearingType").value("AAA3-FPH"))
                .andExpect(jsonPath("$.hearingChannels", contains("INTER")))
                .andExpect(jsonPath("$.hearingPriorityType").value("Standard"))
                // Issued 1 July 2026 in the UK: start is 27 days later and end is 57 days later
                .andExpect(jsonPath("$.hearingWindow.dateRangeStart").value("2026-07-28"))
                .andExpect(jsonPath("$.hearingWindow.dateRangeEnd").value("2026-08-27"))
                .andExpect(jsonPath("$.hearingWindow.firstDateTimeMustBe").doesNotExist())
                .andExpect(jsonPath("$.panelRequirements.roleType", contains("24", "45")))
                .andExpect(jsonPath("$.listingComments").doesNotExist());
        }

        @Test
        @DisplayName("leaves out the location codes and hearing window when the case has neither")
        void shouldOmitLocationAndHearingWindowWhenNotAvailable() throws Exception {
            persist(newCase(CASE_REFERENCE, null));

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseManagementLocationCode").doesNotExist())
                .andExpect(jsonPath("$.hearingLocations[0].locationId").doesNotExist())
                .andExpect(jsonPath("$.hearingWindow").doesNotExist());
        }

        @Test
        @DisplayName("sets an urgent priority when the case has an active Urgent case flag")
        void shouldSetUrgentPriorityForActiveUrgentFlag() throws Exception {
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            addCaseFlag(pcsCase, refData("CF0007", "Urgent case"), "Active", null);
            persist(pcsCase);

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hearingPriorityType").value("Urgent"));
        }

        @Test
        @DisplayName("keeps the standard priority when the Urgent case flag is not active")
        void shouldKeepStandardPriorityForInactiveUrgentFlag() throws Exception {
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            addCaseFlag(pcsCase, refData("CF0007", "Urgent case"), "Inactive", null);
            persist(pcsCase);

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hearingPriorityType").value("Standard"));
        }

        @Test
        @DisplayName("sets an urgent priority when the case has an open general application to suspend")
        void shouldSetUrgentPriorityForOpenSuspendApplication() throws Exception {
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            addGenApp(pcsCase, GenAppType.SUSPEND, GenAppState.GEN_APP_ISSUED);
            persist(pcsCase);

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hearingPriorityType").value("Urgent"));
        }

        @Test
        @DisplayName("keeps the standard priority when the application to suspend has not been issued")
        void shouldKeepStandardPriorityForSuspendApplicationNotYetIssued() throws Exception {
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            addGenApp(pcsCase, GenAppType.SUSPEND, GenAppState.PENDING_GEN_APP_ISSUED);
            persist(pcsCase);

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hearingPriorityType").value("Standard"));
        }

        @Test
        @DisplayName("keeps the standard priority when the open general application is not to suspend")
        void shouldKeepStandardPriorityForOpenApplicationThatIsNotToSuspend() throws Exception {
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            addGenApp(pcsCase, GenAppType.ADJOURN, GenAppState.GEN_APP_ISSUED);
            persist(pcsCase);

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hearingPriorityType").value("Standard"));
        }

        @Test
        @DisplayName("counts active parties plus the duty solicitor as physical attendees")
        void shouldCountActivePartiesPlusDutySolicitor() throws Exception {
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            PartyEntity removed = party("Gone", "Away");
            removed.setRemoved(true);
            pcsCase.addParty(removed);
            persist(pcsCase);

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberOfPhysicalAttendees").value(3))
                .andExpect(jsonPath("$.caseSLAStartDate").doesNotExist());
        }

        @Test
        @DisplayName("puts only active case flags into the listing comments")
        void shouldIncludeOnlyActiveCaseFlagsInListingComments() throws Exception {
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            addCaseFlag(pcsCase, refData("CF0002", "Complex case"), "Active", null);
            addCaseFlag(pcsCase, refData("CF0007", "Urgent case"), "Active", "so urgent");
            addCaseFlag(pcsCase, refData("CF0004", "Vulnerable user"), "Requested", null);
            persist(pcsCase);

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingComments")
                    .value("Case Flags: Complex case, Urgent case (Comments: so urgent)"));
        }

        @Test
        @DisplayName("ignores fields it does not recognise in the request")
        void shouldIgnoreUnknownRequestFields() throws Exception {
            persist(newCase(CASE_REFERENCE, null));

            send(HEARING_VALUES_PATH,
                 "{\"caseReference\":\"" + CASE_REFERENCE + "\",\"hearingId\":\"2000001234\",\"foo\":\"bar\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(String.valueOf(CASE_REFERENCE)));
        }
    }

    @Nested
    @DisplayName("POST /serviceLinkedCases")
    class ServiceLinkedCasesEndpoint {

        @Test
        @DisplayName("returns every link with the linked case's name and reasons")
        void shouldReturnLinkedCasesWithNamesAndReasons() throws Exception {
            persist(newCase(LINKED_CASE_REFERENCE, null));
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            addLink(pcsCase, LINKED_CASE_REFERENCE, "CLRC001", "CLRC002");
            addLink(pcsCase, CASE_NOT_IN_PCS, "CLRC003");
            persist(pcsCase);

            // Neither case links nor their reasons have an @OrderBy, so assert without relying on order
            String linked = "$[?(@.caseReference == '" + LINKED_CASE_REFERENCE + "')]";
            String notInPcs = "$[?(@.caseReference == '" + CASE_NOT_IN_PCS + "')]";

            postFor(LINKED_CASES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].caseReference", containsInAnyOrder(
                    String.valueOf(LINKED_CASE_REFERENCE),
                    String.valueOf(CASE_NOT_IN_PCS))))
                .andExpect(jsonPath(linked + ".caseName", contains(CASE_NAME)))
                .andExpect(jsonPath(linked + ".reasonsForLink[*]", containsInAnyOrder("CLRC001", "CLRC002")))
                .andExpect(jsonPath(notInPcs + ".caseName", empty()))
                .andExpect(jsonPath(notInPcs + ".reasonsForLink[*]", contains("CLRC003")));
        }

        @Test
        @DisplayName("returns an empty list when the case has no links")
        void shouldReturnEmptyListWhenCaseHasNoLinks() throws Exception {
            persist(newCase(CASE_REFERENCE, null));

            postFor(LINKED_CASES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        }
    }

    @Nested
    @DisplayName("Both endpoints")
    class BothEndpoints {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {HEARING_VALUES_PATH, LINKED_CASES_PATH})
        @DisplayName("return 404 when the case is not in PCS")
        void shouldReturnNotFoundWhenCaseIsNotInPcs(String path) throws Exception {
            postFor(path, CASE_NOT_IN_PCS)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Case not found"));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {HEARING_VALUES_PATH, LINKED_CASES_PATH})
        @DisplayName("return 404 without reading the case when HMC integration is disabled")
        void shouldReturnNotFoundWhenHmcIntegrationDisabled(String path) throws Exception {
            givenHmcIntegrationEnabled(false);
            persist(newCase(CASE_REFERENCE, null));

            postFor(path, CASE_REFERENCE)
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

            verifyMapperNotCalled();
        }

        @ParameterizedTest(name = "{0} with body {1}")
        @MethodSource("uk.gov.hmcts.reform.pcs.hearings.endpoint.ServiceHearingControllerIT#invalidRequestBodies")
        @DisplayName("return 400 when the case reference is missing or not 16 digits")
        void shouldRejectInvalidCaseReference(String path, String body) throws Exception {
            send(path, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid data"));

            verifyMapperNotCalled();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {HEARING_VALUES_PATH, LINKED_CASES_PATH})
        @DisplayName("return 400 when the body is not valid JSON")
        void shouldRejectMalformedBody(String path) throws Exception {
            send(path, "{\"caseReference\":")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid data"));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {HEARING_VALUES_PATH, LINKED_CASES_PATH})
        @DisplayName("return 400 when the ServiceAuthorization header is missing")
        void shouldRejectMissingServiceAuthorizationHeader(String path) throws Exception {
            mockMvc.perform(post(path)
                                .header(AUTHORIZATION, AUTH_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(CASE_REFERENCE)))
                .andExpect(status().isBadRequest());

            verifyMapperNotCalled();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {HEARING_VALUES_PATH, LINKED_CASES_PATH})
        @DisplayName("return 401 without calling IDAM when the Authorization header is missing")
        void shouldRejectMissingAuthorizationHeader(String path) throws Exception {
            mockMvc.perform(post(path)
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(CASE_REFERENCE)))
                .andExpect(status().isUnauthorized());

            verify(idamUserInfoApi, never()).getUserInfo(anyString());
            verifyMapperNotCalled();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {HEARING_VALUES_PATH, LINKED_CASES_PATH})
        @DisplayName("return 401 when IDAM rejects the user token")
        void shouldRejectTokenThatIdamRejects(String path) throws Exception {
            when(idamUserInfoApi.getUserInfo(anyString())).thenThrow(FeignException.Unauthorized.class);

            postFor(path, CASE_REFERENCE)
                .andExpect(status().isUnauthorized());

            verifyMapperNotCalled();
        }
    }

    static Stream<Arguments> invalidRequestBodies() {
        return Stream.of(HEARING_VALUES_PATH, LINKED_CASES_PATH).flatMap(path -> Stream.of(
            Arguments.of(path, "{}"),
            Arguments.of(path, "{\"caseReference\":null}"),
            Arguments.of(path, "{\"caseReference\":\"   \"}"),
            Arguments.of(path, "{\"caseReference\":\"123456789012345\"}"),
            Arguments.of(path, "{\"caseReference\":\"12345678901234567\"}"),
            Arguments.of(path, "{\"caseReference\":\"123456789012345a\"}"),
            Arguments.of(path, "{\"caseReference\":\"1234-5678-9012-3456\"}")
        ));
    }

    private ResultActions postFor(String path, long caseReference) throws Exception {
        return send(path, body(caseReference));
    }

    private ResultActions send(String path, String body) throws Exception {
        return mockMvc.perform(post(path)
                                   .header(AUTHORIZATION, AUTH_HEADER)
                                   .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(body));
    }

    private static String body(long caseReference) {
        return "{\"caseReference\":\"" + caseReference + "\"}";
    }

    private void givenHmcIntegrationEnabled(boolean enabled) {
        doReturn(enabled).when(featureToggleService).isEnabled(FeatureFlag.HMC_INTEGRATION);
    }

    private void verifyMapperNotCalled() {
        verify(serviceHearingValuesMapper, never()).getServiceHearingValues(anyLong());
        verify(serviceHearingValuesMapper, never()).getServiceLinkedCases(anyLong());
    }

    /**
     * Saves the case and clears the persistence context, so the request reads it back from Postgres
     * rather than getting the in-memory entity from the first-level cache.
     */
    private void persist(PcsCaseEntity pcsCase) {
        pcsCaseRepository.saveAndFlush(pcsCase);
        entityManager.clear();
    }

    private static PcsCaseEntity newCase(long caseReference, LocalDateTime claimIssuedDate) {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setCaseReference(caseReference);

        ClaimEntity claim = ClaimEntity.builder().claimIssuedDate(claimIssuedDate).build();
        pcsCase.addClaim(claim);

        PartyEntity claimant = party(null, null);
        claimant.setOrgName("Test Claimant");
        PartyEntity defendant = party("John", "Doe");
        defendant.setNameKnown(VerticalYesNo.YES);

        pcsCase.addParty(claimant);
        pcsCase.addParty(defendant);
        claim.addParty(claimant, PartyRole.CLAIMANT);
        claim.addParty(defendant, PartyRole.DEFENDANT);
        return pcsCase;
    }

    private static PartyEntity party(String firstName, String lastName) {
        PartyEntity party = new PartyEntity();
        party.setFirstName(firstName);
        party.setLastName(lastName);
        return party;
    }

    private FlagRefDataEntity refData(String flagCode, String flagName) {
        return flagRefDataRepository.save(FlagRefDataEntity.builder()
                                              .flagCode(flagCode)
                                              .flagName(flagName)
                                              .build());
    }

    private static void addCaseFlag(PcsCaseEntity pcsCase, FlagRefDataEntity refData,
                                    String status, String comment) {
        CaseFlagEntity flag = CaseFlagEntity.builder()
            .flagRefData(refData)
            .defaultStatus(status)
            .flagComment(comment)
            .build();
        // Both columns are NOT NULL; Hibernate inserts explicit nulls, so the DB default on visibility doesn't apply
        flag.setPaths(":Case");
        flag.setVisibility("Internal");
        flag.setPcsCase(pcsCase);
        pcsCase.getCaseFlags().add(flag);
    }

    /** Adds an application made by the case's first party; type, rank, case and party are the NOT NULL columns. */
    private static void addGenApp(PcsCaseEntity pcsCase, GenAppType type, GenAppState state) {
        GenAppEntity genApp = GenAppEntity.builder()
            .type(type)
            .state(state)
            .party(pcsCase.getParties().iterator().next())
            .build();
        pcsCase.addGenApp(genApp);
    }

    private static void addLink(PcsCaseEntity pcsCase, long linkedCaseReference, String... reasonCodes) {
        CaseLinkEntity link = CaseLinkEntity.builder()
            .pcsCase(pcsCase)
            .linkedCaseReference(linkedCaseReference)
            .ccdListId("PCS")
            .build();
        for (String reasonCode : reasonCodes) {
            link.getReasons().add(CaseLinkReasonEntity.builder().caseLink(link).reasonCode(reasonCode).build());
        }
        pcsCase.getCaseLinks().add(link);
    }
}
