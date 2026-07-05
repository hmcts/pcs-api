package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;

import java.util.UUID;

/**
 * One document carried by a pack dispatch: the {@code document} row id and its type, so the pack row is
 * self-describing (e.g. defence form vs counter-claim within the same DEFENCE_PACK).
 */
public record PackDocumentRef(UUID id, DocumentType type) {
}
