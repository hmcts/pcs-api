package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Send Letter's letter status endpoint, which the send-letter-client library does not expose.
 */
@FeignClient(name = "pcs-send-letter-status", url = "${send-letter.url}")
public interface SendLetterStatusApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @GetMapping("/letters/{id}")
    LetterStatusResponse getLetterStatus(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
                                         @PathVariable("id") UUID letterId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LetterStatusResponse(UUID id,
                                String status,
                                @JsonProperty("sent_to_print_at") OffsetDateTime sentToPrintAt,
                                @JsonProperty("printed_at") OffsetDateTime printedAt) {
    }
}
