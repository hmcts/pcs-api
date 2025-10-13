package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;

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
            ;
    }
}
