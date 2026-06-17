package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Component
@AllArgsConstructor
public class LegalRepForDefendantAccessValidator {

    private final OrganisationDetailsService organisationDetailsService;
    private final DefendantPartyExtractor defendantPartyExtractor;
    private final DefendantResponseRepository defendantResponseRepository;

    public List<PartyEntity> validateAndGetDefendants(PcsCaseEntity caseEntity, UUID authenticatedUserId) {
        long caseReference = caseEntity.getCaseReference();
        List<PartyEntity> defendants = defendantPartyExtractor.extractDefendants(caseEntity, caseReference);
        String organisationId = organisationDetailsService.getOrganisationIdentifier(authenticatedUserId.toString());
        return findMatchingLinkedDefendants(defendants, authenticatedUserId, organisationId, caseReference);
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
            .filter(part -> !defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(
                caseReference, part.getId()))
            .toList();

        if (linkedDefendants.isEmpty()) {
            log.error(
                "Access denied: User {} is not linked as a defendant on case {}",
                authenticatedUserId,
                caseReference
            );
            throw new CaseAccessException("User is not linked as a defendant solicitor on this case");

        }
        return linkedDefendants;
    }

    private boolean isUserOrOrganisationMatch(UUID linkedUserId,
                                              String linkedOrganisationId,
                                              UUID authenticatedUserId,
                                              String authenticatedOrganisationId) {
        return authenticatedUserId.equals(linkedUserId)
            || (isNotBlank(authenticatedOrganisationId)
            && authenticatedOrganisationId.equals(linkedOrganisationId));
    }
}
