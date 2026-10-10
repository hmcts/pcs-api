package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppType;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldNotEquals;

public class ConsentAndNotice implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("consentAndNotice")
            .pageLabel("Application consent and notice")
            .showCondition(fieldNotEquals("enter_genapp_ApplicationTypeOption", EnterGenAppType.SUSPEND))
            .label("consentAndNotice-lineSeparator", "---")
            .complex(PCSCase::getEnterGenAppRequest)
            .mandatory(EnterGenAppRequest::getAllPartiesAgree)
            .mandatory(EnterGenAppRequest::getWithoutNotice,
                       fieldEquals("enter_genapp_AllPartiesAgree", VerticalYesNo.NO))
            .done();
    }

}
