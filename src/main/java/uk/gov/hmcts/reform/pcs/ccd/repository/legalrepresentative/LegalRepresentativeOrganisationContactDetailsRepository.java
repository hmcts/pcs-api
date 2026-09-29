package uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyContactDetailsEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalRepresentativeOrganisationContactDetailsRepository
    extends JpaRepository<ClaimPartyContactDetailsEntity, UUID> {

    @Query("""
        SELECT contact
        FROM OrganisationEntity lro
        JOIN lro.claimPartyContactDetails contact
        JOIN contact.pcsCase pcsCase
        WHERE pcsCase.caseReference = :caseReference
        AND lro.organisationId = :organisationId
        """)
    Optional<ClaimPartyContactDetailsEntity> findByOrganisationIdAndCaseReference(
        @Param("organisationId") String organisationId,
        @Param("caseReference") long caseReference
    );


}
