package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.EnforcementStatusReadAccess;

import java.time.LocalDateTime;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

/**
 * The current enforcement's most recent warrant return, set for the case view so that the "Enforcement
 * status" tab can show it under "Eviction outcome" (HDPI-8424). Read only: the returns are held on
 * enf_warrant_return and no event edits these fields.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
// Unwrapped with a prefix, which Jackson prepends as is: the CCD field ids are prefix + UpperCamelCase.
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class LatestWarrantReturn {

    @CCD(label = "Return", searchable = false, access = {EnforcementStatusReadAccess.class})
    private WarrantReturnType type;

    @CCD(label = "Return name", searchable = false, access = {EnforcementStatusReadAccess.class})
    private String name;

    @CCD(label = "Outcome", searchable = false, access = {EnforcementStatusReadAccess.class})
    private WarrantReturnOutcome outcome;

    @CCD(label = "Eviction date and time", searchable = false, access = {EnforcementStatusReadAccess.class})
    private LocalDateTime evictionDateTime;

    @CCD(label = "Additional details", typeOverride = TextArea, searchable = false,
        access = {EnforcementStatusReadAccess.class})
    private String additionalDetails;
}
