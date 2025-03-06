package uk.gov.hmcts.reform.pcs.hearings.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;

public class HearingsJMSConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingsJMSConfiguration.class);

    @JmsListener(destination = "${spring.jms.servicebus.hearings-topic}",
        containerFactory = "topicJmsListenerContainerFactory",
        subscription = "${spring.jms.servicebus.hearings-subscription}")
    public void receiveMessage(String message) {

        LOGGER.info("Message received: {}", message);
    }

}
