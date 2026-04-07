package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;
import uk.gov.hmcts.reform.pcs.dashboard.model.Task;
import uk.gov.hmcts.reform.pcs.dashboard.model.TaskGroup;

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

        List<DashboardNotification> notifications = computeNotifications(state);
        List<TaskGroup> taskGroups = computeTaskGroups(state);

        log.info("DashboardJourneyService computed {} notification(s) and {} taskGroup(s) for state={}",
                 notifications.size(), taskGroups.size(), state);

        // return new DashboardData(claimantName, propertyAddress, notifications, taskGroups);

        return new DashboardData(
            claimantName,
            propertyAddress,
            notifications,
            taskGroups,
            appliedCaseState,
            stateResolution
        );
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

    private List<DashboardNotification> computeNotifications(State state) {
        if (state == State.CASE_ISSUED) {
            return List.of(
                DashboardNotification.builder()
                    .templateId("Notice.PCS.Dashboard.CaseIssued")
                    .templateValues(Map.of(
                        "hearingDateTime", "2026-06-15T10:30:00Z",
                        "responseEndDate", "2026-05-15"
                    ))
                    .build()
            );
        }

        if (state == State.PENDING_CASE_ISSUED) {
            return List.of(
                DashboardNotification.builder()
                    .templateId("Notice.PCS.Dashboard.PendingCaseIssued")
                    .templateValues(Map.of(
                        "submittedDate", "2026-04-01"
                    ))
                    .build()
            );
        }

        return List.of();
    }

    private List<TaskGroup> computeTaskGroups(State state) {
        if (state == State.CASE_ISSUED) {
            return List.of(
                TaskGroup.builder()
                    .groupId("CLAIM")
                    .tasks(List.of(
                        Task.builder()
                            .templateId("Task.PCS.Claim.ViewClaim")
                            .status("AVAILABLE")
                            .build(),
                        Task.builder()
                            .templateId("Task.PCS.Claim.ViewDocuments")
                            .status("NOT_AVAILABLE")
                            .build()
                    ))
                    .build(),
                TaskGroup.builder()
                    .groupId("RESPONSE")
                    .tasks(List.of(
                        Task.builder()
                            .templateId("Task.PCS.Response.RespondToClaim")
                            .status("ACTION_NEEDED")
                            .build()
                    ))
                    .build()
            );
        }

        if (state == State.PENDING_CASE_ISSUED) {
            return List.of(
                TaskGroup.builder()
                    .groupId("CLAIM")
                    .tasks(List.of(
                        Task.builder()
                            .templateId("Task.PCS.Claim.ViewClaim")
                            .status("NOT_AVAILABLE")
                            .build()
                    ))
                    .build()
            );
        }

        return List.of();
    }
}
