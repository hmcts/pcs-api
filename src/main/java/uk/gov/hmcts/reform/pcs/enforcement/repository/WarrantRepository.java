package uk.gov.hmcts.reform.pcs.enforcement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;


public interface WarrantRepository extends JpaRepository<WarrantEntity, Integer> {
}
