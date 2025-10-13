package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementMultiLabel.WRIT_OR_WARRENT_CLARIFICATION;

public class EnforcementApplicationPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("enforcementApplicationPage")
                .pageLabel("Your application")
                .label(
                        "enforcementApplicationPage-content",
                        """
                        ---
                        """)
                .complex(PCSCase::getEnforcementOrder)
                .mandatory(EnforcementOrder::getSelectEnforcementType)
                .label("enforcementApplicationPage-clarification", WRIT_OR_WARRENT_CLARIFICATION);
    }
}
