package uk.gov.hmcts.reform.pcs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import org.springframework.jms.annotation.EnableJms;
import uk.gov.hmcts.reform.pcs.hearings.service.api.HmcHearingApi;

@SpringBootApplication(
    scanBasePackages = {
        "uk.gov.hmcts.reform.pcs",
        "uk.gov.hmcts.ccd.sdk",
        "uk.gov.hmcts.reform.pcs.hearings",
    })
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
@EnableFeignClients(
    clients = {
        HmcHearingApi.class,
        IdamApi.class
    }
)
@EnableJms
public class Application {

    public static void main(final String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Application is shutting down, cleaning up JobRunr resources...");
            if (context.isActive()) {
                try {
                    if (context.containsBean("jobScheduler")) {
                        Object jobScheduler = context.getBean("jobScheduler");
                        if (jobScheduler instanceof AutoCloseable) {
                            ((AutoCloseable) jobScheduler).close();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error shutting down JobRunr: " + e.getMessage());
                }
            }
        }));
    }
}
