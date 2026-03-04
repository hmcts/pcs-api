package uk.gov.hmcts.reform.pcs.ccd.page.caseworkerverifydefendants;

import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class ValidateDefendantList implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("validateDefendantList")
            .pageLabel("Validate defendant list")
            .readonly(PCSCase::getDefendantListMarkdown, ShowConditions.NEVER_SHOW)
            .label("validateDefendantList-info", "${defendantListMarkdown}")
            .mandatory(PCSCase::getDefendantListValid);
    }

}
