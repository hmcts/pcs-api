package uk.gov.hmcts.reform.pcs.ccd.service.message;

import org.springframework.beans.factory.annotation.Value;
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
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Messages between court staff and judges on a case. Sending or replying gives the recipient role's pool a task to
 * review the message; closing one gives no one a task. A judge's message to court staff with an order is a message
 * too, which the order's review task already carries, so it gives no task of its own.
 */
@Service
public class MessageService {

    private final CamundaService camundaService;
    private final TaskDescriptionService taskDescriptionService;
    private final Clock clock;
    private final String frontendUrl;

    public MessageService(CamundaService camundaService,
                          TaskDescriptionService taskDescriptionService,
                          Clock utcClock,
                          @Value("${frontend.url}") String frontendUrl) {
        this.camundaService = camundaService;
        this.taskDescriptionService = taskDescriptionService;
        this.clock = utcClock;
        this.frontendUrl = frontendUrl;
    }

    /** What someone writes in a message: who, as what, to whom, whether it is urgent, and what it says. */
    public record Writing(String senderName, MessageRecipient senderRole, MessageRecipient recipient,
                          boolean urgent, String body) {
    }

    /** The case's open messages whose latest message is for the user's side: a judge's, or court staff's. */
    public List<CaseMessageEntity> awaiting(PcsCaseEntity pcsCase, boolean judge) {
        return pcsCase.getMessages().stream()
            .filter(CaseMessageEntity::isOpen)
            .filter(message -> message.latest().getRecipientRole().isJudicial() == judge)
            .sorted(Comparator.comparing(CaseMessageEntity::getCreatedAt))
            .toList();
    }

    public CaseMessageEntity send(PcsCaseEntity pcsCase, String subject, Collection<MessageCategory> categories,
                                  GenAppEntity genApp, Writing writing) {
        Instant now = Instant.now(clock);
        CaseMessageEntity message = CaseMessageEntity.builder()
            .subject(subject)
            .categories(categories.stream().map(Enum::name).sorted().collect(Collectors.joining(",")))
            .genAppId(genApp == null ? null : genApp.getId())
            .status(CaseMessageEntity.Status.OPEN)
            .createdAt(now)
            .build();
        message.addEntry(entry(writing, now));
        pcsCase.addMessage(message);
        askToReview(pcsCase, message, genApp);
        return message;
    }

    public void reply(PcsCaseEntity pcsCase, CaseMessageEntity message, Writing writing) {
        message.addEntry(entry(writing, Instant.now(clock)));
        GenAppEntity genApp = message.getGenAppId() == null ? null
            : pcsCase.findGenApp(message.getGenAppId()).orElse(null);
        askToReview(pcsCase, message, genApp);
    }

    public void close(CaseMessageEntity message, String closedBy, String note) {
        message.setStatus(CaseMessageEntity.Status.CLOSED);
        message.setClosedByName(closedBy);
        message.setClosureNote(note);
        message.setUpdatedAt(Instant.now(clock));
    }

    /** A judge's message for court staff with their order, kept with the case's other messages. */
    public void recordOrderMessage(OrderEntity order, String judgeName, String orderType, String body) {
        Instant now = Instant.now(clock);
        CaseMessageEntity message = order.getPcsCase().getMessages().stream()
            .filter(existing -> order.getId().equals(existing.getOrderId()) && existing.isOpen())
            .findFirst()
            .orElseGet(() -> {
                CaseMessageEntity started = CaseMessageEntity.builder()
                    .subject(truncate("Message with " + orderType + " order", 60))
                    .categories(order.getGenAppId() == null ? MessageCategory.OTHER.name()
                        : MessageCategory.APPLICATION.name())
                    .genAppId(order.getGenAppId())
                    .orderId(order.getId())
                    .status(CaseMessageEntity.Status.OPEN)
                    .createdAt(now)
                    .build();
                order.getPcsCase().addMessage(started);
                return started;
            });
        message.addEntry(entry(new Writing(judgeName, MessageRecipient.DISTRICT_JUDGE, MessageRecipient.COURT_STAFF,
            false, truncate(body, 750)), now));
    }

    /** Closes the message an order carried once the order is issued. */
    public void closeOrderMessage(OrderEntity order, String closedBy) {
        order.getPcsCase().getMessages().stream()
            .filter(message -> order.getId().equals(message.getOrderId()) && message.isOpen())
            .forEach(message -> close(message, closedBy, "Order issued"));
    }

    private void askToReview(PcsCaseEntity pcsCase, CaseMessageEntity message, GenAppEntity genApp) {
        CaseMessageEntryEntity latest = message.latest();
        long caseReference = pcsCase.getCaseReference();
        boolean toJudge = latest.getRecipientRole().isJudicial();
        String decideUrl = genApp == null || !toJudge || !genApp.isOpen() ? null
            : "%s/cases/%d/event/ext:makeOrder?expected_sub=${[EXPECTED_SUB]}&genAppId=%s&taskId=${[id]}"
                .formatted(frontendUrl, caseReference, genApp.getId());
        String description = taskDescriptionService.createReviewMessageDescription(
            latest.getSenderName(), message.getSubject(), latest.isUrgent(),
            "/cases/case-details/%d#Messages".formatted(caseReference),
            genApp == null ? null : "GA" + genApp.getRank(), decideUrl);
        TaskType taskType = toJudge ? TaskType.REVIEW_MESSAGE_JUDGE : TaskType.REVIEW_MESSAGE_STAFF;
        camundaService.createTask(caseReference, taskType, description, latest.isUrgent());
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

    private static String truncate(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length);
    }
}
