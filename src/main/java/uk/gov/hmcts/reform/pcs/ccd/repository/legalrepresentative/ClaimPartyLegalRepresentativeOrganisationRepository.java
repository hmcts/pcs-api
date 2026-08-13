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

    Optional<ClaimPartyLegalRepresentativeOrganisationEntity> findByPartyIdAndLegalRepresentativeOrganisationIdAndActive(
        UUID partyId, Integer legalRepresentativeOrganisationId, YesOrNo active);
}
