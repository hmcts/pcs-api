package uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative;

import feign.Param;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClaimPartyLegalRepresentativeOrganisationRepository
    extends JpaRepository<ClaimPartyLegalRepresentativeOrganisationEntity, Integer> {

    @Query("""
        SELECT plro
        FROM ClaimPartyLegalRepresentativeOrganisationEntity plro
        JOIN plro.party p
        JOIN p.pcsCase pcsCase
        WHERE p.id = :partyId
        AND plro.legalRepresentativeOrganisation.id = :legalRepresentativeOrganisationId
        AND pcsCase.caseReference = :caseReference
        AND plro.active = 'YES'
        """)
    List<ClaimPartyLegalRepresentativeOrganisationEntity> findAllActiveByPartyIdLegalRepresentativeOrganisationIdAndCase(
        @Param("partyId") UUID partyId,
        @Param("legalRepresentativeOrganisationId") Integer legalRepresentativeOrganisationId,
        @Param("caseReference") long caseReference
    );
}
