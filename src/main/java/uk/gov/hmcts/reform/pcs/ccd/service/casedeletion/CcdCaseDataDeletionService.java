package uk.gov.hmcts.reform.pcs.ccd.service.casedeletion;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.repository.CcdCaseRepository;

/**
 * Deletes decentralised {@code ccd} schema rows for a case. Prefer leaving this to the SDK
 * retain-and-dispose task after central CCD returns 404; kept for explicit local cleanup paths.
 */
@AllArgsConstructor
@Service
@Slf4j
public class CcdCaseDataDeletionService {

    private final CcdCaseRepository ccdCaseRepository;

    public void deleteCcdCaseData(long caseReference) {
        ccdCaseRepository.deleteCcdCaseData(caseReference);
        log.debug("Deleted case data for case reference: {}", caseReference);
    }
}
