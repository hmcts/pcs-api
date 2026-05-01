package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Component
@AllArgsConstructor
public class LegalRepForDefendantAccessValidator {

    private final OrganisationDetailsService organisationDetailsService;

    public List<PartyEntity> validateAndGetDefendants(PcsCaseEntity caseEntity, UUID authenticatedUserId) {
        long caseReference = caseEntity.getCaseReference();
        List<PartyEntity> defendants = extractDefendants(caseEntity, caseReference);
        String organisationId = organisationDetailsService.getOrganisationIdentifier(authenticatedUserId.toString());
        return findMatchingLinkedDefendants(defendants, authenticatedUserId, organisationId, caseReference);
    }

    private List<PartyEntity> extractDefendants(PcsCaseEntity caseEntity, long caseReference) {
        ClaimEntity mainClaim = caseEntity.getClaims().stream()
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


    private List<PartyEntity> findMatchingLinkedDefendants(
        List<PartyEntity> defendants,
        UUID authenticatedUserId,
        String organisationId,
        long caseReference
    ) {
        List<PartyEntity> linkedDefendants =  defendants
            .stream()
            .filter(party -> party.getClaimPartyLegalRepresentativeList()
                .stream()
                .anyMatch(claimPartyLegalRepresentative ->
                              claimPartyLegalRepresentative.getActive().equals(YesOrNo.YES)
                                  && isUserOrOrganisationMatch(
                                  claimPartyLegalRepresentative.getLegalRepresentative().getIdamId(),
                                  claimPartyLegalRepresentative.getLegalRepresentative().getOrganisationId(),
                                  authenticatedUserId,
                                  organisationId
                              )
                ))
            .collect(Collectors.toList());

        if (linkedDefendants.isEmpty()) {
            log.error(
                "Access denied: User {} is not linked as a defendant on case {}",
                authenticatedUserId,
                caseReference
            );
            throw new CaseAccessException("User is not linked as a defendant on this case");

        }
        return linkedDefendants;
    }

    private boolean isUserOrOrganisationMatch(UUID linkedUserId,
                                              String linkedOrganisationId,
                                              UUID authenticatedUserId,
                                              String authenticatedOrganisationId) {
        if (authenticatedUserId.equals(linkedUserId)) {
            return true;
        }

        return isNotBlank(authenticatedOrganisationId)
            && authenticatedOrganisationId.equals(linkedOrganisationId);
    }
}
