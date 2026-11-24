package uk.gov.hmcts.reform.pcs.bulkclaim.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.security.BulkClaimApimS2sFilter;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimV2Handler;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.io.IOException;
import java.io.InputStream;

@Tag(name = "Bulk claims v2")
@RestController
@RequestMapping("/v2/bulk-claims")
@RequiredArgsConstructor
public class BulkClaimV2Controller {

    private static final String XML = MediaType.APPLICATION_XML_VALUE;
    private static final String TEXT_XML = MediaType.TEXT_XML_VALUE;

    private final FeatureToggleService featureToggleService;
    private final BulkClaimProperties properties;
    private final BulkClaimV2Handler handler;

    @Operation(
        summary = "Submit a set of rental possession claims",
        description = "Validates each claim and creates a case for it, returning a response per claim. "
            + "With validateOnly=true nothing is stored and no case is created.",
        requestBody = @RequestBody(content = @Content(
            mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
        responses = {
            @ApiResponse(responseCode = "200", description = "Claims processed; see the overall and claim statuses",
                content = @Content(mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
            @ApiResponse(responseCode = "400", description = "The request does not conform to the XML schema",
                content = @Content(mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
            @ApiResponse(responseCode = "403", description = "The organisation cannot submit bulk claims",
                content = @Content(mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
            @ApiResponse(responseCode = "413", description = "Too many claims or the request is too large",
                content = @Content),
            @ApiResponse(responseCode = "404", description = "Bulk claims are not enabled", content = @Content)
        })
    @PostMapping(value = "/rental", consumes = {XML, TEXT_XML}, produces = XML)
    public ResponseEntity<byte[]> submitRental(HttpServletRequest request,
                                               @Parameter(description = "Validate only, creating nothing")
                                               @RequestParam(defaultValue = "false") boolean validateOnly)
        throws IOException {
        if (!bulkClaimsEnabled()) {
            return notFound();
        }
        byte[] body = readBounded(request);
        if (body == null) {
            return payloadTooLarge();
        }
        return handler.submitRental(body, organisationId(request), validateOnly);
    }

    @Operation(
        summary = "Submit a set of mortgage possession claims",
        description = "Validates each claim and creates a case for it, returning a response per claim. "
            + "With validateOnly=true nothing is stored and no case is created.",
        requestBody = @RequestBody(content = @Content(
            mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
        responses = {
            @ApiResponse(responseCode = "200", description = "Claims processed; see the overall and claim statuses",
                content = @Content(mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
            @ApiResponse(responseCode = "400", description = "The request does not conform to the XML schema",
                content = @Content(mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
            @ApiResponse(responseCode = "403", description = "The organisation cannot submit bulk claims",
                content = @Content(mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
            @ApiResponse(responseCode = "413", description = "Too many claims or the request is too large",
                content = @Content),
            @ApiResponse(responseCode = "404", description = "Mortgage bulk claims are not enabled",
                content = @Content)
        })
    @PostMapping(value = "/mortgage", consumes = {XML, TEXT_XML}, produces = XML)
    public ResponseEntity<byte[]> submitMortgage(HttpServletRequest request,
                                                 @Parameter(description = "Validate only, creating nothing")
                                                 @RequestParam(defaultValue = "false") boolean validateOnly)
        throws IOException {
        if (!bulkClaimsEnabled() || !featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2_MORTGAGE)) {
            return notFound();
        }
        byte[] body = readBounded(request);
        if (body == null) {
            return payloadTooLarge();
        }
        return handler.submitMortgage(body, organisationId(request), validateOnly);
    }

    @Operation(
        summary = "Get the status of a submitted claim",
        description = "Reports the claim outcome, case reference and state, and the fee payment status.",
        responses = {
            @ApiResponse(responseCode = "200", description = "The claim status",
                content = @Content(mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
            @ApiResponse(responseCode = "404", description = "No such claim for this organisation",
                content = @Content)
        })
    @GetMapping(value = "/claims/{customerUniqueIdentifier}", produces = XML)
    public ResponseEntity<byte[]> status(HttpServletRequest request,
                                         @PathVariable String customerUniqueIdentifier) {
        if (!bulkClaimsEnabled()) {
            return notFound();
        }
        return handler.status(organisationId(request), customerUniqueIdentifier);
    }

    @Operation(
        summary = "Get the status of up to 100 submitted claims",
        description = "Reports the status of each requested claim, in request order. A customer unique identifier "
            + "with no claim for this organisation is listed with status code 2 and fail reason code 17 rather "
            + "than failing the call. A repeated identifier is answered at each position.",
        requestBody = @RequestBody(content = @Content(
            mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
        responses = {
            @ApiResponse(responseCode = "200", description = "One status entry per requested identifier",
                content = @Content(mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
            @ApiResponse(responseCode = "400", description = "The request does not conform to the XML schema",
                content = @Content(mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
            @ApiResponse(responseCode = "413", description = "Too many identifiers or the request is too large",
                content = @Content),
            @ApiResponse(responseCode = "404", description = "Bulk claims are not enabled", content = @Content)
        })
    @PostMapping(value = "/claims/status", consumes = {XML, TEXT_XML}, produces = XML)
    public ResponseEntity<byte[]> statusBatch(HttpServletRequest request) throws IOException {
        if (!bulkClaimsEnabled()) {
            return notFound();
        }
        byte[] body = readBounded(request);
        if (body == null) {
            return payloadTooLarge();
        }
        return handler.statusBatch(body, organisationId(request));
    }

    @Operation(
        summary = "Get a bulk claim XML schema file",
        responses = {
            @ApiResponse(responseCode = "200", description = "The schema file",
                content = @Content(mediaType = XML, schema = @Schema(type = "string", format = "xml"))),
            @ApiResponse(responseCode = "404", description = "No such schema file", content = @Content)
        })
    @GetMapping(value = "/schema/{fileName}")
    public ResponseEntity<byte[]> schema(@PathVariable String fileName) {
        if (!bulkClaimsEnabled()) {
            return notFound();
        }
        return handler.schema(fileName);
    }

    private boolean bulkClaimsEnabled() {
        return featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2);
    }

    /**
     * Reads the request body, never holding more than the configured maximum. Returns null when the declared
     * or actual size is over the limit.
     */
    private byte[] readBounded(HttpServletRequest request) throws IOException {
        long maxBytes = properties.getMaxRequestSize().toBytes();
        if (request.getContentLengthLong() > maxBytes) {
            return null;
        }
        try (InputStream in = request.getInputStream()) {
            byte[] body = in.readNBytes((int) Math.min(maxBytes + 1, Integer.MAX_VALUE - 8L));
            return body.length > maxBytes ? null : body;
        }
    }

    private static String organisationId(HttpServletRequest request) {
        return (String) request.getAttribute(BulkClaimApimS2sFilter.ORGANISATION_ID_ATTRIBUTE);
    }

    private static ResponseEntity<byte[]> notFound() {
        return ResponseEntity.notFound().build();
    }

    private static ResponseEntity<byte[]> payloadTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    }
}
