package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * HMC party role reference codes for PCS party types.
 * Values are borrowed from SSCS until PCS-specific codes are allocated.
 */
@Getter
@RequiredArgsConstructor
public enum EntityRoleCode {

    CLAIMANT("APEL"),                   // SSCS: APPELLANT
    DEFENDANT("RESP"),                  // SSCS: RESPONDENT
    UNDERLESSEE_OR_MORTGAGEE("OTPA");   // SSCS: OTHER_PARTY

    @JsonValue
    private final String hmcReference;
}
