package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.defendantpaperresponse;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.DefendantPaperResponseRequest;

public class FreeLegalAdvice implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("freeLegalAdvice")
            .pageLabel("Free legal advice")
            .label("freeLegalAdvice-lineSeparator", "---")
            .complex(PCSCase::getDefendantPaperResponse)
            .optional(DefendantPaperResponseRequest::getFreeLegalAdvice)
            .label("freeLegalAdvice-lineSeparator-bottom", "---")
            .done();
    }
}
