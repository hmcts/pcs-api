package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignment;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentAttributes;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * A firm's group-access roles are held for every claim the firm is on, on either side, so the side of the case
 * comes from the role whose case access group is this case's.
 */
@ExtendWith(MockitoExtension.class)
class EnforcementViewerGroupAccessTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String FIRM = "FIRM1";
    private static final String OTHER_FIRM = "FIRM2";

    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private CaseAssignmentApi caseAssignmentApi;
    @Mock
    private RoleAssignmentApi roleAssignmentApi;

    private EnforcementViewerService underTest;

    @BeforeEach
    void setUp() {
        UserRoleService userRoleService =
            new UserRoleService(securityContextService, authTokenGenerator, caseAssignmentApi, roleAssignmentApi);
        underTest = new EnforcementViewerService(userRoleService, securityContextService);
        when(securityContextService.getCurrentUserDetails()).thenReturn(
            UserInfo.builder().uid(USER_ID).roles(List.of("caseworker", "caseworker-pcs-solicitor")).build());
        when(securityContextService.toUserId(USER_ID)).thenReturn(USER_ID);
        when(securityContextService.getCurrentUserAuthToken()).thenReturn("Bearer user");
        when(authTokenGenerator.generate()).thenReturn("s2s");
        // No case role on any case, and a firm that acts for claimants and for defendants on different claims.
        when(roleAssignmentApi.getRoles("s2s", "Bearer user", USER_ID)).thenReturn(
            RoleAssignmentResponse.builder()
                .roleAssignment(List.of(
                    groupRole("claimant-solicitor", "PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:" + FIRM),
                    groupRole("defendant-solicitor",
                              "PCS:PCS:solicitor-org-defendant-access:defendant-solicitor:" + FIRM)))
                .build());
    }

    @Test
    void shouldGiveTheFirmTheClaimantViewOnAClaimItBrought() {
        CaseView caseView = caseView(FIRM, OTHER_FIRM);

        assertThat(underTest.currentViewer(caseView.entity(), caseView.pcsCase(), CASE_REFERENCE))
            .isEqualTo(EnforcementViewer.CLAIMANT);
    }

    @Test
    void shouldGiveTheFirmTheDefendantViewOnAClaimItDefends() {
        CaseView caseView = caseView(OTHER_FIRM, FIRM);

        assertThat(underTest.currentViewer(caseView.entity(), caseView.pcsCase(), CASE_REFERENCE))
            .isEqualTo(EnforcementViewer.RESTRICTED);
    }

    @Test
    void shouldRestrictTheFirmOnAClaimItIsNotOn() {
        CaseView caseView = caseView(OTHER_FIRM, OTHER_FIRM);

        assertThat(underTest.currentViewer(caseView.entity(), caseView.pcsCase(), CASE_REFERENCE))
            .isEqualTo(EnforcementViewer.RESTRICTED);
    }

    private static CaseView caseView(String claimantFirm, String defendantFirm) {
        PartyEntity claimant = PartyEntity.builder()
            .id(UUID.randomUUID())
            .organisationId(claimantFirm)
            .organisationProfileId("SOLICITOR_PROFILE")
            .claimCreator(true)
            .build();
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        defendant.getClaimPartyOrganisationList().add(ClaimPartyOrganisationEntity.builder()
            .party(defendant)
            .organisation(OrganisationEntity.builder()
                              .organisationId(defendantFirm)
                              .organisationProfileId("SOLICITOR_PROFILE")
                              .build())
            .active(YesOrNo.YES)
            .build());
        PcsCaseEntity entity = new PcsCaseEntity();
        entity.setParties(Set.of(claimant, defendant));
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(ListValue.<Party>builder()
                                       .id(defendant.getId().toString())
                                       .value(new Party())
                                       .build()))
            .build();
        return new CaseView(entity, pcsCase);
    }

    private static RoleAssignment groupRole(String roleName, String caseAccessGroupId) {
        return RoleAssignment.builder()
            .actorId(USER_ID)
            .roleType("ORGANISATION")
            .grantType("STANDARD")
            .roleName(roleName)
            .attributes(RoleAssignmentAttributes.builder().caseAccessGroupId(caseAccessGroupId).build())
            .build();
    }

    private record CaseView(PcsCaseEntity entity, PCSCase pcsCase) {
    }
}
