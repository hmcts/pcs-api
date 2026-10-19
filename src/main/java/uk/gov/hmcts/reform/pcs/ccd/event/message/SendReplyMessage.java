package uk.gov.hmcts.reform.pcs.ccd.event.message;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageAction;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageRecipient;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseMessageEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseMessageEntryEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.message.MessageService;
import uk.gov.hmcts.reform.pcs.ccd.service.message.MessageService.Writing;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.sendReplyMessage;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * Court staff and judges send each other messages on a case, and reply to or close the messages sent to their
 * side. A message is for a role, whose pool gets a task to review it. A message about an application names it, so
 * the judge's task can open the application to decide it with an order.
 */
@Component
@AllArgsConstructor
public class SendReplyMessage implements CCDConfig<PCSCase, State, UserRole> {

    static final int SUBJECT_LIMIT = 60;
    static final int MESSAGE_LIMIT = 750;
    static final int CLOSURE_NOTE_LIMIT = 250;
    static final String NO_MESSAGES = "There are no open messages for you to reply to on this case";
    private static final String JUDICIARY_IDAM_ROLE = "judiciary";
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, h:mma");

    private final PcsCaseService pcsCaseService;
    private final MessageService messageService;
    private final SecurityContextService securityContextService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(sendReplyMessage.name(), this::submit, this::start)
            .forStates(
                State.CASE_ISSUED,
                State.CASE_PROGRESSION,
                State.JUDICIAL_REFERRAL,
                State.HEARING_READINESS,
                State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
                State.DECISION_OUTCOME
            )
            .name("Send and reply to messages")
            .showCondition(ShowConditions.featureFlagsEnabled(FeatureFlag.MAKE_ORDER))
            .grant(Permission.CRU, CASEWORKER_ROLES)
            .grant(Permission.CRU,
                UserRole.JUDGE,
                UserRole.FEE_PAID_JUDGE,
                UserRole.CIRCUIT_JUDGE,
                UserRole.LEADERSHIP_JUDGE)
            .showSummary();

        String sending = fieldEquals("messageAction", MessageAction.SEND);
        String replying = fieldEquals("messageAction", MessageAction.REPLY);
        PageBuilder pages = new PageBuilder(eventBuilder);
        pages.page("messageAction")
            .pageLabel("Send or reply to a message")
            .mandatory(PCSCase::getMessageAction);
        pages.page("sendMessage", this::checkMessage)
            .showCondition(sending)
            .pageLabel("Send a new message")
            .mandatory(PCSCase::getMessageCategories)
            .mandatory(PCSCase::getMessageGenAppList, "messageCategories CONTAINS \"APPLICATION\"")
            .mandatory(PCSCase::getMessageRecipient)
            .mandatory(PCSCase::getMessageSubject)
            .mandatory(PCSCase::getMessageUrgent)
            .mandatory(PCSCase::getMessageBody);
        pages.page("chooseMessage", this::summarise)
            .showCondition(replying)
            .pageLabel("Reply to a message")
            .mandatory(PCSCase::getMessageReplyList);
        pages.page("replyOrClose", this::checkReply)
            .showCondition(replying)
            .pageLabel("Reply to a message")
            .label("replyOrClose-summary", "${messageSummaryMarkdown}")
            .readonly(PCSCase::getMessageSummaryMarkdown, ShowConditions.NEVER_SHOW)
            .mandatory(PCSCase::getMessageNeedReply)
            .mandatory(PCSCase::getMessageReplyRecipient, "messageNeedReply=\"YES\"")
            .mandatory(PCSCase::getMessageReplyUrgent, "messageNeedReply=\"YES\"")
            .mandatory(PCSCase::getMessageReply, "messageNeedReply=\"YES\"")
            .label("replyOrClose-closing", "The message will be marked as closed.", "messageNeedReply=\"NO\"")
            .optional(PCSCase::getMessageClosureNote, "messageNeedReply=\"NO\"");
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(eventPayload.caseReference());
        caseData.setMessageGenAppList(DynamicList.builder().listItems(pcsCase.getGenApps().stream()
            .filter(GenAppEntity::isOpen)
            .sorted(Comparator.comparing(GenAppEntity::getRank))
            .map(genApp -> DynamicListElement.builder().code(genApp.getId())
                .label("GA%d: %s".formatted(genApp.getRank(), genApp.getType().getShortName())).build())
            .toList()).build());
        caseData.setMessageReplyList(DynamicList.builder().listItems(messageService.awaiting(pcsCase, isJudge())
            .stream()
            .map(message -> DynamicListElement.builder().code(message.getId())
                .label("%s, from %s, %s".formatted(message.getSubject(), message.latest().getSenderName(),
                    when(message.latest()))).build())
            .toList()).build());
        return caseData;
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> checkMessage(CaseDetails<PCSCase, State> details,
                                                                      CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();
        limit(errors, caseData.getMessageSubject(), SUBJECT_LIMIT, "Subject");
        limit(errors, caseData.getMessageBody(), MESSAGE_LIMIT, "Message");
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().data(caseData).errors(errors).build();
    }

