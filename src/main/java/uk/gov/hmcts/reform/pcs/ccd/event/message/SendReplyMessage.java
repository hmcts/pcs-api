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
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
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
import uk.gov.hmcts.reform.pcs.ccd.pebble.PlainTextFilter;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.message.MessageService;
import uk.gov.hmcts.reform.pcs.ccd.service.message.MessageService.Writing;
import uk.gov.hmcts.reform.pcs.ccd.util.ClientContextRetriever;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

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
 * side. A message is for a role, whose pool gets a task to review it, as does whoever's message is closed without
 * a reply. A message about an application names it, so the judge's task can open the application to decide it with
 * an order.
 */
@Component
@AllArgsConstructor
public class SendReplyMessage implements CCDConfig<PCSCase, State, UserRole> {

    static final int SUBJECT_LIMIT = 60;
    static final int MESSAGE_LIMIT = 750;
    static final int CLOSURE_NOTE_LIMIT = 250;
    private static final String MESSAGE_ACTION = "messageAction";
    private static final String REPLYING = "messageNeedReply=\"YES\"";
    static final String NO_MESSAGES = "There are no open messages for you to reply to on this case";
    static final String NOT_AVAILABLE = "Messages are not available";
    private static final UserRole[] JUDGE_ROLES = {
        UserRole.JUDGE, UserRole.FEE_PAID_JUDGE, UserRole.CIRCUIT_JUDGE, UserRole.LEADERSHIP_JUDGE};
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, h:mma");

    private final PcsCaseService pcsCaseService;
    private final MessageService messageService;
    private final SecurityContextService securityContextService;
    private final TextAreaValidationService textAreaValidationService;
    private final FeatureToggleService featureToggleService;
    private final UserRoleService userRoleService;
    private final ClientContextRetriever clientContextRetriever;

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
            .grant(Permission.CRU, JUDGE_ROLES)
            .showSummary();

        String sending = fieldEquals(MESSAGE_ACTION, MessageAction.SEND);
        String replying = fieldEquals(MESSAGE_ACTION, MessageAction.REPLY);
        PageBuilder pages = new PageBuilder(eventBuilder);
        pages.page(MESSAGE_ACTION)
            .pageLabel("Send or reply to a message")
            .mandatory(PCSCase::getMessageAction)
            .readonly(PCSCase::getMessageTaskMessageId, ShowConditions.NEVER_SHOW, true)
            .readonly(PCSCase::getMessageOpenGenApps, ShowConditions.NEVER_SHOW, true);
        pages.page("sendMessage", this::checkMessage)
            .showCondition(sending)
            .pageLabel("Send a new message")
            .mandatory(PCSCase::getMessageCategories)
            .mandatory(PCSCase::getMessageGenAppList,
                "messageCategories CONTAINS \"APPLICATION\" AND messageOpenGenApps=\"Yes\"")
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
            .readonly(PCSCase::getMessageSummaryMarkdown, ShowConditions.NEVER_SHOW, true)
            .label("replyOrClose-summary", "${messageSummaryMarkdown}")
            .readonly(PCSCase::getMessageHistory)
            .mandatory(PCSCase::getMessageNeedReply)
            .mandatory(PCSCase::getMessageReplyRecipient, REPLYING)
            .mandatory(PCSCase::getMessageReplyUrgent, REPLYING)
            .mandatory(PCSCase::getMessageReply, REPLYING)
            .label("replyOrClose-closing", "The message will be marked as closed.", "messageNeedReply=\"NO\"")
            .optional(PCSCase::getMessageClosureNote, "messageNeedReply=\"NO\"");
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        if (!featureToggleService.isEnabled(FeatureFlag.MAKE_ORDER)) {
            throw new CaseAccessException(NOT_AVAILABLE);
        }
        PCSCase caseData = eventPayload.caseData();
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(eventPayload.caseReference());
        caseData.setMessageGenAppList(DynamicList.builder().listItems(pcsCase.getGenApps().stream()
            .filter(GenAppEntity::isOpen)
            .sorted(Comparator.comparing(GenAppEntity::getRank))
            .map(genApp -> DynamicListElement.builder().code(genApp.getId())
                .label("GA%d: %s".formatted(genApp.getRank(), genApp.getType().getShortName())).build())
            .toList()).build());
        caseData.setMessageOpenGenApps(YesOrNo.from(!caseData.getMessageGenAppList().getListItems().isEmpty()));
        List<CaseMessageEntity> awaiting = messageService.awaiting(pcsCase, userRoleService.isJudge());
        // Started from a message's task, as XUI also does from the case when the user has one task for the event,
        // the event starts on replying to that message.
        Optional<UUID> taskMessage = taskMessageId(pcsCase);
        List<DynamicListElement> choices = awaiting.stream()
            .map(message -> DynamicListElement.builder().code(message.getId())
                .label("%s, from %s, %s".formatted(message.getSubject(), message.latest().getSenderName(),
                    when(message.latest()))).build())
            .toList();
        Optional<DynamicListElement> tasks = choices.stream()
            .filter(choice -> taskMessage.filter(choice.getCode()::equals).isPresent()).findFirst();
        caseData.setMessageReplyList(DynamicList.builder().listItems(choices).value(tasks.orElse(null)).build());
        if (tasks.isPresent()) {
            caseData.setMessageAction(MessageAction.REPLY);
        }
        caseData.setMessageTaskMessageId(taskMessage.map(UUID::toString).orElse(null));
        return caseData;
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> checkMessage(CaseDetails<PCSCase, State> details,
                                                                      CaseDetails<PCSCase, State> before) {
        return respond(details.getData(), messageErrors(details.getData()));
    }

