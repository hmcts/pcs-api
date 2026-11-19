package uk.gov.hmcts.reform.pcs.health;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "send-letter-health-api", url = "${send-letter.url}")
public interface SendLetterHealthApi {

    @GetMapping("/health")
    DownstreamHealth health();
}
