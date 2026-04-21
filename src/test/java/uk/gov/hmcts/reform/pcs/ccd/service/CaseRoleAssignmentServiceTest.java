package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseRoleAssignmentServiceTest {

    private static final long CASE_REFERENCE = 123456789L;
    private static final String USER_ID = "abc-123-def-456";
    private static final String S2S_TOKEN = "s2s-token";
    private static final String USER_TOKEN = "Bearer user-token";

    @InjectMocks
    private CaseRoleAssignmentService caseRoleAssignmentService;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private IdamService idamService;

    @Mock
    private CaseAssignmentApi caseAssignmentApi;

    @Captor
    private ArgumentCaptor<CaseAssignmentUserRolesRequest> requestCaptor;

    @Test
    void assignRasRole_shouldCallApiWithCorrectCaseRole() {
        // GIVEN
        CaseAssignmentUserRolesResponse caseAssignmentUserRolesResponse =
            CaseAssignmentUserRolesResponse.builder().build();

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(idamService.getSystemUserAuthorisation()).thenReturn(USER_TOKEN);
        when(caseAssignmentApi.addCaseUserRoles(eq(USER_TOKEN), eq(S2S_TOKEN), requestCaptor.capture()))
            .thenReturn(caseAssignmentUserRolesResponse);

        // WHEN
        CaseAssignmentUserRolesResponse result =
            caseRoleAssignmentService.assignRasRole(CASE_REFERENCE, USER_ID, UserRole.DEFENDANT);

        // THEN
        CaseAssignmentUserRoleWithOrganisation assignment = requestCaptor.getValue()
            .getCaseAssignmentUserRolesWithOrganisation().getFirst();

        assertThat(result).isEqualTo(caseAssignmentUserRolesResponse);
        assertThat(assignment.getCaseRole()).isEqualTo(UserRole.DEFENDANT.getRole());
        assertThat(assignment.getCaseDataId()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(assignment.getUserId()).isEqualTo(USER_ID);
    }

}
