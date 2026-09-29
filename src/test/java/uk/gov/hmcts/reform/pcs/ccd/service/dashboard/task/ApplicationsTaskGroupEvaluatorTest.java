package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.Task;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.MAKE_GENERAL_APPLICATION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds.VIEW_ALL_APPLICATIONS;

class ApplicationsTaskGroupEvaluatorTest {

    private static final UUID CURRENT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String CURRENT_ORGANISATION_ID = "org1";
    private static final String OTHER_ORGANISATION_ID = "org2";
    private static final long TEST_CASE_REFERENCE = 100L;

    private final SecurityContextService securityContextService = mock(SecurityContextService.class);
    private final OrganisationService organisationService = mock(OrganisationService.class);
    private final UserRoleService userRoleService = mock(UserRoleService.class);
    private final OrganisationRepository organisationRepository =
        mock(OrganisationRepository.class);
    private final GenAppVisibilityService genAppVisibilityService =
        new GenAppVisibilityService(organisationRepository);
    private final ApplicationsTaskGroupEvaluator underTest =
        new ApplicationsTaskGroupEvaluator(userRoleService, genAppVisibilityService, organisationService);

    @Test
    void shouldReturnApplicationsGroupId() {
        assertThat(underTest.groupId()).isEqualTo(TaskGroupId.APPLICATIONS);
    }

    @Test
    void shouldMarkViewApplicationsAsAvailableWhenCaseHasVisibleApplication() {
        stubUserRoles();
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CURRENT_ORGANISATION_ID);
        GenAppEntity visibleGenApp = createGenApp(VerticalYesNo.NO, OTHER_ORGANISATION_ID);

        TaskGroup taskGroup = underTest.evaluate(contextWith(visibleGenApp));

        assertTaskStatuses(taskGroup, TaskStatus.AVAILABLE);
    }

    @Test
    void shouldUseSecurityContextUserWhenCheckingWithoutNoticeApplicationVisibility() {
        stubUserRoles();
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CURRENT_ORGANISATION_ID);
        GenAppEntity visibleGenApp = createGenApp(VerticalYesNo.YES, CURRENT_ORGANISATION_ID);

        TaskGroup taskGroup = underTest.evaluate(contextWith(visibleGenApp));

        assertTaskStatuses(taskGroup, TaskStatus.AVAILABLE);
    }

    @Test
    void shouldMarkViewApplicationsAsNotAvailableWhenOnlyApplicationIsHiddenFromCurrentUser() {
        stubUserRoles();
        when(securityContextService.getCurrentUserId()).thenReturn(CURRENT_USER_ID);
        GenAppEntity hiddenGenApp = createGenApp(VerticalYesNo.YES, OTHER_ORGANISATION_ID);

        TaskGroup taskGroup = underTest.evaluate(contextWith(hiddenGenApp));

        assertTaskStatuses(taskGroup, TaskStatus.NOT_AVAILABLE);
    }

    @Test
    void shouldCountOnlyVisibleApplicationsWhenDeterminingAvailability() {
        stubUserRoles();
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CURRENT_ORGANISATION_ID);
        GenAppEntity hiddenGenApp = createGenApp(VerticalYesNo.YES, OTHER_ORGANISATION_ID);
        GenAppEntity visibleGenApp = createGenApp(VerticalYesNo.YES, CURRENT_ORGANISATION_ID);

        TaskGroup taskGroup = underTest.evaluate(contextWith(hiddenGenApp, visibleGenApp));

        assertTaskStatuses(taskGroup, TaskStatus.AVAILABLE);
    }

    @Test
    void shouldMarkViewApplicationsAsNotAvailableWhenContextMissing() {
        TaskGroup taskGroup = underTest.evaluate(null);

        assertTaskStatuses(taskGroup, TaskStatus.NOT_AVAILABLE);
    }

    @Test
    void shouldMarkViewApplicationsAsNotAvailableWhenCaseHasNoApplications() {
        TaskGroup taskGroup = underTest.evaluate(contextWith());

        assertTaskStatuses(taskGroup, TaskStatus.NOT_AVAILABLE);
    }

    private static DashboardContext contextWith(GenAppEntity... genApps) {
        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .genApps(Set.of(genApps))
            .build();
        PartyEntity defendant = PartyEntity.builder()
            .organisationId(OTHER_ORGANISATION_ID)
            .build();

        return new DashboardContext(TEST_CASE_REFERENCE, caseEntity, defendant, false, false);
    }

    private static GenAppEntity createGenApp(VerticalYesNo withoutNotice, String organisationId) {
        return GenAppEntity.builder()
            .state(GenAppState.GEN_APP_ISSUED)
            .withoutNotice(withoutNotice)
            .party(PartyEntity.builder().organisationId(organisationId).build())
            .build();
    }

    private void stubUserRoles() {
        when(userRoleService.getCurrentUserCaseRoles(TEST_CASE_REFERENCE))
            .thenReturn(new UserRoles(CURRENT_USER_ID, List.of()));
    }

    private void assertTaskStatuses(TaskGroup taskGroup, TaskStatus viewApplicationsStatus) {
        assertThat(taskGroup.getTasks())
            .extracting(ListValue::getValue)
            .extracting(Task::getTemplateId, Task::getStatus)
            .containsExactly(
                tuple(MAKE_GENERAL_APPLICATION, TaskStatus.AVAILABLE),
                tuple(VIEW_ALL_APPLICATIONS, viewApplicationsStatus)
            );
    }
}
