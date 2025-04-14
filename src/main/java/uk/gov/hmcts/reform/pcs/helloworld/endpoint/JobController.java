package uk.gov.hmcts.reform.pcs.helloworld.endpoint;

import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.helloworld.service.HelloWorldJobService;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobScheduler jobScheduler;
    private final HelloWorldJobService helloWorldJobService;

    @Autowired
    public JobController(JobScheduler jobScheduler, HelloWorldJobService helloWorldJobService) {
        this.jobScheduler = jobScheduler;
        this.helloWorldJobService = helloWorldJobService;
    }

    @GetMapping("/enqueue")
    public ResponseEntity<String> enqueueJob() {
        jobScheduler.enqueue(helloWorldJobService::executeHelloWorldJob);
        return ResponseEntity.ok("Hello World job enqueued successfully!");
    }

    @GetMapping("/schedule")
    public ResponseEntity<String> scheduleJob() {
        jobScheduler.schedule(
            LocalDateTime.now().plus(Duration.ofSeconds(5)),
            helloWorldJobService::executeHelloWorldJob
        );
        return ResponseEntity.ok("Hello World job scheduled to run in seconds!");
    }
}
