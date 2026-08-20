package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.defendantpaperresponse;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class SelectDefendant implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("selectDefendant")
            .pageLabel("Select defendant")
            .label("selectDefendant-lineSeparator", "---")
            .mandatory(PCSCase::getDefendantRadioList)
            .done();
    }
}
