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
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.draft.patch.AddressUKDraftPatch;
import uk.gov.hmcts.reform.pcs.ccd.domain.draft.patch.PartyDraftPatch;
import uk.gov.hmcts.reform.pcs.ccd.domain.draft.patch.PcsDraftPatch;
import uk.gov.hmcts.reform.pcs.ccd.domain.draft.patch.PossessionClaimResponseDraftPatch;
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

        UserInfo userInfo = securityContextService.getCurrentUserDetails();
        UUID authenticatedUserId = UUID.fromString(userInfo.getUid());
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        // Get the main claim (first claim in the list)
        // Counter-claims will be on the same case reference but are not yet implemented
        // When counter-claims are added, they will be additional claims in the list
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

        // Find the specific defendant who is currently authenticated
        PartyEntity matchedDefendant = defendants.stream()
            .filter(defendant -> authenticatedUserId.equals(defendant.getIdamId()))
            .findFirst()
            .orElseThrow(() -> {
                log.error("Access denied: User {} is not linked as a defendant on case {}",
                    authenticatedUserId, caseReference);
                return new CaseAccessException("User is not linked as a defendant on this case");
            });

        // Map address using AddressMapper to ensure all fields are explicitly set (including null values)
        // This ensures consistent JSON structure for CCD event token validation
        AddressUK contactAddress;
        if (matchedDefendant.getAddressSameAsProperty() != null
            && matchedDefendant.getAddressSameAsProperty() == VerticalYesNo.YES) {
            contactAddress = addressMapper.toAddressUK(pcsCaseEntity.getPropertyAddress());
        } else {
            contactAddress = addressMapper.toAddressUK(matchedDefendant.getAddress());
        }

        // Ensure address is never null - always create with all fields for CCD token validation
        // If AddressMapper returns null, create an empty AddressUK with all fields explicitly null
        if (contactAddress == null) {
            contactAddress = AddressUK.builder()
                .addressLine1(null)
                .addressLine2(null)
                .addressLine3(null)
                .postTown(null)
                .county(null)
                .postCode(null)
                .country(null)
                .build();
        }

        // Always create Party object to maintain consistent structure for CCD event token validation
        // The party field must exist in the response structure (even with null field values)
        // If party is null, CCD omits the field entirely, causing "Cannot find matching start trigger"
        // errors when user later submits with populated party data (field appears to be "added")
        Party party = Party.builder()
            .firstName(matchedDefendant.getFirstName())
            .lastName(matchedDefendant.getLastName())
            .address(contactAddress)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .party(party)
            .build();

        PCSCase caseData = eventPayload.caseData();
        caseData.setPossessionClaimResponse(possessionClaimResponse);

        PCSCase filteredDraft = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .build();

        draftCaseDataService.patchUnsubmittedEventData(
            caseReference, filteredDraft, EventId.respondPossessionClaim);

        return PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
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
            //find draft data using idam user and case reference and event

            //Store defendant response to database
            //This will be implemented in a future ticket.
            //Note that defendants will be stored in a list
        } else {
            // ============================================================================
            // DRAFT SUBMISSION - Patch DTO approach to prevent null field overwrites
            // ============================================================================
            // Logic explanation:
            // 1. Incoming domain object (Party) has @JsonInclude(ALWAYS) for CCD token validation
            //    → All fields serialize including nulls → Would overwrite existing draft data
            // 2. Solution: Map to patch DTOs with @JsonInclude(NON_NULL)
            //    → Only non-null fields serialize → Jackson merge preserves existing values
            // 3. Flow: Party → PartyDraftPatch → JSON (nulls omitted) → Merge → Draft updated
            // ============================================================================

            log.info("Draft submission for case {}: Validating and building patch DTO", caseReference);

            // Validation 1: Ensure party object exists
            if (possessionClaimResponse.getParty() == null) {
                log.error(
                    "Draft submit rejected for case {}: party is null. "
                        + "possessionClaimResponse present: {}, isFinalSubmit: {}",
                    caseReference,
                    possessionClaimResponse != null,
                    isFinalSubmit
                );

                return SubmitResponse.<State>builder()
                    .errors(List.of("Invalid response structure. Please refresh the page and try again."))
                    .build();
            }

            Party incomingParty = possessionClaimResponse.getParty();

            // Debug: Log incoming party data
            log.debug("Incoming party data for case {}: firstName='{}', lastName='{}', "
                    + "emailAddress='{}', phoneNumber='{}', orgName='{}', "
                    + "nameKnown={}, addressKnown={}, addressSameAsProperty={}, address={}",
                caseReference,
                incomingParty.getFirstName(),
                incomingParty.getLastName(),
                incomingParty.getEmailAddress(),
                incomingParty.getPhoneNumber(),
                incomingParty.getOrgName(),
                incomingParty.getNameKnown(),
                incomingParty.getAddressKnown(),
                incomingParty.getAddressSameAsProperty(),
                incomingParty.getAddress() != null ? "present" : "null"
            );

            if (incomingParty.getAddress() != null) {
                AddressUK address = incomingParty.getAddress();
                log.debug("Incoming address for case {}: AddressLine1='{}', PostTown='{}', "
                        + "PostCode='{}', Country='{}'",
                    caseReference,
                    address.getAddressLine1(),
                    address.getPostTown(),
                    address.getPostCode(),
                    address.getCountry()
                );
            }

            // Build patch DTO with NON_NULL to omit null fields from JSON
            // This enables PATCH semantics: only non-null fields update the draft
            try {
                log.debug("Building draft patch DTO for case {}: mapping Party → PartyDraftPatch", caseReference);

                PartyDraftPatch partyPatch = toPartyDraftPatch(incomingParty);

                log.debug("PartyDraftPatch created for case {}: firstName='{}', lastName='{}', "
                        + "emailAddress='{}', phoneNumber='{}', address={}",
                    caseReference,
                    partyPatch.getFirstName(),
                    partyPatch.getLastName(),
                    partyPatch.getEmailAddress(),
                    partyPatch.getPhoneNumber(),
                    partyPatch.getAddress() != null ? "present" : "null"
                );

                PossessionClaimResponseDraftPatch responsePatch = PossessionClaimResponseDraftPatch.builder()
                    .contactByPhone(possessionClaimResponse.getContactByPhone())
                    .party(partyPatch)
                    .build();

                log.debug("PossessionClaimResponseDraftPatch created for case {}: contactByPhone={}",
                    caseReference, responsePatch.getContactByPhone());

                PcsDraftPatch draftPatch = PcsDraftPatch.builder()
                    .submitDraftAnswers(isFinalSubmit)
                    .possessionClaimResponse(responsePatch)
                    .build();

                log.info("Persisting draft patch for case {}: Patch DTO will be serialized to JSON "
                        + "(nulls omitted due to @JsonInclude(NON_NULL)), then merged with existing draft",
                    caseReference);

                draftCaseDataService.patchUnsubmittedEventData(
                    caseReference, draftPatch, respondPossessionClaim);

                log.info("Draft saved successfully for case {}: Patch applied, existing values preserved "
                        + "for any fields that were null in incoming request", caseReference);
            } catch (Exception e) {
                log.error("Failed to save draft for case {}: Exception during patch DTO persistence",
                    caseReference, e);
                return SubmitResponse.<State>builder()
                    .errors(List.of("We couldn't save your response. Please try again or contact support."))
                    .build();
            }
        }
        return SubmitResponse.defaultResponse();
    }

    /**
     * Maps Party domain object to PartyDraftPatch DTO.
     * Copies all fields directly - NON_NULL annotation on DTO ensures nulls are omitted from JSON.
     *
     * @param party the party from the incoming request
     * @return PartyDraftPatch with all fields copied from party
     */
    private PartyDraftPatch toPartyDraftPatch(Party party) {
        if (party == null) {
            return null;
        }

        return PartyDraftPatch.builder()
            .firstName(party.getFirstName())
            .lastName(party.getLastName())
            .orgName(party.getOrgName())
            .nameKnown(party.getNameKnown())
            .emailAddress(party.getEmailAddress())
            .address(toAddressUKDraftPatch(party.getAddress()))
            .addressKnown(party.getAddressKnown())
            .addressSameAsProperty(party.getAddressSameAsProperty())
            .phoneNumber(party.getPhoneNumber())
            .build();
    }

    /**
     * Maps AddressUK domain object to AddressUKDraftPatch DTO.
     * Copies all fields directly - NON_NULL annotation on DTO ensures nulls are omitted from JSON.
     *
     * @param address the address from the incoming request
     * @return AddressUKDraftPatch with all fields copied from address
     */
    private AddressUKDraftPatch toAddressUKDraftPatch(AddressUK address) {
        if (address == null) {
            return null;
        }

        return AddressUKDraftPatch.builder()
            .addressLine1(address.getAddressLine1())
            .addressLine2(address.getAddressLine2())
            .addressLine3(address.getAddressLine3())
            .postTown(address.getPostTown())
            .county(address.getCounty())
            .postCode(address.getPostCode())
            .country(address.getCountry())
            .build();
    }
}

