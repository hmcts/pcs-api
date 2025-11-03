package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

public class CheckYourAnswersPlaceHolder implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("checkYourAnswersPlaceHolder")
                .pageLabel("Check your answers (place holder)")
                .label("checkYourAnswersPlaceHolder-content", "---");
    }

}
