package uk.gov.hmcts.reform.pcs.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.Application;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSchedulingConfig.class)
@ActiveProfiles("integration")
class SchedulingConfigIT extends AbstractPostgresContainerIT {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private CountDownLatch countDownLatch;
    @Autowired
    private Scheduler testScheduler;

    @BeforeEach
    void setup() throws Exception {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.execute("DELETE FROM scheduled_tasks");
        testScheduler.start();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Rollback(false)
    void testRecurringTasks() throws InterruptedException {
        Thread.sleep(10000);
        assertThat(countDownLatch.await(20, TimeUnit.SECONDS)).isTrue();
    }

    @AfterEach
    void cleanup() {
        log.info("Stopping test scheduler");
        if (testScheduler != null) {
            testScheduler.stop();
        }
    }

}
