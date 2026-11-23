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
import org.springframework.beans.factory.annotation.Value;
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
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
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

import java.time.Instant;
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

    @Value("${manage_case.url}")
    private String manageCaseUrl;

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
                    manageCaseUrl + "/cases/case-details/PCS/PCS/" + CASE_REFERENCE + "#Case%20File%20View"))
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
                .andExpect(jsonPath("$.listingComments").doesNotExist())
                .andExpect(jsonPath("$.screenFlow[*].screenName", contains(
                    "hearing-panel", "hearing-facilities", "hearing-stage", "hearing-attendance", "hearing-venue",
                    "hearing-welsh", "hearing-judge", "hearing-timing", "hearing-link",
                    "hearing-additional-instructions")))
                .andExpect(jsonPath("$.screenFlow[0].conditionKey").doesNotExist())
                .andExpect(jsonPath("$.screenFlow[0].navigation[0].resultValue").value("hearing-facilities"))
                .andExpect(jsonPath("$.screenFlow[9].navigation").doesNotExist())
                // Venues outside Wales skip the Welsh screen and go straight to the judge screen
                .andExpect(jsonPath("$.screenFlow[4].conditionKey").value("regionId"))
                .andExpect(jsonPath("$.screenFlow[4].navigation[1].conditionOperator").value("NOT INCLUDE"))
                .andExpect(jsonPath("$.screenFlow[4].navigation[1].conditionValue").value("7"))
                .andExpect(jsonPath("$.screenFlow[4].navigation[1].resultValue").value("hearing-judge"));
        }

        @Test
        @DisplayName("sends ExUI to the Welsh hearing screen after the venue when the venue is in Wales")
        void shouldRouteWalesVenueThroughWelshScreen() throws Exception {
            persist(newCase(CASE_REFERENCE, null));

            String venue = "$.screenFlow[?(@.screenName == 'hearing-venue')]";
            String walesRoute = venue + ".navigation[?(@.conditionOperator == 'INCLUDE')]";
            String welsh = "$.screenFlow[?(@.screenName == 'hearing-welsh')]";

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath(venue + ".conditionKey", contains("regionId")))
                .andExpect(jsonPath(walesRoute + ".conditionValue", contains("7")))
                .andExpect(jsonPath(walesRoute + ".resultValue", contains("hearing-welsh")))
                .andExpect(jsonPath(welsh + ".navigation[*].resultValue", contains("hearing-judge")));
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
        @DisplayName("sends organisation details for an organisation party and individual details for a person")
        void shouldBuildOrganisationAndIndividualPartyDetails() throws Exception {
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            PartyEntity defendant = partyWithFirstName(pcsCase, "John");
            defendant.setEmailAddress("john.doe@example.com");
            defendant.setPhoneNumber("07700900123");
            persist(pcsCase);
            PartyEntity claimant = partyWithOrgName(pcsCase, "Test Claimant");

            String organisation = "$.parties[?(@.partyType == 'ORG')]";
            String individual = "$.parties[?(@.partyType == 'IND')]";

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parties[*].partyType", containsInAnyOrder("ORG", "IND")))
                .andExpect(jsonPath(organisation + ".partyID", contains(claimant.getId().toString())))
                .andExpect(jsonPath(organisation + ".organisationDetails.name", contains("Test Claimant")))
                .andExpect(jsonPath(organisation + ".organisationDetails.organisationType", contains("ORG")))
                .andExpect(jsonPath(organisation + ".organisationDetails.cftOrganisationID", contains("ORG1234")))
                .andExpect(jsonPath(organisation + ".individualDetails", empty()))
                .andExpect(jsonPath(individual + ".partyID", contains(defendant.getId().toString())))
                .andExpect(jsonPath(individual + ".individualDetails.firstName", contains("John")))
                .andExpect(jsonPath(individual + ".individualDetails.lastName", contains("Doe")))
                .andExpect(jsonPath(individual + ".individualDetails.hearingChannelEmail[0]",
                                    contains("john.doe@example.com")))
                .andExpect(jsonPath(individual + ".individualDetails.hearingChannelPhone[0]",
                                    contains("07700900123")))
                .andExpect(jsonPath(individual + ".organisationDetails", empty()));
        }

        @Test
        @DisplayName("leaves out party contact details and role when they are not held")
        void shouldOmitPartyDetailsThatAreNotHeld() throws Exception {
            persist(newCase(CASE_REFERENCE, null));

            String individual = "$.parties[?(@.partyType == 'IND')]";

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parties[*].partyRole", empty()))
                .andExpect(jsonPath(individual + ".individualDetails.hearingChannelEmail", empty()))
                .andExpect(jsonPath(individual + ".individualDetails.hearingChannelPhone", empty()))
                .andExpect(jsonPath(individual + ".individualDetails.custodyStatus", empty()));
        }

        @Test
        @DisplayName("sets each party's role from its entity role code")
        void shouldSetPartyRoleFromEntityRoleCode() throws Exception {
            givenEntityRoleCode("TEST-CLAIMANT-ROLE");
            givenEntityRoleCode("TEST-DEFENDANT-ROLE");
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            partyWithOrgName(pcsCase, "Test Claimant").setEntityRoleCodeKey("TEST-CLAIMANT-ROLE");
            partyWithFirstName(pcsCase, "John").setEntityRoleCodeKey("TEST-DEFENDANT-ROLE");
            persist(pcsCase);

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parties[?(@.partyType == 'ORG')].partyRole", contains("TEST-CLAIMANT-ROLE")))
                .andExpect(jsonPath("$.parties[?(@.partyType == 'IND')].partyRole", contains("TEST-DEFENDANT-ROLE")));
        }

        @Test
        @DisplayName("treats the claim creator as an individual when it has no organisation ID")
        void shouldTreatClaimCreatorWithoutOrganisationIdAsIndividual() throws Exception {
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            partyWithOrgName(pcsCase, "Test Claimant").setOrganisationId(null);
            persist(pcsCase);

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parties[*].partyType", contains("IND", "IND")));
        }

        @Test
        @DisplayName("sends individual and legal rep organisation details for a represented defendant")
        void shouldSendIndividualAndLegalRepOrganisationDetailsForRepresentedDefendant() throws Exception {
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            PartyEntity defendant = partyWithFirstName(pcsCase, "John");
            persist(pcsCase);
            linkToClaimPartyOrganisation(defendant);

            String represented = "$.parties[?(@.partyID == '" + defendant.getId() + "')]";

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parties[*].partyType", containsInAnyOrder("ORG", "IND")))
                .andExpect(jsonPath(represented + ".partyType", contains("IND")))
                .andExpect(jsonPath(represented + ".individualDetails.firstName", contains("John")))
                .andExpect(jsonPath(represented + ".individualDetails.lastName", contains("Doe")))
                .andExpect(jsonPath(represented + ".organisationDetails.name", contains("Legal Rep Firm")))
                .andExpect(jsonPath(represented + ".organisationDetails.organisationType", contains("ORG")))
                .andExpect(jsonPath(represented + ".organisationDetails.cftOrganisationID", contains("LR-ORG")));
        }

        @Test
        @DisplayName("sends an underlessee or mortgagee as an individual named from its organisation name")
        void shouldSendUnderlesseeOrMortgageeAsIndividualNamedFromOrganisationName() throws Exception {
            PcsCaseEntity pcsCase = newCase(CASE_REFERENCE, null);
            PartyEntity underlessee = party(null, null);
            underlessee.setOrgName("Acme Lending");
            pcsCase.addParty(underlessee);
            pcsCase.getMainClaim().addParty(underlessee, PartyRole.UNDERLESSEE_OR_MORTGAGEE);
            persist(pcsCase);

            String underlesseeParty = "$.parties[?(@.partyID == '" + underlessee.getId() + "')]";

            postFor(HEARING_VALUES_PATH, CASE_REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath(underlesseeParty + ".partyType", contains("IND")))
                .andExpect(jsonPath(underlesseeParty + ".individualDetails.firstName", contains("Acme Lending")))
                .andExpect(jsonPath(underlesseeParty + ".individualDetails.lastName", contains("N/A")))
                .andExpect(jsonPath(underlesseeParty + ".organisationDetails", empty()));
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
        claimant.setClaimCreator(true);
        claimant.setOrganisationId("ORG1234");
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

    private static PartyEntity partyWithOrgName(PcsCaseEntity pcsCase, String orgName) {
        return pcsCase.getParties().stream()
            .filter(party -> orgName.equals(party.getOrgName()))
            .findFirst()
            .orElseThrow();
    }

    private static PartyEntity partyWithFirstName(PcsCaseEntity pcsCase, String firstName) {
        return pcsCase.getParties().stream()
            .filter(party -> firstName.equals(party.getFirstName()))
            .findFirst()
            .orElseThrow();
    }

    private void linkToClaimPartyOrganisation(PartyEntity party) {
        OrganisationEntity organisation = OrganisationEntity.builder()
            .organisationId("LR-ORG")
            .organisationProfileId("SOLICITOR_PROFILE")
            .organisationName("Legal Rep Firm")
            .createdDate(LocalDateTime.now())
            .build();
        organisation.getClaimPartyOrganisationList().add(ClaimPartyOrganisationEntity.builder()
            .party(party)
            .organisation(organisation)
            .active(YesOrNo.YES)
            .startDate(Instant.now())
            .build());
        entityManager.persist(organisation);
        entityManager.flush();
        entityManager.clear();
    }

    // entity_role_code has no seed data, and party.entity_role_code_key references it
    private void givenEntityRoleCode(String key) {
        entityManager.createNativeQuery("INSERT INTO entity_role_code (key, active) VALUES (:key, 'YES')")
            .setParameter("key", key)
            .executeUpdate();
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
