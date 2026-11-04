package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.ShowConditionsEnforcementType;

public class LanguageUsedPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("languageUsed")
            .pageLabel("Language used")
            .showCondition(ShowConditionsEnforcementType.WARRANT_OR_RESTITUTION_FLOW)
            .label("languageUsed-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .mandatory(WarrantDetails::getLanguageUsed)
            .done()
            .label("languageUsed-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}

