package uk.gov.hmcts.reform.pcs.ccd.event.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType.ADJOURN;
import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.sendReplyMessage;
import static uk.gov.hmcts.reform.pcs.ccd.event.order.MakeOrder.MAKE_ORDER;

/**
 * Court staff and judges message each other on a case: a message gives the recipient role's pool a task, a reply
 * gives the other side one, and closing it gives no one a task. The Messages tab shows each thread.
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
            and(contains("ext:makeOrder?expected_sub=${[EXPECTED_SUB]}&amp;genAppId=" + genAppId),
                contains("/cases/case-details/PCS/%s/%d/trigger/sendReplyMessage?tid=${[id]}"
                    .formatted(CaseType.getCaseType(), caseReference))),
            eq(true));
        String tab = events.view(caseReference, caseworker).getMessagesMarkdown();
        assertThat(tab).contains("Decide GA1", "Urgent", "Tom Caseworker", "District Judge",
            "Please decide this before the hearing on Friday.", "GA1: Adjourn the hearing");
    }

    @Test
    @DisplayName("lets the judge reply, giving court staff a task, then court staff close it without one")
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
            contains("Sarah Hughes sent a message"), eq(false));
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

        verify(camundaService, never()).createTask(eq(caseReference), eq(TaskType.REVIEW_MESSAGE_JUDGE), anyString(),
            eq(false));
        assertThat(replyList(caseworker)).isEmpty();
        String tab = events.view(caseReference, caseworker).getMessagesMarkdown();
        assertThat(tab).contains("Closed messages", "Please list it for a hearing on notice.",
            "Closed by Tom Caseworker: Listed.");
    }

    @Test
    @DisplayName("keeps the judge's message for court staff with their order on the Messages tab")
    void keepsTheOrdersStaffMessage() {
        events.external(caseReference, MAKE_ORDER).as(judge).submitExpectingSuccess(new MakeOrderRequest(
            SUBMIT_FOR_REVIEW, new OrderChange(null, 0, OrderType.FREE_FORM,
                Map.of("staff-message", List.of("yes"), "staff-message-text", "Serve this by post."), null, null),
            null));

        assertThat(events.view(caseReference, caseworker).getMessagesMarkdown())
            .contains("Message with free form order", "Serve this by post.", "Court staff");
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
