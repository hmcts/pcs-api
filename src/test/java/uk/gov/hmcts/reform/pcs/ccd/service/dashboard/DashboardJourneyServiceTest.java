package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardData;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.ApplicationsTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.ClaimTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.ResponseTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.HearingsTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.NoticesTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardJourneyServiceTest {

    private static final long CASE_REFERENCE = 99_887_766L;

    private DashboardJourneyService underTest;

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private DefendantResponseService defendantResponseService;

    @BeforeEach
    void setUp() {
        underTest = new DashboardJourneyService(
            draftCaseDataService, defendantResponseService, List.of(
                new ClaimTaskGroupEvaluator(),
                new ResponseTaskGroupEvaluator(),
                new ApplicationsTaskGroupEvaluator(),
                new HearingsTaskGroupEvaluator(),
                new NoticesTaskGroupEvaluator()
        ));
    }

    @Test
    void shouldPopulateCaseMetadata() {
        AddressUK propertyAddress = AddressUK.builder().addressLine1("1 High Street").postCode("SW1A 1AA").build();
        PCSCase submitted = PCSCase.builder().propertyAddress(propertyAddress).build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted);

        assertThat(result.getCaseId()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(result.getPropertyAddress()).isEqualTo(propertyAddress);
        assertThat(result.getNotifications()).hasSize(2);
        assertThat(result.getTaskGroups()).hasSize(5);
    }

    @Test
    void shouldReturnNotificationsWithExpectedTemplatesAndValues() {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted);

        assertThat(ListValueUtils.unwrapListItems(result.getNotifications()))
            .extracting(n -> n.getTemplateId(), n -> n.getTemplateValues().size())
            .containsExactly(
                tuple("Defendant.NoHearingArranged", 0),
                tuple("Defendant.ResponseNotStarted", 0)
            );
    }

    @Test
    void shouldReturnTaskGroupsWithExpectedStructure() {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted);

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()))
            .extracting(g -> g.getGroupId(), g -> g.getTasks().size())
            .containsExactly(
                tuple(TaskGroupId.CLAIM, 2),
                tuple(TaskGroupId.RESPONSE, 2),
                tuple(TaskGroupId.HEARING, 1),
                tuple(TaskGroupId.NOTICE, 1),
                tuple(TaskGroupId.APPLICATIONS, 2)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(0).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.VIEW_CLAIM, TaskStatus.AVAILABLE),
                tuple(DashboardTaskTemplateIds.VIEW_DOCUMENTS, TaskStatus.NOT_AVAILABLE)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(1).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.RESPOND_TO_CLAIM, TaskStatus.NOT_STARTED),
                tuple(DashboardTaskTemplateIds.VIEW_RESPONSE, TaskStatus.COMPLETED)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(2).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.VIEW_HEARING_DOCUMENTS, TaskStatus.AVAILABLE)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(3).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.VIEW_ORDERS_AND_NOTICES, TaskStatus.AVAILABLE)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(4).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.MAKE_GENERAL_APPLICATION, TaskStatus.AVAILABLE),
                tuple(DashboardTaskTemplateIds.VIEW_ALL_APPLICATIONS, TaskStatus.NOT_AVAILABLE)
            );
    }

    @Test
    void shouldShowViewApplicationsTaskWhenAtLeastOneGeneralApplicationExists() {
        PCSCase submitted = PCSCase.builder().build();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .genApps(java.util.Set.of(GenAppEntity.builder().build()))
            .build();

        DashboardData result = underTest.computeDashboardData(
            CASE_REFERENCE,
            submitted,
            caseEntity,
            null
        );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(4).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.MAKE_GENERAL_APPLICATION, TaskStatus.AVAILABLE),
                tuple(DashboardTaskTemplateIds.VIEW_ALL_APPLICATIONS, TaskStatus.AVAILABLE)
            );
    }

    @Test
    void shouldUseResponseInProgressNotificationWhenDraftExists() {
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(true);
        when(defendantResponseService.hasSubmittedResponse(CASE_REFERENCE)).thenReturn(false);

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, PCSCase.builder().build());

        assertThat(ListValueUtils.unwrapListItems(result.getNotifications()))
            .extracting(n -> n.getTemplateId())
            .containsExactly(
                "Defendant.NoHearingArranged",
                "Defendant.ResponseInProgress"
            );
    }

    @Test
    void shouldMarkRespondToClaimInProgressWhenDraftExists() {
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(true);
        when(defendantResponseService.hasSubmittedResponse(CASE_REFERENCE)).thenReturn(false);

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, PCSCase.builder().build());

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(1).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .contains(tuple(DashboardTaskTemplateIds.RESPOND_TO_CLAIM, TaskStatus.IN_PROGRESS));
    }

    @Test
    void shouldMarkRespondCompletedAndViewResponseAvailableWhenSubmittedExists() {
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(false);
        when(defendantResponseService.hasSubmittedResponse(CASE_REFERENCE)).thenReturn(true);

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, PCSCase.builder().build());

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(1).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .contains(
                tuple(DashboardTaskTemplateIds.RESPOND_TO_CLAIM, TaskStatus.COMPLETED)
            );
    }
}
