package uk.gov.hmcts.reform.pcs.ccd.service.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageRecipient;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseMessageEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseMessageEntryEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.order.OrderTasks;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderStepRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Messages between court staff and judges on a case. Sending or replying gives the recipient role's pool a task to
 * review the message; closing one without a reply gives the pool of whoever wrote its latest message a task to see
 * that it is closed. An order has a thread of its own, holding the judge's messages to court staff with it and court
 * staff's queries when they return it, each pointed at by the order step it was sent with; the order's tasks already
 * ask for those to be read, so they give no task here.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final CamundaService camundaService;
    private final TaskDescriptionService taskDescriptionService;
    private final OrderTasks orderTasks;
    private final OrderStepRepository orderStepRepository;
    private final Clock utcClock;

    /** What someone writes in a message: who, as what, to whom, whether it is urgent, and what it says. */
    public record Writing(String senderName, MessageRecipient senderRole, MessageRecipient recipient,
                          boolean urgent, String body) {
    }

    /** The case's open messages whose latest message is for the user's side: a judge's, or court staff's. */
    public List<CaseMessageEntity> awaiting(PcsCaseEntity pcsCase, boolean judge) {
        return pcsCase.getMessages().stream()
            .filter(CaseMessageEntity::isOpen)
            .filter(message -> message.latest().getRecipientRole().isJudicial() == judge)
            .sorted(Comparator.comparing(CaseMessageEntity::createdAt))
            .toList();
    }

    public void send(PcsCaseEntity pcsCase, String subject, Collection<MessageCategory> categories,
                     GenAppEntity genApp, Writing writing) {
        CaseMessageEntity message = CaseMessageEntity.builder()
            .subject(subject)
            .categories(EnumSet.copyOf(categories))
            .genApp(genApp)
            .build();
        message.addEntry(entry(writing, Instant.now(utcClock)));
        pcsCase.addMessage(message);
        // Saved so it has the id its task names.
        orderStepRepository.flush();
        askToReview(message);
    }

    /**
     * Replies to the message; {@code fromItsTask} says whether the replier came from its task, which XUI closes.
     */
    public void reply(CaseMessageEntity message, Writing writing, boolean fromItsTask) {
        // A reply to the same side gets a task that only the time it is made tells apart from the one it answers.
        closeTaskUnlessFrom(message, fromItsTask || taskFor(writing.recipient()) == taskFor(waitingSide(message)));
        message.addEntry(entry(writing, Instant.now(utcClock)));
        askToReview(message);
    }

    /**
     * The side the message waits for may answer it without its task, such as from the case's events; XUI then leaves
     * the task open with nothing to do, so it is cancelled. A task the user came from is XUI's to close.
     */
    private void closeTaskUnlessFrom(CaseMessageEntity message, boolean keepTask) {
        if (!keepTask) {
            camundaService.cancelMessageTask(message.getPcsCase().getCaseReference(), message.getId(),
                taskFor(waitingSide(message)));
        }
    }

    private static MessageRecipient waitingSide(CaseMessageEntity message) {
        return message.latest().getRecipientRole();
    }

    private static TaskType taskFor(MessageRecipient side) {
        return side.isJudicial() ? TaskType.REVIEW_MESSAGE_JUDGE : TaskType.REVIEW_MESSAGE_STAFF;
    }

    private void close(CaseMessageEntity message, String closedBy, String note) {
        message.setClosedAt(Instant.now(utcClock));
        message.setClosedByName(closedBy);
        message.setClosureNote(note);
    }

    /** Closes the message without a reply, telling the side that wrote its latest message, whose side it returns. */
    public MessageRecipient closeWithoutReply(CaseMessageEntity message, String closedBy, String note,
                                              boolean fromItsTask) {
        closeTaskUnlessFrom(message, fromItsTask);
        close(message, closedBy, note);
        MessageRecipient told = message.latest().getSenderRole();
        long caseReference = message.getPcsCase().getCaseReference();
        String description = taskDescriptionService.createReviewClosedMessageDescription(closedBy,
            message.getSubject(), note, caseReference);
        camundaService.createTask(caseReference, taskFor(told), description, false, null);
        return told;
    }

    /**
     * A judge's message for court staff with their order, or court staff's query when they return it, saved so the
     * step it is sent with can point at it.
     */
    public CaseMessageEntryEntity recordOrderMessage(OrderEntity order, String orderType, String senderName,
                                                     boolean fromJudge, String body) {
        MessageRecipient sender = MessageRecipient.of(fromJudge);
        // Signed with the sender's role if IDAM gave them no name.
        String name = senderName == null || senderName.isBlank() ? sender.getLabel() : senderName;
        CaseMessageEntryEntity entry = entry(new Writing(name, sender, MessageRecipient.of(!fromJudge), false, body),
            Instant.now(utcClock));
        orderThread(order, orderType).addEntry(entry);
        // The case cascades to its messages, so flushing inserts the entry the step is about to point at.
        orderStepRepository.flush();
        return entry;
    }

    /**
     * Closes the case's open messages about the application that wait for a judge, once a judge sends an order
     * deciding it for review: the order answers them.
     */
    public void closeAnsweredByOrder(PcsCaseEntity pcsCase, UUID genAppId, String judgeName) {
        pcsCase.getMessages().stream()
            .filter(CaseMessageEntity::isOpen)
            .filter(message -> message.getGenApp() != null && genAppId.equals(message.getGenApp().getId()))
            .filter(message -> message.latest().getRecipientRole().isJudicial())
            .forEach(message -> {
                close(message, judgeName, "Answered by an order on the application");
                // Its task, if the judge did not come from it, has nothing left to do.
                camundaService.cancelMessageTask(pcsCase.getCaseReference(), message.getId(),
                    TaskType.REVIEW_MESSAGE_JUDGE);
            });
    }

    /** Closes the order's open thread once the order is issued, as nothing more is waited for on it. */
    public void closeOrderThread(OrderEntity order, String closedBy) {
        orderEntries(order).map(CaseMessageEntryEntity::getMessage).distinct()
            .filter(CaseMessageEntity::isOpen)
            .forEach(message -> close(message, closedBy, "Order issued"));
    }

    /** What the judge, or court staff, last wrote with the order: the judge's message, or court staff's query. */
    public Optional<String> lastOrderMessage(OrderEntity order, boolean fromJudge) {
        return orderEntries(order)
            .filter(entry -> entry.getSenderRole().isJudicial() == fromJudge)
            .findFirst()
            .map(CaseMessageEntryEntity::getBody);
    }

    /** The messages sent with the order's steps, the latest first. */
    private Stream<CaseMessageEntryEntity> orderEntries(OrderEntity order) {
        return orderStepRepository.findByOrderIdAndMessageEntryIsNotNullOrderByIdDesc(order.getId()).stream()
            .map(OrderStepEntity::getMessageEntry);
    }

    private CaseMessageEntity orderThread(OrderEntity order, String orderType) {
        return orderEntries(order).map(CaseMessageEntryEntity::getMessage)
            .filter(CaseMessageEntity::isOpen)
            .findFirst()
            .orElseGet(() -> {
                GenAppEntity genApp = order.getGenAppId() == null ? null
                    : order.getPcsCase().findGenApp(order.getGenAppId()).orElse(null);
                CaseMessageEntity started = CaseMessageEntity.builder()
                    .subject(Character.toUpperCase(orderType.charAt(0)) + orderType.substring(1) + " order")
                    .categories(EnumSet.of(genApp == null ? MessageCategory.OTHER : MessageCategory.APPLICATION))
                    .genApp(genApp)
                    .build();
                order.getPcsCase().addMessage(started);
                return started;
            });
    }

    /** Gives the pool of the side the message waits for a task to review it. */
    public void askToReview(CaseMessageEntity message) {
        CaseMessageEntryEntity latest = message.latest();
        GenAppEntity genApp = message.getGenApp();
        long caseReference = message.getPcsCase().getCaseReference();
        boolean toJudge = latest.getRecipientRole().isJudicial();
        String decideUrl = genApp == null || !toJudge || !orderTasks.canDecide(genApp) ? null
            : orderTasks.decideUrl(caseReference, genApp);
        String description = taskDescriptionService.createReviewMessageDescription(
            latest.getSenderName(), message.getSubject(), latest.isUrgent(), caseReference,
            genApp == null ? null : "GA" + genApp.getRank(), decideUrl);
        camundaService.createTask(caseReference, taskFor(latest.getRecipientRole()), description, latest.isUrgent(),
            message.getId());
    }

    private static CaseMessageEntryEntity entry(Writing writing, Instant at) {
        return CaseMessageEntryEntity.builder()
            .createdAt(at)
            .senderName(writing.senderName())
            .senderRole(writing.senderRole())
            .recipientRole(writing.recipient())
            .urgent(writing.urgent())
            .body(writing.body())
            .build();
    }
}
