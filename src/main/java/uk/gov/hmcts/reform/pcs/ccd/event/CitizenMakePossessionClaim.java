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
import uk.gov.hmcts.reform.pcs.ccd.page.citizenmakepossessionclaim.CitizenMakePossessionClaimDraftSavePage;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimSubmitService;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimSubmitService.SubmittedClaim;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimValidator;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private final FeatureToggleService featureToggleService;
    private final CitizenPossessionClaimCaseService citizenPossessionClaimCaseService;
    private final CitizenPossessionClaimValidator citizenPossessionClaimValidator;
    private final CitizenPossessionClaimSubmitService citizenPossessionClaimSubmitService;
    private final CitizenMakePossessionClaimDraftSavePage draftSavePage;
    private final PcsCaseService pcsCaseService;
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
        Optional<String> refusal = refusalFor(details.getId());
        if (refusal.isPresent()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(List.of(refusal.get()))
                .build();
        }

        PCSCase caseData = details.getData();
        caseData.setCitizenPossessionClaim(
            citizenPossessionClaimCaseService.loadOrInitialiseDraft(details.getId()));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();

        Optional<String> refusal = refusalFor(caseReference);
        if (refusal.isPresent()) {
            return refuse(List.of(refusal.get()));
        }

        Optional<CitizenPossessionClaim> draft = citizenPossessionClaimCaseService.loadDraft(caseReference);
        if (draft.isEmpty()) {
            return refuse(List.of(DRAFT_NOT_FOUND));
        }

        CitizenPossessionClaim claim = draft.get();
        List<String> errors = new ArrayList<>(citizenPossessionClaimValidator.validateForSubmit(claim));
        if (pcsCaseService.loadCase(caseReference).getLegislativeCountry() != LegislativeCountry.ENGLAND) {
            errors.add(CitizenPossessionClaimCaseService.PROPERTY_NOT_IN_ENGLAND);
        }
        if (!errors.isEmpty()) {
            log.info("Refusing citizen claim submit for case {}: {}", caseReference, errors);
            return refuse(errors);
        }

        SubmittedClaim submitted = citizenPossessionClaimSubmitService.submit(caseReference, claim);
        citizenPossessionClaimCaseService.deleteDraft(caseReference);

        return SubmitResponse.<State>builder()
            .confirmationBody(confirmationBody(submitted))
            .state(State.PENDING_CASE_ISSUED)
            .build();
    }

    private Optional<String> refusalFor(long caseReference) {
        if (!featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)) {
            return Optional.of(SERVICE_NOT_AVAILABLE);
        }
        if (!citizenPossessionClaimCaseService.isCurrentUserClaimCreator(caseReference)) {
            return Optional.of(CASE_ACCESS_DENIED);
        }
        return Optional.empty();
    }

    private String confirmationBody(SubmittedClaim submitted) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("serviceRequestReference", submitted.serviceRequestReference());
        body.put("feeAmount", submitted.feeDetails().getFeeAmount().movePointRight(2).toBigInteger().toString());
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialise citizen claim submit response", e);
        }
    }

    private static SubmitResponse<State> refuse(List<String> errors) {
        return SubmitResponse.<State>builder().errors(errors).build();
    }

}
