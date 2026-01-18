package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Sanitizes draft data before persistence to prevent null fields from overwriting existing data.
 *
 * <p>The actual sanitisation is performed by draftCaseDataObjectMapper during serialization,
 * which uses NON_NULL to exclude null fields from the persisted JSON.
 * This allows partial updates without losing existing draft data.</p>
 */
@Component
@Slf4j
public class DraftPersistenceSanitizer {

    /**
     * Sanitizes case data by removing null fields before draft persistence.
     * Currently a pass-through - actual sanitisation happens during JSON serialization.
     */
    public PCSCase sanitize(PCSCase caseData) {
        log.debug("Sanitizing draft data - null fields will be removed during serialization");
        return caseData;
    }
}
