package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardData;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardNotificationData;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskData;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupData;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.List;
import java.util.Map;

/**
 * Computes dashboard data (notifications + task groups) from submitted case data and current state.
 * READ-ONLY
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardJourneyService {

    private final AddressFormatter addressFormatter;
    private final ObjectMapper objectMapper;

    public DashboardData computeDashboardData(PCSCase submittedCaseData,
                                              State state,
                                              String appliedCaseState,
                                              String stateResolution) {
        String claimantName = extractClaimantName(submittedCaseData);
        String propertyAddress = addressFormatter.formatShortAddress(
            submittedCaseData.getPropertyAddress(),
            AddressFormatter.COMMA_DELIMITER
        );

        List<ListValue<DashboardNotificationData>> notifications = computeNotifications(state);
        List<ListValue<TaskGroupData>> taskGroups = computeTaskGroups(state);

        log.info("DashboardJourneyService computed {} notification(s) and {} taskGroup(s) for state={}",
                 notifications.size(), taskGroups.size(), state);

        return DashboardData.builder()
            .claimantName(claimantName)
            .possessionPropertyAddress(propertyAddress)
            .notifications(notifications)
            .taskGroups(taskGroups)
            .appliedCaseState(appliedCaseState)
            .stateResolution(stateResolution)
            .build();
    }

    private String extractClaimantName(PCSCase caseData) {
        ClaimantInformation info = caseData.getClaimantInformation();
        if (info == null) {
            return null;
        }
        if (info.getOverriddenClaimantName() != null) {
            return info.getOverriddenClaimantName();
        }
        if (info.getClaimantName() != null) {
            return info.getClaimantName();
        }
        return info.getFallbackClaimantName();
    }

    private List<ListValue<DashboardNotificationData>> computeNotifications(State state) {
        if (state == State.CASE_ISSUED) {
            return ListValueUtils.wrapListItems(List.of(
                DashboardNotificationData.builder()
                    .templateId("Notice.PCS.Dashboard.CaseIssued")
                    .templateValues(serializeMap(Map.of(
                        "hearingDateTime", "2026-06-15T10:30:00Z",
                        "responseEndDate", "2026-05-15"
                    )))
                    .build()
            ));
        }

        if (state == State.PENDING_CASE_ISSUED) {
            return ListValueUtils.wrapListItems(List.of(
                DashboardNotificationData.builder()
                    .templateId("Notice.PCS.Dashboard.PendingCaseIssued")
                    .templateValues(serializeMap(Map.of(
                        "submittedDate", "2026-04-01"
                    )))
                    .build()
            ));
        }

        return List.of();
    }

    private List<ListValue<TaskGroupData>> computeTaskGroups(State state) {
        if (state == State.CASE_ISSUED) {
            return ListValueUtils.wrapListItems(List.of(
                TaskGroupData.builder()
                    .groupId("CLAIM")
                    .tasks(ListValueUtils.wrapListItems(List.of(
                        TaskData.builder()
                            .templateId("Task.PCS.Claim.ViewClaim")
                            .status("AVAILABLE")
                            .build(),
                        TaskData.builder()
                            .templateId("Task.PCS.Claim.ViewDocuments")
                            .status("NOT_AVAILABLE")
                            .build()
                    )))
                    .build(),
                TaskGroupData.builder()
                    .groupId("RESPONSE")
                    .tasks(ListValueUtils.wrapListItems(List.of(
                        TaskData.builder()
                            .templateId("Task.PCS.Response.RespondToClaim")
                            .status("ACTION_NEEDED")
                            .build()
                    )))
                    .build()
            ));
        }

        if (state == State.PENDING_CASE_ISSUED) {
            return ListValueUtils.wrapListItems(List.of(
                TaskGroupData.builder()
                    .groupId("CLAIM")
                    .tasks(ListValueUtils.wrapListItems(List.of(
                        TaskData.builder()
                            .templateId("Task.PCS.Claim.ViewClaim")
                            .status("NOT_AVAILABLE")
                            .build()
                    )))
                    .build()
            ));
        }

        return List.of();
    }

    private String serializeMap(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize template values", e);
            return "{}";
        }
    }
}
