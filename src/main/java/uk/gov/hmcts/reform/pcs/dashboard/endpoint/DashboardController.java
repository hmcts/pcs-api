package uk.gov.hmcts.reform.pcs.dashboard.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;
import uk.gov.hmcts.reform.pcs.dashboard.model.TaskGroup;
import uk.gov.hmcts.reform.pcs.dashboard.service.DashboardNotificationService;
import uk.gov.hmcts.reform.pcs.dashboard.service.DashboardTaskService;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard")
@AllArgsConstructor
public class DashboardController {

    private final DashboardNotificationService dashboardNotificationService;
    private final DashboardTaskService dashboardTaskService;

    @GetMapping(value = "/{caseReference}/notifications",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Get the active dashboard notifications for a case")
    @ApiResponse(responseCode = "200", description = "Request successful",
        content = {
            @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = DashboardNotification.class)))
        })
    @ApiResponse(
        responseCode = "401",
        description = "Access token is missing or invalid",
        content = @Content()
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorised to use the endpoint",
        content = @Content()
    )
    @ApiResponse(
        responseCode = "404",
        description = "Case reference not found",
        content = @Content()
    )
    public ResponseEntity<List<DashboardNotification>> getNotificationsForCase(
        @PathVariable("caseReference") Long caseReference,
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization) {

        List<DashboardNotification> notifications = dashboardNotificationService.getNotifications(caseReference);

        return ResponseEntity.ok(notifications);
    }

    @GetMapping(value = "/{caseReference}/tasks",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Get the tasks associated with the dashboard for a case")
    @ApiResponse(responseCode = "200", description = "Request successful",
        content = {
            @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = TaskGroup.class)))
        })
    @ApiResponse(
        responseCode = "401",
        description = "Access token is missing or invalid",
        content = @Content()
    )
    @ApiResponse(
        responseCode = "403",
        description = "Not authorised to use the endpoint",
        content = @Content()
    )
    @ApiResponse(
        responseCode = "404",
        description = "Case reference not found",
        content = @Content()
    )
    public ResponseEntity<List<TaskGroup>> getTasksForCase(
        @RequestHeader(value = "ServiceAuthorization") String authorisation,
        @Parameter(description = "The unique identifier of the case to fetch tasks for", required = true)
        @PathVariable("caseReference") Long caseReference) {

        var tasks = dashboardTaskService.getTasks(caseReference);

        return ResponseEntity.ok(tasks);
    }
}
