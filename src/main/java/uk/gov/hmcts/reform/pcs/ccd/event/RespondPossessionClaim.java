package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
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
    private final ModelMapper modelMapper;

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

        List<PartyEntity> defendants = pcsCaseEntity.getClaims().stream()
            .flatMap(claim -> claim.getClaimParties().stream())
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .toList();

        if (defendants.isEmpty()) {
            log.error("No defendants found for case {}", caseReference);
            throw new CaseAccessException("No defendants associated with this case");
        }

        PartyEntity matchedDefendant = defendants.stream()
            .filter(defendant -> authenticatedUserId.equals(defendant.getIdamId()))
            .findFirst()
            .orElseThrow(() -> {
                log.error("Access denied: User {} is not linked as a defendant on case {}",
                    authenticatedUserId, caseReference);
                return new CaseAccessException("User is not linked as a defendant on this case");
            });

        AddressUK contactAddress;
        if (matchedDefendant.getAddressSameAsProperty() == VerticalYesNo.YES) {
            contactAddress = pcsCaseEntity.getPropertyAddress() != null
                ? modelMapper.map(pcsCaseEntity.getPropertyAddress(), AddressUK.class)
                : null;
        } else {
            contactAddress = matchedDefendant.getAddress() != null
                ? modelMapper.map(matchedDefendant.getAddress(), AddressUK.class)
                : null;
        }

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
            caseReference, filteredDraft, EventId.respondPossessionClaim, authenticatedUserId);

        PCSCase caseDataForCcd = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        return caseDataForCcd;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        log.info("Update Draft Data for Defendant Response, Case Reference: {}", eventPayload.caseReference());

        long caseReference = eventPayload.caseReference();
        PossessionClaimResponse possessionClaimResponse = eventPayload.caseData().getPossessionClaimResponse();
        YesOrNo isFinalSubmit = eventPayload.caseData().getSubmitDraftAnswers();
        UUID userId = UUID.fromString(securityContextService.getCurrentUserDetails().getUid());

        if (possessionClaimResponse != null && isFinalSubmit != null) {
            if (isFinalSubmit.toBoolean()) {
                //find draft data using idam user and case reference and event

                //Store defendant response to database
                //This will be implemented in a future ticket.
                //Note that defendants will be stored in a list
            } else {
                // Filter to only store firstName, lastName, and address in draft
                if (possessionClaimResponse.getParty() != null) {
                    Party filteredParty = Party.builder()
                        .firstName(possessionClaimResponse.getParty().getFirstName())
                        .lastName(possessionClaimResponse.getParty().getLastName())
                        .address(possessionClaimResponse.getParty().getAddress())
                        .build();

                    PossessionClaimResponse filteredResponse = PossessionClaimResponse.builder()
                        .party(filteredParty)
                        .build();

                    PCSCase filteredDraft = PCSCase.builder()
                        .possessionClaimResponse(filteredResponse)
                        .build();

                    draftCaseDataService.patchUnsubmittedEventData(
                        caseReference, filteredDraft, respondPossessionClaim, userId);
                }
            }
        }
        return SubmitResponse.defaultResponse();
    }
}

