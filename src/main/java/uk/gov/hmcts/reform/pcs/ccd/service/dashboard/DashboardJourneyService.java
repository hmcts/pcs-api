package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardData;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;
import uk.gov.hmcts.reform.pcs.dashboard.model.Task;
import uk.gov.hmcts.reform.pcs.dashboard.model.TaskGroup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

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

    public DashboardData computeDashboardData(PCSCase submittedCaseData,
                                              State state,
                                              String appliedCaseState,
                                              String stateResolution) {
        String claimantName = extractClaimantName(submittedCaseData);
        String propertyAddress = addressFormatter.formatShortAddress(
            submittedCaseData.getPropertyAddress(),
            AddressFormatter.COMMA_DELIMITER
        );

        List<ListValue<DashboardNotification>> notifications = computeNotifications(state);
        List<ListValue<TaskGroup>> taskGroups = computeTaskGroups(state);

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

    private List<ListValue<DashboardNotification>> computeNotifications(State state) {
        if (state == State.CASE_ISSUED) {
            return ListValueUtils.wrapListItems(List.of(
                DashboardNotification.builder()
                    .templateId("Notice.PCS.Dashboard.CaseIssued")
                    .templateValues(Map.of(
                        "hearingDateTime", TextNode.valueOf("2026-06-15T10:30:00Z"),
                        "responseEndDate", TextNode.valueOf("2026-05-15")
                    ))
                    .build()
            ));
        }

        if (state == State.PENDING_CASE_ISSUED) {
            return ListValueUtils.wrapListItems(List.of(
                DashboardNotification.builder()
                    .templateId("Notice.PCS.Dashboard.PendingCaseIssued")
                    .templateValues(Map.of(
                        "submittedDate", TextNode.valueOf("2026-04-01")
                    ))
                    .build()
            ));
        }

        return List.of();
    }

    private List<ListValue<TaskGroup>> computeTaskGroups(State state) {
        if (state == State.CASE_ISSUED) {
            return ListValueUtils.wrapListItems(List.of(
                TaskGroup.builder()
                    .groupId("CLAIM")
                    .tasks(ListValueUtils.wrapListItems(List.of(
                        Task.builder()
                            .templateId("Task.PCS.Claim.ViewClaim")
                            .templateValues(Map.<String, JsonNode>of())
                            .status("AVAILABLE")
                            .build(),
                        Task.builder()
                            .templateId("Task.PCS.Claim.ViewDocuments")
                            .templateValues(Map.<String, JsonNode>of())
                            .status("NOT_AVAILABLE")
                            .build()
                    )))
                    .build(),
                TaskGroup.builder()
                    .groupId("RESPONSE")
                    .tasks(ListValueUtils.wrapListItems(List.of(
                        Task.builder()
                            .templateId("Task.PCS.Response.RespondToClaim")
                            .templateValues(Map.<String, JsonNode>of())
                            .status("ACTION_NEEDED")
                            .build()
                    )))
                    .build()
            ));
        }

        if (state == State.PENDING_CASE_ISSUED) {
            return ListValueUtils.wrapListItems(List.of(
                TaskGroup.builder()
                    .groupId("CLAIM")
                    .tasks(ListValueUtils.wrapListItems(List.of(
                        Task.builder()
                            .templateId("Task.PCS.Claim.ViewClaim")
                            .templateValues(Map.<String, JsonNode>of())
                            .status("NOT_AVAILABLE")
                            .build()
                    )))
                    .build()
            ));
        }

        return List.of();
    }
}
