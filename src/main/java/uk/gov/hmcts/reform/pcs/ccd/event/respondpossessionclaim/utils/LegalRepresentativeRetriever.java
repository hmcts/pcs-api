package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;

import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
public class LegalRepresentativeRetriever {

    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;

    public UUID getLegalRepOrganisationIdForUser(long caseReference, String organisationId) {

        Optional<LegalRepresentativeOrganisationEntity> legalRepresentativeOrganisationEntity =
            findExistingRepresentativeOrganisation(organisationId, caseReference);

        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation = legalRepresentativeOrganisationEntity
            .orElseThrow(() -> new IllegalStateException("Legal Representative Organisation "
                                                             + "not linked to case or party"));

        return legalRepresentativeOrganisation.getId();
    }

    private Optional<LegalRepresentativeOrganisationEntity> findExistingRepresentativeOrganisation(
        String organisationId, long caseReference) {
        return legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(organisationId,
                                                                                              caseReference);
    }

}
