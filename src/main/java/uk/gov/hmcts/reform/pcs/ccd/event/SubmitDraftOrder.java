package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.SubmittedDraftOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.SubmittedDraftOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.DraftOrderWordingHtml;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.submitDraftOrder;

/**
 * A solicitor files the order a party they represent proposes, as rich text, for a judge to consider. A party is one
 * the solicitor's organisation represents on the case; a solicitor representing more than one chooses which.
 */
@Component
@AllArgsConstructor
public class SubmitDraftOrder implements CCDConfig<PCSCase, State, UserRole> {

    static final String NOT_AVAILABLE = "Submitting a draft order is not available";
    static final String NO_WORDING = "Enter the order you propose";
    static final String NOT_REPRESENTED = "You can only submit a draft order for a party you represent on this case";

    private static final String CONFIRMATION_BODY = """
        ---
        <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
        <span class="govuk-panel__title govuk-!-font-size-36">Draft order submitted</span>
        </div>

        <h3 class="govuk-heading-s">What happens next</h3>
        <p class="govuk-body">A judge will consider your draft order.</p>
        """;

    private final SubmittedDraftOrderRepository submittedDraftOrders;
    private final PartyRepository partyRepository;
    private final PartyService partyService;
    private final OrganisationService organisationService;
    private final SecurityContextService securityContextService;
    private final FeatureToggleService featureToggleService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(submitDraftOrder.name(), this::submit, this::start)
            .forStates(EventStates.makeOrder())
            .name("Submit draft order")
            .showCondition(ShowConditions.featureFlagsEnabled(FeatureFlag.MAKE_ORDER))
            .grant(Permission.CRUD,
                UserRole.CLAIMANT_SOLICITOR,
                UserRole.GA_CLAIMANT_SOLICITOR,
                UserRole.DEFENDANT_SOLICITOR,
                UserRole.GA_DEFENDANT_SOLICITOR)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .showSummary()
            .endButtonLabel("Submit");

        PageBuilder pages = new PageBuilder(eventBuilder);
        pages.page("submitDraftOrderParty")
            .pageLabel("Party")
            .showCondition(fieldEquals("multipleRepresentedParties", VerticalYesNo.YES))
            .readonly(PCSCase::getMultipleRepresentedParties, NEVER_SHOW)
            .mandatoryWithLabel(PCSCase::getRepresentedPartyNames, "Which party is proposing the order?");
        pages.page("submitDraftOrderWording")
            .pageLabel("What order do you propose?")
            .label("submitDraftOrderWording-lineSeparator", "---")
            .mandatory(PCSCase::getProposedOrderWording);
    }

    private PCSCase start(EventPayload<PCSCase, State> payload) {
        PCSCase caseData = payload.caseData();
        List<PartyEntity> parties = representedParties(payload.caseReference());
        caseData.setRepresentedPartyNames(DynamicList.builder()
            .listItems(parties.stream()
                .map(party -> DynamicListElement.builder()
                    .code(party.getId())
                    .label(partyService.getPartyName(party))
                    .build())
                .toList())
            .build());
        caseData.setMultipleRepresentedParties(VerticalYesNo.from(parties.size() > 1));
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> payload) {
        if (!featureToggleService.isEnabled(FeatureFlag.MAKE_ORDER)) {
            return error(NOT_AVAILABLE);
        }
        PCSCase caseData = payload.caseData();
        String wording = DraftOrderWordingHtml.forStorage(caseData.getProposedOrderWording());
        if (wording == null) {
            return error(NO_WORDING);
        }
        Optional<PartyEntity> party = filingParty(representedParties(payload.caseReference()),
                                                  caseData.getRepresentedPartyNames());
        if (party.isEmpty()) {
            return error(NOT_REPRESENTED);
        }
        submittedDraftOrders.save(SubmittedDraftOrderEntity.builder()
            .party(party.get())
            .wording(wording)
            .submittedBy(securityContextService.getCurrentUserId())
            .build());
        return SubmitResponse.<State>builder().confirmationBody(CONFIRMATION_BODY).build();
    }

    /** The parties the user's organisation represents on the case. */
    private List<PartyEntity> representedParties(long caseReference) {
        String organisationId = organisationService.getOrganisationIdForCurrentUser();
        return organisationId == null ? List.of()
            : partyRepository.findAllPartiesByOrganisationIdAndCaseReference(organisationId, caseReference);
    }

    /** The party the order is filed for: the one represented, or the one chosen of those represented. */
    private static Optional<PartyEntity> filingParty(List<PartyEntity> represented, DynamicList chosen) {
        if (represented.size() == 1) {
            return Optional.of(represented.getFirst());
        }
        UUID chosenId = Optional.ofNullable(chosen)
            .map(DynamicList::getValue)
            .map(DynamicListElement::getCode)
            .orElse(null);
        return represented.stream().filter(party -> party.getId().equals(chosenId)).findFirst();
    }

    private static SubmitResponse<State> error(String message) {
        return SubmitResponse.<State>builder().errors(List.of(message)).build();
    }
}
