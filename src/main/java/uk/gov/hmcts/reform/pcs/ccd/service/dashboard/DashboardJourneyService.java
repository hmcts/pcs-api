package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardData;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardNotification;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.Task;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TemplateValue;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.List;
import java.util.Map;

/**
 * Computes dashboard data (notifications + task groups) from submitted case data.
 * READ-ONLY
 */
@Service
@Slf4j
public class DashboardJourneyService {

    public DashboardData computeDashboardData(long caseReference, PCSCase submittedCaseData) {
        List<ListValue<DashboardNotification>> notifications = computeNotifications();
        List<ListValue<TaskGroup>> taskGroups = computeTaskGroups();

        log.info("DashboardJourneyService computed {} notification(s) and {} taskGroup(s) for case={}",
                 notifications.size(), taskGroups.size(), caseReference);

        return DashboardData.builder()
            .caseId(String.valueOf(caseReference))
            .propertyAddress(submittedCaseData.getPropertyAddress())
            .notifications(notifications)
            .taskGroups(taskGroups)
            .build();
    }

    private List<ListValue<DashboardNotification>> computeNotifications() {
        return ListValueUtils.wrapListItems(List.of(
            DashboardNotification.builder()
                .templateId("Defendant.CaseIssued")
                .templateValues(toTemplateValues(Map.of(
                    "hearingDateTime", "2026-06-15T10:30:00Z",
                    "responseEndDate", "2026-05-15"
                )))
                .build(),
            DashboardNotification.builder()
                .templateId("Defendant.ResponseToClaim")
                .templateValues(toTemplateValues(Map.of(
                    "ctaLabel", "Start your response."
                )))
                .build()
        ));
    }

    private List<ListValue<TaskGroup>> computeTaskGroups() {
        return ListValueUtils.wrapListItems(List.of(
            TaskGroup.builder()
                .groupId("CLAIM")
                .tasks(ListValueUtils.wrapListItems(List.of(
                    Task.builder()
                        .templateId("Defendant.ViewClaim")
                        .status("AVAILABLE")
                        .build(),
                    Task.builder()
                        .templateId("Defendant.ViewDocuments")
                        .status("NOT_AVAILABLE")
                        .build()
                )))
                .build(),
            TaskGroup.builder()
                .groupId("RESPONSE")
                .tasks(ListValueUtils.wrapListItems(List.of(
                    Task.builder()
                        .templateId("Defendant.RespondToClaim")
                        .status("NOT_STARTED")
                        .build(),
                    Task.builder()
                        .templateId("Defendant.ReviewResponse")
                        .status("IN_PROGRESS")
                        .build(),
                    Task.builder()
                        .templateId("Defendant.SubmitResponse")
                        .status("COMPLETED")
                        .build()
                )))
                .build()
        ));
    }

    /**
     * Entries with a null value are omitted so optional placeholders (for example {@code ctaLink}) can be left out
     * of the map rather than sent as null.
     */
    private List<ListValue<TemplateValue>> toTemplateValues(Map<String, String> values) {
        return ListValueUtils.wrapListItems(
            values.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> TemplateValue.builder()
                    .key(e.getKey())
                    .value(e.getValue())
                    .build())
                .toList()
        );
    }
}
