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

        PCSCase caseDataForCcd = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        return caseDataForCcd;
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
            if (possessionClaimResponse.getParty() != null) {
                try {
                    log.debug("Attempting to save draft for case {} with party data", caseReference);

                    PCSCase draftToSave = PCSCase.builder()
                        .possessionClaimResponse(possessionClaimResponse)
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
            } else {
                // Party is null - could be empty draft OR malformed structure
                log.error(
                    "Draft submit rejected for case {}: party is null. "
                        + "possessionClaimResponse present: {}, isFinalSubmit: {}",
                    caseReference,
                    possessionClaimResponse != null,
                    isFinalSubmit
                );

                return SubmitResponse.<State>builder()
                    .errors(List.of("We couldn't save your response. Please review your details and try again."))
                    .build();
            }
        }
        return SubmitResponse.defaultResponse();
    }
}

