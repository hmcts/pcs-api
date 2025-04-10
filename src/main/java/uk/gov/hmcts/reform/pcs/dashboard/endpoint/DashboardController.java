package uk.gov.hmcts.reform.pcs.dashboard.endpoint;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard")
@AllArgsConstructor
public class DashboardController {

    @GetMapping(value = "/{caseReference}/notifications",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Get the active dashboard notifications for a case")
    @ApiResponse(responseCode = "200", description = "Request successful",
        content = {
            @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = DashboardNotification.class))
                )
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
        @PathVariable("caseReference") Long caseReference) {

        List<DashboardNotification> notifications = List.of();

        return ResponseEntity.ok(notifications);
    }

}
