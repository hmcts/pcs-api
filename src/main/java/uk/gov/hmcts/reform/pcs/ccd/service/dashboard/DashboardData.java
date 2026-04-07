package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;
import uk.gov.hmcts.reform.pcs.dashboard.model.TaskGroup;

import java.util.List;

/**
 * TEMP (HDPI-5421): debugging.
 */
public record DashboardData(
    String claimantName,
    String possessionPropertyAddress,
    List<DashboardNotification> notifications,
    List<TaskGroup> taskGroups,
    // TODO (HDPI-5421): remove when CCD state is available on the callback; see StartDashboardViewHandler */
    String appliedCaseState,
    // TODO (HDPI-5421): readable note for debugging / frontend; remove with appliedCaseState */
    String stateResolution
) {}
