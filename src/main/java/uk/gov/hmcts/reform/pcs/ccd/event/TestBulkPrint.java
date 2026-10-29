package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.testcasesupport.TestBulkPrintDocumentsPage;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.testingsupport.bulkprint.TestBulkPrintTaskComponent;
import uk.gov.hmcts.reform.pcs.testingsupport.bulkprint.TestBulkPrintTaskData;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.testBulkPrint;

/**
 * Non-production caseworker event that posts uploaded documents to the claimant through bulk print, so testers can
 * check what each file type looks like once converted to the print standard. The letter is sent by a background
 * task, after CCD has attached the uploads to the case in CDAM.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TestBulkPrint implements CCDConfig<PCSCase, State, UserRole> {

    // CCD caps event names at 30 characters and descriptions at 65.
    static final String EVENT_NAME = "Test Support: Bulk Print";

    private final SchedulerClient schedulerClient;
    private final DocumentIdExtractor documentIdExtractor;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        if ("preview".equalsIgnoreCase(System.getenv().get("ENVIRONMENT"))
            || Boolean.parseBoolean(System.getenv().get("ENABLE_TESTING_SUPPORT"))) {
            log.info("Test support enabled, configuring event: {}", EVENT_NAME);
            configure(configBuilder);
        }
    }

    void configure(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(testBulkPrint.name(), this::submit)
            .forAllStates()
            .name(EVENT_NAME)
            .description("Post uploaded documents to the claimant by bulk print")
            .showSummary()
            // Posting to a party is a caseworker action.
            .grant(Permission.CRU, CASEWORKER_ROLES);
        new PageBuilder(eventBuilder).add(new TestBulkPrintDocumentsPage());
    }

    SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        List<TestBulkPrintTaskData.Upload> uploads = Optional.ofNullable(
                eventPayload.caseData().getTestBulkPrintDocuments()).orElse(List.of()).stream()
            .map(ListValue::getValue)
            .map(this::toUpload)
            .toList();
        UUID taskId = UUID.randomUUID();
        schedulerClient.scheduleIfNotExists(TestBulkPrintTaskComponent.TEST_BULK_PRINT_TASK
            .instance(taskId.toString())
            .data(new TestBulkPrintTaskData(caseReference, uploads))
            .scheduledTo(Instant.now()));
        log.info("Test bulk print of {} document(s) scheduled for case {} as task {}",
            uploads.size(), caseReference, taskId);
        return SubmitResponse.<State>builder()
            .confirmationHeader("# Letter scheduled")
            .confirmationBody(uploads.size() + " document(s) will be converted and posted to the claimant shortly.")
            .build();
    }

    private TestBulkPrintTaskData.Upload toUpload(Document document) {
        return new TestBulkPrintTaskData.Upload(
            documentIdExtractor.extractDocumentId(document.getUrl()), document.getFilename());
    }

}
