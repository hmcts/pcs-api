package uk.gov.hmcts.reform.pcs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.fees.client.FeesApi;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.pcs.am.RoleAssignmentApi;
import uk.gov.hmcts.reform.pcs.camunda.TaskManagementApi;
import uk.gov.hmcts.reform.pcs.camunda.WorkAllocationWorkflowApi;
import uk.gov.hmcts.reform.pcs.hearings.service.api.HmcHearingApi;
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;
import uk.gov.hmcts.reform.payments.client.PaymentsApi;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;
import uk.gov.hmcts.reform.sendletter.api.proxy.SendLetterApiProxy;

/**
 * We don't scan the HMCTS ccd, payments and send-letter client packages or load their auto-configurations:
 * the payments and send-letter ones register Spring Boot 3 health checks that Spring Boot 4 can't load, and
 * the ccd one would register its Feign clients a second time. Instead we register the Feign clients and
 * PaymentsClient we need ourselves, and the health checks live in DownstreamHealthConfiguration.
 *
 * <p>We only use idam-legacy-auth-support for its password grant (see OAuth2ClientConfig), so its
 * auto-configurations are turned off. Our code doesn't call IdamApi, but the CCD SDK's IdamClient needs it.
 */
@SpringBootApplication(
    scanBasePackages = {
        "uk.gov.hmcts.reform.docassembly",
        "uk.gov.hmcts.reform.pcs",
        "uk.gov.hmcts.ccd.sdk"
    },
    excludeName = {
        "uk.gov.hmcts.reform.ccd.client.CoreCaseDataClientAutoConfiguration",
        "uk.gov.hmcts.reform.sendletter.SendLetterAutoConfiguration",
        "cft.rpe.legacy.s2s.support.config.RpeS2SAutoConfiguration",
        "cft.idam.legacy.auth.support.config.DefaultPasswordGrantAutoConfiguration"
    }
)
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, it's not a utility class
@EnableFeignClients(
    clients = {
        HmcHearingApi.class,
        LocationReferenceApi.class,
        IdamUserInfoApi.class,
        IdamApi.class,
        RdProfessionalApi.class,
        FeesApi.class,
        CoreCaseDataApi.class,
        CaseAssignmentApi.class,
        PaymentsApi.class,
        SendLetterApiProxy.class,
        CaseDocumentClientApi.class,
        WorkAllocationWorkflowApi.class,
        TaskManagementApi.class,
        RoleAssignmentApi.class
    }
)

@Import(Application.PaymentsClientConfiguration.class)
@EnableScheduling
public class Application {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Import(PaymentsClient.class)
    static class PaymentsClientConfiguration {
    }

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
