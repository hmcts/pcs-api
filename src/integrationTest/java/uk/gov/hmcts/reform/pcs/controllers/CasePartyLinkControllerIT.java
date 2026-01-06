package uk.gov.hmcts.reform.pcs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.model.ValidateAccessCodeRequest;
import uk.gov.hmcts.reform.pcs.util.IdamHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
@DisplayName("CasePartyLinkController Integration Tests")
class CasePartyLinkControllerIT extends AbstractPostgresContainerIT {

    private static final String AUTH_HEADER = "Bearer test-token";
    private static final String SERVICE_AUTH_HEADER = "ServiceAuthToken";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String SYSTEM_USER_ID_TOKEN = "system-user-id-token";
    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    private static final String ACCESS_CODE = "ABC123XYZ789";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private PartyAccessCodeRepository partyAccessCodeRepository;

    @Autowired
    private IdamHelper idamHelper;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @MockitoBean
    private IdamClient idamClient;

    @BeforeEach
    void setUp() {
        idamHelper.stubIdamSystemUser(idamClient, SYSTEM_USER_ID_TOKEN);

        // Stub IDAM user info for token validation
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn(USER_ID.toString());
        when(idamClient.getUserInfo(anyString())).thenReturn(userInfo);
    }

    @Test
    @DisplayName("Should successfully validate and link party with valid access code")
    void shouldSuccessfullyValidateAndLinkPartyWithValidAccessCode() throws Exception {
        // Given
        long caseReference = 12345L;
        PcsCaseEntity caseEntity = createTestCaseWithDefendant(caseReference, null);
        String accessCode = createPartyAccessCode(caseEntity, caseEntity.getDefendants().get(0).getPartyId());

        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(accessCode);

        // When/Then
        mockMvc.perform(post("/cases/{caseReference}/validate-access-code", caseReference)
                        .header(AUTHORIZATION, AUTH_HEADER)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        // Verify defendant is linked in database
        PcsCaseEntity updatedCase = pcsCaseRepository.findByCaseReference(caseReference)
                .orElseThrow();
        assertThat(updatedCase.getDefendants().get(0).getIdamUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Should call IdamClient.getUserInfo with the exact Authorization header value")
    void shouldCallIdamClientGetUserInfoWithExactAuthHeader() throws Exception {
        // Given
        long caseReference = 12355L;
        PcsCaseEntity caseEntity = createTestCaseWithDefendant(caseReference, null);
        String accessCode = createPartyAccessCode(caseEntity, caseEntity.getDefendants().get(0).getPartyId());

        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(accessCode);

        // When
        mockMvc.perform(post("/cases/{caseReference}/validate-access-code", caseReference)
                        .header(AUTHORIZATION, AUTH_HEADER)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then - Verify idamClient.getUserInfo was called with the exact AUTH_HEADER value
        // Note: getBearerToken() keeps the "Bearer " prefix if already present,
        // so it should be called with the full "Bearer test-token" value
        verify(idamClient).getUserInfo(eq(AUTH_HEADER));
    }

    @Test
    @DisplayName("Should return 404 when case not found")
    void shouldReturn404WhenCaseNotFound() throws Exception {
        // Given
        long nonExistentCaseReference = 99999L;
        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(ACCESS_CODE);

        // When/Then
        mockMvc.perform(post("/cases/{caseReference}/validate-access-code", nonExistentCaseReference)
                        .header(AUTHORIZATION, AUTH_HEADER)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("Should return 404 when party does not belong to this case")
    void shouldReturn404WhenPartyDoesNotBelongToCase() throws Exception {
        // Given
        long caseReference = 12354L;
        PcsCaseEntity caseEntity = createTestCaseWithDefendant(caseReference, null);
        // Create access code for a DIFFERENT party that doesn't exist in the case
        UUID nonExistentPartyId = UUID.randomUUID();
        String accessCode = createPartyAccessCode(caseEntity, nonExistentPartyId);

        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(accessCode);

        // When/Then
        mockMvc.perform(post("/cases/{caseReference}/validate-access-code", caseReference)
                        .header(AUTHORIZATION, AUTH_HEADER)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Invalid data")));
    }

    @Test
    @DisplayName("Should return 400 when access code not found")
    void shouldReturn400WhenAccessCodeNotFound() throws Exception {
        // Given
        long caseReference = 12346L;
        createTestCaseWithDefendant(caseReference, null);
        String invalidAccessCode = "INVALIDCODE12";

        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(invalidAccessCode);

        // When/Then - Updated to expect 400 (BAD_REQUEST) instead of 404
        mockMvc.perform(post("/cases/{caseReference}/validate-access-code", caseReference)
                        .header(AUTHORIZATION, AUTH_HEADER)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 409 when access code already used")
    void shouldReturn409WhenAccessCodeAlreadyUsed() throws Exception {
        // Given
        long caseReference = 12347L;
        PcsCaseEntity caseEntity = createTestCaseWithDefendant(caseReference, USER_ID);
        String accessCode = createPartyAccessCode(caseEntity, caseEntity.getDefendants().get(0).getPartyId());

        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(accessCode);

        // When/Then - Updated message to match new exception
        mockMvc.perform(post("/cases/{caseReference}/validate-access-code", caseReference)
                        .header(AUTHORIZATION, AUTH_HEADER)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("This access code is already linked to a user.")));
    }

    @Test
    @DisplayName("Should return 409 when user ID already linked to another defendant")
    void shouldReturn409WhenUserIdAlreadyLinkedToAnotherDefendant() throws Exception {
        // Given
        long caseReference = 12348L;
        PcsCaseEntity caseEntity = createTestCaseWithMultipleDefendants(caseReference, USER_ID, null);
        // Get access code for the second defendant (not yet linked)
        UUID secondDefendantPartyId = caseEntity.getDefendants().get(1).getPartyId();
        String accessCode = createPartyAccessCode(caseEntity, secondDefendantPartyId);

        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(accessCode);

        // When/Then - Should fail because USER_ID is already linked to first defendant
        mockMvc.perform(post("/cases/{caseReference}/validate-access-code", caseReference)
                        .header(AUTHORIZATION, AUTH_HEADER)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message",
                        is("This user is already linked to another party in this case.")));
    }

    @Test
    @DisplayName("Should return 400 when access code is missing")
    void shouldReturn400WhenAccessCodeIsMissing() throws Exception {
        // Given
        long caseReference = 12349L;
        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(null);

        // When/Then
        mockMvc.perform(post("/cases/{caseReference}/validate-access-code", caseReference)
                        .header(AUTHORIZATION, AUTH_HEADER)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when access code is blank")
    void shouldReturn400WhenAccessCodeIsBlank() throws Exception {
        // Given
        long caseReference = 12350L;
        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest("");

        // When/Then
        mockMvc.perform(post("/cases/{caseReference}/validate-access-code", caseReference)
                        .header(AUTHORIZATION, AUTH_HEADER)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when authorization header is missing")
    void shouldReturn400WhenAuthorizationHeaderIsMissing() throws Exception {
        // Given
        long caseReference = 12351L;
        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(ACCESS_CODE);

        // When/Then - Spring returns 400 for missing required headers
        mockMvc.perform(post("/cases/{caseReference}/validate-access-code", caseReference)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should rollback transaction when exception occurs - no partial data saved")
    void shouldRollbackTransactionWhenExceptionOccurs() throws Exception {
        // Given - Create a case with a defendant that's already linked
        // This will cause an exception when trying to link again
        long caseReference = 12352L;
        PcsCaseEntity caseEntity = createTestCaseWithDefendant(caseReference, USER_ID);
        String accessCode = createPartyAccessCode(caseEntity, caseEntity.getDefendants().get(0).getPartyId());

        // Capture the initial state before the failed operation
        PcsCaseEntity caseBefore = pcsCaseRepository.findByCaseReference(caseReference)
                .orElseThrow();
        UUID initialIdamUserId = caseBefore.getDefendants().get(0).getIdamUserId();

        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(accessCode);

        // When - Attempt to link (should fail with 409)
        mockMvc.perform(post("/cases/{caseReference}/validate-access-code", caseReference)
                        .header(AUTHORIZATION, AUTH_HEADER)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("This access code is already linked to a user.")));

        // Then - Verify transaction rolled back: database state unchanged
        PcsCaseEntity caseAfter = pcsCaseRepository.findByCaseReference(caseReference)
                .orElseThrow();
        UUID finalIdamUserId = caseAfter.getDefendants().get(0).getIdamUserId();

        // The idamUserId should remain unchanged (transaction rolled back)
        assertThat(finalIdamUserId).isEqualTo(initialIdamUserId);
        assertThat(finalIdamUserId).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Should commit transaction when operation succeeds - data persisted correctly")
    void shouldCommitTransactionWhenOperationSucceeds() throws Exception {
        // Given
        long caseReference = 12353L;
        PcsCaseEntity caseEntity = createTestCaseWithDefendant(caseReference, null);
        String accessCode = createPartyAccessCode(caseEntity, caseEntity.getDefendants().get(0).getPartyId());

        // Verify initial state - defendant not linked
        PcsCaseEntity caseBefore = pcsCaseRepository.findByCaseReference(caseReference)
                .orElseThrow();
        assertThat(caseBefore.getDefendants().get(0).getIdamUserId()).isNull();

        ValidateAccessCodeRequest request = new ValidateAccessCodeRequest(accessCode);

        // When - Successful linking
        mockMvc.perform(post("/cases/{caseReference}/validate-access-code", caseReference)
                        .header(AUTHORIZATION, AUTH_HEADER)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        // Then - Verify transaction committed: data persisted
        PcsCaseEntity caseAfter = pcsCaseRepository.findByCaseReference(caseReference)
                .orElseThrow();
        assertThat(caseAfter.getDefendants().get(0).getIdamUserId()).isEqualTo(USER_ID);
        assertThat(caseAfter.getDefendants().get(0).getIdamUserId()).isNotNull();
    }

    // Helper methods

    private PcsCaseEntity createTestCaseWithDefendant(long caseReference, UUID idamUserId) {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(caseReference);

        UUID partyId = UUID.randomUUID();
        Defendant defendant = new Defendant();
        defendant.setPartyId(partyId);
        defendant.setIdamUserId(idamUserId);
        defendant.setFirstName("John");
        defendant.setLastName("Doe");

        List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant);
        caseEntity.setDefendants(defendants);

        return pcsCaseRepository.save(caseEntity);
    }

    private PcsCaseEntity createTestCaseWithMultipleDefendants(
            long caseReference, UUID firstIdamUserId, UUID secondIdamUserId) {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(caseReference);

        UUID partyId1 = UUID.randomUUID();
        Defendant defendant1 = new Defendant();
        defendant1.setPartyId(partyId1);
        defendant1.setIdamUserId(firstIdamUserId);
        defendant1.setFirstName("John");
        defendant1.setLastName("Doe");

        UUID partyId2 = UUID.randomUUID();
        Defendant defendant2 = new Defendant();
        defendant2.setPartyId(partyId2);
        defendant2.setIdamUserId(secondIdamUserId);
        defendant2.setFirstName("Jane");
        defendant2.setLastName("Smith");

        List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant1);
        defendants.add(defendant2);
        caseEntity.setDefendants(defendants);

        return pcsCaseRepository.save(caseEntity);
    }

    private String createPartyAccessCode(PcsCaseEntity caseEntity, UUID partyId) {
        PartyAccessCodeEntity pac = PartyAccessCodeEntity.builder()
                .partyId(partyId)
                .pcsCase(caseEntity)
                .code(ACCESS_CODE)
                .role(PartyRole.DEFENDANT)
                .build();

        partyAccessCodeRepository.save(pac);
        return ACCESS_CODE;
    }

}
