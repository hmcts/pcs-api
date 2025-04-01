package uk.gov.hmcts.reform.pcs.repository;

import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;

import java.util.Optional;

public interface CustomPcsCaseRepository {

    Optional<PcsCase> findDtoByCaseReference(long caseReference);

}
