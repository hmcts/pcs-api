package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardData;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardTaskTemplateIds;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.ApplicationsTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.ClaimTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.HearingsTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.NoticesTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.ResponseTaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DashboardJourneyServiceTest {

    private static final long CASE_REFERENCE = 99_887_766L;

    private DashboardJourneyService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DashboardJourneyService(List.of(
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
                tuple("Defendant.CaseIssued", 2),
                tuple("Defendant.ResponseToClaim", 1)
            );

        assertThat(ListValueUtils.unwrapListItems(result.getNotifications()).getFirst().getTemplateValues())
            .extracting(lv -> lv.getValue().getKey(), lv -> lv.getValue().getValue())
            .contains(
                tuple("hearingDateTime", "2026-06-15T10:30:00Z"),
                tuple("responseEndDate", "2026-05-15")
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
                tuple(TaskGroupId.RESPONSE, 3),
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
                tuple(DashboardTaskTemplateIds.REVIEW_RESPONSE, TaskStatus.IN_PROGRESS),
                tuple(DashboardTaskTemplateIds.SUBMIT_RESPONSE, TaskStatus.COMPLETED)
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
            new DashboardContext(CASE_REFERENCE, caseEntity, null)
        );

        assertThat(ListValueUtils.unwrapListItems(result.getTaskGroups()).get(4).getTasks())
            .extracting(lv -> lv.getValue().getTemplateId(), lv -> lv.getValue().getStatus())
            .containsExactly(
                tuple(DashboardTaskTemplateIds.MAKE_GENERAL_APPLICATION, TaskStatus.AVAILABLE),
                tuple(DashboardTaskTemplateIds.VIEW_ALL_APPLICATIONS, TaskStatus.AVAILABLE)
            );
    }

    @Test
    void shouldOnlyExposeDeclaredPlaceholdersForResponseToClaimNotification() {
        PCSCase submitted = PCSCase.builder().build();

        DashboardData result = underTest.computeDashboardData(CASE_REFERENCE, submitted);

        List<String> keysForResponseNotification = ListValueUtils.unwrapListItems(result.getNotifications()).stream()
            .filter(n -> "Defendant.ResponseToClaim".equals(n.getTemplateId()))
            .findFirst()
            .orElseThrow()
            .getTemplateValues()
            .stream()
            .map(lv -> lv.getValue().getKey())
            .toList();

        assertThat(keysForResponseNotification).containsExactly("ctaLabel");
    }
}