    private List<String> messageErrors(PCSCase caseData) {
        List<String> errors = new ArrayList<>();
        textAreaValidationService.validateTextArea(caseData.getMessageSubject(), "Subject", SUBJECT_LIMIT, errors);
        textAreaValidationService.validateTextArea(caseData.getMessageBody(), "Message", MESSAGE_LIMIT, errors);
        return errors;
    }

    /** The message chosen to reply to, as the next page shows it, with the reply addressed to whoever sent it. */
    private AboutToStartOrSubmitResponse<PCSCase, State> summarise(CaseDetails<PCSCase, State> details,
                                                                   CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();
        Optional<CaseMessageEntity> message = chosen(pcsCaseService.loadCase(details.getId()), caseData);
        if (message.isEmpty()) {
            return respond(caseData, List.of(NO_MESSAGES));
        }
        caseData.setMessageSummaryMarkdown(summary(message.get()));
        caseData.setMessageHistory(history(message.get()));
        caseData.setMessageReplyRecipient(message.get().latest().getSenderRole());
        return respond(caseData, List.of());
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> checkReply(CaseDetails<PCSCase, State> details,
                                                                    CaseDetails<PCSCase, State> before) {
        return respond(details.getData(), replyErrors(details.getData()));
    }

    private List<String> replyErrors(PCSCase caseData) {
        List<String> errors = new ArrayList<>();
        if (caseData.getMessageNeedReply() == VerticalYesNo.YES) {
            textAreaValidationService.validateTextArea(caseData.getMessageReply(), "Reply", MESSAGE_LIMIT, errors);
        } else {
            textAreaValidationService.validateTextArea(caseData.getMessageClosureNote(),
                "Note for closing the message", CLOSURE_NOTE_LIMIT, errors);
        }
        return errors;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        if (!featureToggleService.isEnabled(FeatureFlag.MAKE_ORDER)) {
            return rejected(List.of(NOT_AVAILABLE));
        }
        PCSCase caseData = eventPayload.caseData();
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(eventPayload.caseReference());
        MessageRecipient senderRole = MessageRecipient.of(userRoleService.isJudge());
        // A message is signed with the sender's name, or their role if IDAM gives none.
        String name = Optional.ofNullable(securityContextService.getCurrentUserDetails().getName())
            .filter(given -> !given.isBlank()).orElse(senderRole.getLabel());

        // The pages check these as the user goes; an event submitted without them is checked here.
        boolean sending = caseData.getMessageAction() == MessageAction.SEND;
        List<String> errors = sending ? messageErrors(caseData) : replyErrors(caseData);
        if (!errors.isEmpty()) {
            return rejected(errors);
        }
        Optional<UUID> tasksMessage = boundMessage(caseData, pcsCase);
        boolean fromItsTask = !sending && tasksMessage.isPresent() && tasksMessage.get().equals(chosenId(caseData));
        SubmitResponse<State> response = sending
            ? send(pcsCase, caseData, name, senderRole)
            : answer(pcsCase, caseData, name, senderRole, fromItsTask);
        // XUI completes the task the event was started from whatever the user did, so a message they left for
        // another is asked about again.
        if ((response.getErrors() == null || response.getErrors().isEmpty()) && !fromItsTask) {
            tasksMessage.flatMap(id -> messageService.awaiting(pcsCase, userRoleService.isJudge()).stream()
                .filter(message -> message.getId().equals(id)).findFirst()).ifPresent(messageService::askToReview);
        }
        return response;
    }

    private SubmitResponse<State> answer(PcsCaseEntity pcsCase, PCSCase caseData, String name,
                                         MessageRecipient senderRole, boolean fromItsTask) {
        Optional<CaseMessageEntity> message = chosen(pcsCase, caseData);
        if (message.isEmpty()) {
            return rejected(List.of(NO_MESSAGES));
        }
        if (caseData.getMessageNeedReply() == VerticalYesNo.YES) {
            messageService.reply(message.get(), new Writing(name, senderRole,
                caseData.getMessageReplyRecipient(), caseData.getMessageReplyUrgent() == VerticalYesNo.YES,
                caseData.getMessageReply().trim()), fromItsTask);
            return confirmed("Reply sent",
                "A task to review the reply has been created for " + caseData.getMessageReplyRecipient().getLabel()
                    + ".");
        }
        String note = caseData.getMessageClosureNote() == null || caseData.getMessageClosureNote().isBlank()
            ? null : caseData.getMessageClosureNote().trim();
        MessageRecipient told = messageService.closeWithoutReply(message.get(), name, note, fromItsTask);
        return confirmed("Message closed",
            "A task has been created for " + told.getLabel() + " to review.");
    }

    private SubmitResponse<State> send(PcsCaseEntity pcsCase, PCSCase caseData, String name,
                                       MessageRecipient senderRole) {
        Set<MessageCategory> categories = caseData.getMessageCategories();
        boolean aboutApplication = categories.contains(MessageCategory.APPLICATION);
        GenAppEntity genApp = aboutApplication ? chosenGenApp(pcsCase, caseData).orElse(null) : null;
        // A case with no open application asks for none, as the message can be about one not yet entered.
        if (aboutApplication && genApp == null && caseData.getMessageOpenGenApps() == YesOrNo.YES) {
            return rejected(List.of("Choose the open application the message is about"));
        }
        messageService.send(pcsCase, caseData.getMessageSubject().trim(), categories, genApp,
            new Writing(name, senderRole, caseData.getMessageRecipient(),
                caseData.getMessageUrgent() == VerticalYesNo.YES, caseData.getMessageBody().trim()));
        return confirmed("Your message has been sent",
            "A task to review the message has been created for " + caseData.getMessageRecipient().getLabel() + ".");
    }

    /** The open message chosen, if it is still waiting for the user's side. */
    private Optional<CaseMessageEntity> chosen(PcsCaseEntity pcsCase, PCSCase caseData) {
        UUID id = chosenId(caseData);
        return messageService.awaiting(pcsCase, userRoleService.isJudge()).stream()
            .filter(message -> message.getId().equals(id))
            .findFirst();
    }

    private static UUID chosenId(PCSCase caseData) {
        return caseData.getMessageReplyList() == null ? null : caseData.getMessageReplyList().getValueCode();
    }

    /** The message the event was started from the task of, as the client context or the event's own data says. */
    private Optional<UUID> boundMessage(PCSCase caseData, PcsCaseEntity pcsCase) {
        return taskMessageId(pcsCase)
            .or(() -> Optional.ofNullable(caseData.getMessageTaskMessageId()).map(UUID::fromString));
    }

    /** The message the task the event was started from is for, if it was started from one. */
    private Optional<UUID> taskMessageId(PcsCaseEntity pcsCase) {
        try {
            // XUI can keep a task's context after the user has left it, so only one of this case's messages counts.
            return clientContextRetriever.getTaskProperty("messageId").map(UUID::fromString)
                .filter(id -> pcsCase.getMessages().stream().anyMatch(message -> message.getId().equals(id)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<GenAppEntity> chosenGenApp(PcsCaseEntity pcsCase, PCSCase caseData) {
        UUID id = caseData.getMessageGenAppList() == null ? null : caseData.getMessageGenAppList().getValueCode();
        return id == null ? Optional.empty() : pcsCase.findGenApp(id).filter(GenAppEntity::isOpen);
    }

    /** The message's subject and what it is about, heading the reply page, the subject as written, not as markdown. */
    static String summary(CaseMessageEntity message) {
        return "### " + PlainTextFilter.encode(message.getSubject()) + "\n\n**What it relates to:** "
            + message.categoryLabels();
    }

    /** Every message so far, which the reply page and check your answers play back. */
    private static String history(CaseMessageEntity message) {
        return message.getEntries().stream()
            .map(entry -> entry.getSenderName() + " to " + entry.getRecipientRole().getLabel() + ", " + when(entry)
                + (entry.isUrgent() ? " (urgent)" : "") + "\n" + entry.getBody())
            .collect(Collectors.joining("\n\n"));
    }

    private static String when(CaseMessageEntryEntity entry) {
        return LocalDateTime.ofInstant(entry.getCreatedAt(), UK_ZONE_ID).format(WHEN)
            .replace("AM", "am").replace("PM", "pm");
    }

    private static AboutToStartOrSubmitResponse<PCSCase, State> respond(PCSCase caseData, List<String> errors) {
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().data(caseData).errors(errors).build();
    }

    private static SubmitResponse<State> rejected(List<String> errors) {
        return SubmitResponse.<State>builder().errors(errors).build();
    }

    private static SubmitResponse<State> confirmed(String header, String next) {
        return SubmitResponse.<State>builder()
            .confirmationHeader("# " + header)
            .confirmationBody(next)
            .build();
    }
}
