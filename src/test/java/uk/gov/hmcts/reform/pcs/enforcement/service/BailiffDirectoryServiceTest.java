package uk.gov.hmcts.reform.pcs.enforcement.service;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.pcs.am.RoleAssignment;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentAttributes;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentResponse;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BailiffDirectoryServiceTest {

    private static final String USER_ID = "bailiff-idam-id";
    private static final String S2S_TOKEN = "s2s-token";
    private static final String SYSTEM_TOKEN = "Bearer system-token";

    @Mock
    private RoleAssignmentApi roleAssignmentApi;
    @Mock
    private IdamClient idamClient;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;

    private BailiffDirectoryService underTest;

    @BeforeEach
    void setUp() {
        underTest = new BailiffDirectoryService(roleAssignmentApi, idamClient, authTokenGenerator,
                                                systemUpdateUserTokenProvider);
    }

    @Test
    void shouldFindAUserWithTheBailiffOrganisationalRole() {
        givenRoles(role("ORGANISATION", "bailiff"));
        when(idamClient.getUserByUserId(SYSTEM_TOKEN, USER_ID)).thenReturn(
            UserDetails.builder().id(USER_ID).forename("Sam").surname("Jones").email("sam.jones@justice.gov.uk")
                .build());

        assertThat(underTest.findBailiff(USER_ID))
            .contains(new BailiffUser(USER_ID, "Sam Jones", "sam.jones@justice.gov.uk"));
    }

    @Test
    void shouldUseTheForenameAloneWhenThereIsNoSurname() {
        givenRoles(role("ORGANISATION", "bailiff"));
        when(idamClient.getUserByUserId(SYSTEM_TOKEN, USER_ID)).thenReturn(
            UserDetails.builder().id(USER_ID).forename("Sam").email("sam@justice.gov.uk").build());

        assertThat(underTest.findBailiff(USER_ID)).map(BailiffUser::name).contains("Sam");
    }

    @Test
    void shouldNotFindAUserWithoutTheBailiffRole() {
        givenRoles(role("ORGANISATION", "hearing-centre-admin"), role("ORGANISATION", "bailiff-manager"));

        assertThat(underTest.findBailiff(USER_ID)).isEmpty();
        verify(idamClient, never()).getUserByUserId(anyString(), anyString());
    }

    @Test
    void shouldNotTreatABailiffCaseRoleAsTheBailiffRole() {
        givenRoles(role("CASE", "bailiff"));

        assertThat(underTest.findBailiff(USER_ID)).isEmpty();
    }

    @Test
    void shouldNotFindABailiffInAnotherJurisdiction() {
        givenRoles(role("ORGANISATION", "bailiff", "CIVIL"));

        assertThat(underTest.findBailiff(USER_ID)).isEmpty();
        verify(idamClient, never()).getUserByUserId(anyString(), anyString());
    }

    @Test
    void shouldNotFindABailiffRoleWithNoJurisdiction() {
        givenRoles(role("ORGANISATION", "bailiff", null));

        assertThat(underTest.findBailiff(USER_ID)).isEmpty();
    }

    @Test
    void shouldNotFindAUserWithNoRoleAssignments() {
        givenRoles();

        assertThat(underTest.findBailiff(USER_ID)).isEmpty();
    }

    @Test
    void shouldNotFindAUserWhenAmReturnsNothing() {
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(SYSTEM_TOKEN);
        when(roleAssignmentApi.getRoles(S2S_TOKEN, SYSTEM_TOKEN, USER_ID)).thenReturn(null);

        assertThat(underTest.findBailiff(USER_ID)).isEmpty();
    }

    @Test
    void shouldNotFindABailiffWithNoIdamAccount() {
        givenRoles(role("ORGANISATION", "bailiff"));
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/users/" + USER_ID, Map.of(), null,
                                         StandardCharsets.UTF_8, null);
        when(idamClient.getUserByUserId(SYSTEM_TOKEN, USER_ID))
            .thenThrow(new FeignException.NotFound("not found", request, null, Map.of()));

        assertThat(underTest.findBailiff(USER_ID)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void shouldNotLookUpAMissingUser(String userId) {
        assertThat(underTest.findBailiff(userId)).isEmpty();
        verifyNoInteractions(roleAssignmentApi, idamClient);
    }

    @Test
    void shouldSayAUserWithTheBailiffOrganisationalRoleIsABailiffWithoutAskingIdam() {
        givenRoles(role("ORGANISATION", "bailiff"));

        assertThat(underTest.isBailiff(USER_ID)).isTrue();
        verifyNoInteractions(idamClient);
    }

    @Test
    void shouldSayAUserWithOnlyOtherRolesIsNotABailiff() {
        givenRoles(role("ORGANISATION", "bailiff-manager"), role("CASE", "bailiff"));

        assertThat(underTest.isBailiff(USER_ID)).isFalse();
    }

    @Test
    void shouldSayABailiffInAnotherJurisdictionIsNotABailiff() {
        givenRoles(role("ORGANISATION", "bailiff", "CIVIL"));

        assertThat(underTest.isBailiff(USER_ID)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void shouldSayAMissingUserIsNotABailiff(String userId) {
        assertThat(underTest.isBailiff(userId)).isFalse();
        verifyNoInteractions(roleAssignmentApi);
    }

    private void givenRoles(RoleAssignment... roles) {
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(SYSTEM_TOKEN);
        when(roleAssignmentApi.getRoles(S2S_TOKEN, SYSTEM_TOKEN, USER_ID))
            .thenReturn(RoleAssignmentResponse.builder().roleAssignment(List.of(roles)).build());
    }

    private static RoleAssignment role(String roleType, String roleName) {
        return role(roleType, roleName, "PCS");
    }

    private static RoleAssignment role(String roleType, String roleName, String jurisdiction) {
        return RoleAssignment.builder()
            .actorId(USER_ID)
            .roleType(roleType)
            .roleName(roleName)
            .attributes(jurisdiction == null ? null
                            : RoleAssignmentAttributes.builder().jurisdiction(jurisdiction).build())
            .build();
    }
}
