package uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

@Slf4j
@AllArgsConstructor
public class WhichLanguage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("whichLanguage")
            .pageLabel("Which language did you use to complete this service?")
            .label("whichLanguage-lineSeparator", "---")
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getLanguageUsed)
            .done();
    }


}
