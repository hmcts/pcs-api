package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Proof of concept page for a Welsh translation bug report. Not for merge.
 *
 * <p>ExUI translates the options of a {@code YesOrNo} field using the {@code yes} and {@code no} columns of the
 * field label's dictionary row, so the Welsh answer can depend on the question. A {@link
 * uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo} field is a {@code FixedRadioList}, whose options are
 * translated as the bare phrases "Yes" and "No", so the same context is never applied.
 */
public class WelshYesNoContextPoc implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("welshYesNoContextPoc")
            .pageLabel("Welsh Yes/No context proof of concept")
            .optional(PCSCase::getWelshPlaceholderYesNo1)
            .optional(PCSCase::getWelshPlaceholderYesNo2)
            .optional(PCSCase::getWelshPlaceholderVerticalYesNo1)
            .optional(PCSCase::getWelshPlaceholderVerticalYesNo2);
    }
}
