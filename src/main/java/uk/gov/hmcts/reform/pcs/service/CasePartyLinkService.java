package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.model.ValidateAccessCodeResponse;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CasePartyLinkService {

    private final PcsCaseService pcsCaseService;
    private final PartyAccessCodeRepository pacRepository;

    public ValidateAccessCodeResponse validateAndLinkParty(
            long caseReference,
            String accessCode,
            UserInfo userInfo
    ) {
        log.debug("Starting validateAndLinkParty - caseReference: {}, accessCode: {}, userId: {}",
                caseReference, accessCode, userInfo.getUid());

        // Load case
        log.debug("Loading case for caseReference: {}", caseReference);
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        log.debug("Case loaded successfully - caseId: {}", caseEntity.getId());

        // Find PAC row
        log.debug("Searching for PartyAccessCode - caseId: {}, accessCode: {}", caseEntity.getId(), accessCode);
        PartyAccessCodeEntity pac = pacRepository
                .findByPcsCase_IdAndCode(caseEntity.getId(), accessCode)
                .orElseThrow(() -> {
                    log.warn("PartyAccessCode not found - caseId: {}, accessCode: {}", caseEntity.getId(), accessCode);
                    return new CaseNotFoundException(caseReference);
                });
        log.debug("PartyAccessCode found - partyId: {}, role: {}", pac.getPartyId(), pac.getRole());

        UUID partyId = pac.getPartyId();
        String currentUserUid = userInfo.getUid();
        log.debug("Matching defendant - partyId: {}, currentUserUid: {}", partyId, currentUserUid);

        List<Defendant> defendants = caseEntity.getDefendants();
        log.debug("Defendants list size: {}", defendants != null ? defendants.size() : 0);

        // Match defendant by partyId
        Defendant match = defendants.stream()
                .filter(d -> partyId.equals(d.getPartyId()))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Defendant not found for partyId: {} in caseReference: {}", partyId, caseReference);
                    return new CaseNotFoundException(caseReference);
                });
        log.debug("Defendant matched - partyId: {}, firstName: {}, lastName: {}, linkedUserId: {}",
                match.getPartyId(), match.getFirstName(), match.getLastName(), match.getLinkedUserId());

        // Conflict: already linked (regardless of which user)
        if (match.getLinkedUserId() != null) {
            log.warn("User already linked - caseReference: {}, partyId: {}, existingLinkedUserId: {}, "
                            + "attemptedUserId: {}",
                    caseReference, partyId, match.getLinkedUserId(), currentUserUid);
            throw new IllegalStateException("User already linked");
        }

        // Link user
        log.debug("Linking user - caseReference: {}, partyId: {}, userId: {}", caseReference, partyId, currentUserUid);
        match.setLinkedUserId(currentUserUid);

        // Persist updated JSON
        caseEntity.setDefendants(defendants);
        log.debug("Saving case entity with updated defendants - caseReference: {}", caseReference);
        pcsCaseService.save(caseEntity);
        log.info("Successfully linked user {} to caseReference: {}, partyId: {}",
                currentUserUid, caseReference, partyId);

        return new ValidateAccessCodeResponse(caseReference, "linked");
    }

}
