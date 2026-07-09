package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;

import java.util.UUID;

/**
 * One document carried by a pack dispatch: the {@code document} row id and its type, so the pack row is
 * self-describing (e.g. defence form vs counter-claim within the same DEFENCE_PACK). {@code defendantNumber}
 * is the owning defendant's 1-based number (from {@code claim_party.rank}; null for the case-level claim
 * form — the same number that drives {@code Defence - Defendant N.pdf}), and {@code self} is true when the
 * pack recipient owns this document — i.e. their own copy versus a copy served on them (packs go to all
 * parties).
 */
public record PackDocumentRef(UUID id, DocumentType type, Integer defendantNumber, boolean self) {
}
