package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@Slf4j
@RequiredArgsConstructor
public class RespondPossessionClaim implements CCDConfig<PCSCase, State, UserRole> {
    private final DraftCaseDataService draftCaseDataService;
    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final AddressMapper addressMapper;

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(respondPossessionClaim.name(), this::submit, this::start)
            // TODO: HDPI-3580 - Revert to .forState(State.CASE_ISSUED) once payments flow is implemented
            // Temporarily enabled for all states to allow testing before case submission/payment
            .forAllStates()
            .showCondition(ShowConditions.NEVER_SHOW)
            .name("Defendant Response Submission")
            .description("Save defendants response as draft or to a case based on flag")
            .grant(Permission.CRU, UserRole.DEFENDANT);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        UUID authenticatedUserId = UUID.fromString(securityContextService.getCurrentUserDetails().getUid());

        // Find and validate that authenticated user is a defendant on this case
        PartyEntity defendantEntity = findAuthenticatedDefendant(caseReference, authenticatedUserId);

        // Build initial response from defendant's database record
        PossessionClaimResponse initialResponse = buildInitialResponseFromDatabase(defendantEntity, caseReference);

        // Set response on eventPayload caseData (required by CCD framework)
        PCSCase caseData = eventPayload.caseData();
        caseData.setPossessionClaimResponse(initialResponse);

