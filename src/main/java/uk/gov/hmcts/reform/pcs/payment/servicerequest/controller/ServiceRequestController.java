package uk.gov.hmcts.reform.pcs.payment.servicerequest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.payment.fee.entity.Fee;
import uk.gov.hmcts.reform.pcs.payment.servicerequest.model.ServiceRequestResponse;
import uk.gov.hmcts.reform.pcs.payment.servicerequest.service.ServiceRequestService;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    @Operation(summary = "Create a service request", description = "Creates a service request for payment")
    @PostMapping(
        value = "/create-service-request",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ServiceRequestResponse> createServiceRequest(
        @Parameter(
            name = "Authorization",
            description = "Bearer token for user authorization",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer AAAAAAAA")
        )
        @RequestHeader(value = AUTHORIZATION) String authorisation,

        @Parameter(
            name = "ServiceAuthorization",
            description = "S2S token for service authorization",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer s2s-token-here")
        )
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,

        @Parameter(
            name = "caseReference",
            description = "Case reference number",
            required = true,
            schema = @Schema(type = "string", example = "REF123")
        )
        @RequestParam(name = "caseReference") String caseReference,

        @Parameter(
            name = "ccdCaseNumber",
            description = "CCD case number",
            required = true,
            schema = @Schema(type = "string", example = "1755784629578000")
        )
        @RequestParam(name = "ccdCaseNumber") String ccdCaseNumber,

        @RequestBody Fee fee
    ) {
        log.info("Auth value: {}", authorisation);
        log.info("Received request to create service request for case: {} with provided fee: {}",
                    ccdCaseNumber, fee.getCode());

        if (fee.getCalculatedAmount() == null) {
            log.error("Fee calculatedAmount is NULL in received object!");
        }


        try {
            ServiceRequestResponse response = serviceRequestService.createServiceRequest(
                authorisation,
                serviceAuthorization,
                caseReference,
                ccdCaseNumber,
                fee
            );

            log.info("Service request created successfully: {}", response.getServiceRequestReference());
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            log.error("Failed to create service request for case {}: {}", ccdCaseNumber, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
