package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.Label;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * This was an attempt as using the type Label (which is read only) within our complex type. However, this field
 * isn't serializable so errors.
 */
public class
    DefendantsDOBComplexLabel {

    private Label firstNameLabel;

    private LocalDate dob;
}