        // Handle draft: initialize on first START or return existing draft to preserve user progress
        return getOrInitializeDraft(caseReference, initialResponse);
    }

    /**
     * Finds and validates that the authenticated user is a defendant on the specified case.
     *
     * @param caseReference The case reference number
     * @param authenticatedUserId The authenticated user's IDAM ID
     * @return The PartyEntity for the authenticated defendant
     * @throws CaseAccessException if user is not a defendant on this case
     */
    private PartyEntity findAuthenticatedDefendant(long caseReference, UUID authenticatedUserId) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        List<PartyEntity> defendants = getDefendantsFromMainClaim(pcsCaseEntity, caseReference);
        return findDefendantByUserId(defendants, authenticatedUserId, caseReference);
    }

    /**
     * Retrieves all defendants from the main claim of the case.
     * Counter-claims are not yet implemented - when added, they will be additional claims in the list.
     *
     * @param pcsCaseEntity The case entity loaded from database
     * @param caseReference The case reference number (for error logging)
     * @return List of all defendant PartyEntity objects from the main claim
     * @throws CaseAccessException if no claim found or no defendants exist
     */
    private List<PartyEntity> getDefendantsFromMainClaim(PcsCaseEntity pcsCaseEntity, long caseReference) {
        // Get the main claim (first claim in the list)
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().stream()
            .findFirst()
            .orElseThrow(() -> {
                log.error("No claim found for case {}", caseReference);
                return new CaseAccessException("No claim found for this case");
            });

        // Get all defendants from the main claim only
        List<PartyEntity> defendants = mainClaim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .toList();

        if (defendants.isEmpty()) {
            log.error("No defendants found for case {}", caseReference);
            throw new CaseAccessException("No defendants associated with this case");
        }

        return defendants;
    }

    /**
     * Finds the defendant that matches the authenticated user's IDAM ID.
     *
     * @param defendants List of all defendants on the case
     * @param authenticatedUserId The authenticated user's IDAM ID
     * @param caseReference The case reference number (for error logging)
     * @return The PartyEntity matching the authenticated user
     * @throws CaseAccessException if no matching defendant found (authorization failure)
     */
    private PartyEntity findDefendantByUserId(
            List<PartyEntity> defendants, UUID authenticatedUserId, long caseReference) {
        return defendants.stream()
            .filter(defendant -> authenticatedUserId.equals(defendant.getIdamId()))
            .findFirst()
            .orElseThrow(() -> {
                log.error("Access denied: User {} is not linked as a defendant on case {}",
                    authenticatedUserId, caseReference);
                return new CaseAccessException("User is not linked as a defendant on this case");
            });
    }

    /**
     * Builds the initial PossessionClaimResponse from the defendant's database record.
     * Creates the base structure with defendant details and resolved address.
     *
     * @param defendantEntity The defendant's party entity from database
     * @param caseReference The case reference number
     * @return Initial PossessionClaimResponse populated from database
     */
    private PossessionClaimResponse buildInitialResponseFromDatabase(PartyEntity defendantEntity, long caseReference) {
        AddressUK contactAddress = resolveDefendantAddress(defendantEntity, caseReference);
        Party party = buildPartyFromDefendant(defendantEntity, contactAddress);

        return PossessionClaimResponse.builder()
            .party(party)
            .build();
    }

    /**
     * Resolves the defendant's contact address.
     * If the defendant's address is the same as the property, uses the property address.
     * Otherwise, uses the defendant's own address.
     *
     * <p>
     * AddressMapper ensures all fields are explicitly set (including nulls) for consistent
     * JSON structure required by CCD event token validation.
     *
     * @param defendantEntity The defendant's party entity
     * @param caseReference The case reference number
     * @return The resolved AddressUK
     */
    private AddressUK resolveDefendantAddress(PartyEntity defendantEntity, long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        if (defendantEntity.getAddressSameAsProperty() != null
            && defendantEntity.getAddressSameAsProperty() == VerticalYesNo.YES) {
            return addressMapper.toAddressUK(pcsCaseEntity.getPropertyAddress());
        } else {
            return addressMapper.toAddressUK(defendantEntity.getAddress());
        }
    }

    /**
     * Builds a Party object from the defendant entity.
     *
     * <p>
     * IMPORTANT: Always creates a Party object (even with null fields) to maintain consistent
     * structure for CCD event token validation. If party is null, CCD omits the field entirely,
     * causing "Cannot find matching start trigger" errors when user later submits with populated
     * party data (the field appears to be "added").
     *
     * @param defendantEntity The defendant's party entity
     * @param contactAddress The resolved contact address
     * @return Party object with firstName, lastName, and address
     */
    private Party buildPartyFromDefendant(PartyEntity defendantEntity, AddressUK contactAddress) {
        return Party.builder()
            .firstName(defendantEntity.getFirstName())
            .lastName(defendantEntity.getLastName())
            .address(contactAddress)
            .build();
    }

    /**
     * Handles draft logic: initializes draft on first START, returns existing draft on subsequent STARTs.
     * This ensures user progress is preserved when they request new CCD event tokens.
     *
     * @param caseReference The case reference number
     * @param initialResponse The initial response built from database
     * @return PCSCase with either initial data (first time) or draft data (returning user)
     */
    private PCSCase getOrInitializeDraft(long caseReference, PossessionClaimResponse initialResponse) {
        boolean draftExists = draftCaseDataService.hasUnsubmittedCaseData(caseReference, respondPossessionClaim);

        if (!draftExists) {
            return initializeDraft(caseReference, initialResponse);
        } else {
            return getExistingDraft(caseReference);
        }
    }

    /**
     * Initializes a new draft on first START call.
     * Saves the initial response to draft store and returns it to CCD.
     *
     * @param caseReference The case reference number
     * @param initialResponse The initial response built from database
     * @return PCSCase with initial data and submitDraftAnswers=NO
     */
    private PCSCase initializeDraft(long caseReference, PossessionClaimResponse initialResponse) {
        PCSCase filteredDraft = PCSCase.builder()
            .possessionClaimResponse(initialResponse)
            .build();

        draftCaseDataService.patchUnsubmittedEventData(
            caseReference, filteredDraft, respondPossessionClaim);

        log.info("Draft seeded for case {} and event {} from defendant data in database",
            caseReference, respondPossessionClaim);

        return PCSCase.builder()
            .possessionClaimResponse(initialResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();
    }

    /**
     * Returns existing draft data to preserve user progress.
     * Called on subsequent START calls when user requests new event token.
     *
     * @param caseReference The case reference number
     * @return PCSCase with existing draft data and submitDraftAnswers=NO
     * @throws IllegalStateException if draft should exist but is not found
     */
    private PCSCase getExistingDraft(long caseReference) {
        log.info("Draft already exists for case {} and event {} - returning draft data to preserve user progress",
            caseReference, respondPossessionClaim);

        PCSCase draftData = draftCaseDataService.getUnsubmittedCaseData(
            caseReference, respondPossessionClaim)
            .orElseThrow(() -> new IllegalStateException(
                "Draft should exist but was not found for case " + caseReference));

        return PCSCase.builder()
            .possessionClaimResponse(draftData.getPossessionClaimResponse())
            .submitDraftAnswers(YesOrNo.NO)
            .build();
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PossessionClaimResponse possessionClaimResponse = eventPayload.caseData().getPossessionClaimResponse();
        YesOrNo isFinalSubmit = eventPayload.caseData().getSubmitDraftAnswers();

        // Validate required fields
        if (possessionClaimResponse == null) {
            log.error("Submit failed for case {}: possessionClaimResponse is null", caseReference);
            return SubmitResponse.<State>builder()
                .errors(List.of("Invalid submission: missing response data"))
                .build();
        }

        if (isFinalSubmit == null) {
            log.error("Submit failed for case {}: submitDraftAnswers is null", caseReference);
            return SubmitResponse.<State>builder()
                .errors(List.of("Invalid submission: missing submit flag"))
                .build();
        }

        if (isFinalSubmit.toBoolean()) {
            //TODO: find draft data using idam user and case reference and event

            //TODO: Store defendant response to database
            //This will be implemented in a future ticket.
            //Note that defendants will be stored in a list
        } else {
            // Draft submission
            if (possessionClaimResponse.getParty() == null) {
                log.error("Draft submit rejected for case {}: party is null", caseReference);
                return SubmitResponse.<State>builder()
                    .errors(List.of("Invalid response structure. Please refresh the page and try again."))
                    .build();
            }

            try {
                // HDPI-3509: draftCaseDataObjectMapper (configured in JacksonConfiguration) uses mix-ins
                // to override Party's @JsonInclude(ALWAYS) annotation, ensuring null fields are omitted
                // during draft persistence to prevent overwriting existing data
                PCSCase draftToSave = PCSCase.builder()
                    .possessionClaimResponse(possessionClaimResponse)
                    .submitDraftAnswers(isFinalSubmit)
                    .build();

                draftCaseDataService.patchUnsubmittedEventData(
                    caseReference, draftToSave, respondPossessionClaim);

                log.debug("Draft saved successfully for case {}", caseReference);
            } catch (Exception e) {
                log.error("Failed to save draft for case {}", caseReference, e);
                return SubmitResponse.<State>builder()
                    .errors(List.of("We couldn't save your response. Please try again or contact support."))
                    .build();
            }
        }

        return SubmitResponse.defaultResponse();
    }
}

