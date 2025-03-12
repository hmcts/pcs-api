package uk.gov.hmcts.reform.pcs;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import org.springframework.jms.annotation.EnableJms;
import uk.gov.hmcts.reform.pcs.hearings.service.api.HmcHearingApi;

@SpringBootApplication(
    scanBasePackages = {
        "uk.gov.hmcts.reform.pcs",
        "uk.gov.hmcts.ccd.sdk",
        "uk.gov.hmcts.reform.pcs.hearings",
    })
@EnableFeignClients(
    clients = {
        HmcHearingApi.class,
        IdamApi.class
    }
)
@EnableJms
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
