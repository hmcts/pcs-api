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
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

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
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final SecurityContextService securityContextService = mock(SecurityContextService.class);
    private final LegalRepresentativeRepository legalRepresentativeRepository =
        mock(LegalRepresentativeRepository.class);
    private final GenAppVisibilityService genAppVisibilityService =
        new GenAppVisibilityService(legalRepresentativeRepository);
    private final ApplicationsTaskGroupEvaluator underTest =
        new ApplicationsTaskGroupEvaluator(securityContextService, genAppVisibilityService);

    @Test
    void shouldReturnApplicationsGroupId() {
        assertThat(underTest.groupId()).isEqualTo(TaskGroupId.APPLICATIONS);
    }

    @Test
    void shouldMarkViewApplicationsAsAvailableWhenCaseHasVisibleApplication() {
        when(securityContextService.getCurrentUserId()).thenReturn(CURRENT_USER_ID);
        GenAppEntity visibleGenApp = createGenApp(VerticalYesNo.NO, OTHER_USER_ID);

        TaskGroup taskGroup = underTest.evaluate(contextWith(visibleGenApp));

        assertTaskStatuses(taskGroup, TaskStatus.AVAILABLE);
    }

    @Test
    void shouldUseSecurityContextUserWhenCheckingWithoutNoticeApplicationVisibility() {
        when(securityContextService.getCurrentUserId()).thenReturn(CURRENT_USER_ID);
        GenAppEntity visibleGenApp = createGenApp(VerticalYesNo.YES, CURRENT_USER_ID);

        TaskGroup taskGroup = underTest.evaluate(contextWith(visibleGenApp));

        assertTaskStatuses(taskGroup, TaskStatus.AVAILABLE);
    }

    @Test
    void shouldMarkViewApplicationsAsNotAvailableWhenOnlyApplicationIsHiddenFromCurrentUser() {
        when(securityContextService.getCurrentUserId()).thenReturn(CURRENT_USER_ID);
        GenAppEntity hiddenGenApp = createGenApp(VerticalYesNo.YES, OTHER_USER_ID);

        TaskGroup taskGroup = underTest.evaluate(contextWith(hiddenGenApp));

        assertTaskStatuses(taskGroup, TaskStatus.NOT_AVAILABLE);
    }

    @Test
    void shouldCountOnlyVisibleApplicationsWhenDeterminingAvailability() {
        when(securityContextService.getCurrentUserId()).thenReturn(CURRENT_USER_ID);
        GenAppEntity hiddenGenApp = createGenApp(VerticalYesNo.YES, OTHER_USER_ID);
        GenAppEntity visibleGenApp = createGenApp(VerticalYesNo.YES, CURRENT_USER_ID);

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
            .idamId(OTHER_USER_ID)
            .build();

        return new DashboardContext(100L, caseEntity, defendant, false, false);
    }

    private static GenAppEntity createGenApp(VerticalYesNo withoutNotice, UUID partyIdamId) {
        return GenAppEntity.builder()
            .state(GenAppState.GEN_APP_ISSUED)
            .withoutNotice(withoutNotice)
            .party(PartyEntity.builder().idamId(partyIdamId).build())
            .build();
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
