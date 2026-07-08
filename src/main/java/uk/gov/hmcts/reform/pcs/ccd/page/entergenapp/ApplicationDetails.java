package uk.gov.hmcts.reform.pcs.ccd.page.entergenapp;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.EnterGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.EnterGenAppType;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

public class ApplicationDetails implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("applicationDetails")
            .pageLabel("Application details")
            .label("applicationDetails-lineSeparator", "---")
            .complex(PCSCase::getEnterGenAppRequest)
            .mandatory(EnterGenAppRequest::getApplicantParty)
            .mandatory(EnterGenAppRequest::getDateReceived)
            .mandatory(EnterGenAppRequest::getApplicationTypeOption)
            .mandatory(
                EnterGenAppRequest::getSomethingElseDetails,
                fieldEquals("enter_genapp_ApplicationTypeOption", EnterGenAppType.SOMETHING_ELSE)
            )
            .done();
    }

}