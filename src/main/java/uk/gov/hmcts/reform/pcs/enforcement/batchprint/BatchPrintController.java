package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.controllers.RestExceptionHandler;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffDirectoryService;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Batch print for bailiffs (HDPI-8012): print the warrant, risk assessment and notice of eviction for several
 * cases in one action.
 *
 * <p>CCD has no screen that acts on more than one case, and XUI's "My work" cannot act on several tasks at
 * once, so batch print is offered as an API for a selection screen to call rather than as a CCD event. The
 * caller must have the bailiff organisational role in AM, checked on every request, and every case is checked
 * against their own allocation. With the {@code enforcement-enabled} flag off every endpoint answers 404.
 *
 * <p>A selection screen lists the bailiff's warrants with {@code GET /cases}, shows what would be printed and
 * why any case is left out with {@code POST /check}, and downloads the file with {@code POST}. The file
 * endpoint names the printed and left-out cases in the {@value #INCLUDED_HEADER} and {@value #EXCLUDED_HEADER}
 * headers too, because a case can drop out between the check and the download.
 *
 * <p>The file endpoint answers 503 with a {@code Retry-After} when the instance is already producing as many
 * batch prints as it will fetch documents for, rather than holding the request thread until the gateway gives
 * up on it.
 */
@RestController
@RequestMapping("/enforcement/batch-print")
@RequiredArgsConstructor
@Tag(name = "Enforcement batch print", description = "Print enforcement documents across a bailiff's cases")
public class BatchPrintController {

    /**
     * The most cases one request may name, to bound the size of the file and the time taken to build it.
     */
    public static final int MAX_CASES = 50;

    static final String INCLUDED_HEADER = "Batch-Print-Included-Cases";
    static final String EXCLUDED_HEADER = "Batch-Print-Excluded-Cases";
    private static final String FILE_NAME = "enforcement-documents.pdf";
    /**
     * How long to ask a bailiff to wait after a refusal: long enough for the batch prints ahead of theirs to
     * finish, rather than coming straight back and being refused again.
     */
    private static final String RETRY_AFTER_SECONDS = "60";

    private final IdamAuthenticator idamAuthenticator;
    private final BatchPrintService batchPrintService;
    private final BatchPrintPlanner batchPrintPlanner;
    private final BailiffDirectoryService bailiffDirectoryService;
    private final FeatureToggleService featureToggleService;

    @GetMapping(value = "/cases", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "List the live warrants allocated to the signed-in bailiff",
        description = "Soonest eviction first. With from and/or to, only evictions scheduled in that range.",
        security = {
            @SecurityRequirement(name = "AuthorizationToken"),
            @SecurityRequirement(name = "ServiceAuthorization")
        }
    )
    @ApiResponse(responseCode = "200", description = "The bailiff's warrants")
    @ApiResponse(responseCode = "401", description = "Invalid access token", content = @Content())
    @ApiResponse(responseCode = "403", description = "Invalid Service Authorization, or the user is not a bailiff",
        content = @Content())
    @ApiResponse(responseCode = "404", description = "Enforcement is not enabled", content = @Content())
    public ResponseEntity<List<AllocatedWarrant>> allocatedWarrants(
        @Parameter(description = "Bearer token for user authentication", required = true)
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @Parameter(description = "Service-to-Service (S2S) authorization token", required = true)
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @Parameter(description = "Earliest eviction date, inclusive")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @Parameter(description = "Latest eviction date, inclusive")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (!enforcementEnabled()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(batchPrintPlanner.allocatedWarrants(bailiffUserId(authorization), from, to));
    }

    @PostMapping(value = "/check", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Show what a batch print of the chosen cases would contain",
        description = "For each printed case, its documents in print order; for each case left out, why.",
        security = {
            @SecurityRequirement(name = "AuthorizationToken"),
            @SecurityRequirement(name = "ServiceAuthorization")
        }
    )
    @ApiResponse(responseCode = "200", description = "The printed and left-out cases")
    @ApiResponse(responseCode = "400", description = "No cases, or more than 50", content = @Content())
    @ApiResponse(responseCode = "401", description = "Invalid access token", content = @Content())
    @ApiResponse(responseCode = "403", description = "Invalid Service Authorization, or the user is not a bailiff",
        content = @Content())
    @ApiResponse(responseCode = "404", description = "Enforcement is not enabled", content = @Content())
    public ResponseEntity<BatchPrintPlan> check(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @Valid @RequestBody BatchPrintRequest request) {

        if (!enforcementEnabled()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(batchPrintPlanner.plan(bailiffUserId(authorization), request.caseReferences()));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Print the chosen cases' enforcement documents as one PDF",
        description = "For each printed case in the order given: the latest warrant, risk assessment and notice "
            + "of eviction. The " + INCLUDED_HEADER + " and " + EXCLUDED_HEADER + " headers list the printed "
            + "cases and the left-out ones with their reason code.",
        security = {
            @SecurityRequirement(name = "AuthorizationToken"),
            @SecurityRequirement(name = "ServiceAuthorization")
        }
    )
    @ApiResponse(responseCode = "200", description = "The PDF")
    @ApiResponse(responseCode = "400", description = "No cases, or more than 50", content = @Content())
    @ApiResponse(responseCode = "401", description = "Invalid access token", content = @Content())
    @ApiResponse(responseCode = "403", description = "Invalid Service Authorization, or the user is not a bailiff",
        content = @Content())
    @ApiResponse(responseCode = "404", description = "Enforcement is not enabled", content = @Content())
    @ApiResponse(responseCode = "422", description = "None of the cases can be printed; the body says why")
    @ApiResponse(responseCode = "503", description = "Too many batch prints are being produced; retry later")
    public ResponseEntity<?> print(
        HttpServletRequest httpRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @Valid @RequestBody BatchPrintRequest request) {

        if (!enforcementEnabled()) {
            return ResponseEntity.notFound().build();
        }
        BatchPrint batchPrint;
        try {
            batchPrint = batchPrintService.print(bailiffUserId(authorization), request.caseReferences());
        } catch (BatchPrintBusyException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RestExceptionHandler.Error(e.getMessage()));
        }
        if (batchPrint.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(batchPrint.plan());
        }
        // The file is streamed after this method returns, and may never be streamed at all.
        BatchPrintFileCleanupFilter.deleteWhenRequestFinishes(httpRequest, batchPrint.directory());
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename(FILE_NAME).build().toString())
            .header(INCLUDED_HEADER, includedHeader(batchPrint.plan()))
            .header(EXCLUDED_HEADER, excludedHeader(batchPrint.plan()))
            .body(new FileSystemResource(batchPrint.pdf()));
    }

    private boolean enforcementEnabled() {
        return featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT);
    }

    /**
     * The signed-in user's IDAM id, once AM confirms they are a bailiff now: an allocation outlives the role.
     */
    private String bailiffUserId(String authorization) {
        String userId = idamAuthenticator.validateAuthToken(authorization).getUserDetails().getUid();
        if (!bailiffDirectoryService.isBailiff(userId)) {
            throw new CaseAccessException("Batch print is for bailiffs only");
        }
        return userId;
    }

    private static String includedHeader(BatchPrintPlan plan) {
        return plan.included().stream().map(PrintableCase::caseReference).collect(Collectors.joining(","));
    }

    /**
     * Each left-out case as {@code reference=CODE}. A reference the bailiff mistyped is not echoed back, because
     * it could hold characters a header cannot carry.
     */
    private static String excludedHeader(BatchPrintPlan plan) {
        return plan.excluded().stream()
            .filter(excluded -> excluded.code() != BatchPrintExclusion.INVALID_REFERENCE)
            .map(excluded -> excluded.caseReference() + "=" + excluded.code())
            .collect(Collectors.joining(","));
    }

    /**
     * The cases to print, in print order. References may be written with or without dashes.
     */
    public record BatchPrintRequest(
        @NotEmpty @Size(max = MAX_CASES) List<String> caseReferences) {
    }
}