    /** The message chosen to reply to, as the next page shows it, with the reply addressed to whoever sent it. */
    private AboutToStartOrSubmitResponse<PCSCase, State> summarise(CaseDetails<PCSCase, State> details,
                                                                   CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();
        Optional<CaseMessageEntity> message = chosen(pcsCaseService.loadCase(details.getId()), caseData);
        if (message.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder().data(caseData)
                .errors(List.of(NO_MESSAGES)).build();
        }
        caseData.setMessageSummaryMarkdown(summary(message.get()));
        if (caseData.getMessageReplyRecipient() == null) {
            caseData.setMessageReplyRecipient(message.get().latest().getSenderRole());
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().data(caseData).build();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> checkReply(CaseDetails<PCSCase, State> details,
                                                                    CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();
        List<String> errors = new ArrayList<>();
        if (caseData.getMessageNeedReply() == VerticalYesNo.YES) {
            limit(errors, caseData.getMessageReply(), MESSAGE_LIMIT, "Reply");
        } else {
            limit(errors, caseData.getMessageClosureNote(), CLOSURE_NOTE_LIMIT, "Note for closing the message");
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().data(caseData).errors(errors).build();
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(eventPayload.caseReference());
        String name = userName();
        MessageRecipient senderRole = MessageRecipient.of(isJudge());

        if (caseData.getMessageAction() == MessageAction.SEND) {
            Set<MessageCategory> categories = caseData.getMessageCategories() == null ? Set.of()
                : caseData.getMessageCategories();
            GenAppEntity genApp = categories.contains(MessageCategory.APPLICATION)
                ? chosenGenApp(pcsCase, caseData).orElse(null) : null;
            if (categories.contains(MessageCategory.APPLICATION) && genApp == null) {
                return rejected("Choose the open application the message is about");
            }
            messageService.send(pcsCase, caseData.getMessageSubject().trim(), categories, genApp,
                new Writing(name, senderRole, caseData.getMessageRecipient(),
                    caseData.getMessageUrgent() == VerticalYesNo.YES, caseData.getMessageBody().trim()));
            return confirmed("Your message has been sent",
                "A task to review the message has been created for " + caseData.getMessageRecipient().getLabel()
                    + ".");
        }

        Optional<CaseMessageEntity> message = chosen(pcsCase, caseData);
        if (message.isEmpty()) {
            return rejected(NO_MESSAGES);
        }
        if (caseData.getMessageNeedReply() == VerticalYesNo.YES) {
            messageService.reply(pcsCase, message.get(), new Writing(name, senderRole,
                caseData.getMessageReplyRecipient(), caseData.getMessageReplyUrgent() == VerticalYesNo.YES,
                caseData.getMessageReply().trim()));
            return confirmed("Reply sent", "A task to review the reply has been created for "
                + caseData.getMessageReplyRecipient().getLabel() + ".");
        }
        String note = caseData.getMessageClosureNote() == null || caseData.getMessageClosureNote().isBlank()
            ? null : caseData.getMessageClosureNote().trim();
        messageService.close(message.get(), name, note);
        return confirmed("Message closed", "The message is closed. No task has been created.");
    }

    /** The open message chosen, if it is still waiting for the user's side. */
    private Optional<CaseMessageEntity> chosen(PcsCaseEntity pcsCase, PCSCase caseData) {
        UUID id = caseData.getMessageReplyList() == null ? null : caseData.getMessageReplyList().getValueCode();
        return messageService.awaiting(pcsCase, isJudge()).stream()
            .filter(message -> message.getId().equals(id))
            .findFirst();
    }

    private static Optional<GenAppEntity> chosenGenApp(PcsCaseEntity pcsCase, PCSCase caseData) {
        UUID id = caseData.getMessageGenAppList() == null ? null : caseData.getMessageGenAppList().getValueCode();
        return id == null ? Optional.empty() : pcsCase.findGenApp(id).filter(GenAppEntity::isOpen);
    }

    /** The message as the reply page plays it back: what it is about, and every message so far. */
    private static String summary(CaseMessageEntity message) {
        StringBuilder summary = new StringBuilder()
            .append("### ").append(message.getSubject()).append("\n\n")
            .append("**What it relates to:** ").append(message.categorySet().stream()
                .sorted().map(MessageCategory::getLabel).collect(Collectors.joining(", "))).append("\n\n");
        for (CaseMessageEntryEntity entry : message.getEntries()) {
            summary.append("**").append(entry.getSenderName()).append("** to ")
                .append(entry.getRecipientRole().getLabel()).append(", ").append(when(entry))
                .append(entry.isUrgent() ? " (urgent)" : "").append("\n\n")
                .append(entry.getBody().replace("\n", "\n\n")).append("\n\n---\n\n");
        }
        return summary.toString();
    }

    private static String when(CaseMessageEntryEntity entry) {
        return LocalDateTime.ofInstant(entry.getCreatedAt(), UK_ZONE_ID).format(WHEN)
            .replace("AM", "am").replace("PM", "pm");
    }

    private static void limit(List<String> errors, String text, int limit, String name) {
        if (text != null && text.trim().length() > limit) {
            errors.add("%s must be %d characters or less".formatted(name, limit));
        }
    }

    private boolean isJudge() {
        UserInfo user = securityContextService.getCurrentUserDetails();
        return user != null && user.getRoles() != null && user.getRoles().contains(JUDICIARY_IDAM_ROLE);
    }

    private String userName() {
        UserInfo user = securityContextService.getCurrentUserDetails();
        return user == null || user.getName() == null || user.getName().isBlank()
            ? (isJudge() ? "A judge" : "Court staff") : user.getName();
    }

    private static SubmitResponse<State> rejected(String error) {
        return SubmitResponse.<State>builder().errors(List.of(error)).build();
    }

    private static SubmitResponse<State> confirmed(String header, String body) {
        return SubmitResponse.<State>builder()
            .confirmationHeader("# " + header)
            .confirmationBody(body)
            .build();
    }
}
