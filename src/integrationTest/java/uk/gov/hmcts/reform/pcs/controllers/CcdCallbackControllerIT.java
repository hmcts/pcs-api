package uk.gov.hmcts.reform.pcs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CITIZEN;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DisplayName("CCD callback controller integration tests")
class CcdCallbackControllerIT extends AbstractPostgresContainerIT {

    private static final long CASE_REFERENCE = 1781622472192628L;
    private static final UUID DEFENDANT_USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    private static final UUID OTHER_USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174099");

    private static final String AUTH_HEADER = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaseCreationHelper caseCreationHelper;

    @MockitoBean
    private IdamAuthenticator idamAuthenticator;

    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @BeforeEach
    void setUp() {
        caseCreationHelper.createTestCaseWithParty(CASE_REFERENCE, DEFENDANT_USER_ID, PartyRole.DEFENDANT);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional
    void dashboardViewAboutToStartShouldReturnForbiddenWhenUserLacksDefendantAccess() throws Exception {
        setAuthenticatedUser(OTHER_USER_ID);

        mockMvc.perform(post("/callbacks/about-to-start")
                .header(AUTHORIZATION, AUTH_HEADER)
                .queryParam("eventId", "dashboardView")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildCallbackRequest())))
            .andExpect(status().isForbidden())
            .andExpect(content().json("""
                {"message":"User is not linked as a defendant on this case"}
                """));
    }

    @Test
    @Transactional
    void respondPossessionClaimAboutToStartShouldReturnForbiddenWhenUserLacksDefendantAccess() throws Exception {
        setAuthenticatedUser(OTHER_USER_ID);

        CallbackRequest callbackRequest = CallbackRequest.builder()
            .eventId("respondPossessionClaim")
            .caseDetails(CaseDetails.builder()
                .id(CASE_REFERENCE)
                .caseTypeId(CaseType.getCaseType())
                .data(Map.of("legislativeCountry", "England"))
                .build())
            .build();

        mockMvc.perform(post("/callbacks/about-to-start")
                .header(AUTHORIZATION, AUTH_HEADER)
                .queryParam("eventId", "respondPossessionClaim")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(callbackRequest)))
            .andExpect(status().isForbidden())
            .andExpect(content().json("""
                {"message":"User is not linked as a defendant on this case"}
                """));
    }

    private CallbackRequest buildCallbackRequest() {
        return CallbackRequest.builder()
            .eventId("dashboardView")
            .caseDetails(CaseDetails.builder()
                .id(CASE_REFERENCE)
                .caseTypeId(CaseType.getCaseType())
                .data(Map.of())
                .build())
            .build();
    }

    private void setAuthenticatedUser(UUID userId) {
        UserInfo userInfo = UserInfo.builder()
            .uid(userId.toString())
            .roles(List.of(CITIZEN.getRole()))
            .build();
        User user = new User("testing", userInfo);
        when(idamAuthenticator.validateAuthToken(eq(AUTH_HEADER))).thenReturn(user);

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
