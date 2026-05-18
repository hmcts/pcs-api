package uk.gov.hmcts.reform.pcs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.fees.client.FeesApi;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.pcs.hearings.service.api.HmcHearingApi;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;

@SpringBootApplication(
    scanBasePackages = {
        "uk.gov.hmcts.reform.docassembly",
        "uk.gov.hmcts.reform.pcs",
        "uk.gov.hmcts.ccd.sdk",
        "uk.gov.hmcts.reform.payments.client",
        "uk.gov.hmcts.reform.ccd.client"
    })
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, it's not a utility class
@EnableFeignClients(
    clients = {
        HmcHearingApi.class,
        LocationReferenceApi.class,
        // Required for ccd-sdk's CcdSdkIdamService → IdamClient → IdamApi chain.
        // pcs-api code does not use IdamApi directly (token-fetch uses Spring OAuth2,
        // user-info uses JwtDecoder). Cannot remove until ccd-sdk drops idam-java-client.
        IdamApi.class,
        RdProfessionalApi.class,
        FeesApi.class,
        CaseDocumentClientApi.class
    }
)
@EnableJms
@EnableScheduling
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
