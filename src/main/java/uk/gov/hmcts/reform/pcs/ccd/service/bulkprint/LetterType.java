package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Send Letter Service "type" codes per pack. Codes are pending final confirmation from the print team.
 */
@Getter
@RequiredArgsConstructor
public enum LetterType {

    CLAIMANT_CLAIM_PACK("CPC-01-IN1_pcs_api"),
    DEFENDANT_CLAIM_PACK("CPD-01-IN1_pcs_api");

    private final String code;
}
