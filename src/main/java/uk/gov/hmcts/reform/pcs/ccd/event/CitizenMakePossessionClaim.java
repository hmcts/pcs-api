package uk.gov.hmcts.reform.pcs.ccd.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaimSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.RespondToClaimCallbackError;
import uk.gov.hmcts.reform.pcs.ccd.page.citizenmakepossessionclaim.CitizenMakePossessionClaimDraftSavePage;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenClaimPaymentUnavailableException;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimSubmitService;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimSubmitService.SubmittedClaim;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimValidator;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenCreatePossessionClaim.SERVICE_NOT_AVAILABLE;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.citizenMakePossessionClaim;

/**
 * Draft saves (mid-event on the single page) and final submit for a citizen private-landlord claim.
 * As for respondPossessionClaim, the submit always reads the saved draft.
 */
@Slf4j
@Component
@AllArgsConstructor
public class CitizenMakePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    public static final String CASE_ACCESS_DENIED = "CASE_ACCESS_DENIED";
    public static final String DRAFT_NOT_FOUND = "DRAFT_NOT_FOUND";
    public static final String DRAFT_CHANGED = RespondToClaimCallbackError.DRAFT_CHANGED;
    public static final String PAYMENT_SERVICE_UNAVAILABLE = "PAYMENT_SERVICE_UNAVAILABLE";

    private final FeatureToggleService featureToggleService;
    private final CitizenPossessionClaimCaseService citizenPossessionClaimCaseService;
    private final CitizenPossessionClaimValidator citizenPossessionClaimValidator;
    private final CitizenPossessionClaimSubmitService citizenPossessionClaimSubmitService;
    private final CitizenMakePossessionClaimDraftSavePage draftSavePage;
    private final ObjectMapper objectMapper;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(citizenMakePossessionClaim.name(), this::submit)
            .forStates(State.AWAITING_SUBMISSION_TO_HMCTS)
            .name("Citizen make a claim")
            .description("Save a citizen private landlord claim as a draft, or submit it")
            .showCondition(ShowConditions.NEVER_SHOW)
            .aboutToStartCallback(this::aboutToStart)
            .grant(Permission.CRU, UserRole.CREATOR)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES);

        new PageBuilder(eventBuilder)
            .add(draftSavePage);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> aboutToStart(CaseDetails<PCSCase, State> details) {
        if (!featureEnabled()) {
            return startError(SERVICE_NOT_AVAILABLE);
        }
        PcsCaseEntity pcsCaseEntity = citizenPossessionClaimCaseService.loadCase(details.getId());
        if (!citizenPossessionClaimCaseService.isCurrentUserClaimCreator(pcsCaseEntity)) {
            return startError(CASE_ACCESS_DENIED);
        }

        PCSCase caseData = details.getData();
        caseData.setCitizenPossessionClaim(citizenPossessionClaimCaseService.loadOrInitialiseDraft(pcsCaseEntity));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();

        if (!featureEnabled()) {
            return refuse(List.of(SERVICE_NOT_AVAILABLE));
        }
        PcsCaseEntity pcsCaseEntity = citizenPossessionClaimCaseService.loadCase(caseReference);
        if (!citizenPossessionClaimCaseService.isCurrentUserClaimCreator(pcsCaseEntity)) {
            return refuse(List.of(CASE_ACCESS_DENIED));
        }

        Optional<CitizenPossessionClaim> draft = citizenPossessionClaimCaseService.loadDraft(caseReference);
        if (draft.isEmpty()) {
            return refuse(List.of(DRAFT_NOT_FOUND));
        }

        CitizenPossessionClaim claim = draft.get();
        if (reviewedVersion(eventPayload).filter(version -> version.equals(claim.getDraftVersion())).isEmpty()) {
            log.warn("Refusing citizen claim submit for case {}: reviewed draft version {} but stored draft is at {}",
                     caseReference, reviewedVersion(eventPayload).orElse(null), claim.getDraftVersion());
            return refuse(List.of(DRAFT_CHANGED));
        }

        List<String> errors = new ArrayList<>(citizenPossessionClaimValidator.validateForSubmit(claim));
        if (pcsCaseEntity.getLegislativeCountry() != LegislativeCountry.ENGLAND) {
            errors.add(CitizenPossessionClaimCaseService.PROPERTY_NOT_IN_ENGLAND);
        }
        if (!errors.isEmpty()) {
            log.info("Refusing citizen claim submit for case {}: {}", caseReference, errors);
            return refuse(errors);
        }

        SubmittedClaim submitted;
        try {
            submitted = citizenPossessionClaimSubmitService.submit(pcsCaseEntity, claim);
        } catch (CitizenClaimPaymentUnavailableException e) {
            log.error("Refusing citizen claim submit for case {}: Payments unavailable", caseReference, e);
            return refuse(List.of(PAYMENT_SERVICE_UNAVAILABLE));
        }
        citizenPossessionClaimCaseService.deleteDraft(caseReference);

        return SubmitResponse.<State>builder()
            .confirmationBody(confirmationBody(submitted))
            .state(State.PENDING_CASE_ISSUED)
            .build();
    }

    private static Optional<Long> reviewedVersion(EventPayload<PCSCase, State> eventPayload) {
        return Optional.ofNullable(eventPayload.caseData())
            .map(PCSCase::getCitizenPossessionClaim)
            .map(CitizenPossessionClaim::getDraftVersion);
    }

    private boolean featureEnabled() {
        return featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM);
    }

    private String confirmationBody(SubmittedClaim submitted) {
        CitizenPossessionClaimSubmitResponse response = CitizenPossessionClaimSubmitResponse.builder()
            .serviceRequestReference(submitted.serviceRequestReference())
            .feeAmount(submitted.feeDetails().getFeeAmount().movePointRight(2).toBigInteger().toString())
            .build();
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialise citizen claim submit response", e);
        }
    }

    private static AboutToStartOrSubmitResponse<PCSCase, State> startError(String error) {
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errors(List.of(error))
            .build();
    }

    private static SubmitResponse<State> refuse(List<String> errors) {
        return SubmitResponse.<State>builder().errors(errors).build();
    }

}
