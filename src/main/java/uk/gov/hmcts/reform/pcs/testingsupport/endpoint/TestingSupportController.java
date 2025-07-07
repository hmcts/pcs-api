package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
import uk.gov.hmcts.reform.pcs.testingsupport.model.DocAssemblyRequest;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
@Tag(name = "Testing Support")
public class TestingSupportController {

    private final SchedulerClient schedulerClient;
    private final Task<Void> helloWorldTask;
    private final DocAssemblyService docAssemblyService;

    public TestingSupportController(
        SchedulerClient schedulerClient,
        @Qualifier("helloWorldTask") Task<Void> helloWorldTask,
        DocAssemblyService docAssemblyService
    ) {
        this.schedulerClient = schedulerClient;
        this.helloWorldTask = helloWorldTask;
        this.docAssemblyService = docAssemblyService;
    }

    @Operation(
        summary = "Schedule a Hello World task",
        description = "Schedules a Hello World task to be executed after a specified delay. "
            + "This endpoint is used for testing the database scheduler functionality."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task scheduled successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authorization token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid or missing service authorization token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/db-scheduler-test")
    public ResponseEntity<String> scheduleHelloWorldTask(
        @Parameter(
            description = "Delay in seconds before task execution (default: 1)",
            example = "5"
        )
        @RequestParam(value = "delaySeconds", defaultValue = "1") int delaySeconds,
        @Parameter(
            description = "Bearer token for user authentication (default: DummyId)",
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @RequestHeader(value = AUTHORIZATION, defaultValue = "DummyId") String authorisation,
        @Parameter(
            description = "Service-to-Service (S2S) authorization token",
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            required = true
        )
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization) {
        try {
            String taskId = "helloWorld-" + UUID.randomUUID();
            Instant executionTime = Instant.now().plusSeconds(delaySeconds);

            schedulerClient.scheduleIfNotExists(
                helloWorldTask.instance(taskId),
                executionTime
            );

            log.info("Scheduled Hello World task with ID: {} to execute at: {}", taskId, executionTime);
            return ResponseEntity.ok(String.format(
                "Hello World task scheduled successfully with ID: %s, execution time: %s",
                taskId, executionTime));
        } catch (Exception e) {
            log.error("Failed to schedule Hello World task", e);
            return ResponseEntity.internalServerError()
                .body("Failed to schedule Hello World task: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Generate a document using Doc Assembly API",
        description = "Generates a document by sending a request to the Doc Assembly service "
            + "with template ID and form payload. Returns the URL of the generated document."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Document generated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - formPayload is required"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authorization token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Invalid or missing service authorization token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/generate-document", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> generateDocument(
        @Parameter(
            description = "Bearer token for user authentication",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @RequestHeader(value = "Authorization") String authorization,
        @Parameter(
            description = "Service-to-Service (S2S) authorization token",
            required = true,
            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @Parameter(
            description = "Document generation request containing template ID and form data",
            required = true
        )
        @RequestBody DocAssemblyRequest request
    ) {
        try {
            if (request == null || request.getFormPayload() == null) {
                return ResponseEntity.internalServerError().body("Doc Assembly service returned invalid document URL");
            }
            String documentUrl = docAssemblyService.generateDocument(request);

            // Validate that we got a valid document URL
            if (documentUrl == null || documentUrl.trim().isEmpty()) {
                log.error("Doc Assembly service returned null or empty document URL");
                return ResponseEntity.internalServerError()
                    .body("Doc Assembly service returned invalid document URL");
            }

            return ResponseEntity.created(URI.create(documentUrl)).body(documentUrl);
        } catch (DocAssemblyException e) {
            log.error("Doc Assembly service error: {}", e.getMessage(), e);
            return handleDocAssemblyException(e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to generate document", e);
            return ResponseEntity.internalServerError()
                .body("An error occurred while processing your request.");
        }
    }


    private ResponseEntity<String> handleDocAssemblyException(DocAssemblyException e) {
        String message = e.getMessage();

        if (message.contains("Bad request")) {
            return ResponseEntity.badRequest().body("Bad request to Doc Assembly service: " + message);
        } else if (message.contains("Authorization failed")) {
            return ResponseEntity.status(401).body("Authorization failed: " + message);
        } else if (message.contains("endpoint not found")) {
            return ResponseEntity.status(404).body("Doc Assembly service endpoint not found: " + message);
        } else if (message.contains("temporarily unavailable") || message.contains("service error")) {
            return ResponseEntity.status(503).body("Doc Assembly service is temporarily unavailable: " + message);
        } else {
            return ResponseEntity.internalServerError().body("Doc Assembly service error: " + message);
        }
    }
}
