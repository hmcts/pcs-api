package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

public class ConsentAndNotice implements CcdPageConfiguration {

    private static final String PLACEHOLDER_BANNER = """
        <div class="govuk-notification-banner" role="region"
        aria-labelledby="govuk-notification-banner-title" data-module="govuk-notification-banner">
          <div class="govuk-notification-banner__content">
            <p class="govuk-notification-banner__heading">
              Placeholder
            </p>
          </div>
        </div>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("consentAndNotice")
            .pageLabel("Application consent and notice")
            .label("consentAndNotice-lineSeparator", "---")
            .complex(PCSCase::getEnterGenAppRequest)
            .mandatory(EnterGenAppRequest::getAllPartiesAgree)
            .mandatory(EnterGenAppRequest::getWithoutNotice,
                       fieldEquals("enter_genapp_AllPartiesAgree", VerticalYesNo.NO))
            .done();
    }

}
