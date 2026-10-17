package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

/**
 * CCD's predefined {@code StaffUser} complex type (CCD-8066). XUI renders it as a type-ahead over staff
 * reference data and stores only the chosen user's IDAM id. The type is defined by CCD, so the SDK refers
 * to it by name and does not generate it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ComplexType(name = "StaffUser")
public class StaffUser {

    private String idamId;
}
