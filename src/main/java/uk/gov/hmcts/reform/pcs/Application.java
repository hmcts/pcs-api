package uk.gov.hmcts.reform.pcs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.jms.annotation.EnableJms;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyApi;
import uk.gov.hmcts.reform.pcs.hearings.service.api.HmcHearingApi;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;

@SpringBootApplication(
    scanBasePackages = {
        "uk.gov.hmcts.reform.pcs",
        "uk.gov.hmcts.ccd.sdk",
        "uk.gov.hmcts.reform.ccd.client"
    })
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
@EnableFeignClients(
    clients = {
        HmcHearingApi.class,
        LocationReferenceApi.class,
        IdamApi.class,
        DocAssemblyApi.class,
    }
)
@EnableJms
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
