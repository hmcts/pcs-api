package uk.gov.hmcts.reform.pcs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.entity.Party;

@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {
}
