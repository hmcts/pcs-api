package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class DefendantPartyExtractor {

    public List<PartyEntity> extractDefendants(PcsCaseEntity caseEntity, long caseReference) {
        ClaimEntity mainClaim = caseEntity.getClaims().stream()
            .findFirst()
            .orElseThrow(() -> {
                log.error("No claim found for case {}", caseReference);
                return new CaseAccessException("No claim found for this case");
            });

        List<PartyEntity> defendants = extractDefendantParties(mainClaim);

        if (defendants.isEmpty()) {
            log.error("No defendants found for case {}", caseReference);
            throw new CaseAccessException("No defendants associated with this case");
        }

        return defendants;
    }

    public List<PartyEntity> summaryScreenSafeExtractDefendants(PcsCaseEntity caseEntity) {
        Optional<ClaimEntity> mainClaim = caseEntity.getClaims().stream()
            .findFirst();

        return mainClaim.map(this::extractDefendantParties).orElse(Collections.emptyList());

    }

    private List<PartyEntity> extractDefendantParties(ClaimEntity mainClaim) {
        return mainClaim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .toList();
    }
}
