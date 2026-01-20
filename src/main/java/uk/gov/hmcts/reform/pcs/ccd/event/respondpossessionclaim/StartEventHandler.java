package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimDraftService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartEventHandler implements Start<PCSCase, State> {

    private final PcsCaseService pcsCaseService;
    private final AddressMapper addressMapper;
    private final RespondPossessionClaimDraftService draftService;
    private final SecurityContextService securityContextService;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        UUID authenticatedUserId = UUID.fromString(securityContextService.getCurrentUserDetails().getUid());

        PartyEntity defendantEntity = validateAccess(caseReference, authenticatedUserId);
        PossessionClaimResponse initialResponse = buildInitialResponse(defendantEntity, caseReference);

        PCSCase caseData = eventPayload.caseData();
        caseData.setPossessionClaimResponse(initialResponse);

        return getOrInitializeDraft(caseReference, initialResponse);
    }

    private PartyEntity validateAccess(long caseReference, UUID authenticatedUserId) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        List<PartyEntity> defendants = extractDefendants(pcsCaseEntity, caseReference);
        return findMatchingDefendant(defendants, authenticatedUserId, caseReference);
    }

    private List<PartyEntity> extractDefendants(PcsCaseEntity pcsCaseEntity, long caseReference) {
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().stream()
            .findFirst()
            .orElseThrow(() -> {
                log.error("No claim found for case {}", caseReference);
                return new CaseAccessException("No claim found for this case");
            });

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

    private PartyEntity findMatchingDefendant(
            List<PartyEntity> defendants,
            UUID authenticatedUserId,
            long caseReference) {
        return defendants.stream()
            .filter(defendant -> authenticatedUserId.equals(defendant.getIdamId()))
            .findFirst()
            .orElseThrow(() -> {
                log.error("Access denied: User {} is not linked as a defendant on case {}",
                    authenticatedUserId, caseReference);
                return new CaseAccessException("User is not linked as a defendant on this case");
            });
    }

    private PossessionClaimResponse buildInitialResponse(PartyEntity defendantEntity, long caseReference) {
        AddressUK contactAddress = resolveAddress(defendantEntity, caseReference);
        Party party = buildParty(defendantEntity, contactAddress);

        return PossessionClaimResponse.builder()
            .party(party)
            .build();
    }

    private AddressUK resolveAddress(PartyEntity defendantEntity, long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        if (defendantEntity.getAddressSameAsProperty() != null
            && defendantEntity.getAddressSameAsProperty() == VerticalYesNo.YES) {
            return addressMapper.toAddressUK(pcsCaseEntity.getPropertyAddress());
        } else {
            return addressMapper.toAddressUK(defendantEntity.getAddress());
        }
    }

    private Party buildParty(PartyEntity defendantEntity, AddressUK contactAddress) {
        return Party.builder()
            .firstName(defendantEntity.getFirstName())
            .lastName(defendantEntity.getLastName())
            .address(contactAddress)
            .build();
    }

    private PCSCase getOrInitializeDraft(long caseReference, PossessionClaimResponse initialResponse) {
        if (draftService.exists(caseReference)) {
            return draftService.load(caseReference);
        }
        return draftService.initialize(caseReference, initialResponse);
    }
}
