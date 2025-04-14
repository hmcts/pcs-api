package uk.gov.hmcts.reform.pcs.helloworld.service;

import org.jobrunr.jobs.annotations.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HelloWorldJobService {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldJobService.class);
    private int attemptCount = 0;

    @Job(name = "Hello World Job", retries = 0)
    public void executeHelloWorldJob() {

        attemptCount++;

        logger.info("Hello World job is running! Attempt #{}", attemptCount);

        // Simulate a failure for the first three attempts
        if (attemptCount <= 2) {
            throw new RuntimeException("Simulated failure in Hello World job - Attempt #" + attemptCount);
        }

        logger.info("Hello World job completed!");
        attemptCount = 0;
    }
}
