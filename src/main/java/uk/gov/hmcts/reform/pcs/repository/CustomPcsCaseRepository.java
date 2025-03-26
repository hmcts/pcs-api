package uk.gov.hmcts.reform.pcs.repository;

import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.util.Optional;

public interface CustomPcsCaseRepository {

    Optional<PCSCase> findDtoByCaseReference(long ccdReference);

}
