package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.order.OrderTasks;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.referGenAppToJudge;

/**
 * Court staff refer a general application to a judge to decide, after reviewing it, with a note for
 * the judge if they need one. That gives the judges a Work Allocation task to decide it, whose link
 * starts an order on the application. Any open application can be referred, again if need be, which
 * replaces the judges' task to decide it.
 */
@Component
public class ReferGenAppToJudge implements CCDConfig<PCSCase, State, UserRole> {

    static final String NO_APPLICATION = "Choose an open application on this case to refer to a judge";

    private final PcsCaseService pcsCaseService;
    private final GenAppRepository genAppRepository;
    private final PartyService partyService;
    private final OrderTasks orderTasks;
    private final TextAreaValidationService textAreaValidationService;
    private final Clock ukClock;

    public ReferGenAppToJudge(PcsCaseService pcsCaseService,
                              GenAppRepository genAppRepository,
                              PartyService partyService,
                              OrderTasks orderTasks,
                              TextAreaValidationService textAreaValidationService,
                              @Qualifier("ukClock") Clock ukClock) {
        this.pcsCaseService = pcsCaseService;
        this.genAppRepository = genAppRepository;
        this.partyService = partyService;
        this.orderTasks = orderTasks;
        this.textAreaValidationService = textAreaValidationService;
        this.ukClock = ukClock;
    }

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(referGenAppToJudge.name(), this::submit, this::start)
            .forStates(
                State.CASE_ISSUED,
                State.CASE_PROGRESSION,
                State.JUDICIAL_REFERRAL,
                State.HEARING_READINESS,
                State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
                State.DECISION_OUTCOME
            )
            .name("Refer application to judge")
            .showCondition(ShowConditions.featureFlagsEnabled(FeatureFlag.MAKE_ORDER))
            .grant(Permission.CRU, CASEWORKER_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .endButtonLabel("Refer to judge")
            .showSummary();

        new PageBuilder(eventBuilder)
            .page("referGenAppToJudge")
            .pageLabel("Refer application to judge")
            .label("referGenAppToJudge-lineSeparator", "---")
            .mandatory(PCSCase::getReferGenAppList)
            .optional(PCSCase::getReferGenAppNote);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(eventPayload.caseReference());
        List<DynamicListElement> applications = pcsCase.getGenApps().stream()
            .filter(GenAppEntity::isOpen)
            .sorted(Comparator.comparing(GenAppEntity::getRank))
            .map(genApp -> DynamicListElement.builder()
                .code(genApp.getId())
                .label(label(genApp))
                .build())
            .toList();
        caseData.setReferGenAppList(DynamicList.builder().listItems(applications).build());
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);

        UUID chosen = caseData.getReferGenAppList() == null ? null : caseData.getReferGenAppList().getValueCode();
        GenAppEntity genApp = pcsCase.findGenApp(chosen).filter(GenAppEntity::isOpen).orElse(null);
        if (genApp == null) {
            return SubmitResponse.<State>builder().errors(List.of(NO_APPLICATION)).build();
        }
        String note = caseData.getReferGenAppNote() == null || caseData.getReferGenAppNote().isBlank()
            ? null : caseData.getReferGenAppNote().trim();
        List<String> errors = textAreaValidationService.validateSingleTextArea(
            note, "Note for the judge", TextAreaValidationService.EXTRA_LONG_TEXT_LIMIT);
        if (!errors.isEmpty()) {
            return SubmitResponse.<State>builder().errors(errors).build();
        }

        genApp.setReferredOn(LocalDate.now(ukClock));
        genApp.setReferralNote(note);
        genAppRepository.saveAndFlush(genApp);
        orderTasks.askJudgeToDecide(caseReference, genApp, applicant(genApp));

        return SubmitResponse.<State>builder()
            .confirmationHeader("# Application referred to a judge")
            .confirmationBody("A judge has a task to decide application GA%d.".formatted(genApp.getRank()))
            .build();
    }

    private String label(GenAppEntity genApp) {
        String withoutNotice = genApp.getWithoutNotice() == VerticalYesNo.YES ? " - without notice" : "";
        return "GA%d: %s, by %s%s".formatted(genApp.getRank(), genApp.getType().getShortName(),
            applicant(genApp), withoutNotice);
    }

    private String applicant(GenAppEntity genApp) {
        return genApp.getParty() == null ? "the applicant" : partyService.getPartyName(genApp.getParty());
    }
}
