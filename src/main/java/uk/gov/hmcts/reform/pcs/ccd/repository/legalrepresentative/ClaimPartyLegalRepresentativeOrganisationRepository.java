package uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClaimPartyLegalRepresentativeOrganisationRepository
    extends JpaRepository<ClaimPartyLegalRepresentativeOrganisationEntity, Integer> {

    /**
     * Matched on the organisation's PRD identifier rather than its generated primary key, as callers
     * hold the identifier from the organisation details response before any entity has been resolved.
     */
    Optional<ClaimPartyLegalRepresentativeOrganisationEntity>
        findByPartyIdAndLegalRepresentativeOrganisation_OrganisationIdAndActive(
            UUID partyId, String organisationId, YesOrNo active);
}
