package uk.gov.hmcts.reform.pcs.payment.fee.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.payment.fee.entity.Fee;
import uk.gov.hmcts.reform.pcs.payment.fee.service.FeeService;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class FeeController {

    private final FeeService feeService;

    public FeeController(FeeService feeService) {
        this.feeService = feeService;
    }

    @GetMapping(value = "/fee-lookup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Fee> getFee(
        @RequestHeader(value = AUTHORIZATION, defaultValue = "DummyId") String authorisation,
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization
    ) {

        log.info("Received request to retrieve fee");

        try {
            Fee fee = feeService.getFeeWithoutHearing();

            log.info("Fee retrieved successfully: code={}, amount={}", fee.getCode(), fee.getCalculatedAmount());
            return ResponseEntity.ok().body(fee);

        } catch (Exception e) {
            log.error("Failed to retrieve fee: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
