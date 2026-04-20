package uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;

import java.util.UUID;

@Repository
public interface LegalRepresentativeRepository extends JpaRepository<LegalRepresentativeEntity, UUID> {
}
