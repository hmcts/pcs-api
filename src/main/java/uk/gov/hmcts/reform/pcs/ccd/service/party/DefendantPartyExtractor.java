package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.exception.ErrorCode;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.DEFENDANT_PARTY_EXTRACTOR;
import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.DEFENDANT_PARTY_EXTRACTOR_NO_DEFENDANTS;

@Component
@Slf4j
public class DefendantPartyExtractor {

    public List<PartyEntity> extractDefendants(PcsCaseEntity caseEntity, long caseReference) {
        ClaimEntity mainClaim = caseEntity.getClaims().stream()
            .findFirst()
            .orElseThrow(() -> {
                log.error("No claim found for case {}", caseReference);
                return new CaseAccessException(DEFENDANT_PARTY_EXTRACTOR);
            });

        List<PartyEntity> defendants = mainClaim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .toList();

        if (defendants.isEmpty()) {
            log.error("No defendants found for case {}", caseReference);
            throw new CaseAccessException(DEFENDANT_PARTY_EXTRACTOR_NO_DEFENDANTS);
        }

        return defendants;
    }
}
