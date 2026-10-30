package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Send Letter Service "type" codes per pack. Codes are pending final confirmation from the print team.
 * No {@code _pcs_api} suffix: Send Letter already appends the service name to the SFTP filename, so a
 * suffix here would duplicate it (e.g. {@code CPC-01-IN0pcsapi_pcsapi_...}).
 */
@RequiredArgsConstructor
public enum LetterType {

    CLAIMANT_CLAIM_PACK("CPC-01-IN0", false),
    DEFENDANT_CLAIM_PACK("CPD-01-IN1", false),
    DEFENCE_PACK("DEF-01-IN0", true),
    DEFENCE_PACK_LEGAL_REP("DEF-01-IN1", true),
    GEN_APP_PACK("GEN-00-IN0", true);

    @Getter
    private final String code;

    private final boolean includesCoveringLetter;

    public boolean includesCoveringLetter() {
        return this.includesCoveringLetter;
    }
}
