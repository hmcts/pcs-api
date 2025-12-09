package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.AccessCodeAlreadyUsedException;
import uk.gov.hmcts.reform.pcs.exception.InvalidAccessCodeException;
import uk.gov.hmcts.reform.pcs.exception.InvalidPartyForCaseException;
import uk.gov.hmcts.reform.pcs.model.ValidateAccessCodeResponse;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasePartyLinkService {

    private final PcsCaseService pcsCaseService;
    private final PartyAccessCodeRepository pacRepository;

    public ValidateAccessCodeResponse validateAndLinkParty(
            long caseReference,
            String accessCode,
            UserInfo userInfo
    ) {
        String userId = userInfo.getUid();
        UUID idamUserId = UUID.fromString(userId);

        // 1) Load case
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        // 2) Validate access code (must belong to this case)
        PartyAccessCodeEntity pac = pacRepository
                .findByPcsCase_IdAndCode(caseEntity.getId(), accessCode)
                .orElseThrow(() -> {
                    return new InvalidAccessCodeException("Invalid access code for this case.");
                });

        UUID partyId = pac.getPartyId();
        // 3) Find matching defendant by partyId
        List<Defendant> defendants = caseEntity.getDefendants();
        Defendant defendant = defendants.stream()
                .filter(d -> partyId.equals(d.getPartyId()))
                .findFirst()
                .orElseThrow(() -> {
                    return new InvalidPartyForCaseException("Party does not belong to this case.");
                });

        // 4) Prevent re-linking this same defendant
        if (defendant.getIdamUserId() != null) {
            throw new AccessCodeAlreadyUsedException("This access code is already linked to a user.");
        }

        // 4a) Prevent linking same user ID to multiple defendants in the same case
        boolean userIdAlreadyLinked = defendants.stream()
                .filter(d -> !d.getPartyId().equals(partyId)) // Exclude the current defendant
                .anyMatch(d -> idamUserId.equals(d.getIdamUserId()));

        if (userIdAlreadyLinked) {
            throw new AccessCodeAlreadyUsedException(
                    "This user ID is already linked to another defendant in this case.");
        }

        // 5) Link user to party
        defendant.setIdamUserId(idamUserId);

        caseEntity.setDefendants(defendants);
        pcsCaseService.save(caseEntity);

        return new ValidateAccessCodeResponse(caseReference, "linked");
    }

}
