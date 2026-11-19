package uk.gov.hmcts.reform.pcs.bulkclaim.endpoint;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.security.BulkClaimApimS2sFilter;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimV2Handler;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

/**
 * Hidden from the published OpenAPI until the handler is implemented, so a 501-only API is not published.
 */
@Hidden
@RestController
@RequestMapping("/v2/bulk-claims")
@RequiredArgsConstructor
public class BulkClaimV2Controller {

    private static final String XML = MediaType.APPLICATION_XML_VALUE;
    private static final String TEXT_XML = MediaType.TEXT_XML_VALUE;

    private final FeatureToggleService featureToggleService;
    private final BulkClaimProperties properties;
    private final BulkClaimV2Handler handler;

    @PostMapping(value = "/rental", consumes = {XML, TEXT_XML}, produces = XML)
    public ResponseEntity<byte[]> submitRental(HttpServletRequest request,
                                               @RequestBody byte[] body,
                                               @RequestParam(defaultValue = "false") boolean validateOnly) {
        if (!bulkClaimsEnabled()) {
            return notFound();
        }
        if (tooLarge(request, body)) {
            return payloadTooLarge();
        }
        return handler.submitRental(body, organisationId(request), validateOnly);
    }

    @PostMapping(value = "/mortgage", consumes = {XML, TEXT_XML}, produces = XML)
    public ResponseEntity<byte[]> submitMortgage(HttpServletRequest request,
                                                 @RequestBody byte[] body,
                                                 @RequestParam(defaultValue = "false") boolean validateOnly) {
        if (!bulkClaimsEnabled() || !featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2_MORTGAGE)) {
            return notFound();
        }
        if (tooLarge(request, body)) {
            return payloadTooLarge();
        }
        return handler.submitMortgage(body, organisationId(request), validateOnly);
    }

    @GetMapping(value = "/claims/{customerUniqueIdentifier}", produces = XML)
    public ResponseEntity<byte[]> status(HttpServletRequest request,
                                         @PathVariable String customerUniqueIdentifier) {
        if (!bulkClaimsEnabled()) {
            return notFound();
        }
        return handler.status(organisationId(request), customerUniqueIdentifier);
    }

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

    private boolean tooLarge(HttpServletRequest request, byte[] body) {
        long maxBytes = properties.getMaxRequestSize().toBytes();
        return request.getContentLengthLong() > maxBytes || body.length > maxBytes;
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
