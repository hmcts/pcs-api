package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.RequiredArgsConstructor;

/**
 * Send Letter Service "type" codes per pack. Codes are pending final confirmation from the print team.
 * No {@code _pcs_api} suffix: Send Letter already appends the service name to the SFTP filename, so a
 * suffix here would duplicate it (e.g. {@code CPC-01-IN0pcsapi_pcsapi_...}).
 *
 * <p>Defence and general-application codes carry a recipient number ({@code DEF-01}, {@code GEN-01},
 * and so on). Claim packs keep a fixed code.</p>
 */
@RequiredArgsConstructor
public enum LetterType {

    CLAIMANT_CLAIM_PACK("CPC-01-IN0", false, false),
    DEFENDANT_CLAIM_PACK("CPD-01-IN1", false, false),
    DEFENCE_PACK("DEF-%02d-IN0", true, true),
    DEFENCE_PACK_LEGAL_REP("DEF-%02d-IN1", true, true),
    GEN_APP_PACK("GEN-%02d-IN0", true, true);

    private final String code;

    private final boolean includesCoveringLetter;

    private final boolean sequential;

    public boolean includesCoveringLetter() {
        return this.includesCoveringLetter;
    }

    /**
     * Wire code for this pack. {@code sequence} is the 1-based recipient number for defence and
     * general-application packs. Claim packs ignore it.
     */
    public String codeFor(int sequence) {
        if (!sequential) {
            return code;
        }
        return code.formatted(Math.max(sequence, 1));
    }

    public String getCode() {
        return codeFor(1);
    }
}
