package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class LegalRepForDefendantAccessValidator {

    private final DefendantPartyExtractor defendantPartyExtractor;
    private final DefendantResponseRepository defendantResponseRepository;

    public List<PartyEntity> validateAndGetDefendants(PcsCaseEntity caseEntity, String organisationId) {
        long caseReference = caseEntity.getCaseReference();
        List<PartyEntity> defendants = defendantPartyExtractor.extractDefendants(caseEntity, caseReference);
        return findMatchingLinkedDefendants(defendants, organisationId, caseReference);
    }

    private List<PartyEntity> findMatchingLinkedDefendants(
        List<PartyEntity> defendants,
        String organisationId,
        long caseReference
    ) {
        List<PartyEntity> linkedDefendants =  defendants
            .stream()
            .filter(party -> party.getClaimPartyOrganisationList()
                .stream()
                .anyMatch(claimPartyOrganisation ->
                              claimPartyOrganisation.getActive().equals(YesOrNo.YES)
                                  && isOrganisationMatch(
                                  claimPartyOrganisation.getOrganisation()
                                      .getOrganisationId(),
                                  organisationId
                              )
                ))
            .filter(party -> !defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(
                caseReference, party.getId()))
            .toList();

        if (linkedDefendants.isEmpty()) {
            log.error(
                "Access denied: User {} is not linked as a defendant on case {}",
                organisationId,
                caseReference
            );
            throw new CaseAccessException("User is not linked as a defendant solicitor on this case");

        }
        return linkedDefendants;
    }


    private boolean isOrganisationMatch(
                                              String linkedOrganisationId,
                                              String authenticatedOrganisationId) {
        return authenticatedOrganisationId.equals(linkedOrganisationId);
    }
}
