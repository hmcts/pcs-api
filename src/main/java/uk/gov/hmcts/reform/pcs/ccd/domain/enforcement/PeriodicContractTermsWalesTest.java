package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

/**
 * Test complex matching branch 2506's PeriodicContractTermsWales structure
 * for testing indentation patterns.
 */
@Data
@Builder
public class PeriodicContractTermsWalesTest {

    @CCD(
        label = "Test: Have you and the contract holder agreed terms? (Branch 2506 pattern test)"
    )
    private VulnerableCategory agreedTermsOfPeriodicContract;

    @CCD(
        label = "Test: Give details of the terms you've agreed (Branch 2506 pattern test)",
        hint = "You can enter up to 250 characters",
        typeOverride = TextArea
    )
    private String detailsOfTerms;
}

