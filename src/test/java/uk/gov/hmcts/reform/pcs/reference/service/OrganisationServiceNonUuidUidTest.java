package uk.gov.hmcts.reform.pcs.reference.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationServiceNonUuidUidTest {

    private static final String LEGACY_UID = "7667";

    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private OrganisationDetailsService organisationDetailsService;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private CaseAssignmentApi caseAssignmentApi;
    @Mock
    private RoleAssignmentApi roleAssignmentApi;

    @Test
    void shouldReturnNoOrganisationIdForStaffUserWithNonUuidIdamUid() {
        UserRoleService userRoleService = new UserRoleService(
            securityContextService, authTokenGenerator, caseAssignmentApi, roleAssignmentApi);
        OrganisationService underTest = new OrganisationService(
            securityContextService, organisationDetailsService, userRoleService);

        when(securityContextService.getCurrentUserDetails()).thenReturn(UserInfo.builder()
            .uid(LEGACY_UID)
            .roles(List.of("caseworker-pcs"))
            .build());
        when(securityContextService.getCurrentUserAuthToken()).thenReturn("Bearer user-token");
        when(authTokenGenerator.generate()).thenReturn("Bearer s2s-token");
        when(roleAssignmentApi.getRoles("Bearer s2s-token", "Bearer user-token", LEGACY_UID))
            .thenReturn(RoleAssignmentResponse.builder().build());

        String result = underTest.getOrganisationIdForCurrentUser();

        assertThat(result).isNull();
        verifyNoInteractions(organisationDetailsService, caseAssignmentApi);
    }
}
