package uk.gov.hmcts.reform.pcs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationResponse;
import uk.gov.hmcts.reform.pcs.notify.model.SendEmail;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class StartupConfig {

    @Autowired
    private NotificationService notificationService;

    @Value("${notify.recipient}")
    private String emailRecipient;

    @Value("${notify.templateId}")
    private String templateId;

    @Bean
    public CommandLineRunner sendStartupEmail() {
        return args -> {
            Map<String, Object> personalisation = new HashMap<>();

            SendEmail emailRequest = new SendEmail();
            emailRequest.setEmailAddress(emailRecipient);
            emailRequest.setTemplateId(templateId);
            emailRequest.setPersonalisation(personalisation);

            NotificationResponse notificationResponse = notificationService.sendEmail(emailRequest);
            System.out.println("Email sent successfully. Notification ID: " + notificationResponse);
        };
    }
}
