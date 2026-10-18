package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementViewerServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String CLAIMANT_GROUP = "PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:ORG1";

    @Mock
    private UserRoleService userRoleService;
    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private EnforcementViewerService underTest;

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "JUDGE", "FEE_PAID_JUDGE", "CIRCUIT_JUDGE", "LEADERSHIP_JUDGE", "CTSC_ADMIN", "CTSC_TEAM_LEADER",
        "HEARING_CENTRE_ADMIN", "HEARING_CENTRE_TEAM_LEADER", "WLU_ADMIN", "WLU_TEAM_LEADER", "BAILIFF",
        "BAILIFF_ADMIN", "BAILIFF_MANAGER", "ALLOCATED_BAILIFF", "SYSTEM_USER", "WA_SYSTEM_USER"
    })
    void shouldTreatInternalRolesAsInternal(UserRole role) {
        assertThat(EnforcementViewerService.viewerWith(List.of("caseworker", role.getRole())))
            .isEqualTo(EnforcementViewer.INTERNAL);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"CREATOR", "CLAIMANT_SOLICITOR", "GA_CLAIMANT_SOLICITOR", "CLAIMANT"})
    void shouldTreatClaimantSideRolesAsTheClaimant(UserRole role) {
        assertThat(EnforcementViewerService.viewerWith(List.of("caseworker-pcs-solicitor", role.getRole())))
            .isEqualTo(EnforcementViewer.CLAIMANT);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"DEFENDANT", "DEFENDANT_SOLICITOR", "GA_DEFENDANT_SOLICITOR"})
    void shouldRestrictDefendantSideRolesEvenAlongsideClaimantOrInternalRoles(UserRole role) {
        assertThat(EnforcementViewerService.viewerWith(
            List.of("caseworker-pcs-solicitor", "[CREATOR]", "judge", role.getRole())))
            .isEqualTo(EnforcementViewer.RESTRICTED);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "PCS_SOLICITOR", "PCS_CASE_WORKER", "CITIZEN", "ORGANISATION_CASE_ACCESS_ADMINISTRATOR", "HMCTS_ADMIN"
    })
    void shouldRestrictAUserWithNoSideOnTheCase(UserRole role) {
        assertThat(EnforcementViewerService.viewerWith(List.of(role.getRole())))
            .isEqualTo(EnforcementViewer.RESTRICTED);
    }

    @Test
    void shouldRestrictAUserWithNoRolesAtAll() {
        assertThat(EnforcementViewerService.viewerWith(List.of())).isEqualTo(EnforcementViewer.RESTRICTED);
    }

    @Test
    void shouldAskForTheCaseRolesWithTheCasesAccessGroups() {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(PartyEntity.builder()
                                            .id(UUID.randomUUID())
                                            .organisationId("ORG1")
                                            .organisationProfileId("SOLICITOR_PROFILE")
                                            .claimCreator(true)
                                            .build()));
        PCSCase pcsCase = PCSCase.builder().build();
        when(userRoleService.getCurrentUserCaseRoles(CASE_REFERENCE, Set.of(CLAIMANT_GROUP)))
            .thenReturn(new UserRoles("user-id", List.of("caseworker-pcs-solicitor", "claimant-solicitor")));

        assertThat(underTest.currentViewer(pcsCaseEntity, pcsCase, CASE_REFERENCE))
            .isEqualTo(EnforcementViewer.CLAIMANT);
    }

    @Test
    void shouldRestrictASolicitorWhoseCaseRolesCannotBeFound() {
        when(userRoleService.getCurrentUserCaseRoles(eq(CASE_REFERENCE), anyCollection()))
            .thenReturn(new UserRoles("user-id", null));

        assertThat(underTest.currentViewer(new PcsCaseEntity(), PCSCase.builder().build(), CASE_REFERENCE))
            .isEqualTo(EnforcementViewer.RESTRICTED);
    }

    @Test
    void shouldTreatPcsIdamSystemUserAsInternalWithoutLookingUpRoles() {
        when(securityContextService.isIdamSystemUser()).thenReturn(true);

        assertThat(underTest.currentViewer(new PcsCaseEntity(), PCSCase.builder().build(), CASE_REFERENCE))
            .isEqualTo(EnforcementViewer.INTERNAL);
        verifyNoInteractions(userRoleService);
    }

    @Test
    void shouldTreatTheSystemUserAsInternal() {
        when(securityContextService.isSystemUser()).thenReturn(true);

        assertThat(underTest.currentViewer(null, null, CASE_REFERENCE)).isEqualTo(EnforcementViewer.INTERNAL);
        verifyNoInteractions(userRoleService);
    }

    @Test
    void shouldAskWithNoAccessGroupsWithoutACase() {
        when(userRoleService.getCurrentUserCaseRoles(CASE_REFERENCE, Set.of()))
            .thenReturn(new UserRoles("user-id", List.of("judge")));

        assertThat(underTest.currentViewer(null, null, CASE_REFERENCE)).isEqualTo(EnforcementViewer.INTERNAL);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"JUDGE", "HEARING_CENTRE_ADMIN", "BAILIFF", "CTSC_ADMIN"})
    void shouldTreatAnInternalOrganisationalRoleAsInternalWithoutCaseRoles(UserRole role) {
        when(userRoleService.getCurrentUserOrganisationalRoles())
            .thenReturn(new UserRoles("user-id", List.of("caseworker-pcs", role.getRole())));

        assertThat(underTest.isInternalWithoutCaseRoles()).isTrue();
        verify(userRoleService, never()).getCurrentUserCaseRoles(anyLong(), anyCollection());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"GA_CLAIMANT_SOLICITOR", "GA_DEFENDANT_SOLICITOR"})
    void shouldNotTreatAGroupAccessSolicitorAsInternalWithoutCaseRoles(UserRole role) {
        when(userRoleService.getCurrentUserOrganisationalRoles())
            .thenReturn(new UserRoles("user-id", List.of("caseworker-pcs-solicitor", "judge", role.getRole())));

        assertThat(underTest.isInternalWithoutCaseRoles()).isFalse();
    }

    @Test
    void shouldNotTreatAUserWithNoOrganisationalRolesAsInternalWithoutCaseRoles() {
        when(userRoleService.getCurrentUserOrganisationalRoles()).thenReturn(new UserRoles("user-id", null));

        assertThat(underTest.isInternalWithoutCaseRoles()).isFalse();
    }

    @Test
    void shouldTreatTheSystemUserAsInternalWithoutCaseRoles() {
        when(securityContextService.isSystemUser()).thenReturn(true);

        assertThat(underTest.isInternalWithoutCaseRoles()).isTrue();
        verifyNoInteractions(userRoleService);
    }
}
