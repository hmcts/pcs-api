package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Component
public class HelpWithFees implements CcdPageConfiguration {

    private static final String APPLIED_FOR_HWF_FIELD = "enter_cc_AppliedForHwf";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("helpWithFees")
            .pageLabel("Help with Fees")
            .label("helpWithFees-lineSeparator", "---")
            .complex(PCSCase::getEnterCounterClaim)
                .mandatory(EnterCounterClaimDetails::getAppliedForHwf)
                .mandatory(
                    EnterCounterClaimDetails::getHwfReferenceNumber,
                    fieldEquals(APPLIED_FOR_HWF_FIELD, VerticalYesNo.YES))
            .done();
    }
}
