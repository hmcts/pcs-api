package uk.gov.hmcts.reform.pcs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import org.springframework.jms.annotation.EnableJms;
import uk.gov.hmcts.reform.pcs.config.JpaPropertiesConfig;
import uk.gov.hmcts.reform.pcs.hearings.service.api.HmcHearingApi;

@SpringBootApplication(
    scanBasePackages = {
        "uk.gov.hmcts.reform.pcs",
        "uk.gov.hmcts.ccd.sdk"
    })
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
@EnableFeignClients(
    clients = {
        HmcHearingApi.class,
        IdamApi.class
    }
)
@EnableJms
@EnableJpaRepositories(basePackages = "uk.gov.hmcts.reform.pcs")
@EnableConfigurationProperties(JpaPropertiesConfig.class)
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
