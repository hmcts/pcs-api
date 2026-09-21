package uk.gov.hmcts.reform.pcs.ccd.event.deletedraftresponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantPartyExtractor;
import uk.gov.hmcts.reform.pcs.exception.MultiplePartiesException;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.deleteDraftResponse;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_4;

@Slf4j
@Component
@AllArgsConstructor
public class DeleteDraftResponse implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final OrganisationService organisationService;
    private final DefendantPartyExtractor defendantPartyExtractor;
    private final DraftCaseDataService draftCaseDataService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(deleteDraftResponse.name(), this::submit)
                .forState(State.CASE_ISSUED)
                .name("Delete draft response")
                .grant(Permission.CRUD, UserRole.DEFENDANT_SOLICITOR)
                .grant(Permission.CRUD, UserRole.GA_DEFENDANT_SOLICITOR)
                .showCondition(ShowConditions.and(
                    "hasDraftResponse=\"Yes\"",
                    ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_4)))
                .endButtonLabel("Continue");

        new PageBuilder(eventBuilder)
            .page("deleteDraftResponse")
            .pageLabel("Delete draft response")
            .label("deleteDraftResponse-separator", "---")
            .mandatory(PCSCase::getDeleteDraftResponse);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        if (eventPayload.caseData().getDeleteDraftResponse() != YesOrNo.YES) {
            return SubmitResponse.defaultResponse();
        }

        long caseReference = eventPayload.caseReference();
        String organisationId = organisationService.getOrganisationIdForCurrentUser();
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        PartyEntity representedDefendant = getRepresentedDefendant(pcsCaseEntity, organisationId);

        draftCaseDataService.deleteUnsubmittedCaseData(
            caseReference, respondPossessionClaim, representedDefendant.getId(), organisationId);

        log.info("Deleted draft response for case {} and organisation {}", caseReference, organisationId);

        return SubmitResponse.<State>builder()
            .confirmationBody(getDraftDeletedConfirmationMarkdown())
            .build();
    }

    private PartyEntity getRepresentedDefendant(PcsCaseEntity pcsCaseEntity, String organisationId) {
        List<PartyEntity> representedDefendants =
            defendantPartyExtractor.extractDefendantsRepresentedBy(pcsCaseEntity, organisationId);

        if (representedDefendants.size() == 1) {
            return representedDefendants.getFirst();
        } else if (representedDefendants.isEmpty()) {
            throw new PartyNotFoundException("No represented party found");
        } else {
            throw new MultiplePartiesException("Deleting a draft response for multiple parties is not supported");
        }
    }

    private static String getDraftDeletedConfirmationMarkdown() {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Your draft response has been deleted</span>
            </div>
            <p class="govuk-body govuk-!-font-size-19">
            <span><a class="govuk-link--no-visited-state" href="/cases">Go back to the case list</a></span>
            </p>
            """;
    }

}
