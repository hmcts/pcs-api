package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

/**
 * CCD's predefined {@code StaffUser} complex type. CCD defines it, so the SDK refers to it by name rather
 * than generating it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ComplexType(name = "StaffUser")
public class StaffUser {

    private String idamId;
}
