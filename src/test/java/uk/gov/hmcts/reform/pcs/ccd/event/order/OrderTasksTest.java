package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskCompletionService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.config.PebbleConfiguration;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTasksTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID ORDER_ID = UUID.fromString("0b6c1a52-5e43-4a7f-9d3b-1f2a3c4d5e6f");
    private static final UUID JUDGE_ID = UUID.fromString("5d2e8f10-7a6b-4c3d-9e1f-a2b3c4d5e6f7");

    @Mock
    private CamundaService camundaService;
    @Mock
    private TaskCompletionService taskCompletionService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private PartyService partyService;
    @Mock
    private ClaimRepository claimRepository;
    @Captor
    private ArgumentCaptor<String> description;

    private OrderTasks underTest;
    private DraftOrderEntity order;

    @BeforeEach
    void setUp() {
        TaskDescriptionService taskDescriptionService = new TaskDescriptionService(
            partyService, new PebbleConfiguration().pebbleEngine(), claimRepository);
        underTest = new OrderTasks(camundaService, taskCompletionService, taskDescriptionService,
                                   securityContextService, "https://pcs.example");
        order = DraftOrderEntity.builder().id(ORDER_ID).orderType("OUTRIGHT_POSSESSION").authorIdamUserId(JUDGE_ID)
            .build();
    }

    @Test
    void asksStaffToReviewTheOrderTheJudgeSubmitted() {
        signedIn("Sarah Hughes");

        underTest.askStaffToReview(CASE_REFERENCE, order, false);

        verify(camundaService).createTask(eq(CASE_REFERENCE), eq(TaskType.CONFIRM_ORDER_REVIEW),
                                          description.capture(), eq(ORDER_ID));
        assertThat(description.getValue())
            .contains("Sarah Hughes has submitted an order for review.")
            .contains("<a href=\"https://pcs.example/cases/1234/event/ext:confirmOrderReview"
                          + "?expected_sub=${[EXPECTED_SUB]}&amp;orderId=" + ORDER_ID + "\">"
                          + "Review the outright possession order</a>");
    }

    @Test
    void saysWhenAReturnedOrderIsSubmittedAgain() {
        signedIn("Sarah Hughes");

        underTest.askStaffToReview(CASE_REFERENCE, order, true);

        verify(camundaService).createTask(eq(CASE_REFERENCE), eq(TaskType.CONFIRM_ORDER_REVIEW),
                                          description.capture(), eq(ORDER_ID));
        assertThat(description.getValue())
            .contains("Sarah Hughes has changed an order that was returned to them, "
                          + "and submitted it for review again.");
    }

    @Test
    void escapesTheJudgesName() {
        signedIn("<b>Judge</b>");

        underTest.askStaffToReview(CASE_REFERENCE, order, false);

        verify(camundaService).createTask(eq(CASE_REFERENCE), eq(TaskType.CONFIRM_ORDER_REVIEW),
                                          description.capture(), eq(ORDER_ID));
        assertThat(description.getValue()).contains("&lt;b&gt;Judge&lt;/b&gt; has submitted").doesNotContain("<b>");
    }

    @Test
    void completesTheStaffTaskForTheReviewedOrderOnlyFallingBackToCancellingIt() {
        underTest.closeStaffReview(CASE_REFERENCE, order);

        verify(camundaService).cancelTask(CASE_REFERENCE, TaskType.CONFIRM_ORDER_REVIEW, ORDER_ID,
                                          OrderTasks.FALLBACK_DELAY);
        verify(taskCompletionService).completeOrderTasks(CASE_REFERENCE, TaskType.CONFIRM_ORDER_REVIEW, ORDER_ID);
    }

    @Test
    void completesTheTaskOnlyOnceTheEventIsSaved() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            underTest.closeStaffReview(CASE_REFERENCE, order);

            verify(camundaService).cancelTask(CASE_REFERENCE, TaskType.CONFIRM_ORDER_REVIEW, ORDER_ID,
                                              OrderTasks.FALLBACK_DELAY);
            verifyNoInteractions(taskCompletionService);

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(taskCompletionService).completeOrderTasks(CASE_REFERENCE, TaskType.CONFIRM_ORDER_REVIEW,
                                                             ORDER_ID);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void givesTheJudgeWhoWroteTheOrderTheQueryTheirOrderWasReturnedWith() {
        signedIn("Tom Baker");

        underTest.askJudgeToChange(CASE_REFERENCE, order, "Which defendant does paragraph 2 mean?");

        verify(camundaService).createTask(eq(CASE_REFERENCE), eq(TaskType.REVIEW_ORDER_QUERY),
                                          description.capture(), eq(ORDER_ID), eq(JUDGE_ID));
        assertThat(description.getValue())
            .contains("Tom Baker has returned your outright possession order with a query:")
            .contains("<div class=\"govuk-inset-text\">Which defendant does paragraph 2 mean?</div>")
            .contains("<a href=\"https://pcs.example/cases/1234/event/ext:makeOrder"
                          + "?expected_sub=${[EXPECTED_SUB]}&amp;orderId=" + ORDER_ID + "\">Change the order</a>");
    }

    @Test
    void escapesTheQuery() {
        signedIn("Tom Baker");

        underTest.askJudgeToChange(CASE_REFERENCE, order, "<script>alert</script>");

        verify(camundaService).createTask(eq(CASE_REFERENCE), eq(TaskType.REVIEW_ORDER_QUERY),
                                          description.capture(), eq(ORDER_ID), eq(JUDGE_ID));
        assertThat(description.getValue()).contains("&lt;script&gt;").doesNotContain("<script>");
    }

    @Test
    void completesTheJudgesQueryTaskForTheOrderOnlyFallingBackToCancellingIt() {
        underTest.closeJudgeQuery(CASE_REFERENCE, order);

        verify(camundaService).cancelTask(CASE_REFERENCE, TaskType.REVIEW_ORDER_QUERY, ORDER_ID,
                                          OrderTasks.FALLBACK_DELAY);
        verify(taskCompletionService).completeOrderTasks(CASE_REFERENCE, TaskType.REVIEW_ORDER_QUERY, ORDER_ID);
    }

    private void signedIn(String name) {
        UserInfo judge = new UserInfo();
        judge.setName(name);
        when(securityContextService.getCurrentUserDetails()).thenReturn(judge);
    }
}
