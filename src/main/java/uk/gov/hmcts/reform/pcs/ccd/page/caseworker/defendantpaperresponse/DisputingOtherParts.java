package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.defendantpaperresponse;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.DefendantPaperResponseRequest;

public class DisputingOtherParts implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("disputingOtherParts")
            .pageLabel("15. Disputing other parts of the claim")
            .label("disputingOtherParts-lineSeparator", "---")
            .complex(PCSCase::getDefendantPaperResponse)
            .optional(DefendantPaperResponseRequest::getHasMadeCounterClaim)
            .label("disputingOtherParts-lineSeparator-bottom", "---")
            .done();
    }

}
