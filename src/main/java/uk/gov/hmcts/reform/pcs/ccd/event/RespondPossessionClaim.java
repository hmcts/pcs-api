package uk.gov.hmcts.reform.pcs.ccd.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
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
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;
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

    /**
     * ObjectMapper for draft serialization that omits null fields.
     * Uses mix-ins to override Party's @JsonInclude(ALWAYS) annotation,
     * preventing null values from overwriting existing draft data during merge.
     * Lazy-initialized to avoid overhead when not needed.
     */
    private ObjectMapper draftSerializer;

    /**
     * Mix-in interface to override Party's @JsonInclude(ALWAYS) during draft serialization.
     * This allows null fields to be omitted from JSON, enabling true PATCH semantics.
     * Party retains @JsonInclude(ALWAYS) for CCD token validation in START callback.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private interface DraftPartyMixIn {}

    /**
     * Mix-in interface to override AddressUK serialization during draft persistence.
     * Ensures null address fields are omitted from JSON to prevent overwriting existing data.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private interface DraftAddressMixIn {}

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

    /**
     * Gets or creates the ObjectMapper for draft serialization.
     * This mapper uses mix-ins to override Party's @JsonInclude(ALWAYS) annotation,
     * allowing null fields to be omitted during draft persistence while preserving
     * the ALWAYS behavior for CCD token validation in the START callback.
     *
     * @return ObjectMapper configured to omit null fields for draft persistence
     */
    private ObjectMapper getDraftSerializer() {
        if (draftSerializer == null) {
            draftSerializer = new ObjectMapper();
            draftSerializer.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            draftSerializer.registerModules(
                new Jdk8Module(),
                new JavaTimeModule(),
                new ParameterNamesModule()
            );

            // Mix-ins override class-level @JsonInclude annotations
            // This allows Party to keep @JsonInclude(ALWAYS) for CCD token validation
            // while using NON_NULL for draft persistence to prevent null overwrites
            draftSerializer.addMixIn(Party.class, DraftPartyMixIn.class);
            draftSerializer.addMixIn(AddressUK.class, DraftAddressMixIn.class);

            log.debug("Initialized draft serializer with NON_NULL mix-ins for Party and AddressUK");
        }
        return draftSerializer;
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

        // Seed the draft only once. Subsequent START calls (new tokens) must not overwrite user progress.
        if (!draftCaseDataService.hasUnsubmittedCaseData(caseReference, respondPossessionClaim)) {
            PCSCase filteredDraft = PCSCase.builder()
                .possessionClaimResponse(possessionClaimResponse)
                .build();

            draftCaseDataService.patchUnsubmittedEventData(
                caseReference, filteredDraft, respondPossessionClaim);

            log.info("Draft seeded for case {} and event {} from defendant data in database",
                caseReference, respondPossessionClaim);
        } else {
            log.info("Draft already exists for case {} and event {} - skipping draft seed to preserve user progress",
                caseReference, respondPossessionClaim);
        }

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
                log.debug("Saving draft for case {} with null-omitting serialization", caseReference);

                PCSCase draftToSave = PCSCase.builder()
                    .possessionClaimResponse(possessionClaimResponse)
                    .build();

                // Serialize with custom mapper that omits nulls (via mix-in override)
                // then deserialize back to PCSCase to pass to service
                ObjectMapper mapper = getDraftSerializer();
                String sanitizedJson = mapper.writeValueAsString(draftToSave);
                PCSCase sanitizedDraft = mapper.readValue(sanitizedJson, PCSCase.class);

                draftCaseDataService.patchUnsubmittedEventData(
                    caseReference, sanitizedDraft, respondPossessionClaim);

                log.debug("Draft saved successfully for case {} (nulls omitted via mix-in)", caseReference);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize/deserialize draft for case {}", caseReference, e);
                throw new UnsubmittedDataException("Failed to prepare draft data", e);
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

