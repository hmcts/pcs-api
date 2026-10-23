package uk.gov.hmcts.reform.pcs.ccd.event.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.UnexpectedResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageAction;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageRecipient;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.OrderEventIT;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.PENDING_GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.ADJOURN;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.sendReplyMessage;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;

/**
 * Court staff and judges message each other on a case: a message gives the recipient role's pool a task, a reply
 * gives the other side one, and closing it without a reply tells the other side with a task. The Messages tab
 * shows each thread.
 */
@DisplayName("Send and reply to messages")
class SendReplyMessageIT extends OrderEventIT {

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;

    private long caseReference;
    private Actor caseworker;
    private Actor judge;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        cases.update(caseReference, pcsCase -> {
            PartyEntity defendant = pcsCase.getParties().stream()
                .filter(party -> party.getOrgName() == null).findFirst().orElseThrow();
            pcsCase.addGenApp(GenAppEntity.builder().party(defendant).type(ADJOURN).state(GEN_APP_ISSUED)
                .whatOrderWanted("The hearing is adjourned.").build());
        });
        caseworker = events.registerActor("Tom", "Caseworker", "caseworker-pcs");
        judge = events.registerActor("Sarah", "Hughes", "caseworker-pcs", "judiciary");
    }

    @Test
    @DisplayName("sends an urgent message about an application to the judges, whose task links to deciding it and to "
        + "replying, which closes the task")
    void sendsAMessageAboutAnApplication() {
        UUID genAppId = send();

        verify(camundaService).createTask(eq(caseReference), eq(TaskType.REVIEW_MESSAGE_JUDGE),
            and(contains("ext:makeOrder?expected_sub=${[EXPECTED_SUB]}&genAppId=" + genAppId),
                contains("/cases/case-details/%d/trigger/sendReplyMessage?tid=${[id]}".formatted(caseReference))),
            eq(true), any(UUID.class));
        // XUI keeps a task's details for its event only up to their first semicolon.
        verify(camundaService).createTask(eq(caseReference), eq(TaskType.REVIEW_MESSAGE_JUDGE),
            argThat(description -> !description.contains(";")), eq(true), any(UUID.class));
        String tab = events.view(caseReference, caseworker).getMessagesMarkdown();
        assertThat(tab).contains("Decide GA1", "Urgent", "Tom Caseworker", "District Judge",
            "Please decide this before the hearing on Friday.", "GA1: Adjourn the hearing");
        // Only a judge can decide the application, so only a judge is offered the link to.
        assertThat(tab).doesNotContain("Decide the application");
        assertThat(events.view(caseReference, judge).getMessagesMarkdown())
            .contains("Decide the application", "make-order?genAppId=" + genAppId);
    }

    @Test
    @DisplayName("lets the judge reply, giving court staff a task, then court staff close it, giving the judges one")
    void repliesThenCloses() {
        send();
        clearInvocations(camundaService);

        assertThat(replyList(caseworker)).isEmpty();
        List<DynamicListElement> forJudge = replyList(judge);
        assertThat(forJudge).extracting(DynamicListElement::getLabel).singleElement().asString()
            .startsWith("Decide GA1, from Tom Caseworker");
        events.start(caseReference, sendReplyMessage.name()).as(judge).startExpectingSuccess()
            .edit(pcsCase -> {
                pcsCase.setMessageAction(MessageAction.REPLY);
                pcsCase.setMessageReplyList(chosen(forJudge));
                pcsCase.setMessageNeedReply(VerticalYesNo.YES);
                pcsCase.setMessageReplyRecipient(MessageRecipient.COURT_STAFF);
                pcsCase.setMessageReplyUrgent(VerticalYesNo.NO);
                pcsCase.setMessageReply("Please list it for a hearing on notice.");
            })
            .as(judge).submitExpectingSuccess();
        verify(camundaService).createTask(eq(caseReference), eq(TaskType.REVIEW_MESSAGE_STAFF),
            contains("Sarah Hughes sent a message"), eq(false), any(UUID.class));
        // The judge answered without their task, which XUI leaves open, so it goes.
        UUID messageId = forJudge.getFirst().getCode();
        verify(camundaService).cancelMessageTask(caseReference, messageId, TaskType.REVIEW_MESSAGE_JUDGE);
        clearInvocations(camundaService);

        assertThat(replyList(judge)).isEmpty();
        List<DynamicListElement> forStaff = replyList(caseworker);
        events.start(caseReference, sendReplyMessage.name()).as(caseworker).startExpectingSuccess()
            .edit(pcsCase -> {
                pcsCase.setMessageAction(MessageAction.REPLY);
                pcsCase.setMessageReplyList(chosen(forStaff));
                pcsCase.setMessageNeedReply(VerticalYesNo.NO);
                pcsCase.setMessageClosureNote("Listed.");
            })
            .as(caseworker).submitExpectingSuccess();

        verify(camundaService).cancelMessageTask(caseReference, messageId, TaskType.REVIEW_MESSAGE_STAFF);
        verify(camundaService).createTask(eq(caseReference), eq(TaskType.REVIEW_MESSAGE_JUDGE),
            contains("Tom Caseworker closed the message without replying: Decide GA1. Their note: Listed."),
            eq(false), isNull());
        assertThat(replyList(caseworker)).isEmpty();
        String tab = events.view(caseReference, caseworker).getMessagesMarkdown();
        assertThat(tab).contains("Closed messages", "Please list it for a hearing on notice.",
            "Closed by Tom Caseworker: Listed.");
    }

    @Test
    @DisplayName("started from a message's task, starts on that message, and asks about it again if the user "
        + "answers another or sends a new one, as XUI completes the task whatever they do")
    void keepsTheTaskToItsMessage() {
        send();
        send();
        List<DynamicListElement> both = replyList(judge);
        UUID tasks = both.getFirst().getCode();
        Map<String, Object> fromTask = Map.of("client_context", Map.of("user_task", Map.of("task_data",
            Map.of("additional_properties", Map.of("messageId", tasks.toString())))));

        var started = events.start(caseReference, sendReplyMessage.name()).as(judge).withClientContext(fromTask)
            .startExpectingSuccess();
        assertThat(started.caseData().getMessageReplyList().getListItems()).hasSize(2);
        assertThat(started.caseData().getMessageReplyList().getValueCode()).isEqualTo(tasks);
        assertThat(started.caseData().getMessageAction()).isEqualTo(MessageAction.REPLY);

        clearInvocations(camundaService);
        started.edit(pcsCase -> {
            pcsCase.setMessageReplyList(DynamicList.builder().value(both.getLast()).listItems(both).build());
            pcsCase.setMessageNeedReply(VerticalYesNo.NO);
        }).as(judge).submitExpectingSuccess();
        verify(camundaService).createTask(eq(caseReference), eq(TaskType.REVIEW_MESSAGE_JUDGE), anyString(),
            eq(true), eq(tasks));

        // Answered from its task, the task is XUI's to close.
        clearInvocations(camundaService);
        events.start(caseReference, sendReplyMessage.name()).as(judge).withClientContext(fromTask)
            .startExpectingSuccess()
            .edit(pcsCase -> pcsCase.setMessageNeedReply(VerticalYesNo.NO))
            .as(judge).submitExpectingSuccess();
        verify(camundaService, never()).cancelMessageTask(anyLong(), any(), any());
        verify(camundaService, never())
            .createTask(anyLong(), eq(TaskType.REVIEW_MESSAGE_JUDGE), anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("leaves the task of a message answered to the same side, whose new task it could otherwise cancel")
    void keepsTheTaskOfAReplyToTheSameSide() {
        send();
        List<DynamicListElement> forJudge = replyList(judge);

        events.start(caseReference, sendReplyMessage.name()).as(judge).startExpectingSuccess()
            .edit(pcsCase -> {
                pcsCase.setMessageAction(MessageAction.REPLY);
                pcsCase.setMessageReplyList(chosen(forJudge));
                pcsCase.setMessageNeedReply(VerticalYesNo.YES);
                pcsCase.setMessageReplyRecipient(MessageRecipient.CIRCUIT_JUDGE);
                pcsCase.setMessageReplyUrgent(VerticalYesNo.NO);
                pcsCase.setMessageReply("Over to you.");
            })
            .as(judge).submitExpectingSuccess();

        verify(camundaService, never()).cancelMessageTask(anyLong(), any(), any());
    }

    @Test
    @DisplayName("ignores a task context XUI kept from another case's message, so the user can send a new message")
    void ignoresAnotherCasesTask() {
        send();
        Map<String, Object> staleTask = Map.of("client_context", Map.of("user_task", Map.of("task_data",
            Map.of("additional_properties", Map.of("messageId", UUID.randomUUID().toString())))));

        var started = events.start(caseReference, sendReplyMessage.name()).as(judge).withClientContext(staleTask)
            .startExpectingSuccess();

        assertThat(started.caseData().getMessageReplyList().getListItems()).hasSize(1);
        assertThat(started.caseData().getMessageTaskMessageId()).isNull();
    }

    @Test
    @DisplayName("closes the messages about an application that wait for a judge once a judge sends any order "
        + "deciding it for review, not only one from the message's link")
    void closesTheApplicationsMessagesWhenItsOrderIsSent() {
        UUID genAppId = send();
        UUID messageId = replyList(judge).getFirst().getCode();

        events.external(caseReference, MAKE_ORDER).as(judge).submitExpectingSuccess(new MakeOrderRequest(
            SUBMIT_FOR_REVIEW, new OrderChange(null, 0, OrderType.FREE_FORM,
                Map.of("app-" + genAppId + "-decision", "grant"), WORDING), null));

        assertThat(replyList(judge)).isEmpty();
        assertThat(events.view(caseReference, caseworker).getMessagesMarkdown())
            .contains("Closed messages", "Answered by an order on the application");
        verify(camundaService).cancelMessageTask(caseReference, messageId, TaskType.REVIEW_MESSAGE_JUDGE);
        // The message's task, opened after, offers nothing to reply to, so the judge marks it done.
        assertThat(events.start(caseReference, sendReplyMessage.name()).as(judge)
            .withClientContext(Map.of("client_context", Map.of("user_task", Map.of("task_data",
                Map.of("additional_properties", Map.of("messageId", messageId.toString()))))))
            .startExpectingSuccess().caseData().getMessageReplyList().getListItems()).isEmpty();
    }

    @Test
    @DisplayName("words a message's task with no semicolon, whatever its subject, so XUI can start its event")
    void keepsSemicolonsOutOfTheTask() {
        events.start(caseReference, sendReplyMessage.name()).as(caseworker).startExpectingSuccess()
            .edit(pcsCase -> {
                pcsCase.setMessageAction(MessageAction.SEND);
                pcsCase.setMessageCategories(Set.of(MessageCategory.OTHER));
                pcsCase.setMessageRecipient(MessageRecipient.DISTRICT_JUDGE);
                pcsCase.setMessageSubject("Tenant's rent; costs & \"arrears\" (GA1)");
                pcsCase.setMessageUrgent(VerticalYesNo.NO);
                pcsCase.setMessageBody("Body");
            })
            .as(caseworker).submitExpectingSuccess();

        verify(camundaService).createTask(eq(caseReference), eq(TaskType.REVIEW_MESSAGE_JUDGE),
            and(argThat(description -> !description.contains(";")),
                contains("Tenant’s rent, costs and ”arrears” [GA1]")),
            eq(false), any(UUID.class));
    }

    @Test
    @DisplayName("keeps the judge's message for court staff with their order on the Messages tab, to be answered by "
        + "reviewing the order, not as a message")
    void keepsTheOrdersStaffMessage() {
        events.external(caseReference, MAKE_ORDER).as(judge).submitExpectingSuccess(new MakeOrderRequest(
            SUBMIT_FOR_REVIEW, new OrderChange(null, 0, OrderType.FREE_FORM,
                Map.of("staff-message", List.of("yes"), "staff-message-text", "Serve this by post."), WORDING),
            null));

        assertThat(events.view(caseReference, caseworker).getMessagesMarkdown())
            .contains("Free form order", "Serve this by post.", "Court staff");
        assertThat(replyList(caseworker)).isEmpty();
    }

    @Test
    @DisplayName("with the make order flag off, refuses to start or submit the event and leaves the Messages tab "
        + "empty")
    void refusedWhenMakeOrderIsOff() {
        send();
        doReturn(false).when(featureToggleService).isEnabled(FeatureFlag.MAKE_ORDER);

        UnexpectedResponse refused = catchThrowableOfType(UnexpectedResponse.class,
            () -> events.start(caseReference, sendReplyMessage.name()).as(caseworker).start());
        assertThat(refused.status()).isEqualTo(403);
        PCSCase message = PCSCase.builder().messageAction(MessageAction.SEND)
            .messageCategories(Set.of(MessageCategory.OTHER)).messageRecipient(MessageRecipient.DISTRICT_JUDGE)
            .messageSubject("Another").messageUrgent(VerticalYesNo.NO).messageBody("Body").build();
        assertThat(events.event(caseReference, sendReplyMessage.name(), message).as(caseworker)
            .submitExpectingErrors().errors()).containsExactly(SendReplyMessage.NOT_AVAILABLE);
        assertThat(events.view(caseReference, caseworker).getMessagesMarkdown()).isNull();
    }

    @Test
    @DisplayName("sends a message about an application on a case with no open application, naming none")
    void sendsAboutAnApplicationWithNoneOpen() {
        cases.update(caseReference, pcsCase -> pcsCase.getGenApps()
            .forEach(genApp -> genApp.setState(PENDING_GEN_APP_ISSUED)));
        assertThat(events.start(caseReference, sendReplyMessage.name()).as(caseworker).startExpectingSuccess()
            .caseData().getMessageOpenGenApps()).isEqualTo(YesOrNo.NO);
        PCSCase message = PCSCase.builder().messageAction(MessageAction.SEND).messageOpenGenApps(YesOrNo.NO)
            .messageCategories(Set.of(MessageCategory.APPLICATION)).messageRecipient(MessageRecipient.DISTRICT_JUDGE)
            .messageSubject("New application").messageUrgent(VerticalYesNo.NO)
            .messageBody("One arrived today, not yet entered.").build();

        events.event(caseReference, sendReplyMessage.name(), message).as(caseworker).submitExpectingSuccess();

        assertThat(events.view(caseReference, caseworker).getMessagesMarkdown()).contains("New application");
    }

    @Test
    @DisplayName("shows a message on the Messages tab as typed, not as markdown or with the case's fields in it")
    void showsAMessageAsTyped() {
        PCSCase message = PCSCase.builder().messageAction(MessageAction.SEND)
            .messageCategories(Set.of(MessageCategory.OTHER)).messageRecipient(MessageRecipient.DISTRICT_JUDGE)
            .messageSubject("*Re* ${caseTitleMarkdown}").messageUrgent(VerticalYesNo.NO)
            .messageBody("They ask for:\n\n1. more time\r\n\r\n2. a hearing\r\r    [a link](http://x)").build();
        events.event(caseReference, sendReplyMessage.name(), message).as(caseworker).submitExpectingSuccess();

        // One line of HTML, as a blank line would end the tab's HTML and leave the rest to markdown.
        assertThat(events.view(caseReference, caseworker).getMessagesMarkdown())
            .contains("&#42;Re&#42; &#36;&#123;caseTitleMarkdown&#125;",
                "They ask for:<br><br>1. more time<br><br>2. a hearing<br><br>    &#91;a link&#93;(http://x)")
            .doesNotContain("${");
    }

    /** Court staff send the judges an urgent message about the case's application, and return it. */
    private UUID send() {
        PCSCase started = events.start(caseReference, sendReplyMessage.name()).as(caseworker)
            .startExpectingSuccess().caseData();
        List<DynamicListElement> applications = started.getMessageGenAppList().getListItems();
        events.start(caseReference, sendReplyMessage.name()).as(caseworker).startExpectingSuccess()
            .edit(pcsCase -> {
                pcsCase.setMessageAction(MessageAction.SEND);
                pcsCase.setMessageCategories(Set.of(MessageCategory.APPLICATION));
                pcsCase.setMessageGenAppList(chosen(applications));
                pcsCase.setMessageRecipient(MessageRecipient.DISTRICT_JUDGE);
                pcsCase.setMessageSubject("Decide GA1");
                pcsCase.setMessageUrgent(VerticalYesNo.YES);
                pcsCase.setMessageBody("Please decide this before the hearing on Friday.");
            })
            .as(caseworker).submitExpectingSuccess();
        return applications.getFirst().getCode();
    }

    private List<DynamicListElement> replyList(Actor actor) {
        return events.start(caseReference, sendReplyMessage.name()).as(actor).startExpectingSuccess()
            .caseData().getMessageReplyList().getListItems();
    }

    private static DynamicList chosen(List<DynamicListElement> items) {
        return DynamicList.builder().value(items.getFirst()).listItems(items).build();
    }
}
