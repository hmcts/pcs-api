package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseMessageEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseMessageEntryEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.tabs.MessagesRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.tabs.MessagesRenderer.Entry;
import uk.gov.hmcts.reform.pcs.ccd.renderer.tabs.MessagesRenderer.Thread;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * The Messages tab: the case's open messages, urgent ones first and then the oldest first, then the closed ones. A
 * message about an open application links judges to deciding it.
 */
@Component
public class MessagesView {

    private final MessagesRenderer messagesRenderer;
    private final String frontendUrl;

    public MessagesView(MessagesRenderer messagesRenderer, @Value("${frontend.url}") String frontendUrl) {
        this.messagesRenderer = messagesRenderer;
        this.frontendUrl = frontendUrl;
    }

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        Comparator<CaseMessageEntity> order = Comparator.comparing((CaseMessageEntity message) -> !message.isUrgent())
            .thenComparing(CaseMessageEntity::getCreatedAt);
        List<Thread> open = pcsCaseEntity.getMessages().stream().filter(CaseMessageEntity::isOpen)
            .sorted(order).map(message -> thread(message, pcsCaseEntity)).toList();
        List<Thread> closed = pcsCaseEntity.getMessages().stream().filter(message -> !message.isOpen())
            .sorted(Comparator.comparing(CaseMessageEntity::getUpdatedAt).reversed())
            .map(message -> thread(message, pcsCaseEntity)).toList();
        pcsCase.setMessagesMarkdown(messagesRenderer.render(open, closed));
    }

    private Thread thread(CaseMessageEntity message, PcsCaseEntity pcsCase) {
        CaseMessageEntryEntity first = message.getEntries().getFirst();
        CaseMessageEntryEntity latest = message.latest();
        GenAppEntity genApp = message.getGenAppId() == null ? null
            : pcsCase.findGenApp(message.getGenAppId()).orElse(null);
        return new Thread(
            message.getSubject(),
            message.categorySet().stream().sorted().map(MessageCategory::getLabel).collect(Collectors.joining(", ")),
            genApp == null ? null : "GA%d: %s".formatted(genApp.getRank(), genApp.getType().getShortName()),
            genApp == null || !genApp.isOpen() || !message.isOpen() ? null
                : "%s/case/%d/make-order?genAppId=%s".formatted(frontendUrl, pcsCase.getCaseReference(),
                    genApp.getId()),
            first.getSenderName(),
            latest.getRecipientRole().getLabel(),
            message.isUrgent(),
            LocalDateTime.ofInstant(first.getCreatedAt(), UK_ZONE_ID),
            LocalDateTime.ofInstant(message.getUpdatedAt(), UK_ZONE_ID),
            message.getEntries().stream().map(entry -> new Entry(entry.getSenderName(),
                entry.getRecipientRole().getLabel(), entry.isUrgent(),
                LocalDateTime.ofInstant(entry.getCreatedAt(), UK_ZONE_ID), entry.getBody())).toList(),
            message.getClosedByName(),
            message.getClosureNote());
    }
}
