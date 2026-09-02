package uk.gov.hmcts.reform.pcs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.noc.service.NoticeOfChangeAppliedEventService;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Records the noticeOfChangeApplied event from a plain background thread, the way the NoC
 * db-scheduler task does - no web request, no open persistence session. Regression test for the
 * LazyInitializationException seen when the recording ran outside a transaction.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NoticeOfChangeAppliedEventTest extends CftlibTest {

    @Autowired
    private CaseCreationService caseCreationService;
    @Autowired
    private NoticeOfChangeAppliedEventService underTest;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private IdamClient idamClient;
    @Autowired
    @Qualifier("systemUpdateUserTokenProvider")
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Autowired
    private IdamAuthenticator idamAuthenticator;

    @Test
    void recordsTheEventFromABackgroundThread() throws Exception {
        long caseReference = caseCreationService.createMinimalCase(
            idamClient.getAccessToken("pcs-solicitor1@test.com", "password"));

        NocAccessChangeTaskData taskData = NocAccessChangeTaskData.builder()
            .caseReference(String.valueOf(caseReference))
            .userId("40460563-4f42-479f-995f-4dc77399ade1")
            .email("james-solicitor-user1@test.com")
            .firstName("James")
            .lastName("Solicitor")
            .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> underTest.submit(caseReference, taskData)).get();
        } finally {
            executor.shutdown();
        }

        var event = jdbcTemplate.queryForMap("""
            select ce.event_id, ce.summary, ce.user_id, ce.proxied_by
            from ccd.case_event ce
            join ccd.case_data cd on cd.id = ce.case_data_id
            where cd.reference = ? order by ce.id desc limit 1
            """, caseReference);

        assertThat(event.get("event_id")).isEqualTo("noticeOfChangeApplied");
        assertThat(event.get("summary")).isEqualTo("Notice of change by james-solicitor-user1@test.com");
        assertThat(event.get("user_id")).isEqualTo("40460563-4f42-479f-995f-4dc77399ade1");
        assertThat(event.get("proxied_by")).isEqualTo(systemUserId());
    }

    private String systemUserId() {
        return idamAuthenticator.validateAuthToken(systemUpdateUserTokenProvider.getAuthToken())
            .getUserDetails().getUid();
    }
}
