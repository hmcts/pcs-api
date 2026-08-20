package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.defendantpaperresponse;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.DefendantPaperResponseRequest;

public class DefendantDetails implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantDetails")
            .pageLabel("1. Defendant’s details")
            .label("defendantDetails-lineSeparator", "---")
            .complex(PCSCase::getDefendantPaperResponse)
            .label("defendantDetails-name", "1.1 What is the defendant’s name?")
            .optional(DefendantPaperResponseRequest::getFirstName)
            .optional(DefendantPaperResponseRequest::getLastName)
            .optional(DefendantPaperResponseRequest::getDateOfBirth)
            .optional(DefendantPaperResponseRequest::getAddress)
            .done();
    }

}
