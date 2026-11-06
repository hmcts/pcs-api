package uk.gov.hmcts.reform.pcs.ccd.view;

import io.pebbletemplates.pebble.PebbleEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseMessageEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseMessageEntryEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.order.OrderTasks;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.exception.TemplateRenderingException;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * The Messages tab: the case's open messages, urgent ones first and then the oldest first, then the closed ones. A
 * message about an open application links judges to deciding it.
 */
@Component
public class MessagesView {

    private final PebbleEngine pebbleEngine;
    private final String frontendUrl;
    private final FeatureToggleService featureToggleService;
    private final OrderTasks orderTasks;
    private final UserRoleService userRoleService;

    /** A message thread as the tab shows it; decideUrl is set for an open message about an open application. */
    public record Thread(String subject, String categories, String application, String decideUrl, String from,
                         String to, boolean urgent, String status, LocalDateTime sent, LocalDateTime updated,
                         List<Entry> entries, String closedBy, String closureNote) {
    }

    public record Entry(String from, String to, boolean urgent, LocalDateTime sent, String body) {
    }

    public MessagesView(PebbleEngine pebbleEngine, @Value("${frontend.url}") String frontendUrl,
                        FeatureToggleService featureToggleService, OrderTasks orderTasks,
                        UserRoleService userRoleService) {
        this.pebbleEngine = pebbleEngine;
        this.frontendUrl = frontendUrl;
        this.featureToggleService = featureToggleService;
        this.orderTasks = orderTasks;
        this.userRoleService = userRoleService;
    }

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        if (!featureToggleService.isEnabled(FeatureFlag.MAKE_ORDER)) {
            return;
        }
        // Only a judge can decide an application, so only a judge is offered the link.
        boolean judge = !pcsCaseEntity.getMessages().isEmpty() && userRoleService.isJudge();
        Comparator<CaseMessageEntity> order = Comparator.comparing((CaseMessageEntity message) -> !message.isUrgent())
            .thenComparing(CaseMessageEntity::createdAt);
        List<Thread> open = pcsCaseEntity.getMessages().stream().filter(CaseMessageEntity::isOpen)
            .sorted(order).map(message -> thread(message, pcsCaseEntity, judge)).toList();
        List<Thread> closed = pcsCaseEntity.getMessages().stream().filter(message -> !message.isOpen())
            .sorted(Comparator.comparing(CaseMessageEntity::getClosedAt).reversed())
            .map(message -> thread(message, pcsCaseEntity, judge)).toList();
        StringWriter writer = new StringWriter();
        try {
            pebbleEngine.getTemplate("tabs/messages").evaluate(writer, Map.of("open", open, "closed", closed));
        } catch (IOException e) {
            throw new TemplateRenderingException("Failed to render template", e);
        }
        pcsCase.setMessagesMarkdown(writer.toString());
    }

    private Thread thread(CaseMessageEntity message, PcsCaseEntity pcsCase, boolean judge) {
        CaseMessageEntryEntity first = message.getEntries().getFirst();
        CaseMessageEntryEntity latest = message.latest();
        GenAppEntity genApp = message.getGenApp();
        return new Thread(
            message.getSubject(),
            message.categoryLabels(),
            genApp == null ? null : "GA%d: %s".formatted(genApp.getRank(), genApp.getType().getShortName()),
            !judge || genApp == null || !message.isOpen() || !orderTasks.canDecide(genApp) ? null
                : "%s/case/%d/make-order?genAppId=%s".formatted(frontendUrl, pcsCase.getCaseReference(),
                    genApp.getId()),
            first.getSenderName(),
            first.getRecipientRole().getLabel(),
            message.isUrgent(),
            message.isOpen() ? "Open, waiting for " + latest.getRecipientRole().getLabel() : "Closed",
            LocalDateTime.ofInstant(first.getCreatedAt(), UK_ZONE_ID),
            LocalDateTime.ofInstant(message.updatedAt(), UK_ZONE_ID),
            message.getEntries().stream().map(entry -> new Entry(entry.getSenderName(),
                entry.getRecipientRole().getLabel(), entry.isUrgent(),
                LocalDateTime.ofInstant(entry.getCreatedAt(), UK_ZONE_ID), entry.getBody())).toList(),
            message.getClosedByName(),
            message.getClosureNote());
    }
}
