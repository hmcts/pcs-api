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
import uk.gov.hmcts.reform.pcs.ccd.event.order.OrderTasks;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderStepRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * Messages between court staff and judges on a case. Sending or replying gives the recipient role's pool a task to
 * review the message; closing one without a reply gives the pool of whoever wrote its latest message a task to see
 * that it is closed. Court staff's query on an order they return is kept as a closed message, pointed at by the step
 * that returned it: the judge answers it by changing the order, from the order's own task, so it gives no task here.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private static final DateTimeFormatter WRITTEN = DateTimeFormatter.ofPattern("d MMM yyyy");

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
     * Court staff's query on the order they return to its judge, kept on the Messages tab as a closed message that says
     * it came with the order and when the judge wrote it. Saved so the returning step can point at it.
     */
    public CaseMessageEntryEntity recordOrderQuery(OrderEntity order, Instant written, String staffName,
                                                   String query) {
        // Signed with the role if IDAM gave them no name.
        String name = staffName == null || staffName.isBlank() ? MessageRecipient.COURT_STAFF.getLabel() : staffName;
        CaseMessageEntity message = CaseMessageEntity.builder()
            .subject("Query on the order written " + WRITTEN.format(written.atZone(UK_ZONE_ID)))
            .categories(EnumSet.of(MessageCategory.OTHER))
            .build();
        CaseMessageEntryEntity entry = entry(new Writing(name, MessageRecipient.COURT_STAFF,
            MessageRecipient.of(true), false, query), Instant.now(utcClock));
        message.addEntry(entry);
        close(message, name, "Sent with the order, which went back to its judge");
        order.getPcsCase().addMessage(message);
        // The case cascades to its messages, so flushing inserts the entry the step is about to point at.
        orderStepRepository.flush();
        return entry;
    }

    /**
     * Closes the case's open messages about the applications that wait for a judge, once a judge sends an order
     * deciding them for review: the order answers them.
     */
    public void closeAnsweredByOrder(OrderEntity order, Collection<GenAppEntity> decided, String judgeName) {
        PcsCaseEntity pcsCase = order.getPcsCase();
        pcsCase.getMessages().stream()
            .filter(CaseMessageEntity::isOpen)
            .filter(message -> decided.contains(message.getGenApp()))
            .filter(message -> message.latest().getRecipientRole().isJudicial())
            .forEach(message -> {
                close(message, judgeName, "Answered by an order on the application");
                // Its task, if the judge did not come from it, has nothing left to do.
                camundaService.cancelMessageTask(pcsCase.getCaseReference(), message.getId(),
                    TaskType.REVIEW_MESSAGE_JUDGE);
            });
    }

    /** Court staff's last query on the order, which they returned it with. */
    public Optional<String> lastQuery(OrderEntity order) {
        return orderStepRepository.findByOrderIdAndMessageEntryIsNotNullOrderByIdDesc(order.getId()).stream()
            .findFirst()
            .map(step -> step.getMessageEntry().getBody());
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
