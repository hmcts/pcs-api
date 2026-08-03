package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Send Letter Service "type" codes per pack. Codes are pending final confirmation from the print team.
 * No {@code _pcs_api} suffix — Send Letter already appends the service name to the SFTP filename, so a
 * suffix here would duplicate it (e.g. {@code CPC-01-IN1pcsapi_pcsapi_...}).
 */
@Getter
@RequiredArgsConstructor
public enum LetterType {

    CLAIMANT_CLAIM_PACK("CPC-01-IN1"),
    DEFENDANT_CLAIM_PACK("CPD-01-IN1"),
    DEFENCE_PACK("DEF-01-IN1");

    private final String code;
}
