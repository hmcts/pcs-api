package uk.gov.hmcts.reform.pcs.ccd.page.entergenapp;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.EnterGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.EnterGenAppType;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

public class HearingDate implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("hearingDate")
            .pageLabel("Hearing date")
            .showCondition(fieldEquals("enter_genapp_ApplicationTypeOption", EnterGenAppType.ADJOURN))
            .label("hearingDate-lineSeparator", "---")
            .complex(PCSCase::getEnterGenAppRequest)
            .mandatory(EnterGenAppRequest::getWithin14Days)
            .done();
    }

}